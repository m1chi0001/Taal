package pl.lublin.wsei.plan.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.ui.theme.WseiSpacing

/** Colours used for inline Markdown; resolved from the theme by [rememberMarkdownColors]. */
@Immutable
data class MarkdownColors(
    val link: Color,
    val codeBackground: Color,
    val codeContent: Color
)

@Composable
fun rememberMarkdownColors(): MarkdownColors {
    val colors = MaterialTheme.colorScheme
    return remember(colors.primary, colors.surfaceContainerHighest, colors.onSurface) {
        MarkdownColors(
            link = colors.primary,
            codeBackground = colors.surfaceContainerHighest,
            codeContent = colors.onSurface
        )
    }
}

/** Turns inline Markdown into an [AnnotatedString] with clickable links (opened by `Text`). */
fun markdownAnnotatedString(text: String, colors: MarkdownColors): AnnotatedString {
    val spans = MarkdownInline.parse(text)
    return buildAnnotatedString {
        for (span in spans) {
            val style = SpanStyle(
                fontWeight = if (span.bold) FontWeight.Bold else null,
                fontStyle = if (span.italic) FontStyle.Italic else null,
                fontFamily = if (span.code) FontFamily.Monospace else null,
                background = if (span.code) colors.codeBackground else Color.Unspecified,
                color = if (span.code) colors.codeContent else Color.Unspecified,
                textDecoration = if (span.strike) TextDecoration.LineThrough else null
            )
            val url = span.url
            if (url != null) {
                val linkStyles = TextLinkStyles(
                    style = SpanStyle(color = colors.link, textDecoration = TextDecoration.Underline)
                )
                withLink(LinkAnnotation.Url(url, linkStyles)) {
                    withStyle(style) { append(span.text) }
                }
            } else {
                withStyle(style) { append(span.text) }
            }
        }
    }
}

/**
 * Renders GitHub-flavoured release notes with Material typography — no raw `#`, `*` or `` ` ``
 * is ever shown. Links open in the browser.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    secondaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }
    val markdownColors = rememberMarkdownColors()
    val typography = MaterialTheme.typography
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
    ) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> typography.titleLarge
                        2 -> typography.titleMedium
                        else -> typography.titleSmall
                    }
                    InlineText(
                        text = block.text,
                        style = style.copy(fontWeight = FontWeight.Bold),
                        color = color,
                        colors = markdownColors,
                        modifier = Modifier.padding(top = if (index == 0) 0.dp else WseiSpacing.xs)
                    )
                }
                is MarkdownBlock.Paragraph -> InlineText(
                    text = block.text,
                    style = typography.bodyMedium,
                    color = color,
                    colors = markdownColors
                )
                is MarkdownBlock.ListItem -> MarkdownListItem(
                    item = block,
                    style = typography.bodyMedium,
                    color = color,
                    markerColor = MaterialTheme.colorScheme.primary,
                    colors = markdownColors
                )
                is MarkdownBlock.Quote -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    InlineText(
                        text = block.text,
                        style = typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = secondaryColor,
                        colors = markdownColors,
                        modifier = Modifier.weight(1f)
                    )
                }
                is MarkdownBlock.Code -> Surface(
                    shape = RoundedCornerShape(WseiSpacing.m),
                    color = markdownColors.codeBackground,
                    contentColor = markdownColors.codeContent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = block.text,
                        style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        softWrap = false,
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(WseiSpacing.m)
                    )
                }
                MarkdownBlock.Rule -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = WseiSpacing.xs),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

/**
 * Compact bullet list of highlights (update prompt). Each line is limited to [maxLinesPerItem].
 */
@Composable
fun MarkdownHighlights(
    items: List<String>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    markerColor: Color = MaterialTheme.colorScheme.primary,
    maxLinesPerItem: Int = 2
) {
    val markdownColors = rememberMarkdownColors()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)) {
        items.forEach { item ->
            MarkdownListItem(
                item = MarkdownBlock.ListItem(item, depth = 0, marker = null),
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                markerColor = markerColor,
                colors = markdownColors,
                maxLines = maxLinesPerItem
            )
        }
    }
}

@Composable
private fun MarkdownListItem(
    item: MarkdownBlock.ListItem,
    style: TextStyle,
    color: Color,
    markerColor: Color,
    colors: MarkdownColors,
    maxLines: Int = Int.MAX_VALUE
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = WseiSpacing.l * item.depth),
        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
    ) {
        val marker = item.marker
        if (marker == null) {
            // Dot centred on the first text line.
            val density = LocalDensity.current
            val lineHeight = if (style.lineHeight.isSp) with(density) { style.lineHeight.toDp() } else 20.dp
            Box(
                modifier = Modifier.height(lineHeight),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (item.depth == 0) 6.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (item.depth == 0) markerColor else markerColor.copy(alpha = 0.6f))
                )
            }
        } else {
            Text(
                text = marker,
                style = style.copy(fontWeight = FontWeight.Bold),
                color = markerColor,
                modifier = Modifier.widthIn(min = 18.dp)
            )
        }
        InlineText(
            text = item.text,
            style = style,
            color = color,
            colors = colors,
            maxLines = maxLines,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InlineText(
    text: String,
    style: TextStyle,
    color: Color,
    colors: MarkdownColors,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE
) {
    val annotated = remember(text, colors) { markdownAnnotatedString(text, colors) }
    Text(
        text = annotated,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
