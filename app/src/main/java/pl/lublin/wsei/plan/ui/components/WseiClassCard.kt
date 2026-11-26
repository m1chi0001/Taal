@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package pl.lublin.wsei.plan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.DeliveryMode
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.theme.WseiCardShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import pl.lublin.wsei.plan.ui.theme.mutedContentColor
import pl.lublin.wsei.plan.ui.theme.tonalLayer
import pl.lublin.wsei.plan.ui.theme.withContrastAgainst
import androidx.compose.ui.res.stringResource
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.l10n.labelRes
import pl.lublin.wsei.plan.l10n.localizedLabel
import pl.lublin.wsei.plan.l10n.localizedNameOrPlaceholder

enum class ClassRowState { UPCOMING, NEXT, ONGOING, FINISHED }

/** Time-based state of a class card — one function instead of several copies of the same logic. */
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
 * Class card (Start and Plan zajęć): a time tile on the left; subject, class type, room and
 * lecturer on the right. An ongoing class sits on `primaryContainer` with an inverted time tile
 * (`onPrimaryContainer` / `primaryContainer`, always ≥ 4.5:1) and a "minutes left" badge — no
 * progress bar (that lives in the "Trwa teraz" hero above the list).
 *
 * Metadata is never repeated: the delivery mode chip only appears when the location line does
 * not already say it, and the data layer's placeholder group ("Grupa dziekanatowa") is hidden.
 *
 * @param lecturerLabel lecturer line as it may be shown — pass `uiState.maskLecturer(...)` so
 *   "Ukryj dane" hides the name; defaults to the unmasked name for previews and legacy callers.
 */
@Composable
fun WseiClassCard(
    event: ClassEvent,
    nowEpochMillis: Long,
    state: ClassRowState,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showContainer: Boolean = true,
    lecturerLabel: String = event.lecturer.localizedNameOrPlaceholder()
) {
    val ongoing = state == ClassRowState.ONGOING
    val finished = state == ClassRowState.FINISHED
    val colors = MaterialTheme.colorScheme
    // Rows change state while the screen is open (the ticker updates every minute), so colors
    // cross-fade on an effects spring instead of jumping.
    val containerColor by animateColorAsState(
        targetValue = when {
            ongoing -> colors.primaryContainer
            showContainer -> colors.surfaceContainer
            else -> colors.surfaceContainer.copy(alpha = 0f)
        },
        animationSpec = WseiMotion.effects(),
        label = "classCardContainer"
    )
    val contentColor = if (ongoing) colors.onPrimaryContainer else colors.onSurface
    val mutedColor = if (ongoing) {
        mutedContentColor(colors.onPrimaryContainer, colors.primaryContainer)
    } else {
        colors.onSurfaceVariant
    }
    // The ongoing tile inverts the container pair, so it can never blend into the card.
    val timeTileTarget = when (state) {
        ClassRowState.ONGOING -> colors.onPrimaryContainer
        ClassRowState.NEXT -> colors.tertiaryContainer
        else -> colors.surfaceContainerHighest
    }
    val timeTileColor by animateColorAsState(
        targetValue = timeTileTarget,
        animationSpec = WseiMotion.effects(),
        label = "classTimeTile"
    )
    val timeTileContent = when (state) {
        ClassRowState.ONGOING -> colors.primaryContainer
        ClassRowState.NEXT -> colors.onTertiaryContainer
        else -> colors.onSurface
    }
    val timeTileMuted = mutedContentColor(timeTileContent, timeTileTarget)
    val stateLabel = stringResource(
        when (state) {
            ClassRowState.ONGOING -> R.string.class_state_ongoing
            ClassRowState.NEXT -> R.string.class_state_next
            ClassRowState.FINISHED -> R.string.class_state_finished
            ClassRowState.UPCOMING -> R.string.class_state_upcoming
        }
    )
    val classTypeLabel = stringResource(event.classType.labelRes)
    val roomLabel = event.room.localizedLabel()
    // Read in the graphics layer only, so the fade never recomposes the row.
    val rowAlpha = animateFloatAsState(
        targetValue = if (finished) 0.55f else 1f,
        animationSpec = WseiMotion.effects(),
        label = "classRowAlpha"
    )

    Surface(
        shape = if (showContainer || ongoing) WseiCardShape else RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                stateDescription = stateLabel
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
                    .graphicsLayer { alpha = rowAlpha.value },
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = timeTileColor,
                    contentColor = timeTileContent,
                    // Min width only: with large font scales the time must not be clipped.
                    modifier = Modifier.widthIn(min = 64.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = WseiSpacing.xs, vertical = 10.dp),
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
                            color = timeTileMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (state) {
                        ClassRowState.ONGOING -> OngoingBadge(
                            text = stringResource(
                                R.string.schedule_class_ongoing_badge,
                                event.remainingMinutes(nowEpochMillis)
                            )
                        )
                        ClassRowState.NEXT -> StatusLabel(stringResource(R.string.class_badge_next), colors.tertiary)
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
                            text = stringResource(R.string.class_card_type_room, classTypeLabel, roomLabel),
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
                                tonalLayer(colors.primaryContainer, colors.onPrimaryContainer, 0.12f)
                            } else {
                                colors.surfaceContainerHighest
                            }
                            val chipContent = contentColor.withContrastAgainst(chipColor)
                            WseiPill(classTypeLabel, containerColor = chipColor, contentColor = chipContent)
                            deliveryChipLabel(event)?.let { label ->
                                WseiPill(label, containerColor = chipColor, contentColor = chipContent)
                            }
                            displayGroupCode(event.groupCode)?.let { group ->
                                WseiPill(group, containerColor = chipColor, contentColor = chipContent)
                            }
                        }
                        ClassDetailLine(WseiIcons.LocationOn, roomLabel, mutedColor)
                        event.room.addressLabel?.let { address ->
                            ClassDetailLine(WseiIcons.Home, address, mutedColor)
                        }
                        ClassDetailLine(WseiIcons.Person, lecturerLabel, mutedColor)
                    }
                }
            }
        }
    }
}

/**
 * Group code worth showing on a card, or `null` for a blank code or the placeholder the data
 * layer writes when the dean's office gives no group ([GroupCodePlaceholder]).
 */
fun displayGroupCode(groupCode: String): String? =
    groupCode.trim().takeIf { it.isNotEmpty() && !it.equals(GroupCodePlaceholder, ignoreCase = true) }

/** Fallback group name of the timetable sources — not real data, so never shown as a chip. */
private const val GroupCodePlaceholder = "Grupa dziekanatowa"

/**
 * Delivery mode chip text, or `null` when it would only repeat the location line: on-site
 * classes show their room, remote / e-learning rooms already read "Streaming (zajęcia zdalne)"
 * or "E-learning".
 */
@Composable
private fun deliveryChipLabel(event: ClassEvent): String? = when {
    event.deliveryMode == DeliveryMode.ON_SITE -> null
    event.room.isRemoteTeams -> null
    else -> stringResource(event.deliveryMode.labelRes)
}

/** Subtle "live" badge of the ongoing class: a tonal pill with a sunny marker, no progress bar. */
@Composable
private fun OngoingBadge(text: String) {
    val colors = MaterialTheme.colorScheme
    val badgeColor = tonalLayer(colors.primaryContainer, colors.onPrimaryContainer, 0.14f)
    val badgeContent = colors.onPrimaryContainer.withContrastAgainst(badgeColor)
    Surface(
        shape = CircleShape,
        color = badgeColor,
        contentColor = badgeContent
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(MaterialShapes.Sunny.toShape())
                    .background(badgeContent)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
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
