package pl.lublin.wsei.plan.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Wspólne tokeny odstępów, zaokrągleń i ruchu. Jedno miejsce zamiast „magicznych liczb”
 * rozsianych po ekranach — zmiana tutaj zmienia wygląd całej aplikacji.
 */
object WseiSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val s: Dp = 8.dp
    val m: Dp = 12.dp
    val l: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp

    /** Margines boczny ekranu. */
    val screen: Dp = 16.dp

    /** Odstęp między kartami na liście. */
    val betweenCards: Dp = 12.dp

    /** Odstęp między elementami połączonej grupy listy (M3 Expressive „segmented list”). */
    val inGroup: Dp = 2.dp

    /** Miejsce na pływający pasek nawigacji nad dolną krawędzią. */
    val bottomBarClearance: Dp = 120.dp

    val screenContent = PaddingValues(
        start = screen,
        end = screen,
        top = xs,
        bottom = bottomBarClearance
    )
}

object WseiCorners {
    /** Karty treści. */
    val card: Dp = 28.dp

    /** Karty „hero” na górze ekranu. */
    val hero: Dp = 36.dp

    /** Zewnętrzne rogi połączonej grupy listy. */
    val groupOuter: Dp = 24.dp

    /** Wewnętrzne rogi połączonej grupy listy. */
    val groupInner: Dp = 6.dp

    /** Kontenery wewnątrz kart. */
    val inset: Dp = 18.dp
}

val WseiCardShape = RoundedCornerShape(WseiCorners.card)
val WseiHeroShape = RoundedCornerShape(WseiCorners.hero)
val WseiInsetShape = RoundedCornerShape(WseiCorners.inset)

/**
 * Kształt elementu połączonej grupy: zewnętrzne rogi duże, wewnętrzne małe.
 */
fun wseiGroupItemShape(
    index: Int,
    count: Int,
    outer: Dp = WseiCorners.groupOuter,
    inner: Dp = WseiCorners.groupInner
): RoundedCornerShape {
    val top = if (index == 0) outer else inner
    val bottom = if (index == count - 1) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/**
 * Sprężyny w duchu `MotionScheme.expressive()`:
 * - [spatial] — ruch i zmiana kształtu, z lekkim odbiciem,
 * - [fastSpatial] — reakcje na dotyk (wciśnięcie, morfing rogów),
 * - [size] — zmiana rozmiaru bez odbicia (bez migania przy rozwijaniu kart),
 * - [effects] — kolor i przezroczystość, bez odbicia.
 */
object WseiMotion {
    fun <T> spatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)
    fun <T> size(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    fun <T> effects(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
}
