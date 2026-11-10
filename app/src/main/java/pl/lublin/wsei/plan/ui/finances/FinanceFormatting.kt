package pl.lublin.wsei.plan.ui.finances

import pl.lublin.wsei.plan.domain.model.FinancialObligation
import java.util.Locale
import kotlin.math.abs

/**
 * Konwertuje datę `dd.MM.yyyy` lub `yyyy-MM-dd` na klucz `YYYYMMDD`, umożliwiając sortowanie
 * chronologiczne. Nieczytelna data trafia na koniec listy.
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

/** Czy nieopłacona należność ma termin w bieżącym miesiącu. */
internal fun FinancialObligation.isDueThisMonth(today: java.time.LocalDate = java.time.LocalDate.now()): Boolean {
    if (isPaid) return false
    val key = sortableDueDateKey(dueDateIso)
    return key / 100L == today.year * 100L + today.monthValue
}

/** Kwota do zapłaty (bieżący stan albo kwota naliczona). */
internal val FinancialObligation.outstandingAmountPln: Double
    get() = abs(currentStatePln).takeIf { it > 0.0 } ?: amountPln

internal fun formatPln(amount: Double): String = String.format(Locale("pl", "PL"), "%,.2f zł", amount)

/** Należności posortowane: najbliższe do zapłaty → najpóźniejsze → zapłacone (od najnowszych). */
internal fun List<FinancialObligation>.sortedForDisplay(): List<FinancialObligation> {
    val unpaid = filter { !it.isPaid }.sortedBy { sortableDueDateKey(it.dueDateIso) }
    val paid = filter { it.isPaid }.sortedByDescending { sortableDueDateKey(it.dueDateIso) }
    return unpaid + paid
}
