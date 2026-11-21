package pl.lublin.wsei.plan.data.local

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves string resources in the language chosen in Taal's settings for code that runs
 * outside an Activity (ViewModel messages, notifications, calendar export, workers).
 *
 * The application context does not follow per-app locales on Android 12 and older, so the
 * selected locale from [AppCompatDelegate.getApplicationLocales] is applied explicitly.
 */
@Singleton
class LocalizedStrings @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    /** Last localized context, reused while the app language and base configuration stay the same. */
    @Volatile
    private var cached: CachedContext? = null

    private class CachedContext(val tags: String, val base: Configuration, val context: Context)

    /** Context whose resources use the app language; recreated when the language changes. */
    fun context(): Context {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return appContext
        val tags = locales.toLanguageTags()
        val base = appContext.resources.configuration
        cached?.let { if (it.tags == tags && it.base == base) return it.context }
        val config = Configuration(base)
        config.setLocales(android.os.LocaleList.forLanguageTags(tags))
        val localized = appContext.createConfigurationContext(config)
        cached = CachedContext(tags, Configuration(base), localized)
        return localized
    }

    fun get(@StringRes id: Int, vararg args: Any): String =
        if (args.isEmpty()) context().getString(id) else context().getString(id, *args)

    fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        if (args.isEmpty()) {
            context().resources.getQuantityString(id, count)
        } else {
            context().resources.getQuantityString(id, count, *args)
        }
}
