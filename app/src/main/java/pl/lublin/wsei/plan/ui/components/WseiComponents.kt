@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.theme.M3XCookieShape
import pl.lublin.wsei.plan.ui.theme.M3XSquircleShape
import pl.lublin.wsei.plan.ui.theme.WseiCardShape
import pl.lublin.wsei.plan.ui.theme.WseiInsetShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import pl.lublin.wsei.plan.ui.theme.rememberDecodedStudentPhoto
import pl.lublin.wsei.plan.ui.theme.mutedContentColor
import pl.lublin.wsei.plan.ui.theme.tonalLayer
import pl.lublin.wsei.plan.ui.theme.wseiGroupItemShape
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import pl.lublin.wsei.plan.R

// =========================================================================================
// Basic UI building blocks. Every screen is made of the same elements, so the app looks
// consistent and a fix in one place applies everywhere.
// =========================================================================================

/**
 * Expressive touch feedback: the element is slightly "pressed in" (scale) on a spring. The
 * value is read in the draw phase (graphicsLayer), so it causes no recomposition.
 */
@Composable
fun Modifier.wseiPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = WseiMotion.fastSpatial(),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Content card. With [onClick] the card reacts to touch with an expressive press. Pass a
 * container role with its matching on-colour ([containerColor] / [contentColor]).
 */
@Composable
fun WseiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = WseiCardShape,
    contentPadding: PaddingValues = PaddingValues(WseiSpacing.xl),
    verticalSpacing: Dp = WseiSpacing.m,
    content: @Composable ColumnScope.() -> Unit
) {
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content
        )
    }
    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            interactionSource = interactionSource,
            modifier = modifier
                .fillMaxWidth()
                .wseiPressScale(interactionSource, pressedScale = 0.98f),
            content = body
        )
    } else {
        Surface(
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            modifier = modifier.fillMaxWidth(),
            content = body
        )
    }
}

/**
 * Icon in a container (squircle by default). On a tinted tile use a contrasting pair, e.g. the
 * inverted container pair (`onXContainer` bubble with an `xContainer` icon), so it never blends.
 */
@Composable
fun WseiIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    shape: Shape = M3XSquircleShape,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** Small status / label pill. */
@Composable
fun WseiPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    emphasized: Boolean = false
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Card header: icon, overline, title and an optional trailing element.
 */
@Composable
fun WseiCardHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    overline: String? = null,
    overlineColor: Color = MaterialTheme.colorScheme.primary,
    iconContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    titleMaxLines: Int = 2,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
    ) {
        if (icon != null) {
            WseiIconBadge(
                icon = icon,
                containerColor = iconContainerColor,
                contentColor = iconContentColor
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
        ) {
            if (overline != null) {
                Text(
                    text = overline,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = overlineColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing?.invoke(this)
    }
}

/** Section title above a group of cards or a list. */
@Composable
fun WseiSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = WseiSpacing.xs, end = WseiSpacing.xs, top = WseiSpacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * "Label — value" row of detail cards. The value takes at most 60% of the width and wraps,
 * so long names (programme, faculty) never push the label out of the card.
 */
@Composable
fun WseiInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    val spacing = WseiSpacing.m
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
        }
    ) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        if (!constraints.hasBoundedWidth) {
            val labelPlaceable = measurables[0].measure(loose)
            val valuePlaceable = measurables[1].measure(loose)
            val width = labelPlaceable.width + gap + valuePlaceable.width
            val height = maxOf(labelPlaceable.height, valuePlaceable.height)
            return@Layout layout(width, height) {
                labelPlaceable.placeRelative(0, (height - labelPlaceable.height) / 2)
                valuePlaceable.placeRelative(width - valuePlaceable.width, (height - valuePlaceable.height) / 2)
            }
        }
        val total = constraints.maxWidth
        val valuePlaceable = measurables[1].measure(loose.copy(maxWidth = (total * 0.6f).roundToInt()))
        val labelWidth = (total - valuePlaceable.width - gap).coerceAtLeast(0)
        val labelPlaceable = measurables[0].measure(loose.copy(maxWidth = labelWidth))
        val height = maxOf(labelPlaceable.height, valuePlaceable.height)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(total, height) {
            labelPlaceable.placeRelative(0, (height - labelPlaceable.height) / 2)
            valuePlaceable.placeRelative(total - valuePlaceable.width, (height - valuePlaceable.height) / 2)
        }
    }
}

/** Inner container inside a card (details, metrics). */
@Composable
fun WseiInset(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = WseiInsetShape,
        color = containerColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = WseiSpacing.l, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.s),
            content = content
        )
    }
}

