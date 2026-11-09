package pl.lublin.wsei.plan.ui.theme

import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.ui.schedule.AppThemeMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive Design System for Lubelska Akademia WSEI.
 *
 * - Typography: Google Sans Flex (czcionka zmienna: wght, wdth, opsz, ROND)
 * - Shapes: Material 3 Expressive (M3X) silhouettes (`M3XCookieShape`, `M3XSquircleShape`,
 *   `M3XArchShape`, `M3XFloatingPillShape`, `ActiveHeroCardAsymmetricShape`)
 * - Color: Material You Dynamic Color with vibrant WSEI Emerald & Amber high-contrast fallbacks
 * - Motion: Expressive spring physics (`MotionScheme.expressive()`)
 */

/**
 * Google Sans Flex jako czcionka zmienna dołączona do aplikacji (res/font/google_sans_flex.ttf).
 * Osie: wght (grubość), wdth (szerokość), opsz (rozmiar optyczny), ROND (zaokrąglenie), slnt (pochylenie).
 * Dołączony plik działa offline i nie zależy od Usług Google Play.
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
    listOf(400, 500, 600, 700).map { flexFont(it, width, round, opticalSize) }
)

/** Nagłówki: szersze litery i wyraźne zaokrąglenie, jak w emfatycznych stylach M3 Expressive. */
val WseiDisplayFontFamily: FontFamily = flexFamily(width = 112f, round = 70f, opticalSize = 48f)

/** Tekst ciągły i etykiety: węższy, spokojniejszy rysunek dla czytelności małych rozmiarów. */
val WseiPlainFontFamily: FontFamily = flexFamily(width = 100f, round = 0f, opticalSize = 16f)

private val WseiLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF006B58),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF7BF8D8),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF3F6375),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC3E8FD),
    onSecondaryContainer = Color(0xFF001E2B),
    tertiary = Color(0xFF8F4900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC2),
    onTertiaryContainer = Color(0xFF2E1500),
    background = Color(0xFFF5FBF7),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFF5FBF7),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDBE5E0),
    onSurfaceVariant = Color(0xFF3F4945),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F1),
    surfaceContainer = Color(0xFFE9EFEA),
    surfaceContainerHigh = Color(0xFFE3EAE5),
    surfaceContainerHighest = Color(0xFFDEE4DF),
    outline = Color(0xFF6F7975),
    outlineVariant = Color(0xFFBFC9C4)
)

private val WseiDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF5CDBBC),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF005142),
    onPrimaryContainer = Color(0xFF7BF8D8),
    secondary = Color(0xFFA7CCE0),
    onSecondary = Color(0xFF0B3445),
    secondaryContainer = Color(0xFF264B5C),
    onSecondaryContainer = Color(0xFFC3E8FD),
    tertiary = Color(0xFFFFB77C),
    onTertiary = Color(0xFF4D2600),
    tertiaryContainer = Color(0xFF6D3900),
    onTertiaryContainer = Color(0xFFFFDCC2),
    background = Color(0xFF0E1513),
    onBackground = Color(0xFFDDE4E0),
    surface = Color(0xFF0E1513),
    onSurface = Color(0xFFDDE4E0),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBFC9C4),
    surfaceContainerLowest = Color(0xFF090F0E),
    surfaceContainerLow = Color(0xFF171D1B),
    surfaceContainer = Color(0xFF1B211F),
    surfaceContainerHigh = Color(0xFF252B29),
    surfaceContainerHighest = Color(0xFF303634),
    outline = Color(0xFF89938E),
    outlineVariant = Color(0xFF3F4945)
)

private val BaseTypography = Typography()

val WseiExpressiveTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = WseiDisplayFontFamily, fontWeight = FontWeight.Bold),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = WseiDisplayFontFamily, fontWeight = FontWeight.Bold),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = WseiDisplayFontFamily, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(
        fontFamily = WseiDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = WseiDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = WseiDisplayFontFamily, fontWeight = FontWeight.SemiBold),
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

val WseiExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Asymmetric expressive shape reserved for Hero Cards and primary dashboard tiles.
 */
val ActiveHeroCardAsymmetricShape = RoundedCornerShape(
    topStart = 32.dp,
    topEnd = 28.dp,
    bottomEnd = 32.dp,
    bottomStart = 18.dp
)

/**
 * Material 3 Expressive Floating Pill Shape for the bottom icon-only navigation capsule.
 */
val M3XFloatingPillShape: Shape = CircleShape

/**
 * Material 3 Expressive Squircle / Superellipse container for subject icons and avatars.
 */
val M3XSquircleShape: Shape = RoundedCornerShape(percent = 36)

/**
 * Material 3 Expressive Arch silhouette (rounded top arch, softly grounded corners at base).
 */
val M3XArchShape: Shape = RoundedCornerShape(
    topStart = 36.dp,
    topEnd = 36.dp,
    bottomStart = 18.dp,
    bottomEnd = 18.dp
)

/**
 * Material 3 Expressive 8-Lobed Soft Cookie (`Cookie8Sided` / `Sunny`) shape for
 * profile avatars and highlighted status badges.
 */
val M3XCookieShape: Shape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val baseRadius = minOf(cx, cy) * 0.91f
    val waveAmplitude = minOf(cx, cy) * 0.08f
    val lobes = 8
    val steps = 120
    for (i in 0..steps) {
        val theta = (2.0 * PI * i) / steps
        val r = baseRadius + waveAmplitude * cos(lobes * theta).toFloat()
        val x = cx + r * cos(theta).toFloat()
        val y = cy + r * sin(theta).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/**
 * Decodes a Base64-encoded student photo from Wirtualny Dziekanat (`MsPictureDto` / `personPicture`)
 * into a Compose [ImageBitmap], stripping any optional `data:image/...;base64,` URI header.
 */
@Composable
fun rememberDecodedStudentPhoto(base64Photo: String?): ImageBitmap? {
    return remember(base64Photo) {
        if (base64Photo.isNullOrBlank()) return@remember null
        runCatching {
            val cleanBase64 = base64Photo
                .substringAfter("base64,", base64Photo)
                .replace("\\s".toRegex(), "")
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            if (bytes.isEmpty()) return@runCatching null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WseiExpressiveTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.DYNAMIC -> systemInDark
    }
    val isDynamic = when (themeMode) {
        AppThemeMode.SYSTEM -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> false
        AppThemeMode.DYNAMIC -> true
    }

    val colorScheme = when {
        isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isDark ->
            dynamicDarkColorScheme(context)
        isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        isDark -> WseiDarkColorScheme
        else -> WseiLightColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = WseiExpressiveTypography,
        shapes = WseiExpressiveShapes,
        motionScheme = MotionScheme.expressive(),
    ) {
        // Tło okna zgodne z motywem — eliminuje biały błysk wokół widoku podczas przejść.
        val windowBackground = MaterialTheme.colorScheme.background
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as? Activity)?.window
                window?.setBackgroundDrawable(ColorDrawable(windowBackground.toArgb()))
                if (window != null) {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !isDark
                    controller.isAppearanceLightNavigationBars = !isDark
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = windowBackground,
            content = content
        )
    }
}
