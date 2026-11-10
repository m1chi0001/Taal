package pl.lublin.wsei.plan.data.remote.api

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Typed failures of the APR System (ABP) REST API, so the repository can decide
 * between "re-login", "fall back to HTML" and "show cached data".
 */
sealed class AprApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** HTTP 401, ABP `unAuthorizedRequest`, or a 302 to the login page. Automatic re-auth already failed. */
class AprSessionExpiredException(
    message: String = "Sesja w Wirtualnym Dziekanacie wygasła. Zaloguj się ponownie."
) : AprApiException(message)

/** ABP returned `success: false` with an `error` object (validation, business rule, 500). */
class AprServerException(
    val errorCode: Int?,
    message: String,
    val details: String?
) : AprApiException(message)

/** Non-2xx status without an ABP envelope (404, 502, 503, unexpected redirect…). */
class AprHttpException(val statusCode: Int, message: String) : AprApiException(message)

/** Body is not JSON (e.g. an HTML error/login page) or doesn't match the DTO. */
class AprMalformedResponseException(message: String, cause: Throwable? = null) :
    AprApiException(message, cause)

/**
 * Unwraps ABP `AjaxResponse<T>` envelopes:
 * `{ "result": …, "targetUrl": null, "success": true, "error": null, "unAuthorizedRequest": false, "__abp": true }`.
 *
 * Endpoints that return a bare object (no envelope) are passed through unchanged, so a DTO can be
 * decoded regardless of whether the server wraps it.
 */
object AbpEnvelope {

    private val LOGIN_REDIRECT_HINT = Regex("(?i)(login|logowanie|account|konto|signin)")

    fun isLoginRedirect(location: String?): Boolean =
        location != null && LOGIN_REDIRECT_HINT.containsMatchIn(location)

    fun unwrap(json: Json, rawBody: String): JsonElement {
        val text = rawBody.trim().removePrefix("﻿")
        if (text.isEmpty()) return JsonNull
        if (text.startsWith("<")) {
            throw AprMalformedResponseException(
                "Serwer zwrócił stronę HTML zamiast JSON (możliwa zmiana API lub przekierowanie do logowania)."
            )
        }
        val element = try {
            json.parseToJsonElement(text)
        } catch (e: SerializationException) {
            throw AprMalformedResponseException("Nieprawidłowa odpowiedź JSON z Wirtualnego Dziekanatu.", e)
        }
        val obj = element as? JsonObject ?: return element
        val looksLikeEnvelope = "__abp" in obj ||
            ("success" in obj && ("result" in obj || "error" in obj || "unAuthorizedRequest" in obj))
        if (!looksLikeEnvelope) return element

        val unauthorized = (obj["unAuthorizedRequest"] as? JsonPrimitive)?.booleanOrNull == true
        if (unauthorized) throw AprSessionExpiredException()

        val success = (obj["success"] as? JsonPrimitive)?.booleanOrNull ?: true
        if (!success) {
            val error = obj["error"]?.takeIf { it is JsonObject }?.jsonObject
            val message = (error?.get("message") as? JsonPrimitive)?.contentOrNull
                ?: "Wirtualny Dziekanat zgłosił błąd."
            val details = (error?.get("details") as? JsonPrimitive)?.contentOrNull
            val code = (error?.get("code") as? JsonPrimitive)?.intOrNull
            throw AprServerException(code, message, details)
        }
        return obj["result"] ?: JsonNull
    }

    fun <T> decode(json: Json, rawBody: String, deserializer: DeserializationStrategy<T>): T? {
        val result = unwrap(json, rawBody)
        if (result is JsonNull) return null
        return try {
            json.decodeFromJsonElement(deserializer, result)
        } catch (e: SerializationException) {
            throw AprMalformedResponseException(
                "Nie udało się odczytać danych (${deserializer.descriptor.serialName}): ${e.message}",
                e
            )
        } catch (e: IllegalArgumentException) {
            throw AprMalformedResponseException(
                "Nie udało się odczytać danych (${deserializer.descriptor.serialName}): ${e.message}",
                e
            )
        }
    }
}

/**
 * Validates the HTTP status and decodes the ABP envelope. Never calls Ktor's `body()`, so an
 * empty 401 or an HTML page can't surface as an obscure `NoTransformationFoundException`.
 */
internal suspend fun <T> HttpResponse.decodeAbp(
    json: Json,
    deserializer: DeserializationStrategy<T>
): T? {
    val code = status.value
    if (status == HttpStatusCode.Unauthorized) throw AprSessionExpiredException()
    if (code in 300..399) {
        val location = headers[HttpHeaders.Location]
        if (AbpEnvelope.isLoginRedirect(location)) throw AprSessionExpiredException()
        throw AprHttpException(code, "Nieoczekiwane przekierowanie HTTP $code${location?.let { " → $it" }.orEmpty()}.")
    }
    val text = bodyAsText()
    if (!status.isSuccess()) {
        // ABP error envelopes come with 4xx/5xx; unwrap() turns them into AprServerException.
        runCatching { AbpEnvelope.unwrap(json, text) }
            .exceptionOrNull()
            ?.let { if (it is AprApiException && it !is AprMalformedResponseException) throw it }
        throw AprHttpException(code, "Wirtualny Dziekanat odpowiedział kodem HTTP $code.")
    }
    return AbpEnvelope.decode(json, text, deserializer)
}
