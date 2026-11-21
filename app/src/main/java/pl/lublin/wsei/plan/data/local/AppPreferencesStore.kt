package pl.lublin.wsei.plan.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pl.lublin.wsei.plan.domain.model.AppColorPalette
import pl.lublin.wsei.plan.domain.model.AppSettings
import pl.lublin.wsei.plan.domain.model.AppThemeMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists [AppSettings] in SharedPreferences and exposes them as a [StateFlow], so the UI,
 * the Live Notification service and the calendar sync all read the same values.
 *
 * App language is not stored here — it is owned by `AppCompatDelegate.setApplicationLocales`.
 */
@Singleton
class AppPreferencesStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /** Current snapshot, for non-coroutine callers (services, workers). */
    val current: AppSettings get() = _settings.value

    fun setThemeMode(mode: AppThemeMode) = edit { it.copy(themeMode = mode) }

    fun setColorPalette(palette: AppColorPalette) = edit { it.copy(colorPalette = palette) }

    fun setLiveNotificationEnabled(enabled: Boolean) = edit { it.copy(isLiveNotificationEnabled = enabled) }

    fun setHideSensitiveData(hidden: Boolean) = edit { it.copy(hideSensitiveData = hidden) }

    fun setSyncCalendarId(calendarId: Long?) = edit { it.copy(syncCalendarId = calendarId) }

    private fun edit(transform: (AppSettings) -> AppSettings) {
        _settings.update(transform)
        write(_settings.value)
    }

    private fun read(): AppSettings {
        val storedTheme = prefs.getString(KEY_THEME_MODE, null)
        // 0.7.0 and older stored "DYNAMIC" as a theme mode — migrate to System + Dynamic palette.
        val legacyDynamic = storedTheme == LEGACY_DYNAMIC
        val themeMode = AppThemeMode.entries.firstOrNull { it.name == storedTheme } ?: AppThemeMode.SYSTEM
        val palette = AppColorPalette.entries.firstOrNull { it.name == prefs.getString(KEY_COLOR_PALETTE, null) }
            ?: if (legacyDynamic) AppColorPalette.DYNAMIC else AppColorPalette.TAAL
        return AppSettings(
            themeMode = themeMode,
            colorPalette = palette,
            isLiveNotificationEnabled = prefs.getBoolean(KEY_LIVE_NOTIFICATION, true),
            hideSensitiveData = prefs.getBoolean(KEY_HIDE_SENSITIVE, false),
            syncCalendarId = prefs.getLong(KEY_SYNC_CALENDAR, -1L).takeIf { it >= 0 }
        )
    }

    private fun write(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putString(KEY_COLOR_PALETTE, settings.colorPalette.name)
            .putBoolean(KEY_LIVE_NOTIFICATION, settings.isLiveNotificationEnabled)
            .putBoolean(KEY_HIDE_SENSITIVE, settings.hideSensitiveData)
            .apply {
                if (settings.syncCalendarId != null) putLong(KEY_SYNC_CALENDAR, settings.syncCalendarId)
                else remove(KEY_SYNC_CALENDAR)
            }
            .apply()
    }

    private companion object {
        const val PREFS_FILE = "taal_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_COLOR_PALETTE = "color_palette"
        const val KEY_LIVE_NOTIFICATION = "live_notification_enabled"
        const val KEY_HIDE_SENSITIVE = "hide_sensitive_data"
        const val KEY_SYNC_CALENDAR = "sync_calendar_id"
        const val LEGACY_DYNAMIC = "DYNAMIC"
    }
}
