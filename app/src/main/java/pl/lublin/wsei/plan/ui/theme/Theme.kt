package pl.lublin.wsei.plan.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.domain.model.AppColorPalette
import pl.lublin.wsei.plan.ui.schedule.AppThemeMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Taal design system built on Material 3 Expressive.
 *
 * - Type: Google Sans Flex (wght, wdth, opsz, ROND axes) — emphatic headlines, calm body text.
 * - Shape: the M3 Expressive corner scale plus wavy silhouettes (cookie, clover, sunny).
 * - Colour: Taal, Cobalt, Terracotta, Plum and Ocean palettes ([wseiStaticColorScheme]) and
 *   Dynamic Color (Material You), all passed through the WCAG guard [ensureContrast].
 * - Motion: snappier expressive springs ([WseiMotionScheme]) and [WseiMotion] for custom animations.
 */

@OptIn(ExperimentalTextApi::class)
private fun flexFont(weight: Int, width: Float, round: Float, opticalSize: Float): Font = Font(
    resId = R.font.google_sans_flex,
    weight = FontWeight(weight),
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.width(width),
        FontVariation.Setting("ROND", round),
        FontVariation.Setting("opsz", opticalSize)
    )
)

private fun flexFamily(width: Float, round: Float, opticalSize: Float): FontFamily = FontFamily(
    listOf(400, 500, 600, 700, 800).map { flexFont(it, width, round, opticalSize) }
)

/** Headlines: wider, fully rounded letterforms — the emphatic M3 Expressive style. */
val WseiDisplayFontFamily: FontFamily = flexFamily(width = 112f, round = 100f, opticalSize = 48f)

/** Body text and labels: narrower, calm drawing that stays legible at small sizes. */
val WseiPlainFontFamily: FontFamily = flexFamily(width = 100f, round = 0f, opticalSize = 16f)


private val BaseTypography = Typography()

val WseiExpressiveTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = WseiDisplayFontFamily, fontWeight = FontWeight.Bold),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = WseiDisplayFontFamily, fontWeight = FontWeight.Bold),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = WseiDisplayFontFamily, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(
        fontFamily = WseiDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = WseiDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = WseiDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = WseiDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = WseiPlainFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = WseiPlainFontFamily, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(
        fontFamily = WseiPlainFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = WseiPlainFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = WseiPlainFontFamily),
    labelLarge = TextStyle(
        fontFamily = WseiPlainFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = WseiPlainFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = WseiPlainFontFamily, fontWeight = FontWeight.Medium)
)

/** M3 Expressive corner scale (extraLarge = "large increased"). */
val WseiExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/**
 * Asymmetric shape of the "Trwa teraz" hero: large corners on one diagonal (top-start,
 * bottom-end), smaller on the other — an expressive "leaf" that stands apart from plain cards.
 */
val ActiveHeroCardAsymmetricShape = RoundedCornerShape(
    topStart = 44.dp,
    topEnd = 20.dp,
    bottomEnd = 44.dp,
    bottomStart = 20.dp
)

/** Pill — floating navigation bar, chips, buttons. */
val M3XFloatingPillShape: Shape = CircleShape

/** Squircle — icon and thumbnail containers. */
val M3XSquircleShape: Shape = RoundedCornerShape(percent = 36)

/** Arch — rounded top, gentle base. */
val M3XArchShape: Shape = RoundedCornerShape(
    topStart = 36.dp,
    topEnd = 36.dp,
    bottomStart = 18.dp,
    bottomEnd = 18.dp
)

/**
 * Wavy silhouette in the style of `MaterialShapes` (cookie / clover / sunny):
 * a circle whose radius oscillates sinusoidally [lobes] times.
 */
