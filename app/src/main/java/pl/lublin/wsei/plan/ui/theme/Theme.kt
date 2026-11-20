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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
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
 * System projektowy Taal oparty o Material 3 Expressive.
 *
 * - Typografia: Google Sans Flex (osie wght, wdth, opsz, ROND) — emfatyczne nagłówki, spokojny tekst.
 * - Kształty: skala zaokrągleń M3 Expressive + falowane sylwetki (cookie, clover, sunny).
 * - Kolor: palety Taal, Cobalt, Terracotta, Plum, Ocean oraz Dynamic Color (Material You).
 * - Ruch: przyspieszone sprężyny expressive ([WseiMotionScheme]) oraz [WseiMotion] dla własnych animacji.
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

/** Nagłówki: szersze, mocno zaokrąglone litery — emfatyczny styl M3 Expressive. */
val WseiDisplayFontFamily: FontFamily = flexFamily(width = 112f, round = 100f, opticalSize = 48f)

/** Tekst ciągły i etykiety: węższy, spokojny rysunek dla czytelności w małych rozmiarach. */
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

// -----------------------------------------------------------------------------------------
// Curated palettes. Every scheme is one tonal system derived from a single key hue:
// primary (vivid key chroma), secondary (same hue, low chroma), tertiary (hue + 60°),
// neutral / neutral-variant (same hue, chroma 4 / 8). Roles map to tones exactly as in
// Material 3 — light: accent T40, container T90, on-container T10, surface N98,
// containers N100→N90; dark: accent T80, container T30, on-container T90, surface N6,
// containers N4→N22. Text/accent pairs meet WCAG AA (≥ 4.5:1). Error roles use M3 defaults.
// -----------------------------------------------------------------------------------------

/* Cobalt — indigo / cobalt blue, rose tertiary. */
private val CobaltLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF005DB9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBE1FF),
    onPrimaryContainer = Color(0xFF001B3E),
    inversePrimary = Color(0xFFB5C4FF),
    secondary = Color(0xFF575D77),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBE1FF),
    onSecondaryContainer = Color(0xFF121B30),
    tertiary = Color(0xFF834D6B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8EC),
    onTertiaryContainer = Color(0xFF380726),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF1A1B21),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF1A1B21),
    surfaceVariant = Color(0xFFDFE2F1),
    onSurfaceVariant = Color(0xFF444653),
    inverseSurface = Color(0xFF2F3036),
    inverseOnSurface = Color(0xFFEFF0F8),
    outline = Color(0xFF747684),
    outlineVariant = Color(0xFFC3C6D5),
    surfaceBright = Color(0xFFF8F9FF),
    surfaceDim = Color(0xFFD8DAE1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3FB),
    surfaceContainer = Color(0xFFECEDF5),
    surfaceContainerHigh = Color(0xFFE6E8EF),
    surfaceContainerHighest = Color(0xFFE1E2EA),
    primaryFixed = Color(0xFFDBE1FF),
    primaryFixedDim = Color(0xFFB5C4FF),
    onPrimaryFixed = Color(0xFF001B3E),
    onPrimaryFixedVariant = Color(0xFF00458E),
    secondaryFixed = Color(0xFFDBE1FF),
    secondaryFixedDim = Color(0xFFBFC5E3),
    onSecondaryFixed = Color(0xFF121B30),
    onSecondaryFixedVariant = Color(0xFF3F465F),
    tertiaryFixed = Color(0xFFFFD8EC),
    tertiaryFixedDim = Color(0xFFF2B5D5),
    onTertiaryFixed = Color(0xFF380726),
    onTertiaryFixedVariant = Color(0xFF693653)
)

