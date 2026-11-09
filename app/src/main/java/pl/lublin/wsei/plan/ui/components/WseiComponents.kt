@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package pl.lublin.wsei.plan.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.theme.M3XCookieShape

/** Wspólny kształt dużych kart w całej aplikacji. */
val WseiCardShape = RoundedCornerShape(24.dp)

/**
 * Jedna sprężyna dla wszystkich zmian rozmiaru. Bez odbicia: odbicie powodowało,
 * że karta na chwilę wychodziła poza docelowy rozmiar i migała.
 */
private fun <T> wseiSizeSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * Górny pasek każdego ekranu: opcjonalny przycisk powrotu, tytuł i ikona w kształcie cookie.
 * Stała wysokość i marginesy, więc tytuł nie skacze między ekranami.
 */
@Composable
fun WseiScreenHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (onBack != null) 12.dp else 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 8.dp
            )
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBack != null) {
            FilledTonalIconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = WseiIcons.ChevronLeft,
                    contentDescription = "Wróć",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        Surface(
            shape = M3XCookieShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Jedyne pole „przeciągnij, aby odświeżyć” w aplikacji.
 * Wskaźnik jest w pełnym kolorze głównym, żeby nie zlewał się z kartami (`surfaceContainer*`).
 */
@Composable
fun WseiRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.primary,
                color = MaterialTheme.colorScheme.onPrimary,
                elevation = 6.dp
            )
        },
        content = content
    )
}

/**
 * Karta, którą rozwija się dotknięciem nagłówka.
 *
 * Tylko nagłówek reaguje na dotyk, więc ripple nie obejmuje całej rosnącej karty
 * (to dawało okrągłą poświatę po rozwinięciu). Rozmiar zmienia się bez odbicia.
 *
 * @param pinned elementy widoczne zawsze, pod nagłówkiem (np. przycisk płatności)
 * @param body elementy widoczne po rozwinięciu
 */
@Composable
fun WseiExpandableCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    pinned: (@Composable ColumnScope.() -> Unit)? = null,
    header: @Composable RowScope.() -> Unit,
    body: @Composable ColumnScope.() -> Unit
) {
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = wseiSizeSpring(),
        label = "cardChevron"
    )
    Surface(
        shape = WseiCardShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.animateContentSize(animationSpec = wseiSizeSpring())) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (expanded) "Zwiń" else "Rozwiń",
                        onClick = onToggle
                    )
                    .padding(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                header()
                Icon(
                    imageVector = WseiIcons.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(chevronAngle)
                )
            }
            if (pinned != null) {
                Column(
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) { pinned() }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = wseiSizeSpring()) + fadeIn(),
                exit = shrinkVertically(animationSpec = wseiSizeSpring()) + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) { body() }
            }
        }
    }
}

enum class ClassRowState { UPCOMING, NEXT, ONGOING, FINISHED }

/**
 * Karta zajęć używana na ekranie Start (compact) i w Planie zajęć.
 * Układ: godziny po lewej, po prawej przedmiot, forma, sala i prowadzący.
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
    val containerColor = when {
        ongoing -> MaterialTheme.colorScheme.primaryContainer
        showContainer -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> Color.Transparent
    }
    val contentColor = if (ongoing) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val mutedColor = contentColor.copy(alpha = 0.72f)

    Surface(
        shape = if (showContainer) WseiCardShape else RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (showContainer) 20.dp else 12.dp,
                vertical = if (showContainer) 18.dp else 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (finished) 0.6f else 1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.width(52.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = event.timeSlot.startFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = event.timeSlot.endFormatted,
                        style = MaterialTheme.typography.labelMedium,
                        color = mutedColor
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (state) {
                        ClassRowState.ONGOING -> Text(
                            text = "Trwa teraz",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        ClassRowState.NEXT -> Text(
                            text = "Następne",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
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
                            ClassChip(event.classType.polishLabel, ongoing)
                            ClassChip(event.deliveryMode.polishBadge, ongoing)
                            if (event.groupCode.isNotBlank()) ClassChip(event.groupCode, ongoing)
                        }
                        ClassDetailLine(WseiIcons.LocationOn, event.room.displayLabel, mutedColor)
                        ClassDetailLine(WseiIcons.Person, event.lecturer.fullDisplayName, mutedColor)
                    }
                }
            }

            if (ongoing) {
                val progress by animateFloatAsState(
                    targetValue = event.progressFraction(nowEpochMillis),
                    animationSpec = wseiSizeSpring(),
                    label = "classProgress"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearWavyProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = contentColor.copy(alpha = 0.2f)
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
private fun ClassChip(text: String, onPrimaryContainer: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (onPrimaryContainer) {
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ClassDetailLine(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