// -----------------------------------------------------------------------------------------
// Connected lists (M3 Expressive "segmented list"): items touch, outer corners are large and
// inner ones small. Used in settings, profile and the "Więcej" hub.
// -----------------------------------------------------------------------------------------

/** A single item of a connected list; [WseiListGroup] gives it its shape. */
class WseiListEntry(
    val key: String,
    val content: @Composable (shape: Shape) -> Unit
)

@Composable
fun WseiListGroup(
    entries: List<WseiListEntry>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.inGroup)
    ) {
        entries.forEachIndexed { index, entry ->
            // Stable item identity: row state does not jump to a neighbour when the list
            // changes (e.g. after the student groups refresh).
            key(entry.key) {
                entry.content(wseiGroupItemShape(index, entries.size))
            }
        }
    }
}

/**
 * List row: icon, title, supporting text and a trailing element (switch, chevron, pill).
 */
@Composable
fun WseiListItem(
    title: String,
    shape: Shape,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    iconContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val color = if (selected) MaterialTheme.colorScheme.secondaryContainer else containerColor
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val row: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WseiSpacing.l, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.l)
        ) {
            if (icon != null) {
                WseiIconBadge(
                    icon = icon,
                    // Selected rows sit on secondaryContainer: the inverted pair keeps the icon
                    // bubble distinct from the row whatever the palette.
                    containerColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else iconContainerColor,
                    contentColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else iconContentColor,
                    size = 40.dp,
                    iconSize = 20.dp
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (supporting != null) {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) mutedContentColor(contentColor, color) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailing?.invoke(this)
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = color,
            contentColor = contentColor,
            modifier = modifier.fillMaxWidth(),
            content = row
        )
    } else {
        Surface(
            shape = shape,
            color = color,
            contentColor = contentColor,
            modifier = modifier.fillMaxWidth(),
            content = row
        )
    }
}

/** "Go on" chevron of list rows. */
@Composable
fun WseiChevron() {
    Icon(
        imageVector = WseiIcons.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp)
    )
}

