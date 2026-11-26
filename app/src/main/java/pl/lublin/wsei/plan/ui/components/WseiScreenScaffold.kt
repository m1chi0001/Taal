@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.max
import androidx.window.core.layout.WindowSizeClass
import pl.lublin.wsei.plan.ui.adaptive.WseiReadableContentMaxWidth
import pl.lublin.wsei.plan.ui.adaptive.wseiScreenContentPadding
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import androidx.compose.ui.res.stringResource
import pl.lublin.wsei.plan.R

/**
 * Profile shortcut (student photo) in the top-right corner of every main screen. Provided once
 * by the app shell, so screens do not pass it around.
 */
@Immutable
data class WseiProfileShortcut(
    val photoBase64: String?,
    val fullName: String,
    val onOpen: () -> Unit,
    /**
     * `false` when Profile is already reachable from the navigation rail header (tablets), so
     * the screen's top bar does not repeat the avatar.
     */
    val showInTopBar: Boolean = true
)

val LocalWseiProfileShortcut = compositionLocalOf<WseiProfileShortcut?> { null }

/**
 * Screen frame: a large collapsing title bar (M3 Expressive `LargeFlexibleTopAppBar`, the
 * smaller `MediumFlexibleTopAppBar` on short landscape windows), optional subtitle, back button,
 * profile shortcut and pull-to-refresh.
 *
 * The bar uses the same colours on every screen and pane — [wseiTopBarColors]: the window
 * background at rest and `surfaceContainer` once content scrolls under it (M3
 * `scrolledContainerColor`). Its title is inset so that it starts at the same edge as the
 * content column: [contentHorizontalPadding] (adaptive screen margin by default) plus the
 * centring margin of a column capped at [contentMaxWidth].
 *
 * [content] receives a modifier that must be applied to the scrolling container, so the title
 * bar collapses on scroll and expands again before the refresh gesture.
 */
@Composable
fun WseiScreenFrame(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    showProfileShortcut: Boolean = onBack == null,
    subtitle: String? = null,
    contentMaxWidth: Dp = Dp.Unspecified,
    contentHorizontalPadding: Dp = Dp.Unspecified,
    content: @Composable (scrollModifier: Modifier) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val profileShortcut = LocalWseiProfileShortcut.current
    val colors = MaterialTheme.colorScheme
    val layoutDirection = LocalLayoutDirection.current
    val horizontalPadding = if (contentHorizontalPadding.isSpecified) {
        contentHorizontalPadding
    } else {
        wseiScreenContentPadding().calculateStartPadding(layoutDirection)
    }
    // Short landscape phone: an expanded large bar would take most of the screen height.
    val isShortWindow = !currentWindowAdaptiveInfo().windowSizeClass
        .isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)

    val titleSlot: @Composable () -> Unit = {
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    val subtitleSlot: (@Composable () -> Unit)? = if (subtitle != null) {
        {
            Text(
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        null
    }
    val navigationIcon: @Composable () -> Unit = {
        if (onBack != null) {
            FilledTonalIconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = WseiSpacing.xs)
            ) {
                Icon(
                    imageVector = WseiIcons.ChevronLeft,
                    contentDescription = stringResource(R.string.common_back),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
    val barColors = wseiTopBarColors()

    BoxWithConstraints(modifier = modifier) {
        // Aligns the bar title with the content column: the bar already insets its title by
        // TopBarTitleInset, the rest comes from the screen margin and the centring margin.
        val centring = if (contentMaxWidth.isSpecified && constraints.hasBoundedWidth) {
            max(0.dp, (maxWidth - horizontalPadding * 2 - contentMaxWidth) / 2)
        } else {
            0.dp
        }
        val titleInset = max(0.dp, horizontalPadding - TopBarTitleInset) + centring
        val barInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = titleInset, right = titleInset))

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colors.background,
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                if (isShortWindow) {
                    MediumFlexibleTopAppBar(
                        title = titleSlot,
                        subtitle = subtitleSlot,
                        navigationIcon = navigationIcon,
                        actions = {
                            if (showProfileShortcut && profileShortcut != null && profileShortcut.showInTopBar) {
                                ProfileShortcutButton(profileShortcut)
                            }
                        },
                        colors = barColors,
                        windowInsets = barInsets,
                        scrollBehavior = scrollBehavior
                    )
                } else {
                    LargeFlexibleTopAppBar(
                        title = titleSlot,
                        subtitle = subtitleSlot,
                        navigationIcon = navigationIcon,
                        actions = {
                            if (showProfileShortcut && profileShortcut != null && profileShortcut.showInTopBar) {
                                ProfileShortcutButton(profileShortcut)
                            }
                        },
                        colors = barColors,
                        windowInsets = barInsets,
                        scrollBehavior = scrollBehavior
                    )
                }
            }
        ) { innerPadding ->
            val scrollModifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (onRefresh != null) {
                    WseiRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) { content(scrollModifier) }
                } else {
                    content(scrollModifier)
                }
            }
        }
    }
}