private fun waveShape(lobes: Int, amplitude: Float, baseFraction: Float = 1f - amplitude): Shape =
    GenericShape { size, _ ->
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = minOf(cx, cy)
        val base = radius * baseFraction
        val wave = radius * amplitude
        val steps = lobes * 24
        for (i in 0..steps) {
            val theta = (2.0 * PI * i) / steps - PI / 2.0
            val r = base + wave * cos(lobes * (theta + PI / 2.0)).toFloat()
            val x = cx + r * cos(theta).toFloat()
            val y = cy + r * sin(theta).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

/** Cookie 8 — avatars, screen headers, highlighted grades. */
val M3XCookieShape: Shape = waveShape(lobes = 8, amplitude = 0.08f)

/** Cookie 12 — subtler waves for large avatars. */
val M3XCookie12Shape: Shape = waveShape(lobes = 12, amplitude = 0.05f)

/** Clover 4 — quick-action tiles. */
val M3XCloverShape: Shape = waveShape(lobes = 4, amplitude = 0.14f)

/** Sunny — status badges. */
val M3XSunnyShape: Shape = waveShape(lobes = 10, amplitude = 0.09f)

/**
 * Decodes the student photo (Base64 from Wirtualny Dziekanat) into an [ImageBitmap],
 * stripping an optional `data:image/...;base64,` header.
 */
@Composable
fun rememberDecodedStudentPhoto(base64Photo: String?): ImageBitmap? {
    return remember(base64Photo) {
        if (base64Photo.isNullOrBlank()) return@remember null
        runCatching {
            val cleanBase64 = base64Photo
                .substringAfter("base64,", base64Photo)
                .filterNot { it.isWhitespace() }
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            if (bytes.isEmpty()) return@runCatching null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
}

/**
 * Whether the app is dark for [themeMode]. Single source of truth for the colour scheme and
 * the system bar icons.
 */
@Composable
fun isWseiDarkTheme(themeMode: AppThemeMode): Boolean {
    val systemInDark = isSystemInDarkTheme()
    return when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> systemInDark
    }
}

/**
 * Full colour scheme for [palette] in the light or dark variant. [AppColorPalette.DYNAMIC] uses
 * the wallpaper colours (Material You) on Android 12+, and falls back to Taal on older devices.
 * Every scheme passes [ensureContrast]: wallpapers can yield high-chroma roles whose "on" colours
 * do not read, so dynamic schemes are repaired the same way as the curated ones are verified.
 */
@Composable
fun wseiColorScheme(palette: AppColorPalette, isDark: Boolean): ColorScheme {
    val context = LocalContext.current
    return if (palette == AppColorPalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Read on every theme recomposition, so a wallpaper change is picked up immediately.
        ensureContrast(if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context))
    } else {
        wseiStaticColorScheme(palette, isDark)
    }
}

/**
 * Swatch colours for a palette picker: primary, secondary and tertiary of [palette] in the
 * requested variant (wallpaper colours for [AppColorPalette.DYNAMIC] on Android 12+, otherwise Taal).
 */
@Composable
fun wseiPaletteSwatch(palette: AppColorPalette, isDark: Boolean): List<Color> {
    val scheme = wseiColorScheme(palette, isDark)
    return listOf(scheme.primary, scheme.secondary, scheme.tertiary)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WseiExpressiveTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    colorPalette: AppColorPalette = AppColorPalette.TAAL,
    content: @Composable () -> Unit
) {
    val isDark = isWseiDarkTheme(themeMode)
    // „Zgodny z systemem” to paleta Taal w jasnej lub ciemnej wersji jak w systemie; kolory
    // z tapety (Material You) tylko w trybie „Dynamiczny”.
    val colorScheme = wseiColorScheme(colorPalette, isDark)

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = WseiExpressiveTypography,
        shapes = WseiExpressiveShapes,
        // Expressive springs, tuned stiffer for a snappier feel (see WseiMotionScheme).
        motionScheme = WseiMotionScheme,
    ) {
        val windowBackground = MaterialTheme.colorScheme.background
        // Translucent scrim behind the 3-button system bar (Android 8–9; from Android 10 the
        // system picks it from the icon brightness).
        val navigationScrim = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
        val view = LocalView.current
        if (!view.isInEditMode) {
            DisposableEffect(view, isDark, windowBackground, navigationScrim) {
                val activity = view.context.findActivity()
                val window = activity?.window
                // Window background matches the theme — no flash around the view during transitions.
                window?.setBackgroundDrawable(ColorDrawable(windowBackground.toArgb()))
                if (activity is ComponentActivity) {
                    // System bar icons follow the app theme, not the system one, so they stay
                    // legible when dark mode is forced on a light system (or vice versa).
                    val transparent = Color.Transparent.toArgb()
                    val scrim = navigationScrim.toArgb()
                    activity.enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(transparent, transparent) { isDark },
                        navigationBarStyle = SystemBarStyle.auto(scrim, scrim) { isDark }
                    )
                } else if (window != null) {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !isDark
                    controller.isAppearanceLightNavigationBars = !isDark
                }
                onDispose { }
            }
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = windowBackground,
            content = content
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
