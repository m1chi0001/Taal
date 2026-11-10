package pl.lublin.wsei.plan.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.ui.components.wseiPressScale
import pl.lublin.wsei.plan.ui.theme.WseiMotion

/** Zakładki widoczne na pasku nawigacji (Profil otwiera się z awatara w górnym pasku). */
internal val WseiNavigationTabs: List<WseiDestination> = listOf(
    WseiDestination.START,
    WseiDestination.SCHEDULE,
    WseiDestination.SUBJECTS,
    WseiDestination.FINANCES,
    WseiDestination.MORE
)

/**
 * Pływający pasek nawigacji w stylu M3 Expressive (floating toolbar):
 * aktywna zakładka rozszerza się w pigułkę z ikoną i podpisem, pozostałe są samymi ikonami.
 * Szerokość, kolor i wciśnięcie animują się na sprężynach.
 */
@Composable
internal fun WseiFloatingNavigationBar(
    currentDestination: WseiDestination,
    unreadAnnouncementsCount: Int,
    onSelectDestination: (WseiDestination) -> Unit,
    modifier: Modifier = Modifier
) {
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
                NavigationPillItem(
                    destination = destination,
                    selected = destination == currentDestination,
                    showBadge = destination == WseiDestination.MORE && unreadAnnouncementsCount > 0,
                    onClick = { onSelectDestination(destination) }
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
        targetValue = if (selected) colors.primary else colors.primary.copy(alpha = 0f),
        animationSpec = WseiMotion.effects(),
        label = "navItemContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        animationSpec = WseiMotion.effects(),
        label = "navItemContent"
    )
    val interactionSource = remember { MutableInteractionSource() }

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
                contentDescription = destination.contentDescription
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
                            .background(if (selected) colors.onPrimary else colors.error)
                    )
                }
            }
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(animationSpec = WseiMotion.spatial()) + fadeIn(WseiMotion.effects()),
                exit = shrinkHorizontally(animationSpec = WseiMotion.spatial()) + fadeOut(WseiMotion.effects())
            ) {
                Text(
                    text = destination.shortLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