/**
 * Top bar colours shared by every screen and pane: the window background at rest and the M3
 * `scrolledContainerColor` (`surfaceContainer`) once content scrolls beneath the bar.
 */
@Composable
fun wseiTopBarColors(): TopAppBarColors {
    val colors = MaterialTheme.colorScheme
    return TopAppBarDefaults.topAppBarColors(
        containerColor = colors.background,
        scrolledContainerColor = colors.surfaceContainer,
        titleContentColor = colors.onSurface,
        subtitleContentColor = colors.onSurfaceVariant,
        actionIconContentColor = colors.onSurfaceVariant,
        navigationIconContentColor = colors.onSurface
    )
}

/** Start inset the M3 top bars give their title (4 dp bar padding + 12 dp title inset). */
private val TopBarTitleInset: Dp = 16.dp

/**
 * Standard list screen: [WseiScreenFrame] + a LazyColumn with uniform margins.
 *
 * @param contentPadding adaptive [wseiScreenContentPadding] by default (wider on tablets, room
 *   for the floating bar only where it is shown).
 * @param maxContentWidth on wide windows the cards are centred and no wider than this (the list
 *   still scrolls over the full width); the title bar is aligned with the same column.
 *   `Dp.Unspecified` removes the cap, e.g. for a custom two-pane layout.
 */
@Composable
fun WseiScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    itemSpacing: Dp = WseiSpacing.betweenCards,
    contentPadding: PaddingValues = wseiScreenContentPadding(),
    subtitle: String? = null,
    maxContentWidth: Dp = WseiReadableContentMaxWidth,
    content: LazyListScope.() -> Unit
) {
    WseiScreenFrame(
        title = title,
        modifier = modifier,
        onBack = onBack,
        onRefresh = onRefresh,
        isRefreshing = isRefreshing,
        subtitle = subtitle,
        contentMaxWidth = maxContentWidth,
        contentHorizontalPadding = contentPadding.calculateStartPadding(LocalLayoutDirection.current)
    ) { scrollModifier ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val layoutDirection = LocalLayoutDirection.current
            val start = contentPadding.calculateStartPadding(layoutDirection)
            val end = contentPadding.calculateEndPadding(layoutDirection)
            // Extra side margin centres the card column on a wide window.
            val extra = if (maxContentWidth.isSpecified && constraints.hasBoundedWidth) {
                max(0.dp, (maxWidth - start - end - maxContentWidth) / 2)
            } else {
                0.dp
            }
            val effectivePadding = if (extra > 0.dp) {
                PaddingValues(
                    start = start + extra,
                    top = contentPadding.calculateTopPadding(),
                    end = end + extra,
                    bottom = contentPadding.calculateBottomPadding()
                )
            } else {
                contentPadding
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(scrollModifier),
                contentPadding = effectivePadding,
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
                content = content
            )
        }
    }
}

@Composable
private fun ProfileShortcutButton(shortcut: WseiProfileShortcut) {
    val interactionSource = remember { MutableInteractionSource() }
    val profileDescription = stringResource(R.string.common_profile_of, shortcut.fullName)
    Box(
        modifier = Modifier
            .padding(end = WseiSpacing.m)
            .wseiPressScale(interactionSource, pressedScale = 0.9f)
    ) {
        Surface(
            onClick = shortcut.onOpen,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            interactionSource = interactionSource,
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = profileDescription }
        ) {
            StudentAvatar(
                base64Photo = shortcut.photoBase64,
                fullName = shortcut.fullName,
                size = 44.dp,
                shape = CircleShape
            )
        }
    }
}

/**
 * Shared list margins, kept for existing screens. New code should use the adaptive
 * [wseiScreenContentPadding].
 */
val WseiScreenContentPadding: PaddingValues = WseiSpacing.screenContent
