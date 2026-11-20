package pl.lublin.wsei.plan.data.local

import androidx.annotation.StringRes
import kotlinx.serialization.SerializationException
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.remote.api.AprHttpException
import pl.lublin.wsei.plan.data.remote.api.AprMalformedResponseException
import pl.lublin.wsei.plan.data.remote.api.AprServerException
import pl.lublin.wsei.plan.data.remote.api.AprSessionExpiredException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Turns a failure into a message that can be shown to the student, in the app language.
 *
 * Network and API failures get a localized explanation by type (their own messages are technical
 * diagnostics); messages written by the dean's office server are shown as they are; other errors
 * already carry a localized message (thrown via [LocalizedStrings]). [fallback] is used when
 * nothing better is known.
 */
fun LocalizedStrings.describeError(error: Throwable?, @StringRes fallback: Int): String = when (error) {
    null -> get(fallback)
    is AprSessionExpiredException -> get(R.string.error_session_expired)
    is AprServerException -> error.message?.takeIf { it.isNotBlank() } ?: get(R.string.error_wd_server)
    is AprHttpException -> get(R.string.error_wd_http, error.statusCode)
    is AprMalformedResponseException, is SerializationException -> get(R.string.error_wd_malformed)
    is UnknownHostException -> get(R.string.error_no_internet)
    is SocketTimeoutException -> get(R.string.error_timeout)
    is IOException -> get(R.string.error_connection)
    else -> error.message?.takeIf { it.isNotBlank() } ?: get(fallback)
}
