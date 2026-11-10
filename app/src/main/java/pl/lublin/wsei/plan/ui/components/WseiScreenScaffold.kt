@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.theme.M3XCookieShape
import pl.lublin.wsei.plan.ui.theme.WseiSpacing

/**
 * Skrót do profilu w prawym górnym rogu każdego głównego ekranu (zdjęcie studenta).
 * Dostarczany raz przez powłokę aplikacji, więc ekrany nie muszą go przekazywać.
 */
@Immutable
data class WseiProfileShortcut(
    val photoBase64: String?,
    val fullName: String,
    val onOpen: () -> Unit
)

val LocalWseiProfileShortcut = compositionLocalOf<WseiProfileShortcut?> { null }

/**
 * Rama ekranu: duży, zwijający się pasek tytułu (M3 LargeTopAppBar), opcjonalny przycisk
 * powrotu, skrót do profilu i „przeciągnij, aby odświeżyć”.
 *
 * [content] dostaje modyfikator, który trzeba nałożyć na przewijaną listę — dzięki temu pasek
 * tytułu zwija się przy przewijaniu i rozwija przed gestem odświeżania.
 */
@Composable
fun WseiScreenFrame(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    showProfileShortcut: Boolean = onBack == null,
    content: @Composable (scrollModifier: Modifier) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val profileShortcut = LocalWseiProfileShortcut.current
    val colors = MaterialTheme.colorScheme

    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        FilledTonalIconButton(
                            onClick = onBack,
                            modifier = Modifier.padding(start = WseiSpacing.xs)
                        ) {
                            Icon(
                                imageVector = WseiIcons.ChevronLeft,
                                contentDescription = "Wróć",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (showProfileShortcut && profileShortcut != null) {
                        ProfileShortcutButton(profileShortcut)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = colors.background,
                    scrolledContainerColor = colors.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
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

/**
 * Standardowy ekran z listą: [WseiScreenFrame] + LazyColumn z jednolitymi marginesami.
 */
@Composable
fun WseiScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    itemSpacing: Dp = WseiSpacing.betweenCards,
    contentPadding: PaddingValues = WseiSpacing.screenContent,
    content: LazyListScope.() -> Unit
) {
    WseiScreenFrame(
        title = title,
        modifier = modifier,
        onBack = onBack,
        onRefresh = onRefresh,
        isRefreshing = isRefreshing
    ) { scrollModifier ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            content = content
        )
    }
}

@Composable
private fun ProfileShortcutButton(shortcut: WseiProfileShortcut) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .padding(end = WseiSpacing.m)
            .wseiPressScale(interactionSource, pressedScale = 0.9f)
    ) {
        Surface(
            onClick = shortcut.onOpen,
            shape = M3XCookieShape,
            color = MaterialTheme.colorScheme.primary,
            interactionSource = interactionSource,
            modifier = Modifier.size(44.dp)
        ) {
            StudentAvatar(
                base64Photo = shortcut.photoBase64,
                fullName = shortcut.fullName,
                size = 44.dp
            )
        }
    }
}

/** Wspólny margines list — zachowany dla zgodności z istniejącymi ekranami. */
val WseiScreenContentPadding: PaddingValues = WseiSpacing.screenContent
