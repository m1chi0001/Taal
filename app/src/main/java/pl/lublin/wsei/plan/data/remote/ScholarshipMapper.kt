package pl.lublin.wsei.plan.data.remote

import pl.lublin.wsei.plan.data.remote.api.MsPaymentDto
import pl.lublin.wsei.plan.data.remote.api.MsSchoolarshipInSemesterDto
import pl.lublin.wsei.plan.data.remote.api.MsScholarshipDetailsDto
import pl.lublin.wsei.plan.data.remote.api.MsScholarshipRulingDto
import pl.lublin.wsei.plan.domain.model.ScholarshipApplication
import pl.lublin.wsei.plan.domain.model.ScholarshipPayoutItem
import java.util.Locale

/**
 * Pure mapping of Mobilny Student scholarship DTOs (`SchoolarshipData`) to [ScholarshipApplication].
 *
 * Rules:
 * - nothing is invented: a missing status stays `null`, a missing ruling means no decision,
 *   and zero/blank amounts are dropped;
 * - the application number `0` is treated as "no number";
 * - C# default dates (`0001-01-01`) count as missing.
 */
object ScholarshipMapper {

    /** Earliest year accepted as a real date; older values are .NET `DateTime.MinValue` placeholders. */
    private const val MIN_REAL_YEAR = 1900

    /**
     * Maps one list [item] plus its optional [details] (the details request may fail) to the domain model.
     */
    fun map(
        item: MsSchoolarshipInSemesterDto,
        details: MsScholarshipDetailsDto?,
        semesterLabel: String
    ): ScholarshipApplication {
        val number = details?.number?.takeIf { it > 0 } ?: item.number.takeIf { it > 0 }
        val typeName = details?.typeName?.trim().orEmpty()
        val rawTitle = details?.name?.trim()?.ifBlank { null }
            ?: item.name.trim().ifBlank { null }
            ?: typeName
        val ruling = details?.let { realRuling(it) }
        val accruals = details?.accruals?.payments.orEmpty().mapNotNull { positivePayout(it) }
        val payouts = details?.payments?.payments.orEmpty().mapNotNull { positivePayout(it) }

        return ScholarshipApplication(
            id = item.id,
            semesterLabel = semesterLabel,
            number = number,
            title = stripNumberPrefix(rawTitle, number),
            typeName = typeName,
            typeVariantName = details?.typeVariantName?.trim().orEmpty(),
            submissionDateIso = realDate(details?.applicationDate) ?: realDate(item.date).orEmpty(),
            statusString = details?.applicationStatusString?.trim()?.ifBlank { null }
                ?: item.state.trim().ifBlank { null },
            isGranted = item.schoolarshipGranted,
            decisionNumber = ruling?.number?.let { realText(it) },
            decisionDateIso = ruling?.let { realDate(it.date) },
            decisionStatus = ruling?.statusString?.let { realText(it) },
            acceptedRules = ruling?.acceptedRules ?: true,
            amountFormatted = (accruals.firstOrNull() ?: payouts.firstOrNull())?.amountFormatted,
            payouts = payouts
        )
    }

    /** First ruling that carries any real data (number, date or status); `null` when there is none. */
    internal fun realRuling(details: MsScholarshipDetailsDto): MsScholarshipRulingDto? =
        (details.rulings + listOfNotNull(details.ruling)).firstOrNull { r ->
            realText(r.number) != null || realDate(r.date) != null || realText(r.statusString) != null
        }

    /**
     * Parses a backend amount such as `"1 450.00 PLN"`, `"1450,5"` or `"1.450,00 zł"`; `null` when
     * no number can be read.
     */
    internal fun parseAmount(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val digits = raw.filter { it.isDigit() || it == ',' || it == '.' || it == '-' }
        if (digits.none { it.isDigit() }) return null
        val lastDot = digits.lastIndexOf('.')
        val lastComma = digits.lastIndexOf(',')
        val normalized = when {
            lastDot >= 0 && lastComma >= 0 && lastComma > lastDot -> digits.replace(".", "").replace(',', '.')
            lastDot >= 0 && lastComma >= 0 -> digits.replace(",", "")
            else -> digits.replace(',', '.')
        }
        return normalized.toDoubleOrNull()
    }

    /** Keeps the backend text when it already names a currency, otherwise formats as `"%.2f PLN"`. */
    internal fun formatAmount(raw: String, value: Double): String =
        if (raw.any { it.isLetter() || it == '$' || it == '€' }) raw.trim()
        else String.format(Locale.US, "%.2f PLN", value)

    private fun positivePayout(p: MsPaymentDto): ScholarshipPayoutItem? {
        val value = parseAmount(p.amount)?.takeIf { it > 0.0 } ?: return null
        return ScholarshipPayoutItem(
            id = p.id,
            amountFormatted = formatAmount(p.amount, value),
            dateIso = realDate(p.date).orEmpty(),
            status = p.status.trim()
        )
    }

    /** Normalized dd.MM.yyyy date, or `null` for blank/unparseable/placeholder dates. */
    private fun realDate(raw: String?): String? {
        val iso = ScheduleNormalizer.normalizeIsoDate(raw) ?: return null
        val year = iso.substringBefore('-').toIntOrNull() ?: return null
        if (year < MIN_REAL_YEAR) return null
        return ScheduleNormalizer.isoToPolishDate(iso)
    }

    /** Trimmed text, or `null` for blank and `"0"` placeholders. */
    private fun realText(raw: String?): String? =
        raw?.trim()?.takeUnless { it.isEmpty() || it == "0" }

    /** Removes a leading `"70 / "` prefix that duplicates the application number. */
    private fun stripNumberPrefix(title: String, number: Int?): String {
        if (number == null) return title
        val prefix = Regex("^\\s*$number\\s*/\\s*")
        return title.replace(prefix, "").ifBlank { title }
    }
}
