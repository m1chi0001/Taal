package pl.lublin.wsei.plan.ui.update

import androidx.compose.runtime.Immutable

/**
 * A tiny, dependency-free Markdown model for GitHub release notes.
 *
 * Supported: ATX headings (`#`–`######`), bullets (`-`, `*`, `+`, nested by indentation),
 * numbered lists (`1.` / `1)`), block quotes (`>`), fenced code (```` ``` ````), horizontal
 * rules, paragraphs separated by blank lines and simple tables (cells joined with " · ").
 * HTML comments and tags are dropped. Inline syntax is handled by [MarkdownInline].
 *
 * Everything here is pure Kotlin (no Compose runtime), so it is cheap to `remember` and easy to test.
 */
@Immutable
sealed interface MarkdownBlock {
    /** [level] 1..6; [text] is still inline Markdown. */
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock

    /** List item; [marker] is null for bullets and "3." for numbered items; [depth] 0..3. */
    data class ListItem(val text: String, val depth: Int, val marker: String?) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class Code(val text: String) : MarkdownBlock
    data object Rule : MarkdownBlock
}

object MarkdownParser {
    private val headingRegex = Regex("^\\s{0,3}(#{1,6})\\s+(.*?)(?:\\s+#+)?\\s*$")
    private val bulletRegex = Regex("^(\\s*)[-*+]\\s+(.*)$")
    private val numberedRegex = Regex("^(\\s*)(\\d{1,4})[.)]\\s+(.*)$")
    private val quoteRegex = Regex("^\\s{0,3}>\\s?(.*)$")
    private val ruleRegex = Regex("^\\s{0,3}([-*_])(\\s*\\1){2,}\\s*$")
    private val fenceRegex = Regex("^\\s{0,3}(```|~~~).*$")
    private val tableSeparatorRegex = Regex("^\\s*\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?\\s*$")
    private val taskRegex = Regex("^\\[([ xX])]\\s+")
    private val htmlCommentRegex = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val htmlBreakRegex = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)

    // Tags only — `<https://…>` autolinks (scheme followed by ':') are kept.
    private val htmlTagRegex = Regex("</?(?![a-zA-Z][a-zA-Z0-9+.-]*:)[a-zA-Z][^<>]*>")

    /** Parses [markdown] into blocks; never throws, unknown syntax becomes plain text. */
    fun parse(markdown: String): List<MarkdownBlock> {
        if (markdown.isBlank()) return emptyList()
        val cleaned = markdown
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(htmlCommentRegex, "")
            .replace(htmlBreakRegex, "\n")
            .replace(htmlTagRegex, "")
        val blocks = mutableListOf<MarkdownBlock>()
        val paragraph = StringBuilder()
        val quote = StringBuilder()
        val code = StringBuilder()
        var inCode = false
        var lastWasListItem = false

        fun flushParagraph() {
            if (paragraph.isNotBlank()) blocks += MarkdownBlock.Paragraph(paragraph.toString().trim())
            paragraph.clear()
        }

        fun flushQuote() {
            if (quote.isNotBlank()) blocks += MarkdownBlock.Quote(quote.toString().trim())
            quote.clear()
        }

        fun flushAll() {
            flushParagraph()
            flushQuote()
        }

        for (rawLine in cleaned.split('\n')) {
            val line = rawLine.replace("\t", "    ")
            if (fenceRegex.matches(line)) {
                if (inCode) {
                    blocks += MarkdownBlock.Code(code.toString().trimEnd('\n'))
                    code.clear()
                    inCode = false
                } else {
                    flushAll()
                    inCode = true
                }
                lastWasListItem = false
                continue
            }
            if (inCode) {
                code.append(line).append('\n')
                continue
            }
            if (line.isBlank()) {
                flushAll()
                lastWasListItem = false
                continue
            }
            if (tableSeparatorRegex.matches(line) && line.contains('-') && line.contains('|')) continue

            val heading = headingRegex.matchEntire(line)
            if (heading != null) {
                flushAll()
                blocks += MarkdownBlock.Heading(heading.groupValues[1].length, heading.groupValues[2])
                lastWasListItem = false
                continue
            }
            if (ruleRegex.matches(line)) {
                flushAll()
                blocks += MarkdownBlock.Rule
                lastWasListItem = false
                continue
            }
            val bullet = bulletRegex.matchEntire(line)
            if (bullet != null) {
                flushAll()
                val text = bullet.groupValues[2].replace(taskRegex, "")
                blocks += MarkdownBlock.ListItem(text, depthOf(bullet.groupValues[1]), marker = null)
                lastWasListItem = true
                continue
            }
            val numbered = numberedRegex.matchEntire(line)
            if (numbered != null) {
                flushAll()
                blocks += MarkdownBlock.ListItem(
                    text = numbered.groupValues[3],
                    depth = depthOf(numbered.groupValues[1]),
                    marker = "${numbered.groupValues[2].toIntOrNull() ?: 1}."
                )
                lastWasListItem = true
                continue
            }
            val quoted = quoteRegex.matchEntire(line)
            if (quoted != null) {
                flushParagraph()
                if (quote.isNotEmpty()) quote.append(' ')
                quote.append(quoted.groupValues[1].trim())
                lastWasListItem = false
                continue
            }
            // Lazy continuation of a list item (wrapped text right below it).
            val previous = blocks.lastOrNull()
            if (lastWasListItem && previous is MarkdownBlock.ListItem && paragraph.isEmpty()) {
                blocks[blocks.lastIndex] = previous.copy(text = previous.text + " " + line.trim())
                continue
            }
            flushQuote()
            lastWasListItem = false
            val trimmed = line.trim()
            if (isTableRow(trimmed)) {
                // Each table row stays on its own line.
                flushParagraph()
                blocks += MarkdownBlock.Paragraph(tableRowText(trimmed))
                continue
            }
            if (paragraph.isNotEmpty()) paragraph.append(' ')
            paragraph.append(trimmed)
        }
        if (inCode && code.isNotEmpty()) blocks += MarkdownBlock.Code(code.toString().trimEnd('\n'))
        flushAll()
        return blocks
    }

    /**
     * Short highlights for the update prompt: the first [max] list items, or the first
     * paragraph when the notes have no list.
     */
    fun highlights(markdown: String, max: Int = 3): List<String> {
        val blocks = parse(markdown)
        val items = blocks.filterIsInstance<MarkdownBlock.ListItem>().filter { it.depth == 0 }.take(max).map { it.text }
        if (items.isNotEmpty()) return items
        return blocks.filterIsInstance<MarkdownBlock.Paragraph>().take(1).map { it.text }
    }

    private fun depthOf(indent: String): Int = (indent.length / 2).coerceIn(0, 3)

    private fun isTableRow(line: String): Boolean = line.startsWith('|') && line.count { it == '|' } >= 2

    /** `| a | b |` → `a · b`. */
    private fun tableRowText(line: String): String =
        line.trim('|').split('|').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · ")
}

