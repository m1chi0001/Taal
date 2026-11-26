package pl.lublin.wsei.plan.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MotionScheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared spacing, corner and motion tokens: one place instead of "magic numbers" spread over
 * the screens — a change here restyles the whole app.
 */
object WseiSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val s: Dp = 8.dp
    val m: Dp = 12.dp
    val l: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp

    /** Horizontal screen margin. */
    val screen: Dp = 16.dp

    /** Gap between cards in a list. */
    val betweenCards: Dp = 12.dp

    /** Gap between items of a connected list group (M3 Expressive "segmented list"). */
    val inGroup: Dp = 2.dp

    /** Room for the floating navigation bar above the bottom edge. */
    val bottomBarClearance: Dp = 120.dp

    /** Maximum width of forms (sign-in, welcome) on tablets and in landscape. */
    val formMaxWidth: Dp = 480.dp

    val screenContent = PaddingValues(
        start = screen,
        end = screen,
        top = xs,
        bottom = bottomBarClearance
    )
}

object WseiCorners {
    /** Content cards. */
    val card: Dp = 28.dp

    /** Hero cards at the top of a screen. */
    val hero: Dp = 36.dp

    /** Outer corners of a connected list group. */
    val groupOuter: Dp = 24.dp

    /** Inner corners of a connected list group. */
    val groupInner: Dp = 6.dp

    /** Containers inside cards. */
    val inset: Dp = 18.dp
}

val WseiCardShape = RoundedCornerShape(WseiCorners.card)
val WseiHeroShape = RoundedCornerShape(WseiCorners.hero)
val WseiInsetShape = RoundedCornerShape(WseiCorners.inset)

/**
 * Shape of a connected group item: large outer corners, small inner ones.
 */
fun wseiGroupItemShape(
    index: Int,
    count: Int,
    outer: Dp = WseiCorners.groupOuter,
    inner: Dp = WseiCorners.groupInner
): RoundedCornerShape {
    val top = if (index <= 0) outer else inner
    val bottom = if (index >= count - 1) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/**
 * Shape of a horizontal connected group item (e.g. a 3-stat strip): large outer start/end
 * corners, small inner ones.
 */
fun wseiHorizontalGroupItemShape(
    index: Int,
    count: Int,
    outer: Dp = 22.dp,
    inner: Dp = WseiCorners.groupInner
): RoundedCornerShape {
    val start = if (index <= 0) outer else inner
    val end = if (index >= count - 1) outer else inner
    return RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
}

/**
 * Springs in the spirit of `MotionScheme.expressive()`, but faster — the UI reacts at once
 * while keeping the springy M3 Expressive character (no tween/linear):
 * - [spatial] — movement and shape change, with a slight bounce,
 * - [fastSpatial] — touch feedback (press, corner morph, pill expansion),
 * - [size] — size changes without bounce (no flicker when cards expand),
 * - [effects] — colour and alpha, no bounce,
 * - [fastEffects] — elements fading out to make room for new content.
 */
object WseiMotion {
    /** Default spatial spring: ~1.8× stiffer than expressive's 380, same 0.8 damping. */
    const val SpatialStiffness: Float = 700f
    const val SpatialDamping: Float = 0.8f

    /** Touch feedback spring: ~1.75× stiffer than expressive's fast spatial (800). */
    const val FastSpatialStiffness: Float = 1400f
    const val FastSpatialDamping: Float = 0.7f

    /** Size changes: between `StiffnessMedium` and `StiffnessMediumLow`, never bouncy. */
    const val SizeStiffness: Float = 1000f

    /** Colour/alpha: "medium-high" stiffness (Compose has no such constant; 2× `StiffnessMedium`). */
    const val EffectsStiffness: Float = 3000f

    fun <T> spatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = SpatialDamping, stiffness = SpatialStiffness)

    fun <T> fastSpatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = FastSpatialDamping, stiffness = FastSpatialStiffness)

    fun <T> size(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = SizeStiffness)

    fun <T> effects(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = EffectsStiffness)

    fun <T> fastEffects(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
}

/**
 * Snappier variant of [MotionScheme.expressive] used by the app theme, so Material components
 * (button-group morphs, toggle buttons, loading indicators, top app bars) move at the same pace
 * as [WseiMotion]. Spatial springs keep expressive's damping (the characteristic overshoot) with
 * higher stiffness; effects springs are already fast and are delegated to expressive unchanged.
 */
@Suppress("UNCHECKED_CAST")
object WseiMotionScheme : MotionScheme {
    private val expressive: MotionScheme = MotionScheme.expressive()

    private val defaultSpatial = spring<Any?>(
        dampingRatio = WseiMotion.SpatialDamping,
        stiffness = WseiMotion.SpatialStiffness
    )
    private val fastSpatial = spring<Any?>(
        dampingRatio = 0.6f,
        stiffness = WseiMotion.FastSpatialStiffness
    )
    private val slowSpatial = spring<Any?>(dampingRatio = 0.8f, stiffness = 380f)

    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = defaultSpatial as FiniteAnimationSpec<T>

    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = fastSpatial as FiniteAnimationSpec<T>

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = slowSpatial as FiniteAnimationSpec<T>

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = expressive.defaultEffectsSpec()

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = expressive.fastEffectsSpec()

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = expressive.slowEffectsSpec()
}