private val CobaltDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFB5C4FF),
    onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF00458E),
    onPrimaryContainer = Color(0xFFDBE1FF),
    inversePrimary = Color(0xFF005DB9),
    secondary = Color(0xFFBFC5E3),
    onSecondary = Color(0xFF283047),
    secondaryContainer = Color(0xFF3F465F),
    onSecondaryContainer = Color(0xFFDBE1FF),
    tertiary = Color(0xFFF2B5D5),
    onTertiary = Color(0xFF501F3C),
    tertiaryContainer = Color(0xFF693653),
    onTertiaryContainer = Color(0xFFFFD8EC),
    background = Color(0xFF121319),
    onBackground = Color(0xFFE1E2EA),
    surface = Color(0xFF121319),
    onSurface = Color(0xFFE1E2EA),
    surfaceVariant = Color(0xFF444653),
    onSurfaceVariant = Color(0xFFC3C6D5),
    inverseSurface = Color(0xFFE1E2EA),
    inverseOnSurface = Color(0xFF2F3036),
    outline = Color(0xFF8D909E),
    outlineVariant = Color(0xFF444653),
    surfaceBright = Color(0xFF38393F),
    surfaceDim = Color(0xFF121319),
    surfaceContainerLowest = Color(0xFF0C0E15),
    surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainer = Color(0xFF1E1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF33343A),
    primaryFixed = Color(0xFFDBE1FF),
    primaryFixedDim = Color(0xFFB5C4FF),
    onPrimaryFixed = Color(0xFF001B3E),
    onPrimaryFixedVariant = Color(0xFF00458E),
    secondaryFixed = Color(0xFFDBE1FF),
    secondaryFixedDim = Color(0xFFBFC5E3),
    onSecondaryFixed = Color(0xFF121B30),
    onSecondaryFixedVariant = Color(0xFF3F465F),
    tertiaryFixed = Color(0xFFFFD8EC),
    tertiaryFixedDim = Color(0xFFF2B5D5),
    onTertiaryFixed = Color(0xFF380726),
    onTertiaryFixedVariant = Color(0xFF693653)
)

/* Terracotta — coral / terracotta, olive-gold tertiary. */
private val TerracottaLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF9F4124),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF341100),
    inversePrimary = Color(0xFFFFB59D),
    secondary = Color(0xFF77574D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCF),
    onSecondaryContainer = Color(0xFF2E150A),
    tertiary = Color(0xFF626031),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE9E5AE),
    onTertiaryContainer = Color(0xFF1E1C00),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF211A18),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF211A18),
    surfaceVariant = Color(0xFFF2DED8),
    onSurfaceVariant = Color(0xFF53433E),
    inverseSurface = Color(0xFF362F2C),
    inverseOnSurface = Color(0xFFF9EFEB),
    outline = Color(0xFF85736E),
    outlineVariant = Color(0xFFD6C2BC),
    surfaceBright = Color(0xFFFFF8F6),
    surfaceDim = Color(0xFFE2D8D5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCF1EE),
    surfaceContainer = Color(0xFFF6ECE9),
    surfaceContainerHigh = Color(0xFFF0E6E3),
    surfaceContainerHighest = Color(0xFFEAE0DD),
    primaryFixed = Color(0xFFFFDBCF),
    primaryFixedDim = Color(0xFFFFB59D),
    onPrimaryFixed = Color(0xFF341100),
    onPrimaryFixedVariant = Color(0xFF81280E),
    secondaryFixed = Color(0xFFFFDBCF),
    secondaryFixedDim = Color(0xFFE4BEB2),
    onSecondaryFixed = Color(0xFF2E150A),
    onSecondaryFixedVariant = Color(0xFF5D4036),
    tertiaryFixed = Color(0xFFE9E5AE),
    tertiaryFixedDim = Color(0xFFCDC993),
    onTertiaryFixed = Color(0xFF1E1C00),
    onTertiaryFixedVariant = Color(0xFF4A491B)
)