/** Empty state: cookie-shaped icon, title and a short hint. */
@Composable
fun WseiEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    WseiCard(modifier = modifier, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.l)
        ) {
            WseiIconBadge(
                icon = icon,
                shape = M3XCookieShape,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                size = 56.dp,
                iconSize = 26.dp
            )
            Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Big figure with a caption — statistics on summary cards. */
@Composable
fun WseiStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        shape = WseiInsetShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = WseiSpacing.m),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
        ) {
            val valueStyle = MaterialTheme.typography.headlineSmall
            // Narrow columns (3 stats side by side on a small phone) shrink the font instead
            // of clipping the value.
            Text(
                text = value,
                style = valueStyle,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 14.sp,
                    maxFontSize = valueStyle.fontSize,
                    stepSize = 1.sp
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = mutedContentColor(contentColor, containerColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// Awatar studenta
// -----------------------------------------------------------------------------------------

/**
 * Student photo from the dean's office, or initials on a coloured background.
 *
 * Every frame layer is drawn with `background(color, shape)` (anti-aliased) and only the photo
 * itself is clipped, so a smooth [shape] such as `CircleShape` gives clean edges even for ID
 * photos with a bright white background.
 *
 * @param shape outline of the frame and of the photo. Prefer `CircleShape` for large photos:
 *   wavy outlines (cookie) clip the white photo background into jagged edges.
 * @param ringWidth when > 0 the photo sits inside a tonal ring of [ringColor] drawn with the same
 *   [shape], so the (often white) photo background never touches the card behind it.
 * @param photoZoom slight zoom of the photo (≥ 1) that crops the white margins of ID photos.
 * @param ringGap optional thin gap of [ringGapColor] between the ring and the photo.
 * @param ringGapColor colour of [ringGap]; also used for the hairline that hides the
 *   anti-aliasing fringe of the photo edge when a ring or gap is present.
 */
@Composable
fun StudentAvatar(
    base64Photo: String?,
    fullName: String,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = M3XCookieShape,
    ringWidth: Dp = 0.dp,
    ringColor: Color = MaterialTheme.colorScheme.primary,
    photoZoom: Float = 1f,
    ringGap: Dp = 0.dp,
    ringGapColor: Color = MaterialTheme.colorScheme.surface
) {
    val decodedBitmap = rememberDecodedStudentPhoto(base64Photo)
    val initials = remember(fullName) {
        fullName
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "ST" }
    }
    var frame = modifier.size(size)
    if (ringWidth > 0.dp) frame = frame.background(ringColor, shape).padding(ringWidth)
    if (ringGap > 0.dp) frame = frame.background(ringGapColor, shape).padding(ringGap)
    val isFramed = ringWidth > 0.dp || ringGap > 0.dp
    // A hairline in the adjacent frame colour, drawn over the photo edge, hides the white fringe
    // that anti-aliasing leaves where a bright photo background meets the frame.
    val photoEdge = if (isFramed && decodedBitmap != null) {
        Modifier.border(StudentAvatarEdgeWidth, if (ringGap > 0.dp) ringGapColor else ringColor, shape)
    } else {
        Modifier
    }
    // Initials sit on the container pair (a large tinted area), never on raw primary.
    Box(
        modifier = frame
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .then(photoEdge),
        contentAlignment = Alignment.Center
    ) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap,
                contentDescription = stringResource(R.string.common_photo_of, fullName),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (photoZoom != 1f) {
                            Modifier.graphicsLayer {
                                scaleX = photoZoom
                                scaleY = photoZoom
                            }
                        } else {
                            Modifier
                        }
                    )
            )
        } else {
            Text(
                text = initials,
                style = if (size >= 64.dp) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Width of the hairline drawn over a framed photo's edge (see [StudentAvatar]). */
private val StudentAvatarEdgeWidth: Dp = 1.dp

// -----------------------------------------------------------------------------------------
// Refresh and expandable cards
// -----------------------------------------------------------------------------------------

/**
 * The app's only pull-to-refresh container, with the M3 Expressive LoadingIndicator.
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
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = 4.dp
            )
        },
        content = content
    )
}

/**
 * Card expanded by tapping its header. The chevron turns on a spring and its container morphs
 * from a circle into a squircle when expanded (M3 Expressive shape morph).
 *
 * @param pinned always visible content under the header (e.g. a payment button)
 * @param body content visible when expanded
 */
@Composable
fun WseiExpandableCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    pinned: (@Composable ColumnScope.() -> Unit)? = null,
    header: @Composable RowScope.() -> Unit,
    body: @Composable ColumnScope.() -> Unit
) {
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = WseiMotion.spatial(),
        label = "cardChevron"
    )
    val chevronCorner by animateDpAsState(
        targetValue = if (expanded) 12.dp else 20.dp,
        animationSpec = WseiMotion.spatial(),
        label = "cardChevronCorner"
    )
    Surface(
        shape = WseiCardShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier.fillMaxWidth()
    ) {
        // AnimatedVisibility below is the only size animation: an extra animateContentSize here
        // used to chase it frame by frame and made the card jitter when expanding.
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(if (expanded) R.string.common_collapse else R.string.common_expand),
                        onClick = onToggle
                    )
                    .padding(WseiSpacing.xl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
            ) {
                header()
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(chevronCorner))
                        .background(tonalLayer(containerColor, contentColor, 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = WseiIcons.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { rotationZ = chevronAngle }
                    )
                }
            }
            if (pinned != null) {
                Column(
                    modifier = Modifier.padding(start = WseiSpacing.xl, end = WseiSpacing.xl, bottom = WseiSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(WseiSpacing.m),
                    content = pinned
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = WseiMotion.size(), expandFrom = Alignment.Top) +
                    fadeIn(WseiMotion.effects()),
                exit = shrinkVertically(animationSpec = WseiMotion.size(), shrinkTowards = Alignment.Top) +
                    fadeOut(WseiMotion.fastEffects())
            ) {
                Column(
                    modifier = Modifier.padding(start = WseiSpacing.xl, end = WseiSpacing.xl, bottom = WseiSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(WseiSpacing.m),
                    content = body
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Privacy, switches and loading
// -----------------------------------------------------------------------------------------

/** Shared mask of sensitive data (amounts, grades, album number, bank account). */
const val WseiSensitiveMask: String = "••••••"

/** [value], or the mask while "Ukryj dane" is on. */
fun wseiMasked(value: String, hidden: Boolean): String = if (hidden) WseiSensitiveMask else value

private fun wseiSymbol(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Black)
    ).build()

private val SwitchCheckIcon: ImageVector by lazy {
    wseiSymbol("SwitchCheck", "M9,16.17L4.83,12l-1.42,1.41L9,19L21,7l-1.41,-1.41z")
}

private val SwitchCloseIcon: ImageVector by lazy {
    wseiSymbol(
        "SwitchClose",
        "M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12z"
    )
}

/**
 * M3 switch with an icon in the thumb (✓ / ✕) — the state reads without relying on colour.
 * Pass `onCheckedChange = null` when the whole list row toggles it.
 */
@Composable
fun WseiSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = {
            Icon(
                imageVector = if (checked) SwitchCheckIcon else SwitchCloseIcon,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize)
            )
        }
    )
}

/** Expressive loading indicator (M3 LoadingIndicator) with an optional caption. */
@Composable
fun WseiLoadingState(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = WseiSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)
    ) {
        LoadingIndicator(color = MaterialTheme.colorScheme.primary)
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
