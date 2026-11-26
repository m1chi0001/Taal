package pl.lublin.wsei.plan.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

// -----------------------------------------------------------------------------------------
// WCAG contrast engine. Pure functions on Compose `Color` (no Android or Compose runtime), so
// they run in JVM unit tests and are cheap enough to apply to every scheme on every theme
// change. Wallpaper-based (dynamic) schemes are not under our control: some devices produce
// high-chroma roles whose "on" colour barely reads on top of them. [ensureContrast] repairs
// such schemes; it is a no-op for schemes that already pass, so it is idempotent.
// -----------------------------------------------------------------------------------------

/** WCAG 2.x AA minimum for body text. */
const val WcagTextContrast: Float = 4.5f

/** WCAG 2.x AA minimum for large text, icons and other meaningful non-text UI. */
const val WcagNonTextContrast: Float = 3f

/**
 * Dark themes keep their containers dark: a container brighter than this relative luminance
 * (≈ tone 40; M3 dark containers are tone 30) reads as a "neon" slab and is pulled down toward
 * black, keeping its hue. Light on-colours then clearly win (white reaches ≥ 6:1 on it).
 */
internal const val DarkContainerMaxLuminance: Float = 0.12f

/** Light themes keep their containers light (≈ tone 62 and up). */
internal const val LightContainerMinLuminance: Float = 0.30f

/**
 * WCAG contrast ratio (1…21) between [foreground] and an opaque [background]. A translucent
 * foreground is composited over the background first, exactly as it is drawn.
 */
fun contrastRatio(foreground: Color, background: Color): Float {
    // Unset roles (e.g. fixed roles of a hand-built scheme) are not drawn: nothing to check.
    if (foreground == Color.Unspecified || background == Color.Unspecified) return MaxContrast
    val fg = if (foreground.alpha < 1f) foreground.compositeOver(background) else foreground
    val a = fg.luminance()
    val b = background.luminance()
    val lighter = maxOf(a, b)
    val darker = minOf(a, b)
    return (lighter + 0.05f) / (darker + 0.05f)
}

/**
 * This colour, or the closest colour on its way to black or white (whichever end contrasts
 * more with [background]) that reaches [minRatio]. Hue is kept as long as possible because the
 * blend runs in Oklab. A colour that already passes is returned unchanged.
 */
fun Color.withContrastAgainst(background: Color, minRatio: Float = WcagTextContrast): Color {
    if (this == Color.Unspecified || background == Color.Unspecified) return this
    val fg = if (alpha < 1f) compositeOver(background) else this
    if (contrastRatio(fg, background) >= minRatio) return if (alpha < 1f) fg else this
    val target = if (contrastRatio(Color.White, background) >= contrastRatio(Color.Black, background)) {
        Color.White
    } else {
        Color.Black
    }
    var low = 0f
    var high = 1f
    repeat(SearchSteps) {
        val mid = (low + high) / 2f
        if (contrastRatio(lerp(fg, target, mid), background) >= minRatio) high = mid else low = mid
    }
    val candidate = lerp(fg, target, high)
    return if (contrastRatio(candidate, background) >= minRatio) candidate else target
}

/** [withContrastAgainst] for several backgrounds at once (e.g. every surface container). */
fun Color.withContrastAgainstAll(backgrounds: List<Color>, minRatio: Float = WcagTextContrast): Color {
    var color = this
    repeat(backgrounds.size + 1) {
        val worst = backgrounds.minByOrNull { contrastRatio(color, it) } ?: return color
        if (contrastRatio(color, worst) >= minRatio) return color
        color = color.withContrastAgainst(worst, minRatio)
    }
    return color
}

/**
 * Secondary ("muted") content on [background]: [content] at [emphasis] opacity, flattened to an
 * opaque colour and pushed back to [minRatio] if the fade made it too weak. Use it instead of
 * `content.copy(alpha = …)` for captions on tinted containers.
 */
fun mutedContentColor(
    content: Color,
    background: Color,
    emphasis: Float = 0.78f,
    minRatio: Float = WcagTextContrast
): Color = content.copy(alpha = emphasis).compositeOver(background).withContrastAgainst(background, minRatio)

