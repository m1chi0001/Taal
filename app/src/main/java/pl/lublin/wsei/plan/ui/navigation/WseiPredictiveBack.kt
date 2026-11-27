package pl.lublin.wsei.plan.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.ui.adaptive.LocalWseiFloatingBarVisible
import pl.lublin.wsei.plan.ui.theme.WseiMotion

/**
 * Typed route names for the single app-level `NavHost` in [WseiAppShell].
 *
 * Top-level routes show the floating navigation bar on compact windows; detail sub-routes hide it
 * outside `NavHost` so the bar never scales with or appears inside detail screens during
 * Predictive Back gestures.
 */
internal object WseiRoute {
    const val START = "start"
    const val SCHEDULE = "schedule"
    const val SUBJECTS = "subjects"
    const val FINANCES = "finances"
    const val MORE = "more"

    const val ANNOUNCEMENTS = "more/announcements"
    const val SETTINGS = "more/settings"
    const val ABOUT = "more/about"
    const val PROFILE = "profile"

    val TopLevelOrdered: List<String> = listOf(START, SCHEDULE, SUBJECTS, FINANCES, MORE)
    val TopLevelSet: Set<String> = TopLevelOrdered.toSet()
    val MoreSubRoutes: Set<String> = setOf(ANNOUNCEMENTS, SETTINGS, ABOUT)
}

/** True only on the five primary tabs (`Start`, `Plan`, `Przedmioty`, `Finanse`, `Więcej` hub). */
internal fun isTopLevelRoute(route: String?): Boolean = route != null && route in WseiRoute.TopLevelSet

/** Maps any active `NavHost` route (including `more/...` sub-routes) to its highlighted tab/rail item. */
internal fun railDestinationForRoute(route: String?): WseiDestination = when (route) {
    WseiRoute.START, null -> WseiDestination.START
    WseiRoute.SCHEDULE -> WseiDestination.SCHEDULE
    WseiRoute.SUBJECTS -> WseiDestination.SUBJECTS
    WseiRoute.FINANCES -> WseiDestination.FINANCES
    WseiRoute.MORE, in WseiRoute.MoreSubRoutes -> WseiDestination.MORE
    WseiRoute.PROFILE -> WseiDestination.PROFILE
    else -> WseiDestination.START
}

internal fun destinationToRoute(destination: WseiDestination): String = when (destination) {
    WseiDestination.START -> WseiRoute.START
    WseiDestination.SCHEDULE -> WseiRoute.SCHEDULE
    WseiDestination.SUBJECTS -> WseiRoute.SUBJECTS
    WseiDestination.FINANCES -> WseiRoute.FINANCES
    WseiDestination.MORE -> WseiRoute.MORE
    WseiDestination.PROFILE -> WseiRoute.PROFILE
}

internal fun moreSubViewToRoute(subView: MoreSubView): String = when (subView) {
    MoreSubView.HUB -> WseiRoute.MORE
    MoreSubView.ANNOUNCEMENTS -> WseiRoute.ANNOUNCEMENTS
    MoreSubView.SETTINGS -> WseiRoute.SETTINGS
    MoreSubView.ABOUT -> WseiRoute.ABOUT
}

internal fun routeToMoreSubView(route: String?): MoreSubView = when (route) {
    WseiRoute.ANNOUNCEMENTS -> MoreSubView.ANNOUNCEMENTS
    WseiRoute.SETTINGS -> MoreSubView.SETTINGS
    WseiRoute.ABOUT -> MoreSubView.ABOUT
    else -> MoreSubView.HUB
}

/**
 * Direction of travel when moving between two top-level tabs (`true` = moving toward the end of
 * the bar, `false` = moving back toward `Start`). Entering a sub-screen (`Profile` or `more/...`)
 * is always treated as a forward hierarchical push (`true`).
 */
internal fun isForwardRouteTransition(initialRoute: String?, targetRoute: String?): Boolean {
    if (targetRoute == null || !isTopLevelRoute(targetRoute)) return true
    if (initialRoute == null || !isTopLevelRoute(initialRoute)) return false
    val initialIndex = WseiRoute.TopLevelOrdered.indexOf(initialRoute)
    val targetIndex = WseiRoute.TopLevelOrdered.indexOf(targetRoute)
    return targetIndex >= initialIndex
}