/** One styled run of inline Markdown. Pure data, turned into an AnnotatedString by the renderer. */
@Immutable
data class InlineSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
    val strike: Boolean = false,
    val url: String? = null
)

/**
 * Inline Markdown: `**bold**` / `__bold__`, `*italic*` / `_italic_`, `` `code` ``,
 * `~~strike~~`, `[text](url)`, `![alt](url)` (alt text only), `<https://…>` and bare
 * `https://…` links, backslash escapes. Unclosed markers are kept as literal text.
 */
object MarkdownInline {
    private val bareUrlRegex = Regex("https?://[^\\s<>()\\[\\]]+")

    fun parse(text: String): List<InlineSpan> {
        val out = mutableListOf<InlineSpan>()
        parseInto(text, Style(), out)
        return merge(out)
    }

    /** Plain text without any Markdown syntax (for content descriptions or ellipsized lines). */
    fun plain(text: String): String = parse(text).joinToString("") { it.text }

    private data class Style(
        val bold: Boolean = false,
        val italic: Boolean = false,
        val strike: Boolean = false,
        val url: String? = null
    )

    private fun parseInto(text: String, style: Style, out: MutableList<InlineSpan>) {
        val buffer = StringBuilder()
        fun flush() {
            if (buffer.isNotEmpty()) {
                out += InlineSpan(buffer.toString(), style.bold, style.italic, false, style.strike, style.url)
                buffer.clear()
            }
        }

        var i = 0
        while (i < text.length) {
            val c = text[i]
            // Escapes: \* \_ \` \[ …
            if (c == '\\' && i + 1 < text.length && !text[i + 1].isLetterOrDigit() && !text[i + 1].isWhitespace()) {
                buffer.append(text[i + 1])
                i += 2
                continue
            }
            if (c == '`') {
                val ticks = countRun(text, i, '`')
                val close = text.indexOf("`".repeat(ticks), i + ticks)
                if (close > i) {
                    flush()
                    out += InlineSpan(
                        text = text.substring(i + ticks, close).trim(),
                        bold = style.bold,
                        italic = style.italic,
                        code = true,
                        strike = style.strike,
                        url = style.url
                    )
                    i = close + ticks
                    continue
                }
            }
            if ((c == '*' || c == '_') && i + 1 < text.length && text[i + 1] == c) {
                val marker = "$c$c"
                val close = findClose(text, i + 2, marker)
                if (close > i + 2 && (c == '*' || isWordBoundary(text, i - 1))) {
                    flush()
                    parseInto(text.substring(i + 2, close), style.copy(bold = true), out)
                    i = close + 2
                    continue
                }
            }
            if (c == '~' && text.startsWith("~~", i)) {
                val close = findClose(text, i + 2, "~~")
                if (close > i + 2) {
                    flush()
                    parseInto(text.substring(i + 2, close), style.copy(strike = true), out)
                    i = close + 2
                    continue
                }
            }
            if ((c == '*' || c == '_') && i + 1 < text.length && !text[i + 1].isWhitespace()) {
                val close = findSingleClose(text, i + 1, c)
                if (close > i + 1 && (c == '*' || isWordBoundary(text, i - 1))) {
                    flush()
                    parseInto(text.substring(i + 1, close), style.copy(italic = true), out)
                    i = close + 1
                    continue
                }
            }
            if (c == '!' && i + 1 < text.length && text[i + 1] == '[') {
                val link = parseLink(text, i + 1)
                if (link != null) {
                    flush()
                    parseInto(link.label, style, out)
                    i = link.end
                    continue
                }
            }
            if (c == '[') {
                val link = parseLink(text, i)
                if (link != null) {
                    flush()
                    parseInto(link.label.ifBlank { link.url }, style.copy(url = link.url.takeIf { isSafeUrl(it) }), out)
                    i = link.end
                    continue
                }
            }
            if (c == '<') {
                val close = text.indexOf('>', i + 1)
                if (close > i) {
                    val inner = text.substring(i + 1, close)
                    if (isSafeUrl(inner) && inner.none { it.isWhitespace() }) {
                        flush()
                        out += InlineSpan(inner, style.bold, style.italic, false, style.strike, inner)
                        i = close + 1
                        continue
                    }
                }
            }
            if (c == 'h' && style.url == null && isWordBoundary(text, i - 1) &&
                (text.startsWith("https://", i) || text.startsWith("http://", i))
            ) {
                val match = bareUrlRegex.find(text, i)
                if (match != null && match.range.first == i) {
                    val url = match.value.trimEnd('.', ',', ';', ':', '!', '?', '\'', '"')
                    flush()
                    out += InlineSpan(url, style.bold, style.italic, false, style.strike, url)
                    i += url.length
                    continue
                }
            }
            buffer.append(c)
            i++
        }
        flush()
    }

