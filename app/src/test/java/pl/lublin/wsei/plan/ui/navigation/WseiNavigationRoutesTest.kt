package pl.lublin.wsei.plan.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.lublin.wsei.plan.domain.update.VersionComparator

class WseiNavigationRoutesTest {

    @Test
    fun `isTopLevelRoute is true only for primary tabs and false for detail sub-screens`() {
        assertTrue(isTopLevelRoute(WseiRoute.START))
        assertTrue(isTopLevelRoute(WseiRoute.SCHEDULE))
        assertTrue(isTopLevelRoute(WseiRoute.SUBJECTS))
        assertTrue(isTopLevelRoute(WseiRoute.FINANCES))
        assertTrue(isTopLevelRoute(WseiRoute.MORE))

        assertFalse(isTopLevelRoute(WseiRoute.ANNOUNCEMENTS))
        assertFalse(isTopLevelRoute(WseiRoute.SETTINGS))
        assertFalse(isTopLevelRoute(WseiRoute.ABOUT))
        assertFalse(isTopLevelRoute(WseiRoute.PROFILE))
        assertFalse(isTopLevelRoute(null))
    }

    @Test
    fun `doesRouteHierarchySelectTab selects exactly one tab and switches immediately from More to Start`() {
        // Start -> Więcej -> Ustawienia -> Więcej -> Start
        val onMoreHierarchy = sequenceOf(WseiRoute.MORE, null)
        assertTrue(doesRouteHierarchySelectTab(onMoreHierarchy, WseiDestination.MORE))
        assertFalse(doesRouteHierarchySelectTab(onMoreHierarchy, WseiDestination.START))

        val onSettingsHierarchy = sequenceOf(WseiRoute.SETTINGS, null)
        assertTrue(doesRouteHierarchySelectTab(onSettingsHierarchy, WseiDestination.MORE))
        assertFalse(doesRouteHierarchySelectTab(onSettingsHierarchy, WseiDestination.START))

        val backOnStartHierarchy = sequenceOf(WseiRoute.START, null)
        assertTrue(doesRouteHierarchySelectTab(backOnStartHierarchy, WseiDestination.START))
        assertFalse(doesRouteHierarchySelectTab(backOnStartHierarchy, WseiDestination.MORE))
        assertFalse(doesRouteHierarchySelectTab(backOnStartHierarchy, WseiDestination.SUBJECTS))
    }

    @Test
    fun `railDestinationForRoute keeps More selected while inside More sub-routes`() {
        assertEquals(WseiDestination.START, railDestinationForRoute(WseiRoute.START))
        assertEquals(WseiDestination.SCHEDULE, railDestinationForRoute(WseiRoute.SCHEDULE))
        assertEquals(WseiDestination.SUBJECTS, railDestinationForRoute(WseiRoute.SUBJECTS))
        assertEquals(WseiDestination.FINANCES, railDestinationForRoute(WseiRoute.FINANCES))
        assertEquals(WseiDestination.MORE, railDestinationForRoute(WseiRoute.MORE))
        assertEquals(WseiDestination.MORE, railDestinationForRoute(WseiRoute.ANNOUNCEMENTS))
        assertEquals(WseiDestination.MORE, railDestinationForRoute(WseiRoute.SETTINGS))
        assertEquals(WseiDestination.MORE, railDestinationForRoute(WseiRoute.ABOUT))
        assertEquals(WseiDestination.PROFILE, railDestinationForRoute(WseiRoute.PROFILE))
    }

    @Test
    fun `moreSubViewToRoute and routeToMoreSubView round-trip cleanly across fold and unfold`() {
        for (subView in listOf(MoreSubView.ANNOUNCEMENTS, MoreSubView.SETTINGS, MoreSubView.ABOUT)) {
            val route = moreSubViewToRoute(subView)
            assertEquals(subView, routeToMoreSubView(route))
        }
        assertEquals(MoreSubView.HUB, routeToMoreSubView(WseiRoute.MORE))
        assertEquals(WseiRoute.MORE, moreSubViewToRoute(MoreSubView.HUB))
    }

    @Test
    fun `isForwardRouteTransition follows tab order and treats sub-screens as forward pushes`() {
        assertTrue(isForwardRouteTransition(WseiRoute.START, WseiRoute.SCHEDULE))
        assertTrue(isForwardRouteTransition(WseiRoute.SCHEDULE, WseiRoute.MORE))
        assertFalse(isForwardRouteTransition(WseiRoute.MORE, WseiRoute.START))
        assertFalse(isForwardRouteTransition(WseiRoute.FINANCES, WseiRoute.SCHEDULE))

        // Detail sub-screens always push forward and return backward.
        assertTrue(isForwardRouteTransition(WseiRoute.MORE, WseiRoute.ABOUT))
        assertTrue(isForwardRouteTransition(WseiRoute.FINANCES, WseiRoute.PROFILE))
        assertFalse(isForwardRouteTransition(WseiRoute.ABOUT, WseiRoute.MORE))
    }

    @Test
    fun `version 0_8_3 compares as newer than 0_8_2_1 and 0_8_2`() {
        assertTrue(VersionComparator.compare("0.8.2.1", "0.8.2") > 0)
        assertTrue(VersionComparator.compare("0.8.3", "0.8.2.1") > 0)
        assertTrue(VersionComparator.compare("v0.8.3", "v0.8.2.1") > 0)
        assertEquals(0, VersionComparator.compare("0.8.3", "0.8.3"))
    }
}
