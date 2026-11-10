@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package pl.lublin.wsei.plan.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.theme.WseiCardShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing

enum class ClassRowState { UPCOMING, NEXT, ONGOING, FINISHED }

/** Stan karty zajęć na podstawie czasu — jedna funkcja zamiast kilku kopii tej samej logiki. */
fun classRowStateOf(
    event: ClassEvent,
    nowEpochMillis: Long,
    activeId: String?,
    nextId: String?
): ClassRowState = when {
    event.id == activeId || event.isOngoing(nowEpochMillis) -> ClassRowState.ONGOING
    event.timeSlot.endEpochMillis <= nowEpochMillis -> ClassRowState.FINISHED
    event.id == nextId -> ClassRowState.NEXT
    else -> ClassRowState.UPCOMING
}

/**
 * Karta zajęć (Start i Plan zajęć).
 * Po lewej „kafelek czasu”, po prawej przedmiot, forma, sala i prowadzący.
 * Trwające zajęcia dostają kolor główny i falujący pasek postępu.
 */
@Composable
fun WseiClassCard(
    event: ClassEvent,
    nowEpochMillis: Long,
    state: ClassRowState,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showContainer: Boolean = true
) {
    val ongoing = state == ClassRowState.ONGOING
    val finished = state == ClassRowState.FINISHED
    val colors = MaterialTheme.colorScheme
    val containerColor = when {
        ongoing -> colors.primaryContainer
        showContainer -> colors.surfaceContainer
        else -> Color.Transparent
    }
    val contentColor = if (ongoing) colors.onPrimaryContainer else colors.onSurface
    val mutedColor = if (ongoing) contentColor.copy(alpha = 0.75f) else colors.onSurfaceVariant
    val timeTileColor = when (state) {
        ClassRowState.ONGOING -> colors.primary
        ClassRowState.NEXT -> colors.tertiaryContainer
        else -> colors.surfaceContainerHighest
    }
    val timeTileContent = when (state) {
        ClassRowState.ONGOING -> colors.onPrimary
        ClassRowState.NEXT -> colors.onTertiaryContainer
        else -> colors.onSurface
    }

    Surface(
        shape = if (showContainer || ongoing) WseiCardShape else RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                stateDescription = when (state) {
                    ClassRowState.ONGOING -> "Trwa teraz"
                    ClassRowState.NEXT -> "Następne zajęcia"
                    ClassRowState.FINISHED -> "Zakończone"
                    ClassRowState.UPCOMING -> "Zaplanowane"
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (showContainer || ongoing) WseiSpacing.l else WseiSpacing.s,
                vertical = if (showContainer || ongoing) WseiSpacing.l else WseiSpacing.s
            ),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (finished) 0.55f else 1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = timeTileColor,
                    contentColor = timeTileContent,
                    modifier = Modifier.width(64.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Text(
                            text = event.timeSlot.startFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = event.timeSlot.endFormatted,
                            style = MaterialTheme.typography.labelMedium,
                            color = timeTileContent.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (state) {
                        ClassRowState.ONGOING -> StatusLabel("Trwa teraz", colors.primary)
                        ClassRowState.NEXT -> StatusLabel("Następne", colors.tertiary)
                        else -> Unit
                    }
                    Text(
                        text = event.subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (compact) {
                        Text(
                            text = "${event.classType.polishLabel} • ${event.room.displayLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = mutedColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val chipColor = if (ongoing) {
                                colors.onPrimaryContainer.copy(alpha = 0.12f)
                            } else {
                                colors.surfaceContainerHighest
                            }
                            WseiPill(event.classType.polishLabel, containerColor = chipColor, contentColor = contentColor)
                            WseiPill(event.deliveryMode.polishBadge, containerColor = chipColor, contentColor = contentColor)
                            if (event.groupCode.isNotBlank()) {
                                WseiPill(event.groupCode, containerColor = chipColor, contentColor = contentColor)
                            }
                        }
                        ClassDetailLine(WseiIcons.LocationOn, event.room.displayLabel, mutedColor)
                        ClassDetailLine(WseiIcons.Person, event.lecturer.fullDisplayName, mutedColor)
                    }
                }
            }

            if (ongoing) {
                val progress by animateFloatAsState(
                    targetValue = event.progressFraction(nowEpochMillis),
                    animationSpec = WseiMotion.size(),
                    label = "classProgress"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearWavyProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp),
                        color = colors.primary,
                        trackColor = contentColor.copy(alpha = 0.18f)
                    )
                    Text(
                        text = "${event.elapsedMinutes(nowEpochMillis)} z ${event.totalDurationMinutes} min • do ${event.timeSlot.endFormatted}",
                        style = MaterialTheme.typography.labelMedium,
                        color = mutedColor
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusLabel(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

@Composable
private fun ClassDetailLine(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