private val TerracottaDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB59D),
    onPrimary = Color(0xFF5E1700),
    primaryContainer = Color(0xFF81280E),
    onPrimaryContainer = Color(0xFFFFDBCF),
    inversePrimary = Color(0xFF9F4124),
    secondary = Color(0xFFE4BEB2),
    onSecondary = Color(0xFF452A21),
    secondaryContainer = Color(0xFF5D4036),
    onSecondaryContainer = Color(0xFFFFDBCF),
    tertiary = Color(0xFFCDC993),
    onTertiary = Color(0xFF323202),
    tertiaryContainer = Color(0xFF4A491B),
    onTertiaryContainer = Color(0xFFE9E5AE),
    background = Color(0xFF19120F),
    onBackground = Color(0xFFEAE0DD),
    surface = Color(0xFF19120F),
    onSurface = Color(0xFFEAE0DD),
    surfaceVariant = Color(0xFF53433E),
    onSurfaceVariant = Color(0xFFD6C2BC),
    inverseSurface = Color(0xFFEAE0DD),
    inverseOnSurface = Color(0xFF362F2C),
    outline = Color(0xFF9F8D87),
    outlineVariant = Color(0xFF53433E),
    surfaceBright = Color(0xFF3F3735),
    surfaceDim = Color(0xFF19120F),
    surfaceContainerLowest = Color(0xFF150C08),
    surfaceContainerLow = Color(0xFF211A18),
    surfaceContainer = Color(0xFF251E1C),
    surfaceContainerHigh = Color(0xFF302826),
    surfaceContainerHighest = Color(0xFF3B3331),
    primaryFixed = Color(0xFFFFDBCF),
    primaryFixedDim = Color(0xFFFFB59D),
    onPrimaryFixed = Color(0xFF341100),
    onPrimaryFixedVariant = Color(0xFF81280E),
    secondaryFixed = Color(0xFFFFDBCF),
    secondaryFixedDim = Color(0xFFE4BEB2),
    onSecondaryFixed = Color(0xFF2E150A),
    onSecondaryFixedVariant = Color(0xFF5D4036),
    tertiaryFixed = Color(0xFFE9E5AE),
    tertiaryFixedDim = Color(0xFFCDC993),
    onTertiaryFixed = Color(0xFF1E1C00),
    onTertiaryFixedVariant = Color(0xFF4A491B)
)

/* Plum — plum / violet, warm rose tertiary. */
private val PlumLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF8E4185),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD6F8),
    onPrimaryContainer = Color(0xFF390035),
    inversePrimary = Color(0xFFFFABF2),
    secondary = Color(0xFF6F576B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF7DAF1),
    onSecondaryContainer = Color(0xFF291426),
    tertiary = Color(0xFF894E48),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDAD5),
    onTertiaryContainer = Color(0xFF3B0903),
    background = Color(0xFFFFF7FD),
    onBackground = Color(0xFF1F1A1E),
    surface = Color(0xFFFFF7FD),
    onSurface = Color(0xFF1F1A1E),
    surfaceVariant = Color(0xFFEDDEEA),
    onSurfaceVariant = Color(0xFF4F434D),
    inverseSurface = Color(0xFF342F33),
    inverseOnSurface = Color(0xFFF6EFF4),
    outline = Color(0xFF80737E),
    outlineVariant = Color(0xFFD1C2CE),
    surfaceBright = Color(0xFFFFF7FD),
    surfaceDim = Color(0xFFDFD8DE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F1F7),
    surfaceContainer = Color(0xFFF3ECF2),
    surfaceContainerHigh = Color(0xFFEDE6EC),
    surfaceContainerHighest = Color(0xFFE8E0E6),
    primaryFixed = Color(0xFFFFD6F8),
    primaryFixedDim = Color(0xFFFFABF2),
    onPrimaryFixed = Color(0xFF390035),
    onPrimaryFixedVariant = Color(0xFF73286B),
    secondaryFixed = Color(0xFFF7DAF1),
    secondaryFixedDim = Color(0xFFDABED5),
    onSecondaryFixed = Color(0xFF291426),
    onSecondaryFixedVariant = Color(0xFF573F53),
    tertiaryFixed = Color(0xFFFFDAD5),
    tertiaryFixedDim = Color(0xFFFBB5AD),
    onTertiaryFixed = Color(0xFF3B0903),
    onTertiaryFixedVariant = Color(0xFF6E3731)
)

