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

/**
 * Skrót do profilu w prawym górnym rogu każdego głównego ekranu (zdjęcie studenta).
 * Dostarczany raz przez powłokę aplikacji, więc ekrany nie muszą go przekazywać.
 */
@Immutable
data class WseiProfileShortcut(
    val photoBase64: String?,
    val fullName: String,
    val onOpen: () -> Unit,
    /**
     * `false`, gdy Profil jest już dostępny z nagłówka bocznej szyny nawigacji (tablety) —
     * górny pasek ekranu nie powiela wtedy awatara.
     */
    val showInTopBar: Boolean = true
)

val LocalWseiProfileShortcut = compositionLocalOf<WseiProfileShortcut?> { null }

/**
 * Rama ekranu: duży, zwijający się pasek tytułu (M3 Expressive `LargeFlexibleTopAppBar`,
 * na niskich oknach w poziomie mniejszy `MediumFlexibleTopAppBar`), opcjonalny podtytuł,
 * przycisk powrotu, skrót do profilu i „przeciągnij, aby odświeżyć”.
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
    subtitle: String? = null,
    content: @Composable (scrollModifier: Modifier) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val profileShortcut = LocalWseiProfileShortcut.current
    val colors = MaterialTheme.colorScheme
    // Telefon w poziomie: rozwinięty duży pasek zabrałby większość wysokości ekranu.
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
                    contentDescription = "Wróć",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
    val barColors = TopAppBarDefaults.topAppBarColors(
        containerColor = colors.background,
        scrolledContainerColor = colors.surfaceContainer
    )

    Scaffold(
        modifier = modifier,
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

/**
 * Standardowy ekran z listą: [WseiScreenFrame] + LazyColumn z jednolitymi marginesami.
 *
 * @param contentPadding domyślnie adaptacyjne marginesy [wseiScreenContentPadding] (większe na
 *   tabletach, miejsce na pływający pasek tylko tam, gdzie on jest).
 * @param maxContentWidth na szerokich oknach karty są wyśrodkowane i nie szersze niż ta wartość
 *   (lista nadal przewija się na całej szerokości). `Dp.Unspecified` wyłącza ograniczenie,
 *   np. dla własnego układu dwupanelowego.
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
        subtitle = subtitle
    ) { scrollModifier ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val layoutDirection = LocalLayoutDirection.current
            val start = contentPadding.calculateStartPadding(layoutDirection)
            val end = contentPadding.calculateEndPadding(layoutDirection)
            // Dodatkowy margines boczny centruje kolumnę kart na szerokim oknie.
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
    Box(
        modifier = Modifier
            .padding(end = WseiSpacing.m)
            .wseiPressScale(interactionSource, pressedScale = 0.9f)
    ) {
        Surface(
            onClick = shortcut.onOpen,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            interactionSource = interactionSource,
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = "Profil: ${shortcut.fullName}" }
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
 * Wspólny margines list — zachowany dla zgodności z istniejącymi ekranami.
 * Nowy kod powinien używać adaptacyjnego [wseiScreenContentPadding].
 */
val WseiScreenContentPadding: PaddingValues = WseiSpacing.screenContent
