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
    /** Context whose resources use the app language; recreated when the language changes. */
    fun context(): Context {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return appContext
        val config = Configuration(appContext.resources.configuration)
        config.setLocales(android.os.LocaleList.forLanguageTags(locales.toLanguageTags()))
        return appContext.createConfigurationContext(config)
    }

    fun get(@StringRes id: Int, vararg args: Any): String = context().getString(id, *args)

    fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        context().resources.getQuantityString(id, count, *args)
}