private val PlumDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFABF2),
    onPrimary = Color(0xFF590A53),
    primaryContainer = Color(0xFF73286B),
    onPrimaryContainer = Color(0xFFFFD6F8),
    inversePrimary = Color(0xFF8E4185),
    secondary = Color(0xFFDABED5),
    onSecondary = Color(0xFF3F293C),
    secondaryContainer = Color(0xFF573F53),
    onSecondaryContainer = Color(0xFFF7DAF1),
    tertiary = Color(0xFFFBB5AD),
    onTertiary = Color(0xFF54201C),
    tertiaryContainer = Color(0xFF6E3731),
    onTertiaryContainer = Color(0xFFFFDAD5),
    background = Color(0xFF171216),
    onBackground = Color(0xFFE8E0E6),
    surface = Color(0xFF171216),
    onSurface = Color(0xFFE8E0E6),
    surfaceVariant = Color(0xFF4F434D),
    onSurfaceVariant = Color(0xFFD1C2CE),
    inverseSurface = Color(0xFFE8E0E6),
    inverseOnSurface = Color(0xFF342F33),
    outline = Color(0xFF9A8D98),
    outlineVariant = Color(0xFF4F434D),
    surfaceBright = Color(0xFF3D373C),
    surfaceDim = Color(0xFF171216),
    surfaceContainerLowest = Color(0xFF130C12),
    surfaceContainerLow = Color(0xFF1F1A1E),
    surfaceContainer = Color(0xFF231E22),
    surfaceContainerHigh = Color(0xFF2E282D),
    surfaceContainerHighest = Color(0xFF393338),
    primaryFixed = Color(0xFFFFD6F8),
    primaryFixedDim = Color(0xFFFFABF2),
    onPrimaryFixed = Color(0xFF390035),
    onPrimaryFixedVariant = Color(0xFF73286B),
    secondaryFixed = Color(0xFFF7DAF1),
    secondaryFixedDim = Color(0xFFDABED5),
    onSecondaryFixed = Color(0xFF291426),
    onSecondaryFixedVariant = Color(0xFF573F53),
    tertiaryFixed = Color(0xFFFFDAD5),
    tertiaryFixedDim = Color(0xFFFBB5AD),
    onTertiaryFixed = Color(0xFF3B0903),
    onTertiaryFixedVariant = Color(0xFF6E3731)
)

/* Ocean — teal / ocean blue, periwinkle tertiary. */
private val OceanLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF00677E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB4EBFF),
    onPrimaryContainer = Color(0xFF001F28),
    inversePrimary = Color(0xFF43D6FE),
    secondary = Color(0xFF3B6472),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBFE9F9),
    onSecondaryContainer = Color(0xFF001F28),
    tertiary = Color(0xFF555B89),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDFE0FF),
    onTertiaryContainer = Color(0xFF09193F),
    background = Color(0xFFF1FBFF),
    onBackground = Color(0xFF151D1F),
    surface = Color(0xFFF1FBFF),
    onSurface = Color(0xFF151D1F),
    surfaceVariant = Color(0xFFD2E6EE),
    onSurfaceVariant = Color(0xFF384950),
    inverseSurface = Color(0xFF2A3235),
    inverseOnSurface = Color(0xFFE8F2F6),
    outline = Color(0xFF677A81),
    outlineVariant = Color(0xFFB6CAD1),
    surfaceBright = Color(0xFFF1FBFF),
    surfaceDim = Color(0xFFD2DCE0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEBF5F9),
    surfaceContainer = Color(0xFFE6F0F3),
    surfaceContainerHigh = Color(0xFFE0EAEE),
    surfaceContainerHighest = Color(0xFFDAE4E8),
    primaryFixed = Color(0xFFB4EBFF),
    primaryFixedDim = Color(0xFF43D6FE),
    onPrimaryFixed = Color(0xFF001F28),
    onPrimaryFixedVariant = Color(0xFF004E5F),
    secondaryFixed = Color(0xFFBFE9F9),
    secondaryFixedDim = Color(0xFFA3CDDD),
    onSecondaryFixed = Color(0xFF001F28),
    onSecondaryFixedVariant = Color(0xFF224C59),
    tertiaryFixed = Color(0xFFDFE0FF),
    tertiaryFixedDim = Color(0xFFBFC3F7),
    onTertiaryFixed = Color(0xFF09193F),
    onTertiaryFixedVariant = Color(0xFF3C4470)
)