    private class Link(val label: String, val url: String, val end: Int)

    /** `[label](url "title")` starting at [start] (which points at '['). */
    private fun parseLink(text: String, start: Int): Link? {
        var depth = 0
        var j = start
        while (j < text.length) {
            when (text[j]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) break
                }
            }
            j++
        }
        if (j >= text.length || j + 1 >= text.length || text[j + 1] != '(') return null
        val close = text.indexOf(')', j + 2)
        if (close < 0) return null
        val target = text.substring(j + 2, close).trim().substringBefore(' ').removeSurrounding("<", ">")
        return Link(label = text.substring(start + 1, j), url = target, end = close + 1)
    }

    private fun countRun(text: String, start: Int, c: Char): Int {
        var n = 0
        while (start + n < text.length && text[start + n] == c) n++
        return n
    }

    private fun findClose(text: String, from: Int, marker: String): Int {
        var idx = text.indexOf(marker, from)
        while (idx >= 0) {
            if (idx > from && !text[idx - 1].isWhitespace()) return idx
            idx = text.indexOf(marker, idx + 1)
        }
        return -1
    }

    /** Closing single `*`/`_` that is not part of a double marker and not preceded by whitespace. */
    private fun findSingleClose(text: String, from: Int, c: Char): Int {
        var j = from
        while (j < text.length) {
            if (text[j] == c) {
                val doubled = (j + 1 < text.length && text[j + 1] == c)
                if (doubled) {
                    j += 2
                    continue
                }
                val closesWord = c == '*' || j + 1 >= text.length || !text[j + 1].isLetterOrDigit()
                if (!text[j - 1].isWhitespace() && closesWord) return j
            }
            j++
        }
        return -1
    }

    private fun isWordBoundary(text: String, index: Int): Boolean =
        index < 0 || !text[index].isLetterOrDigit()

    private fun isSafeUrl(url: String): Boolean =
        url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("mailto:", ignoreCase = true)

    private fun merge(spans: List<InlineSpan>): List<InlineSpan> {
        val result = ArrayList<InlineSpan>(spans.size)
        for (span in spans) {
            if (span.text.isEmpty()) continue
            val last = result.lastOrNull()
            if (last != null && last.copy(text = "") == span.copy(text = "")) {
                result[result.lastIndex] = last.copy(text = last.text + span.text)
            } else {
                result += span
            }
        }
        return result
    }
}