/**
 * Opaque full-screen container for every `NavHost` destination:
 * - guarantees a 100% opaque `MaterialTheme.colorScheme.background` surface so previous screens
 *   on the back stack never bleed/ghost through during Predictive Back or tab transitions,
 * - synchronises subtle corner rounding (`0.dp -> 24.dp`) and elevation (`0.dp -> 16.dp`) with
 *   `NavHost`'s `SeekableTransitionState` while a screen is being popped off the top,
 * - scopes [LocalWseiFloatingBarVisible] per destination so bottom list padding stays constant
 *   throughout push/pop animations.
 */
@Composable
internal fun WseiDestinationSurface(
    scope: AnimatedContentScope,
    floatingBarVisible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val cornerRadius by scope.transition.animateDp(
        transitionSpec = { WseiMotion.navigation() },
        label = "destinationCorner"
    ) { state ->
        when (state) {
            EnterExitState.PreEnter -> 0.dp
            EnterExitState.Visible -> 0.dp
            EnterExitState.PostExit -> 24.dp
        }
    }
    val elevation by scope.transition.animateDp(
        transitionSpec = { WseiMotion.navigation() },
        label = "destinationElevation"
    ) { state ->
        when (state) {
            EnterExitState.PreEnter -> 10.dp
            EnterExitState.Visible -> 0.dp
            EnterExitState.PostExit -> 16.dp
        }
    }

    CompositionLocalProvider(LocalWseiFloatingBarVisible provides floatingBarVisible) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    val cornerPx = cornerRadius.toPx()
                    val elevationPx = elevation.toPx()
                    if (cornerPx > 1f || elevationPx > 0.5f) {
                        shape = if (cornerPx > 1f) RoundedCornerShape(cornerRadius) else RectangleShape
                        clip = cornerPx > 1f
                        shadowElevation = elevationPx
                    }
                }
                .clipToBounds(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content
        )
    }
}

/**
 * Opaque spatial enter transition for `NavHost`: slides in at 100% opacity (no `fadeIn` cross-fade
 * that would let text on the outgoing screen ghost through).
 */
internal fun wseiNavEnterTransition(initialRoute: String?, targetRoute: String?): EnterTransition {
    val forward = isForwardRouteTransition(initialRoute, targetRoute)
    return slideInHorizontally(WseiMotion.navigation()) { fullWidth ->
        if (forward) fullWidth else -fullWidth
    }
}

/**
 * Opaque spatial exit transition for `NavHost`: recedes with subtle parallax (and gentle scale
 * when pushing a detail sub-screen) at 100% opacity.
 */
internal fun wseiNavExitTransition(initialRoute: String?, targetRoute: String?): ExitTransition {
    val forward = isForwardRouteTransition(initialRoute, targetRoute)
    val slide = slideOutHorizontally(WseiMotion.navigation()) { fullWidth ->
        if (forward) -fullWidth / 4 else fullWidth / 4
    }
    return if (!isTopLevelRoute(targetRoute)) {
        slide + scaleOut(WseiMotion.navigation(), targetScale = 0.95f)
    } else {
        slide
    }
}

/**
 * Opaque `popEnterTransition` driven by `NavHost`'s `SeekableTransitionState` during Predictive
 * Back: the underlying destination returns from `-25%` parallax and `95%` scale at 100% opacity.
 */
internal fun wseiNavPopEnterTransition(): EnterTransition =
    slideInHorizontally(WseiMotion.navigation()) { fullWidth -> -fullWidth / 4 } +
        scaleIn(WseiMotion.navigation(), initialScale = 0.95f)

/**
 * Opaque `popExitTransition` driven by `NavHost`'s `SeekableTransitionState` during Predictive
 * Back: the foreground screen shrinks to `90%` and slides completely off the end edge at 100%
 * opacity — never turning semi-transparent over the destination beneath it.
 */
internal fun wseiNavPopExitTransition(): ExitTransition =
    slideOutHorizontally(WseiMotion.navigation()) { fullWidth -> fullWidth } +
        scaleOut(WseiMotion.navigation(), targetScale = 0.90f)
