package pl.lublin.wsei.plan.ui.adaptive

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.separatingVerticalHingeBounds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import kotlin.math.roundToInt

/**
 * Responsive layout foundation for phones, tablets and foldables.
 *
 * The app shell computes a [WseiWindowLayout] once from `currentWindowAdaptiveInfo()` and
 * provides it through [LocalWseiWindowLayout]; screens read it instead of measuring the window.
 */
enum class WseiWidthClass { COMPACT, MEDIUM, EXPANDED }

@Immutable
data class WseiWindowLayout(
    val widthClass: WseiWidthClass,
    /** True when the shell shows a side navigation rail instead of the floating bottom bar. */
    val useNavigationRail: Boolean,
    /** Half-opened foldable lying flat (horizontal hinge). */
    val isTabletop: Boolean,
    /** Vertical hinge bounds (window coordinates, px) that physically split the screen. */
    val separatingVerticalHinges: List<Rect>
) {
    val isCompact: Boolean get() = widthClass == WseiWidthClass.COMPACT

    /** Two panes side by side make sense on expanded windows and on unfolded book-style foldables. */
    val supportsTwoPane: Boolean
        get() = widthClass == WseiWidthClass.EXPANDED || separatingVerticalHinges.isNotEmpty()

    /** Suggested number of columns for card / tile grids. */
    val gridColumns: Int
        get() = when (widthClass) {
            WseiWidthClass.COMPACT -> 1
            WseiWidthClass.MEDIUM -> 2
            WseiWidthClass.EXPANDED -> 3
        }

    companion object {
        val Compact = WseiWindowLayout(
            widthClass = WseiWidthClass.COMPACT,
            useNavigationRail = false,
            isTabletop = false,
            separatingVerticalHinges = emptyList()
        )
    }
}

val LocalWseiWindowLayout = staticCompositionLocalOf { WseiWindowLayout.Compact }

/** Maximum width of single-column, text-heavy content on large screens. */
val WseiReadableContentMaxWidth: Dp = 840.dp

@Composable
fun rememberWseiWindowLayout(): WseiWindowLayout {
    val info = currentWindowAdaptiveInfo()
    val sizeClass = info.windowSizeClass
    val posture = info.windowPosture
    val widthClass = when {
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> WseiWidthClass.EXPANDED
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> WseiWidthClass.MEDIUM
        else -> WseiWidthClass.COMPACT
    }
    val hinges = posture.separatingVerticalHingeBounds
    // Medium and wider windows (tablets, unfolded foldables, phones in landscape) get the side
    // rail — it also saves vertical space on short landscape windows.
    val useRail = widthClass != WseiWidthClass.COMPACT
    return remember(widthClass, useRail, posture.isTabletop, hinges) {
        WseiWindowLayout(
            widthClass = widthClass,
            useNavigationRail = useRail,
            isTabletop = posture.isTabletop,
            separatingVerticalHinges = hinges
        )
    }
}

/**
 * Bottom space a scrolling list must leave free: the floating navigation bar on phones,
 * only the system navigation bar (+ breathing room) when the rail is used.
 */
@Composable
fun wseiBottomClearance(): Dp {
    val layout = LocalWseiWindowLayout.current
    return if (layout.useNavigationRail) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + WseiSpacing.xxl
    } else {
        WseiSpacing.bottomBarClearance
    }
}

/** Adaptive replacement for [WseiSpacing.screenContent]. */
@Composable
fun wseiScreenContentPadding(): PaddingValues {
    val layout = LocalWseiWindowLayout.current
    val horizontal = if (layout.isCompact) WseiSpacing.screen else WseiSpacing.xxl
    return PaddingValues(
        start = horizontal,
        end = horizontal,
        top = WseiSpacing.xs,
        bottom = wseiBottomClearance()
    )
}

/** Centers content and caps its width so cards are not stretched edge to edge on tablets. */
fun Modifier.wseiReadableWidth(maxWidth: Dp = WseiReadableContentMaxWidth): Modifier =
    this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = maxWidth)

/**
 * Two panes side by side. When a separating vertical hinge (book-style foldable) crosses
 * this layout, the split snaps to the hinge so no content lands on the fold; otherwise the
 * first pane takes [firstPaneFraction] of the width with a [gap] in between.
 */
@Composable
fun WseiTwoPane(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    firstPaneFraction: Float = 0.42f,
    gap: Dp = WseiSpacing.l
) {
    val hinges = LocalWseiWindowLayout.current.separatingVerticalHinges
    var originX by remember { mutableStateOf(0f) }
    Layout(
        contents = listOf(first, second),
        modifier = modifier.onGloballyPositioned { originX = it.positionInWindow().x }
    ) { (firstMeasurables, secondMeasurables), constraints ->
        val width = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val hinge = hinges.firstOrNull { it.left - originX > 0 && it.right - originX < width }
        val firstWidth: Int
        val secondStart: Int
        if (hinge != null) {
            firstWidth = (hinge.left - originX).roundToInt().coerceIn(0, width)
            secondStart = (hinge.right - originX).roundToInt().coerceIn(firstWidth, width)
        } else {
            firstWidth = ((width - gapPx) * firstPaneFraction).roundToInt()
            secondStart = firstWidth + gapPx
        }
        val secondWidth = (width - secondStart).coerceAtLeast(0)
        val height = constraints.maxHeight
        fun paneConstraints(w: Int) = Constraints(
            minWidth = w, maxWidth = w,
            minHeight = if (constraints.hasBoundedHeight) height else 0,
            maxHeight = height
        )
        val firstPlaceables = firstMeasurables.map { it.measure(paneConstraints(firstWidth)) }
        val secondPlaceables = secondMeasurables.map { it.measure(paneConstraints(secondWidth)) }
        val layoutHeight = if (constraints.hasBoundedHeight) height else
            (firstPlaceables + secondPlaceables).maxOfOrNull { it.height } ?: 0
        layout(width, layoutHeight) {
            firstPlaceables.forEach { it.placeRelative(0, 0) }
            secondPlaceables.forEach { it.placeRelative(secondStart, 0) }
        }
    }
}