/**
 * An opaque tonal layer: [tint] at [fraction] over [base] (chips and bubbles inside a tinted
 * card). Pair it with `content.withContrastAgainst(layer)` for the text drawn on top.
 */
fun tonalLayer(base: Color, tint: Color, fraction: Float): Color =
    tint.copy(alpha = fraction.coerceIn(0f, 1f)).compositeOver(base)

/** One foreground/background pair of a scheme with its required minimum contrast. */
internal data class ContrastRequirement(
    val name: String,
    val foreground: Color,
    val background: Color,
    val minRatio: Float
) {
    val ratio: Float get() = contrastRatio(foreground, background)
    val passes: Boolean get() = ratio >= minRatio
}

private fun ColorScheme.surfaceRoles(): List<Pair<String, Color>> = listOf(
    "background" to background,
    "surface" to surface,
    "surfaceVariant" to surfaceVariant,
    "surfaceBright" to surfaceBright,
    "surfaceDim" to surfaceDim,
    "surfaceContainerLowest" to surfaceContainerLowest,
    "surfaceContainerLow" to surfaceContainerLow,
    "surfaceContainer" to surfaceContainer,
    "surfaceContainerHigh" to surfaceContainerHigh,
    "surfaceContainerHighest" to surfaceContainerHighest
)

/**
 * Every role pairing the app relies on. Text pairs need [WcagTextContrast]; accents are also
 * used as text (overlines, section titles), so they are held to the text minimum on surfaces.
 */
internal fun ColorScheme.contrastRequirements(): List<ContrastRequirement> = buildList {
    fun text(name: String, fg: Color, bg: Color) = add(ContrastRequirement(name, fg, bg, WcagTextContrast))
    text("onPrimary/primary", onPrimary, primary)
    text("onPrimaryContainer/primaryContainer", onPrimaryContainer, primaryContainer)
    text("onSecondary/secondary", onSecondary, secondary)
    text("onSecondaryContainer/secondaryContainer", onSecondaryContainer, secondaryContainer)
    text("onTertiary/tertiary", onTertiary, tertiary)
    text("onTertiaryContainer/tertiaryContainer", onTertiaryContainer, tertiaryContainer)
    text("onError/error", onError, error)
    text("onErrorContainer/errorContainer", onErrorContainer, errorContainer)
    text("onBackground/background", onBackground, background)
    text("inverseOnSurface/inverseSurface", inverseOnSurface, inverseSurface)
    text("inversePrimary/inverseSurface", inversePrimary, inverseSurface)
    text("onPrimaryFixed/primaryFixed", onPrimaryFixed, primaryFixed)
    text("onPrimaryFixed/primaryFixedDim", onPrimaryFixed, primaryFixedDim)
    text("onSecondaryFixed/secondaryFixed", onSecondaryFixed, secondaryFixed)
    text("onSecondaryFixed/secondaryFixedDim", onSecondaryFixed, secondaryFixedDim)
    text("onTertiaryFixed/tertiaryFixed", onTertiaryFixed, tertiaryFixed)
    text("onTertiaryFixed/tertiaryFixedDim", onTertiaryFixed, tertiaryFixedDim)
    surfaceRoles().forEach { (name, surfaceColor) ->
        text("onSurface/$name", onSurface, surfaceColor)
        text("onSurfaceVariant/$name", onSurfaceVariant, surfaceColor)
        text("primary/$name", primary, surfaceColor)
        text("secondary/$name", secondary, surfaceColor)
        text("tertiary/$name", tertiary, surfaceColor)
        text("error/$name", error, surfaceColor)
    }
}

