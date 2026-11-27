@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailItemDefaults
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.ui.components.StudentAvatar
import pl.lublin.wsei.plan.ui.components.WseiProfileShortcut
import pl.lublin.wsei.plan.ui.components.wseiPressScale
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing

/** Tabs shown in the navigation bar (Profile opens from the avatar in the top bar). */
internal val WseiNavigationTabs: List<WseiDestination> = listOf(
    WseiDestination.START,
    WseiDestination.SCHEDULE,
    WseiDestination.SUBJECTS,
    WseiDestination.FINANCES,
    WseiDestination.MORE
)

/**
 * Floating navigation bar in the M3 Expressive style (floating toolbar): the active tab expands
 * into a pill with icon and label, the others are icons only. Width, colour and press animate
 * on springs.
 *
 * Selection state is derived directly from [navController] (`rememberActiveBackStackEntry` +
 * `NavDestination.hierarchy`) so Predictive Back gestures never desynchronise or lock the bar.
 */
@Composable
internal fun WseiFloatingNavigationBar(
    navController: NavHostController,
    unreadAnnouncementsCount: Int,
    modifier: Modifier = Modifier,
    onResetMoreTwoPane: () -> Unit = {}
) {
    val activeEntry = navController.rememberActiveBackStackEntry()
    val currentDestination = activeEntry?.destination

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp,
        modifier = modifier.height(68.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WseiNavigationTabs.forEach { destination ->
                val isSelected = currentDestination.isTabSelected(destination)
                NavigationPillItem(
                    destination = destination,
                    selected = isSelected,
                    showBadge = destination == WseiDestination.MORE && unreadAnnouncementsCount > 0,
                    onClick = {
                        navController.navigateToTopLevelTab(
                            destination = destination,
                            onResetMoreTwoPane = onResetMoreTwoPane
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun NavigationPillItem(
    destination: WseiDestination,
    selected: Boolean,
    showBadge: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else colors.primaryContainer.copy(alpha = 0f),
        animationSpec = WseiMotion.effects(),
        label = "navItemContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        animationSpec = WseiMotion.effects(),
        label = "navItemContent"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val destinationDescription = stringResource(destination.contentDescriptionRes)

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
        modifier = Modifier
            .height(52.dp)
            .widthIn(min = 52.dp)
            .wseiPressScale(interactionSource, pressedScale = 0.88f)
            .semantics {
                role = Role.Tab
                this.selected = selected
                contentDescription = destinationDescription
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box {
                Icon(
                    imageVector = destination.iconVector,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                if (showBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (selected) colors.onPrimaryContainer else colors.error)
                    )
                }
            }
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(animationSpec = WseiMotion.fastSpatial()) + fadeIn(WseiMotion.effects()),
                exit = shrinkHorizontally(animationSpec = WseiMotion.size()) + fadeOut(WseiMotion.fastEffects())
            ) {
                Text(
                    text = stringResource(destination.shortLabelRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/**
 * Side navigation for tablets, unfolded foldables and landscape phones (M3 Expressive
 * `WideNavigationRail`) on a subtle `surfaceContainerLow` rail surface.
 *
 * Selection state is derived directly from [navController] (`rememberActiveBackStackEntry` +
 * `NavDestination.hierarchy`).
 */
@Composable
internal fun WseiNavigationRail(
    navController: NavHostController,
    unreadAnnouncementsCount: Int,
    expanded: Boolean,
    profileShortcut: WseiProfileShortcut?,
    modifier: Modifier = Modifier,
    onResetMoreTwoPane: () -> Unit = {}
) {
    val activeEntry = navController.rememberActiveBackStackEntry()
    val currentDestination = activeEntry?.destination

    val railState = rememberWideNavigationRailState(
        initialValue = if (expanded) WideNavigationRailValue.Expanded else WideNavigationRailValue.Collapsed
    )
    LaunchedEffect(expanded) {
        if (expanded) railState.expand() else railState.collapse()
    }
    val railExpanded = railState.targetValue == WideNavigationRailValue.Expanded
    val colors = MaterialTheme.colorScheme
    val itemColors = WideNavigationRailItemDefaults.colors(
        selectedIconColor = colors.onSecondaryContainer,
        selectedTextColorTopIconPosition = colors.onSurface,
        selectedTextColorStartIconPosition = colors.onSecondaryContainer,
        selectedIndicatorColor = colors.secondaryContainer,
        unselectedIconColor = colors.onSurfaceVariant,
        unselectedTextColor = colors.onSurfaceVariant
    )
    val header: (@Composable () -> Unit)? = if (profileShortcut != null) {
        {
            RailProfileHeader(
                shortcut = profileShortcut,
                selected = currentDestination.isTabSelected(WseiDestination.PROFILE),
                expanded = railExpanded
            )
        }
    } else {
        null
    }

    WideNavigationRail(
        modifier = modifier,
        state = railState,
        colors = WideNavigationRailDefaults.colors(containerColor = colors.surfaceContainerLow),
        header = header,
        // Without a header (short landscape window) the tabs are centred vertically.
        arrangement = if (header != null) Arrangement.Top else Arrangement.Center,
        contentPadding = PaddingValues(top = if (header != null) WseiSpacing.l else 0.dp)
    ) {
        WseiNavigationTabs.forEach { destination ->
            val isSelected = currentDestination.isTabSelected(destination)
            val showBadge = destination == WseiDestination.MORE && unreadAnnouncementsCount > 0
            WideNavigationRailItem(
                selected = isSelected,
                onClick = {
                    navController.navigateToTopLevelTab(
                        destination = destination,
                        onResetMoreTwoPane = onResetMoreTwoPane
                    )
                },
                icon = {
                    if (showBadge) {
                        BadgedBox(badge = { Badge() }) {
                            Icon(
                                imageVector = destination.iconVector,
                                contentDescription = pluralStringResource(
                                    R.plurals.nav_unread_announcements_cd,
                                    unreadAnnouncementsCount,
                                    unreadAnnouncementsCount
                                )
                            )
                        }
                    } else {
                        Icon(imageVector = destination.iconVector, contentDescription = null)
                    }
                },
                label = {
                    Text(
                        text = stringResource(if (railExpanded) destination.labelRes else destination.shortLabelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                railExpanded = railExpanded,
                colors = itemColors
            )
        }
    }
}

/** Rail header: the student avatar (plus the "Profil" label on a wide rail) opening Profile. */
@Composable
private fun RailProfileHeader(
    shortcut: WseiProfileShortcut,
    selected: Boolean,
    expanded: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val profileLabel = stringResource(WseiDestination.PROFILE.labelRes)
    val profileDescription = stringResource(R.string.common_profile_of, shortcut.fullName)
    // The collapsed rail is 96 dp wide — the avatar (48 dp with its ring) lands in the centre.
    val startPadding by animateDpAsState(
        targetValue = if (expanded) WseiSpacing.xl else WseiSpacing.xxl,
        animationSpec = WseiMotion.spatial(),
        label = "railHeaderPadding"
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
        animationSpec = WseiMotion.effects(),
        label = "railHeaderContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onPrimaryContainer else colors.onSurface,
        animationSpec = WseiMotion.effects(),
        label = "railHeaderContent"
    )

    Surface(
        onClick = shortcut.onOpen,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
        modifier = Modifier
            .padding(start = startPadding, top = WseiSpacing.s)
            .wseiPressScale(interactionSource, pressedScale = 0.92f)
            .semantics {
                role = Role.Tab
                this.selected = selected
                contentDescription = profileDescription
            }
    ) {
        Row(
            modifier = Modifier.padding(WseiSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudentAvatar(
                base64Photo = shortcut.photoBase64,
                fullName = shortcut.fullName,
                size = 40.dp,
                shape = CircleShape,
                ringWidth = 1.5.dp,
                ringColor = containerColor
            )
            AnimatedVisibility(
                visible = expanded,
                enter = expandHorizontally(animationSpec = WseiMotion.spatial()) + fadeIn(WseiMotion.effects()),
                exit = shrinkHorizontally(animationSpec = WseiMotion.spatial()) + fadeOut(WseiMotion.effects())
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = WseiSpacing.m, end = WseiSpacing.l)
                        .widthIn(max = 160.dp)
                ) {
                    Text(
                        text = profileLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = shortcut.fullName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
