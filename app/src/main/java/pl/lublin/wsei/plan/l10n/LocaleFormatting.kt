package pl.lublin.wsei.plan.l10n

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Locale of the app UI. Follows the per-app language chosen in Taal's settings (AppCompat applies it
 * to the Activity configuration), falling back to the system language.
 */
@Composable
@ReadOnlyComposable
fun appLocale(): Locale {
    val locales = LocalConfiguration.current.locales
    return if (locales.isEmpty) Locale.ROOT else locales[0]
}

/**
 * Date formatter for a CLDR [skeleton] (e.g. `"EEEEdMMMM"`) in [locale]; the platform picks the
 * field order and separators that are natural for the language ("piątek, 20 listopada",
 * "Friday, November 20", "пʼятниця, 20 листопада").
 */
fun localizedDateFormatter(locale: Locale, skeleton: String): DateTimeFormatter =
    runCatching { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale) }
        // An ICU pattern letter java.time does not know: fall back to the locale's long date.
        .getOrElse { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale) }

/** [localizedDateFormatter] for the current [appLocale], remembered across recompositions. */
@Composable
fun rememberLocalizedDateFormatter(skeleton: String): DateTimeFormatter {
    val locale = appLocale()
    return remember(locale, skeleton) { localizedDateFormatter(locale, skeleton) }
}

/** Upper-cases the first letter using [locale] rules (day names are lower-case in Polish and Ukrainian). */
fun String.capitalizeFirst(locale: Locale): String = replaceFirstChar { it.titlecase(locale) }
