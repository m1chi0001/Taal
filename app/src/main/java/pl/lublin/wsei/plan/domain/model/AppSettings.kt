package pl.lublin.wsei.plan.domain.model

import androidx.compose.runtime.Immutable

/** Light / dark appearance. Colour source is chosen separately via [AppColorPalette]. */
enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Colour source of the app theme, applied in every [AppThemeMode].
 * [DYNAMIC] uses the wallpaper colours (Material You, Android 12+); on older devices it falls
 * back to [TAAL]. The remaining entries are curated Material 3 Expressive palettes.
 */
enum class AppColorPalette {
    TAAL,
    DYNAMIC,
    COBALT,
    TERRACOTTA,
    PLUM,
    OCEAN
}

/**
 * User preferences persisted across app restarts.
 *
 * [hideSensitiveData] ("Ukryj dane") masks grades, financial figures, personal identifiers
 * and lecturer names/details everywhere: screens, Live Notification and calendar sync.
 */
@Immutable
data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val colorPalette: AppColorPalette = AppColorPalette.TAAL,
    val isLiveNotificationEnabled: Boolean = true,
    val hideSensitiveData: Boolean = false,
    /** Device calendar chosen for schedule sync (`CalendarContract.Calendars._ID`), null if never chosen. */
    val syncCalendarId: Long? = null
)