private val OceanDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF43D6FE),
    onPrimary = Color(0xFF003543),
    primaryContainer = Color(0xFF004E5F),
    onPrimaryContainer = Color(0xFFB4EBFF),
    inversePrimary = Color(0xFF00677E),
    secondary = Color(0xFFA3CDDD),
    onSecondary = Color(0xFF043542),
    secondaryContainer = Color(0xFF224C59),
    onSecondaryContainer = Color(0xFFBFE9F9),
    tertiary = Color(0xFFBFC3F7),
    onTertiary = Color(0xFF242E57),
    tertiaryContainer = Color(0xFF3C4470),
    onTertiaryContainer = Color(0xFFDFE0FF),
    background = Color(0xFF0C1518),
    onBackground = Color(0xFFDAE4E8),
    surface = Color(0xFF0C1518),
    onSurface = Color(0xFFDAE4E8),
    surfaceVariant = Color(0xFF384950),
    onSurfaceVariant = Color(0xFFB6CAD1),
    inverseSurface = Color(0xFFDAE4E8),
    inverseOnSurface = Color(0xFF2A3235),
    outline = Color(0xFF81949B),
    outlineVariant = Color(0xFF384950),
    surfaceBright = Color(0xFF323A3E),
    surfaceDim = Color(0xFF0C1518),
    surfaceContainerLowest = Color(0xFF041013),
    surfaceContainerLow = Color(0xFF151D1F),
    surfaceContainer = Color(0xFF192124),
    surfaceContainerHigh = Color(0xFF232B2E),
    surfaceContainerHighest = Color(0xFF2E3639),
    primaryFixed = Color(0xFFB4EBFF),
    primaryFixedDim = Color(0xFF43D6FE),
    onPrimaryFixed = Color(0xFF001F28),
    onPrimaryFixedVariant = Color(0xFF004E5F),
    secondaryFixed = Color(0xFFBFE9F9),
    secondaryFixedDim = Color(0xFFA3CDDD),
    onSecondaryFixed = Color(0xFF001F28),
    onSecondaryFixedVariant = Color(0xFF224C59),
    tertiaryFixed = Color(0xFFDFE0FF),
    tertiaryFixedDim = Color(0xFFBFC3F7),
    onTertiaryFixed = Color(0xFF09193F),
    onTertiaryFixedVariant = Color(0xFF3C4470)
)

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

/** Skala zaokrągleń M3 Expressive (extraLarge = „large increased”). */
val WseiExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/**
 * Asymetryczny kształt karty „Trwa teraz”: duże rogi na przekątnej (góra-start, dół-koniec),
 * mniejsze na drugiej — ekspresyjny „liść” M3 Expressive, czytelnie odróżniony od zwykłych kart.
 */
val ActiveHeroCardAsymmetricShape = RoundedCornerShape(
    topStart = 44.dp,
    topEnd = 20.dp,
    bottomEnd = 44.dp,
    bottomStart = 20.dp
)

/** Pigułka — pływający pasek nawigacji, chipy, przyciski. */
val M3XFloatingPillShape: Shape = CircleShape

