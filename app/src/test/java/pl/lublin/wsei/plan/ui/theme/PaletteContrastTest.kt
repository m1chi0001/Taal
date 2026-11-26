package pl.lublin.wsei.plan.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.lublin.wsei.plan.domain.model.AppColorPalette

/**
 * WCAG AA checks of every curated palette (light and dark) and of the [ensureContrast] guard
 * that also repairs wallpaper-based (dynamic) schemes.
 */
class PaletteContrastTest {

    private val variants: List<Pair<AppColorPalette, Boolean>> =
        WseiStaticPalettes.flatMap { palette -> listOf(palette to false, palette to true) }

    @Test
    fun `every curated palette meets WCAG AA for all role pairs`() {
        val failures = variants.flatMap { (palette, isDark) ->
            wseiStaticColorScheme(palette, isDark).contrastRequirements()
                .filterNot { it.passes }
                .map { "$palette/${if (isDark) "dark" else "light"} ${it.name} = ${"%.2f".format(it.ratio)}" }
        }
        assertTrue("Pairs below WCAG AA:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `key pairs of every palette reach text and icon contrast`() {
        variants.forEach { (palette, isDark) ->
            val s = wseiStaticColorScheme(palette, isDark)
            val label = "$palette/${if (isDark) "dark" else "light"}"
            listOf(
                "primary/onPrimary" to contrastRatio(s.onPrimary, s.primary),
                "primaryContainer/onPrimaryContainer" to contrastRatio(s.onPrimaryContainer, s.primaryContainer),
                "secondaryContainer/onSecondaryContainer" to contrastRatio(s.onSecondaryContainer, s.secondaryContainer),
                "tertiaryContainer/onTertiaryContainer" to contrastRatio(s.onTertiaryContainer, s.tertiaryContainer),
                "surfaceContainerHigh/onSurface" to contrastRatio(s.onSurface, s.surfaceContainerHigh),
                "surfaceContainerHighest/onSurfaceVariant" to contrastRatio(s.onSurfaceVariant, s.surfaceContainerHighest),
                "errorContainer/onErrorContainer" to contrastRatio(s.onErrorContainer, s.errorContainer)
            ).forEach { (pair, ratio) ->
                assertTrue("$label $pair = $ratio", ratio >= WcagTextContrast)
            }
            listOf(s.surfaceContainerLow, s.surfaceContainer, s.surfaceContainerHigh, s.surfaceContainerHighest).forEach { bg ->
                assertTrue("$label primary icon on surface container", contrastRatio(s.primary, bg) >= WcagNonTextContrast)
            }
            // Inverted container pair used for icon bubbles on tinted tiles must never blend.
            assertTrue(
                "$label icon bubble on primaryContainer",
                contrastRatio(s.primaryContainer, s.onPrimaryContainer) >= WcagNonTextContrast
            )
        }
    }

    @Test
    fun `dynamic palette falls back to Taal`() {
        val dynamic = wseiStaticColorScheme(AppColorPalette.DYNAMIC, isDark = true)
        val taal = wseiStaticColorScheme(AppColorPalette.TAAL, isDark = true)
        assertEquals(taal.primary, dynamic.primary)
        assertEquals(taal.surface, dynamic.surface)
    }

    @Test
    fun `ensureContrast leaves passing schemes untouched`() {
        variants.forEach { (palette, isDark) ->
            val scheme = wseiStaticColorScheme(palette, isDark)
            assertSameRoles(scheme, ensureContrast(scheme))
        }
    }

    @Test
    fun `neon container with white text is repaired`() {
        val neon = Color(0xFF00E5FF)
        assertTrue(contrastRatio(Color.White, neon) < WcagTextContrast)

        val fixedOn = Color.White.withContrastAgainst(neon)
        assertTrue(contrastRatio(fixedOn, neon) >= WcagTextContrast)

        val scheme = ensureContrast(
            darkColorScheme(
                primary = neon,
                onPrimary = Color.White,
                primaryContainer = neon,
                onPrimaryContainer = Color.White,
                secondaryContainer = Color(0xFF2979FF),
                onSecondaryContainer = Color(0xFF2962FF),
                tertiaryContainer = Color(0xFFFF4081),
                onTertiaryContainer = Color.White
            )
        )
        assertTrue(contrastRatio(scheme.onPrimary, scheme.primary) >= WcagTextContrast)
        assertTrue(contrastRatio(scheme.onPrimaryContainer, scheme.primaryContainer) >= WcagTextContrast)
        assertTrue(contrastRatio(scheme.onSecondaryContainer, scheme.secondaryContainer) >= WcagTextContrast)
        assertTrue(contrastRatio(scheme.onTertiaryContainer, scheme.tertiaryContainer) >= WcagTextContrast)
        // Dark theme containers are pulled out of the neon range.
        listOf(scheme.primaryContainer, scheme.secondaryContainer, scheme.tertiaryContainer).forEach {
            assertTrue(contrastRatio(it, Color.Black) <= contrastRatio(Color(0xFF00E5FF), Color.Black))
        }
        val failures = scheme.contrastRequirements().filterNot { it.passes }.map { it.name }
        assertTrue("Still failing: $failures", failures.isEmpty())
    }

    @Test
    fun `high chroma light scheme is repaired`() {
        val scheme = ensureContrast(
            lightColorScheme(
                primary = Color(0xFF40C4FF),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF0050FF),
                onPrimaryContainer = Color(0xFF0030AA),
                secondary = Color(0xFFFFEB3B),
                onSecondary = Color.White
            )
        )
        val failures = scheme.contrastRequirements().filterNot { it.passes }.map { it.name }
        assertTrue("Still failing: $failures", failures.isEmpty())
    }

    @Test
    fun `ensureContrast is idempotent`() {
        val once = ensureContrast(
            darkColorScheme(
                primary = Color(0xFF00E5FF),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF00B8D4),
                onPrimaryContainer = Color.White,
                surfaceContainerHighest = Color(0xFF5A5A5A),
                onSurfaceVariant = Color(0xFF8A8A8A)
            )
        )
        assertSameRoles(once, ensureContrast(once))
    }

    @Test
    fun `muted content keeps text contrast`() {
        val container = Color(0xFF00B8D4)
        val muted = mutedContentColor(content = Color(0xFF00363D), background = container)
        assertTrue(contrastRatio(muted, container) >= WcagTextContrast)
        val layer = tonalLayer(container, Color(0xFF00363D), 0.1f)
        assertTrue(contrastRatio(Color(0xFF00363D).withContrastAgainst(layer), layer) >= WcagTextContrast)
    }

    private fun assertSameRoles(expected: ColorScheme, actual: ColorScheme) {
        val roles: List<(ColorScheme) -> Color> = listOf(
            { it.primary }, { it.onPrimary }, { it.primaryContainer }, { it.onPrimaryContainer },
            { it.secondary }, { it.onSecondary }, { it.secondaryContainer }, { it.onSecondaryContainer },
            { it.tertiary }, { it.onTertiary }, { it.tertiaryContainer }, { it.onTertiaryContainer },
            { it.error }, { it.onError }, { it.errorContainer }, { it.onErrorContainer },
            { it.background }, { it.onBackground }, { it.surface }, { it.onSurface }, { it.onSurfaceVariant },
            { it.inversePrimary }, { it.inverseOnSurface }, { it.onPrimaryFixed }, { it.onSecondaryFixed },
            { it.onTertiaryFixed }, { it.surfaceTint }
        )
        roles.forEachIndexed { index, role -> assertEquals("role #$index", role(expected), role(actual)) }
    }
}
