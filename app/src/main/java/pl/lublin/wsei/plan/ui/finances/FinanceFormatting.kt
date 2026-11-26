package pl.lublin.wsei.plan.ui.finances

import pl.lublin.wsei.plan.domain.model.FinancialObligation
import java.util.Locale
import kotlin.math.abs

/**
 * Converts a `dd.MM.yyyy` or `yyyy-MM-dd` date into a sortable `YYYYMMDD` key. Unreadable dates
 * sort to the end of the list.
 */
internal fun sortableDueDateKey(rawDate: String): Long {
    val clean = rawDate.trim()
    val dotParts = clean.split(".")
    if (dotParts.size == 3) {
        val day = dotParts[0].toLongOrNull() ?: 1L
        val month = dotParts[1].toLongOrNull() ?: 1L
        val year = dotParts[2].take(4).toLongOrNull() ?: 2099L
        return year * 10_000L + month * 100L + day
    }
    val dashParts = clean.split("-")
    if (dashParts.size == 3) {
        val year = dashParts[0].toLongOrNull() ?: 2099L
        val month = dashParts[1].toLongOrNull() ?: 1L
        val day = dashParts[2].take(2).toLongOrNull() ?: 1L
        return year * 10_000L + month * 100L + day
    }
    return 9999_12_31L
}

/** Whether an unpaid obligation is due in the current month. */
internal fun FinancialObligation.isDueThisMonth(today: java.time.LocalDate = java.time.LocalDate.now()): Boolean {
    if (isPaid) return false
    val key = sortableDueDateKey(dueDateIso)
    return key / 100L == today.year * 100L + today.monthValue
}

/** Amount still to pay (the current state, else the charged amount). */
internal val FinancialObligation.outstandingAmountPln: Double
    get() = abs(currentStatePln).takeIf { it > 0.0 } ?: amountPln

private val PolishLocale: Locale = Locale.forLanguageTag("pl-PL")

/** The single money format of the finance UI: "1 234,56 zł". */
internal fun formatPln(amount: Double): String = String.format(PolishLocale, "%,.2f zł", amount)

/** A parsed dean's office amount: the value and its currency code (null = PLN). */
internal class ParsedMoney(val value: Double, val currency: String?)

private val MoneyNumberRegex = Regex("""[-+]?\d[\d.,]*""")
private val CurrencyCodeRegex = Regex("""\b[A-Z]{3}\b""")

/**
 * Parses amount text as returned by the dean's office ("600.00 PLN", "1 234,56 zł", "600,00").
 * Returns null when the text holds no number.
 */
internal fun parseMoneyText(raw: String?): ParsedMoney? {
    if (raw.isNullOrBlank()) return null
    val compact = raw.trim().replace(Regex("""[\s\u00A0\u202F]"""), "")
    val number = MoneyNumberRegex.find(compact)?.value?.let(::normalizeDecimal)?.toDoubleOrNull() ?: return null
    if (raw.contains("zł", ignoreCase = true)) return ParsedMoney(number, null)
    val code = CurrencyCodeRegex.find(raw.uppercase(Locale.ROOT))?.value
    val symbol = raw.firstOrNull { Character.getType(it) == Character.CURRENCY_SYMBOL.toInt() }
    val currency = code?.takeUnless { it == "PLN" } ?: symbol?.toString()
    return ParsedMoney(number, currency)
}

/**
 * "1.234,56" / "1,234.56" / "600,00" → "1234.56" / "600.00": the last separator is the decimal
 * point when both kinds occur; a separator repeated several times is a thousands separator.
 */
private fun normalizeDecimal(text: String): String {
    val trimmed = text.trimEnd('.', ',')
    val lastDot = trimmed.lastIndexOf('.')
    val lastComma = trimmed.lastIndexOf(',')
    val decimalIndex = when {
        lastDot >= 0 && lastComma >= 0 -> maxOf(lastDot, lastComma)
        lastDot >= 0 -> lastDot.takeIf { trimmed.count { it == '.' } == 1 } ?: -1
        lastComma >= 0 -> lastComma.takeIf { trimmed.count { it == ',' } == 1 } ?: -1
        else -> -1
    }
    return buildString {
        trimmed.forEachIndexed { index, char ->
            when {
                index == decimalIndex -> append('.')
                char == '.' || char == ',' -> Unit
                else -> append(char)
            }
        }
    }
}

/**
 * Re-formats dean's office amount text in the same style as [formatPln], so the whole screen reads
 * "600,00 zł" instead of a mix with "600.00 PLN". Text that cannot be parsed is returned as is;
 * other currencies keep their code ("12,50 EUR").
 */
internal fun formatMoneyText(raw: String): String {
    val parsed = parseMoneyText(raw) ?: return raw.trim()
    return if (parsed.currency == null) {
        formatPln(parsed.value)
    } else {
        String.format(PolishLocale, "%,.2f %s", parsed.value, parsed.currency)
    }
}

/** True when the amount text is a number equal to zero ("0.00 PLN", "0,00 zł"). */
internal fun isZeroMoneyText(raw: String): Boolean = parseMoneyText(raw)?.let { abs(it.value) < 0.005 } == true

/** Obligations ordered for display: nearest due → latest due → paid (newest first). */
internal fun List<FinancialObligation>.sortedForDisplay(): List<FinancialObligation> {
    val unpaid = filter { !it.isPaid }.sortedBy { sortableDueDateKey(it.dueDateIso) }
    val paid = filter { it.isPaid }.sortedByDescending { sortableDueDateKey(it.dueDateIso) }
    return unpaid + paid
}