/** Squircle — kontenery ikon i miniatur. */
val M3XSquircleShape: Shape = RoundedCornerShape(percent = 36)

/** Łuk — zaokrąglona góra, łagodna podstawa. */
val M3XArchShape: Shape = RoundedCornerShape(
    topStart = 36.dp,
    topEnd = 36.dp,
    bottomStart = 18.dp,
    bottomEnd = 18.dp
)

/**
 * Falowana sylwetka w stylu `MaterialShapes` (cookie / clover / sunny):
 * okrąg, którego promień zmienia się sinusoidalnie [lobes] razy.
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

/** Cookie 8 — awatary, nagłówki ekranów, wyróżnione oceny. */
val M3XCookieShape: Shape = waveShape(lobes = 8, amplitude = 0.08f)

/** Cookie 12 — subtelniejsze falowanie dla dużych awatarów. */
val M3XCookie12Shape: Shape = waveShape(lobes = 12, amplitude = 0.05f)

/** Clover 4 — kafelki szybkich akcji. */
val M3XCloverShape: Shape = waveShape(lobes = 4, amplitude = 0.14f)

/** Sunny — znaczki i odznaki statusu. */
val M3XSunnyShape: Shape = waveShape(lobes = 10, amplitude = 0.09f)

/**
 * Dekoduje zdjęcie studenta (Base64 z Wirtualnego Dziekanatu) do [ImageBitmap],
 * usuwając opcjonalny nagłówek `data:image/...;base64,`.
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
 * Czy dla danego trybu motywu aplikacja jest ciemna. Wspólne źródło prawdy dla schematu
 * kolorów i ikon pasków systemowych.
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

/** Static (non-wallpaper) scheme of [palette]; [AppColorPalette.DYNAMIC] maps to Taal here. */
private fun staticColorScheme(palette: AppColorPalette, isDark: Boolean): ColorScheme = when (palette) {
    AppColorPalette.TAAL, AppColorPalette.DYNAMIC -> if (isDark) WseiDarkColorScheme else WseiLightColorScheme
    AppColorPalette.COBALT -> if (isDark) CobaltDarkColorScheme else CobaltLightColorScheme
    AppColorPalette.TERRACOTTA -> if (isDark) TerracottaDarkColorScheme else TerracottaLightColorScheme
    AppColorPalette.PLUM -> if (isDark) PlumDarkColorScheme else PlumLightColorScheme
    AppColorPalette.OCEAN -> if (isDark) OceanDarkColorScheme else OceanLightColorScheme
}

/**
 * Full colour scheme for [palette] in the light or dark variant. [AppColorPalette.DYNAMIC] uses
 * the wallpaper colours (Material You) on Android 12+, and falls back to Taal on older devices.
 */
@Composable
fun wseiColorScheme(palette: AppColorPalette, isDark: Boolean): ColorScheme {
    val context = LocalContext.current
    return if (palette == AppColorPalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Read on every theme recomposition, so a wallpaper change is picked up immediately.
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        staticColorScheme(palette, isDark)
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
        // Półprzezroczysta osłona 3-przyciskowego paska systemu (Android 8–9; od Androida 10
        // system dobiera ją sam według jasności ikon).
        val navigationScrim = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
        val view = LocalView.current
        if (!view.isInEditMode) {
            DisposableEffect(view, isDark, windowBackground, navigationScrim) {
                val activity = view.context.findActivity()
                val window = activity?.window
                // Tło okna zgodne z motywem — eliminuje błysk wokół widoku podczas przejść.
                window?.setBackgroundDrawable(ColorDrawable(windowBackground.toArgb()))
                if (activity is ComponentActivity) {
                    // Ikony pasków systemowych według motywu aplikacji, a nie systemu: przy
                    // wymuszonym trybie ciemnym na jasnym systemie (lub odwrotnie) zostają czytelne.
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
