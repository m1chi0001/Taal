package pl.lublin.wsei.plan.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.theme.M3XCookieShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import pl.lublin.wsei.plan.ui.theme.wseiGroupItemShape

/**
 * Ekran powitalny pokazywany raz, przed pierwszym logowaniem.
 *
 * Na telefonie w pionie i na tabletach — wyśrodkowana kolumna (maks. [WseiSpacing.formMaxWidth]);
 * na niskich, szerokich oknach (telefon w poziomie) logo stoi obok listy funkcji i przycisku.
 */
@Composable
fun WseiWelcomeScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoScale = remember { Animatable(0.6f) }
    val contentAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    LaunchedEffect(Unit) {
        // Treść pojawia się chwilę po logo.
        delay(150)
        contentAlpha.animateTo(1f, animationSpec = WseiMotion.effects())
    }
    // Ciągły, jednostajny obrót tła logo — jedyne miejsce, gdzie potrzebny jest liniowy czas trwania.
    val rotation by rememberInfiniteTransition(label = "welcomeRotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
        label = "welcomeCookieRotation"
    )

    val logo: @Composable (Dp) -> Unit = { logoSize ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = logoScale.value
                scaleY = logoScale.value
            }
        ) {
            Box(
                modifier = Modifier
                    .size(logoSize)
                    .graphicsLayer { rotationZ = rotation }
                    .clip(M3XCookieShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
            Icon(
                imageVector = WseiIcons.TaalVolcano,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(logoSize * 0.475f)
            )
        }
    }

    val intro: @Composable (TextAlign) -> Unit = { align ->
        Text(
            text = "Taal",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(WseiSpacing.s))
        Text(
            text = "Plan zajęć, oceny i opłaty z Wirtualnego Dziekanatu WSEI — w jednym miejscu.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = align
        )
    }

    val actions: @Composable () -> Unit = {
        Column(
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.inGroup),
            modifier = Modifier.fillMaxWidth()
        ) {
            val points = listOf(
                WseiIcons.CalendarMonth to "Plan na dziś i na cały tydzień",
                WseiIcons.School to "Oceny, średnia i punkty ECTS",
                WseiIcons.AccountBalanceWallet to "Opłaty, numer konta i stypendia"
            )
            points.forEachIndexed { index, (icon, text) ->
                WseiListItem(
                    title = text,
                    shape = wseiGroupItemShape(index, points.size),
                    icon = icon,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }
        }

        Spacer(Modifier.height(WseiSpacing.xxl))

        Button(
            onClick = onContinue,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text(text = "Zaczynamy", style = MaterialTheme.typography.titleMedium)
            Icon(
                imageVector = WseiIcons.ArrowForward,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = WseiSpacing.s)
                    .size(20.dp)
            )
        }

        Spacer(Modifier.height(WseiSpacing.l))

        Text(
            text = "Niezależny projekt studencki, niepowiązany oficjalnie z Uniwersytetem WSEI ani z APR System.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            // Paski systemowe i wycięcie aparatu (w poziomie z boku ekranu).
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val sideBySide = maxWidth >= 600.dp && maxHeight < 480.dp
        if (sideBySide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = WseiSpacing.xxl),
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.xxl, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .widthIn(max = 360.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    logo(112.dp)
                    Spacer(Modifier.height(WseiSpacing.l))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { alpha = contentAlpha.value }
                    ) {
                        intro(TextAlign.Center)
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = WseiSpacing.formMaxWidth)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = WseiSpacing.l)
                        .graphicsLayer { alpha = contentAlpha.value },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    actions()
                }
            }
        } else {
            val viewportHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    // Minimalna wysokość = okno, aby treść była wyśrodkowana w pionie,
                    // a przy braku miejsca po prostu się przewijała.
                    .heightIn(min = viewportHeight)
                    .padding(horizontal = 28.dp, vertical = WseiSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier.widthIn(max = WseiSpacing.formMaxWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    logo(if (viewportHeight < 640.dp) 128.dp else 160.dp)

                    Spacer(Modifier.height(32.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { alpha = contentAlpha.value }
                    ) {
                        intro(TextAlign.Center)
                        Spacer(Modifier.height(28.dp))
                        actions()
                    }
                }
            }
        }
    }
}
