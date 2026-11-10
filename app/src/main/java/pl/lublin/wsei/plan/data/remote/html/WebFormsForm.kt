package pl.lublin.wsei.plan.data.remote.html

import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * A parsed HTML form ready to be re-submitted, including all ASP.NET state fields
 * (`__VIEWSTATE`, `__VIEWSTATEGENERATOR`, `__EVENTVALIDATION`, `__EVENTTARGET`, `__EVENTARGUMENT`,
 * `__RequestVerificationToken` for MVC). Field order is preserved — WebForms validates
 * `__EVENTVALIDATION` against the posted control set, so nothing may be dropped.
 */
data class WebFormsForm(
    val actionUrl: HttpUrl,
    val method: String,
    val fields: LinkedHashMap<String, String>,
    val usernameField: String?,
    val passwordField: String?,
    /** name/value of the button that triggers the login (posted only for that one button). */
    val submitButton: Pair<String, String>?,
    /** `__doPostBack('target','arg')` when the "Zaloguj" control is a LinkButton. */
    val postBackTarget: Pair<String, String>?
) {
    val hasViewState: Boolean get() = "__VIEWSTATE" in fields

    fun toLoginBody(login: String, password: String): FormBody {
        val values = LinkedHashMap(fields)
        usernameField?.let { values[it] = login }
        passwordField?.let { values[it] = password }
        postBackTarget?.let { (target, arg) ->
            values["__EVENTTARGET"] = target
            values["__EVENTARGUMENT"] = arg
        }
        submitButton?.let { (name, value) -> values[name] = value }
        return FormBody.Builder().apply { values.forEach { (k, v) -> add(k, v) } }.build()
    }

    /** Body for a WebForms postback (e.g. "next week" LinkButton) on an already loaded page. */
    fun toPostBackBody(eventTarget: String, eventArgument: String = "", overrides: Map<String, String> = emptyMap()): FormBody {
        val values = LinkedHashMap(fields)
        values["__EVENTTARGET"] = eventTarget
        values["__EVENTARGUMENT"] = eventArgument
        values.putAll(overrides)
        return FormBody.Builder().apply { values.forEach { (k, v) -> add(k, v) } }.build()
    }
}

object WebFormsFormParser {

    private val USERNAME_HINT = Regex("(?i)(login|user|uzytk|użytk|album|email|mail|nazwa|identyf)")
    private val SUBMIT_TEXT_HINT = Regex("(?i)(zaloguj|loguj|login|sign\\s*in|wejd)")
    private val DO_POSTBACK = Regex("""__doPostBack\(\s*['"]([^'"]*)['"]\s*,\s*['"]([^'"]*)['"]\s*\)""")

    /** Picks the form containing a password input (login), otherwise the main ASP.NET form. */
    fun parseLoginForm(document: Document, pageUrl: HttpUrl): WebFormsForm? {
        val form = document.selectFirst("form:has(input[type=password])")
            ?: return null
        return parse(form, pageUrl)
    }

    /** Main page form (WebForms pages wrap everything in one `<form id="aspnetForm">`). */
    fun parseMainForm(document: Document, pageUrl: HttpUrl): WebFormsForm? {
        val form = document.selectFirst("form#aspnetForm, form#form1, form[action]:has(input[name=__VIEWSTATE])")
            ?: document.selectFirst("form")
            ?: return null
        return parse(form, pageUrl)
    }

    fun parse(form: Element, pageUrl: HttpUrl): WebFormsForm {
        val action = form.attr("action").trim()
        val actionUrl = if (action.isEmpty()) pageUrl else pageUrl.resolve(action) ?: action.toHttpUrlOrNull() ?: pageUrl
        val fields = LinkedHashMap<String, String>()

        for (input in form.select("input[name], select[name], textarea[name]")) {
            val name = input.attr("name")
            when (input.tagName().lowercase()) {
                "select" -> {
                    val selected = input.selectFirst("option[selected]") ?: input.selectFirst("option")
                    fields[name] = selected?.let { it.attr("value").ifEmpty { it.text() } }.orEmpty()
                }
                "textarea" -> fields[name] = input.text()
                else -> when (input.attr("type").lowercase()) {
                    "submit", "button", "image", "reset", "file" -> Unit // only the clicked button is posted
                    "checkbox", "radio" -> if (input.hasAttr("checked")) fields[name] = input.attr("value").ifEmpty { "on" }
                    "password" -> fields[name] = ""
                    else -> fields[name] = input.attr("value")
                }
            }
        }
        // WebForms expects these even when empty.
        if ("__VIEWSTATE" in fields) {
            fields.putIfAbsent("__EVENTTARGET", "")
            fields.putIfAbsent("__EVENTARGUMENT", "")
        }

        val textInputs = form.select("input[name]").filter {
            it.attr("type").lowercase() in setOf("", "text", "email", "tel", "number")
        }
        val username = textInputs.firstOrNull {
            USERNAME_HINT.containsMatchIn(it.attr("name") + " " + it.id() + " " + it.attr("autocomplete"))
        } ?: textInputs.firstOrNull { !it.attr("name").startsWith("__") }
        val password = form.selectFirst("input[type=password][name]")

        val submit = form.select("input[type=submit][name], button[type=submit][name], input[type=image][name], button[name]:not([type])")
            .let { buttons -> buttons.firstOrNull { SUBMIT_TEXT_HINT.containsMatchIn(it.attr("value") + it.text() + it.id()) } ?: buttons.firstOrNull() }
            ?.let { it.attr("name") to it.attr("value").ifEmpty { it.text() } }

        val postBack = if (submit == null) {
            form.select("a[href*=__doPostBack], [onclick*=__doPostBack]")
                .firstOrNull { SUBMIT_TEXT_HINT.containsMatchIn(it.text() + it.id()) }
                ?.let { DO_POSTBACK.find(it.attr("href") + " " + it.attr("onclick")) }
                ?.let { it.groupValues[1] to it.groupValues[2] }
        } else null

        return WebFormsForm(
            actionUrl = actionUrl,
            method = form.attr("method").ifBlank { "post" }.lowercase(),
            fields = fields,
            usernameField = username?.attr("name"),
            passwordField = password?.attr("name"),
            submitButton = submit,
            postBackTarget = postBack
        )
    }

    /** Validation / error text shown on a failed login, if any. */
    fun extractLoginError(document: Document): String? =
        document.select(
            ".validation-summary-errors, .field-validation-error, .text-danger, .alert-danger, " +
                ".error, .errors, span[style*=red], [id*=lblError], [id*=Blad], [id*=blad]"
        ).map { it.text().trim() }.firstOrNull { it.isNotBlank() }
}
