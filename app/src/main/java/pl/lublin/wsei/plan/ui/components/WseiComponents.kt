@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import pl.lublin.wsei.plan.ui.theme.wseiGroupItemShape
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import pl.lublin.wsei.plan.R

// =========================================================================================
// Podstawowe klocki interfejsu. Każdy ekran składa się z tych samych elementów, więc
// aplikacja wygląda spójnie, a poprawka w jednym miejscu działa wszędzie.
// =========================================================================================

/**
 * Ekspresyjna reakcja na dotyk: element lekko się „wciska” (skala) na sprężynie.
 * Odczyt wartości odbywa się w fazie rysowania (graphicsLayer), więc nie wywołuje rekompozycji.
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
 * Karta treści. Gdy podano [onClick], karta reaguje na dotyk ekspresyjnym „wciśnięciem”
 * i delikatnym zmniejszeniem zaokrągleń.
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

/** Ikona w kontenerze (domyślnie squircle). Zastępuje ~25 powielonych bloków Surface+Box+Icon. */
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

/** Mała pigułka statusu / etykiety. */
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
 * Nagłówek karty: ikona, nadtytuł (overline), tytuł i opcjonalny element po prawej.
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

/** Nagłówek sekcji nad grupą kart lub listą. */
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
 * Wiersz „etykieta — wartość” w kartach szczegółów. Wartość zajmuje najwyżej 60% szerokości
 * i zawija się do kolejnych linii, więc długie nazwy (kierunek, wydział) nie wypychają etykiety
 * poza kartę.
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

/** Wewnętrzny, jaśniejszy kontener wewnątrz karty (szczegóły, metryki). */
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
// Połączone listy (M3 Expressive „segmented list”): elementy stykają się, zewnętrzne rogi
// są duże, wewnętrzne małe. Używane w ustawieniach, profilu i hubie „Więcej”.
// -----------------------------------------------------------------------------------------

/** Pojedynczy element połączonej listy. Kształt nadaje [WseiListGroup]. */
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
            // Stabilna tożsamość elementu: stan wewnątrz wiersza nie „przeskakuje” na sąsiada,
            // gdy lista się zmienia (np. po odświeżeniu grup studenckich).
            key(entry.key) {
                entry.content(wseiGroupItemShape(index, entries.size))
            }
        }
    }
}

/**
 * Wiersz listy: ikona, tytuł, opis i element po prawej (przełącznik, strzałka, pigułka).
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
                    containerColor = if (selected) MaterialTheme.colorScheme.primary else iconContainerColor,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else iconContentColor,
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
                        color = if (selected) contentColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
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

/** Strzałka „przejdź dalej” dla wierszy listy. */
@Composable
fun WseiChevron() {
    Icon(
        imageVector = WseiIcons.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp)
    )
}

/** Stan pusty: ikona w kształcie cookie, tytuł i krótka podpowiedź. */
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

/** Duża liczba z podpisem — statystyki na kartach podsumowań. */
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
            // Wąskie kolumny (3 statystyki obok siebie na małym telefonie) zmniejszają czcionkę
            // zamiast ucinać wartość.
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
                color = contentColor.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// Awatar studenta
// -----------------------------------------------------------------------------------------

/** Zdjęcie studenta z Wirtualnego Dziekanatu albo inicjały na kolorowym tle. */
@Composable
fun StudentAvatar(
    base64Photo: String?,
    fullName: String,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = M3XCookieShape
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
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap,
                contentDescription = stringResource(R.string.common_photo_of, fullName),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                style = if (size >= 64.dp) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// Odświeżanie i karty rozwijane
// -----------------------------------------------------------------------------------------

/**
 * Jedyne pole „przeciągnij, aby odświeżyć” w aplikacji — ze wskaźnikiem LoadingIndicator M3 Expressive.
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
 * Karta rozwijana dotknięciem nagłówka. Strzałka obraca się na sprężynie, a przy rozwinięciu
 * jej kontener zmienia kształt z koła w squircle (morfing M3 Expressive).
 *
 * @param pinned elementy widoczne zawsze, pod nagłówkiem (np. przycisk płatności)
 * @param body elementy widoczne po rozwinięciu
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
        Column(modifier = Modifier.animateContentSize(animationSpec = WseiMotion.size())) {
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
                        .background(contentColor.copy(alpha = 0.08f)),
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
                enter = expandVertically(animationSpec = WseiMotion.size()) + fadeIn(WseiMotion.effects()),
                exit = shrinkVertically(animationSpec = WseiMotion.size()) + fadeOut(WseiMotion.effects())
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
// Prywatność, przełączniki i ładowanie
// -----------------------------------------------------------------------------------------

/** Wspólna maska dla danych wrażliwych (kwoty, oceny, numer albumu, numer konta). */
const val WseiSensitiveMask: String = "••••••"

/** Zwraca [value] albo maskę, gdy włączono tryb „Ukryj dane”. */
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
 * Przełącznik M3 z ikoną w kciuku (✓ / ✕) — stan jest czytelny także bez rozróżniania kolorów.
 * Przekaż `onCheckedChange = null`, gdy cały wiersz listy obsługuje przełączanie.
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

/** Ekspresyjny wskaźnik ładowania (M3 LoadingIndicator) z opcjonalnym podpisem. */
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