/** Keeps a container on the correct side of the tone scale for the theme (see the limits). */
private fun Color.asContainer(isDark: Boolean): Color {
    if (this == Color.Unspecified) return this
    val tooBright = isDark && luminance() > DarkContainerMaxLuminance
    val tooDark = !isDark && luminance() < LightContainerMinLuminance
    if (!tooBright && !tooDark) return this
    val target = if (isDark) Color.Black else Color.White
    val ok: (Color) -> Boolean = if (isDark) {
        { it.luminance() <= DarkContainerMaxLuminance }
    } else {
        { it.luminance() >= LightContainerMinLuminance }
    }
    var low = 0f
    var high = 1f
    repeat(SearchSteps) {
        val mid = (low + high) / 2f
        if (ok(lerp(this, target, mid))) high = mid else low = mid
    }
    val candidate = lerp(this, target, high)
    return if (ok(candidate)) candidate else target
}

/**
 * Contrast guard applied to every colour scheme of the app (curated palettes and wallpaper
 * colours alike):
 * 1. containers stay dark in dark themes and light in light themes (no neon slabs),
 * 2. accents (primary/secondary/tertiary/error, inversePrimary) reach text contrast on every
 *    surface role,
 * 3. every "on" colour reaches text contrast on its role (and on every surface for
 *    onSurface/onSurfaceVariant).
 * Only colours that fail are moved, so a passing scheme comes back unchanged and applying the
 * guard twice equals applying it once.
 */
fun ensureContrast(scheme: ColorScheme): ColorScheme {
    val isDark = scheme.background.luminance() < 0.5f
    // 1. Containers.
    val primaryContainer = scheme.primaryContainer.asContainer(isDark)
    val secondaryContainer = scheme.secondaryContainer.asContainer(isDark)
    val tertiaryContainer = scheme.tertiaryContainer.asContainer(isDark)
    val errorContainer = scheme.errorContainer.asContainer(isDark)

    // 2. Accents on surfaces.
    val surfaces = scheme.surfaceRoles().map { it.second }
    val primary = scheme.primary.withContrastAgainstAll(surfaces)
    val secondary = scheme.secondary.withContrastAgainstAll(surfaces)
    val tertiary = scheme.tertiary.withContrastAgainstAll(surfaces)
    val error = scheme.error.withContrastAgainstAll(surfaces)
    val inversePrimary = scheme.inversePrimary.withContrastAgainst(scheme.inverseSurface)

    // 3. On-colours.
    return scheme.copy(
        primary = primary,
        onPrimary = scheme.onPrimary.withContrastAgainst(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = scheme.onPrimaryContainer.withContrastAgainst(primaryContainer),
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = scheme.onSecondary.withContrastAgainst(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = scheme.onSecondaryContainer.withContrastAgainst(secondaryContainer),
        tertiary = tertiary,
        onTertiary = scheme.onTertiary.withContrastAgainst(tertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = scheme.onTertiaryContainer.withContrastAgainst(tertiaryContainer),
        onBackground = scheme.onBackground.withContrastAgainst(scheme.background),
        onSurface = scheme.onSurface.withContrastAgainstAll(surfaces),
        onSurfaceVariant = scheme.onSurfaceVariant.withContrastAgainstAll(surfaces),
        surfaceTint = if (scheme.surfaceTint == scheme.primary) primary else scheme.surfaceTint,
        inverseOnSurface = scheme.inverseOnSurface.withContrastAgainst(scheme.inverseSurface),
        error = error,
        onError = scheme.onError.withContrastAgainst(error),
        errorContainer = errorContainer,
        onErrorContainer = scheme.onErrorContainer.withContrastAgainst(errorContainer),
        onPrimaryFixed = scheme.onPrimaryFixed.withContrastAgainstAll(listOf(scheme.primaryFixed, scheme.primaryFixedDim)),
        onSecondaryFixed = scheme.onSecondaryFixed.withContrastAgainstAll(listOf(scheme.secondaryFixed, scheme.secondaryFixedDim)),
        onTertiaryFixed = scheme.onTertiaryFixed.withContrastAgainstAll(listOf(scheme.tertiaryFixed, scheme.tertiaryFixedDim))
    )
}

/** Contrast of black on white; also reported for pairs with an unset role. */
private const val MaxContrast = 21f

/** Bisection steps of the colour searches (2^-16 precision, far below 8-bit quantisation). */
private const val SearchSteps = 16
