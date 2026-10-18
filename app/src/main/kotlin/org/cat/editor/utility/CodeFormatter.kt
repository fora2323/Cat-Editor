package org.cat.editor.utility

/**
 * Intelligent code formatter supporting Java, Kotlin, XML, and Gradle.
 * Formats messy or pasted code to adhere to Android Studio and Material standards.
 */
object CodeFormatter {

    private const val INDENT_SIZE = 4
    private val INDENT_STR = " ".repeat(INDENT_SIZE)

    // --- XML FORMATTER CONSTANTS ---
    private val XML_TAG_REGEX = Regex("<!--.*?-->|<\\?.*?\\?>|<!\\[CDATA\\[.*?\\]\\]>|<[^>]+>", RegexOption.DOT_MATCHES_ALL)
    private val XML_ATTR_REGEX = Regex("""[^\s=]+=("[^"]*"|'[^']*')""")
    private val XML_ALWAYS_WRAP_TAGS = setOf(
        "manifest", "application", "uses-library", "meta-data", 
        "activity", "activity-alias", "service", "receiver", 
        "provider", "intent-filter"
    )
    private const val XML_CUSTOM_LINE_LENGTH = 150
    private const val XML_MAX_LINE_LENGTH = 100

    /**
     * Format given code based on file path extension.
     */
    fun format(code: String, filePath: String): String {
        if (code.isBlank()) return code
        val lower = filePath.lowercase().trim()
        return try {
            when {
                lower.endsWith(".xml") -> formatXml(code)
                lower.endsWith(".java") -> formatBraceLanguage(code, isKotlin = false)
                lower.endsWith(".kt") || lower.endsWith(".kts") -> formatBraceLanguage(code, isKotlin = true)
                lower.endsWith(".gradle") -> formatBraceLanguage(code, isKotlin = false)
                else -> code
            }
        } catch (_: Throwable) {
            code
        }
    }

    /**
     * Formats XML code with clean indentation and Android Studio style attribute alignment.
     */
    fun formatXml(rawXml: String): String {
        if (rawXml.isBlank()) return rawXml

        val tokens = parseXmlTokens(rawXml)
        val sb = StringBuilder()
        var depth = 0
        var inlineMode = false

        for (i in tokens.indices) {
            val token = tokens[i]

            when (token) {
                is XmlToken.Decl -> {
                    if (sb.isNotEmpty() && !sb.endsWith("\n")) sb.append("\n")
                    sb.append(token.raw).append("\n")
                    inlineMode = false
                }
                is XmlToken.Comment -> {
                    if (sb.isNotEmpty() && !sb.endsWith("\n")) sb.append("\n")
                    sb.append(INDENT_STR.repeat(depth)).append(token.raw).append("\n")
                    inlineMode = false
                }
                is XmlToken.Text -> {
                    val isBlank = token.raw.isBlank()
                    val hasNewline = token.raw.contains("\n")

                    if (isBlank && hasNewline) {
                        continue
                    }

                    if (!hasNewline) {
                        if (inlineMode) {
                            sb.append(token.raw)
                        } else {
                            sb.append(INDENT_STR.repeat(depth)).append(token.raw.trim())
                            inlineMode = true
                        }
                    } else {
                        if (sb.isNotEmpty() && !sb.endsWith("\n")) sb.append("\n")
                        val lines = token.raw.lines()
                        for (line in lines) {
                            if (line.isBlank()) continue
                            sb.append(INDENT_STR.repeat(depth)).append(line.trim()).append("\n")
                        }
                        inlineMode = false
                    }
                }
                is XmlToken.Tag -> {
                    if (token.isClosing) {
                        depth = (depth - 1).coerceAtLeast(0)
                        
                        if (depth == 0 && !inlineMode) {
                            if (sb.isNotEmpty() && !sb.endsWith("\n\n")) sb.append("\n")
                        }

                        if (inlineMode) {
                            sb.append(token.raw).append("\n")
                        } else {
                            if (sb.isNotEmpty() && !sb.endsWith("\n")) sb.append("\n")
                            sb.append(INDENT_STR.repeat(depth)).append(token.raw).append("\n")
                        }
                        inlineMode = false
                    } else {
                        if (sb.isNotEmpty() && !sb.endsWith("\n")) sb.append("\n")
                        
                        if (depth == 1) {
                            if (sb.isNotEmpty() && !sb.endsWith("\n\n")) sb.append("\n")
                        }

                        val formattedTag = formatTag(token, depth)

                        var isInlineElement = false
                        if (!token.isSelfClosing) {
                            var j = i + 1
                            var validInline = true
                            var hasText = false
                            while (j < tokens.size) {
                                val next = tokens[j]
                                if (next is XmlToken.Text) {
                                    if (next.raw.contains("\n")) {
                                        validInline = false
                                        break
                                    }
                                    if (next.raw.isNotBlank()) {
                                        if (hasText) { validInline = false; break }
                                        hasText = true
                                    }
                                } else if (next is XmlToken.Tag && next.isClosing && next.name == token.name) {
                                    break
                                } else {
                                    validInline = false
                                    break
                                }
                                j++
                            }
                            isInlineElement = validInline
                        }

                        if (isInlineElement) {
                            sb.append(INDENT_STR.repeat(depth)).append(formattedTag)
                            inlineMode = true
                        } else {
                            sb.append(INDENT_STR.repeat(depth)).append(formattedTag).append("\n")
                            inlineMode = false
                        }

                        if (!token.isSelfClosing) {
                            depth++
                        }
                    }
                }
            }
        }
        return sb.toString().trim() + "\n"
    }

    private fun parseXmlTokens(rawXml: String): List<XmlToken> {
        val tokens = mutableListOf<XmlToken>()
        var lastEnd = 0
        XML_TAG_REGEX.findAll(rawXml).forEach { match ->
            val textBefore = rawXml.substring(lastEnd, match.range.first)
            if (textBefore.isNotEmpty()) {
                tokens.add(XmlToken.Text(textBefore))
            }
            val value = match.value
            when {
                value.startsWith("<!--") -> tokens.add(XmlToken.Comment(value))
                value.startsWith("<?") || value.startsWith("<!") -> tokens.add(XmlToken.Decl(value))
                value.startsWith("</") -> {
                    val name = value.removePrefix("</").removeSuffix(">").trim()
                    tokens.add(XmlToken.Tag(value, name, isClosing = true, isSelfClosing = false, attrsText = ""))
                }
                value.startsWith("<") -> {
                    val inner = value.removePrefix("<").removeSuffix(">")
                    val isSelfClosing = inner.endsWith("/")
                    val cleanInner = if (isSelfClosing) inner.removeSuffix("/").trimEnd() else inner
                    val firstSpace = cleanInner.indexOfFirst { it.isWhitespace() }
                    if (firstSpace == -1) {
                        tokens.add(XmlToken.Tag(value, cleanInner, isClosing = false, isSelfClosing = isSelfClosing, attrsText = ""))
                    } else {
                        val name = cleanInner.substring(0, firstSpace)
                        val attrs = cleanInner.substring(firstSpace).trim()
                        tokens.add(XmlToken.Tag(value, name, isClosing = false, isSelfClosing = isSelfClosing, attrsText = attrs))
                    }
                }
            }
            lastEnd = match.range.last + 1
        }
        val textAfter = rawXml.substring(lastEnd)
        if (textAfter.isNotEmpty()) {
            tokens.add(XmlToken.Text(textAfter))
        }
        return tokens
    }

    private fun formatTag(token: XmlToken.Tag, depth: Int): String {
        if (token.attrsText.isBlank()) {
            return if (token.isSelfClosing) "<${token.name} />" else "<${token.name}>"
        }

        val attrs = XML_ATTR_REGEX.findAll(token.attrsText).map { it.value }.toList()
        
        val originalAttrsNoSpaces = token.attrsText.replace(Regex("\\s+"), "")
        val parsedAttrsNoSpaces = attrs.joinToString("").replace(Regex("\\s+"), "")
        if (originalAttrsNoSpaces != parsedAttrsNoSpaces) {
            return token.raw
        }

        var shouldSplit = false
        if (attrs.size >= 2) {
            val isAlwaysWrap = XML_ALWAYS_WRAP_TAGS.contains(token.name)
            val maxLength = if (token.name == "uses-permission") XML_CUSTOM_LINE_LENGTH else XML_MAX_LINE_LENGTH
            
            val singleLineTag = buildString {
                append("<").append(token.name)
                for (attr in attrs) {
                    append(" ").append(attr)
                }
                if (token.isSelfClosing) append(" />") else append(">")
            }
            val totalLength = depth * INDENT_STR.length + singleLineTag.length
            
            if (isAlwaysWrap || totalLength > maxLength) {
                shouldSplit = true
            }
        }

        // Pengecualian khusus: tag <item> biarkan lurus sejajar satu baris
        if (token.name == "item") {
            shouldSplit = false
        }

        if (!shouldSplit) {
            return buildString {
                append("<").append(token.name)
                for (attr in attrs) {
                    append(" ").append(attr)
                }
                if (token.isSelfClosing) append(" />") else append(">")
            }
        }

        return buildString {
            append("<").append(token.name)
            
            var firstAttrIsXmlns = false
            if (attrs.isNotEmpty() && attrs[0].startsWith("xmlns:")) {
                firstAttrIsXmlns = true
                append(" ").append(attrs[0])
            }

            val startIndex = if (firstAttrIsXmlns) 1 else 0
            val attrIndent = INDENT_STR.repeat(depth + 1)
            
            for (i in startIndex until attrs.size) {
                append("\n").append(attrIndent).append(attrs[i])
            }
            
            if (token.isSelfClosing) {
                append(" />")
            } else {
                append(">")
            }
        }
    }

    private sealed class XmlToken {
        class Tag(val raw: String, val name: String, val isClosing: Boolean, val isSelfClosing: Boolean, val attrsText: String) : XmlToken()
        class Comment(val raw: String) : XmlToken()
        class Text(val raw: String) : XmlToken()
        class Decl(val raw: String) : XmlToken()
    }

    private fun countOccurrences(haystack: String, needle: String): Int {
        var count = 0
        var idx = 0
        while (true) {
            val next = haystack.indexOf(needle, idx)
            if (next == -1) break
            count++
            idx = next + needle.length
        }
        return count
    }

    /**
     * Formatter for Java, Kotlin, and Gradle code.
     * Accurately parses scopes, preserves string literals & comments, adjusts spacing,
     * and normalizes indentation.
     */
    fun formatBraceLanguage(source: String, isKotlin: Boolean): String {
        if (source.isBlank()) return source

        val rawLines = source.lines()
        val processedLines = mutableListOf<String>()

        var indentLevel = 0
        var inBlockComment = false
        var inTripleQuote = false

        for (rawLine in rawLines) {
            val trimmed = rawLine.trim()

            if (trimmed.isEmpty()) {
                if (processedLines.isNotEmpty() && processedLines.last().isNotEmpty()) {
                    processedLines.add("")
                }
                continue
            }

            if (inTripleQuote) {
                processedLines.add(rawLine) 
                if (trimmed.contains("\"\"\"")) {
                    inTripleQuote = false
                }
                continue
            }
            if (isKotlin && countOccurrences(trimmed, "\"\"\"") % 2 != 0) {
                inTripleQuote = true
                processedLines.add(INDENT_STR.repeat(indentLevel) + trimmed)
                continue
            }

            if (inBlockComment) {
                val commentIndent = if (trimmed.startsWith("*")) " " else ""
                processedLines.add(INDENT_STR.repeat(indentLevel) + commentIndent + trimmed)
                if (trimmed.contains("*/")) {
                    inBlockComment = false
                }
                continue
            }
            if (trimmed.startsWith("/*") && !trimmed.contains("*/")) {
                inBlockComment = true
                processedLines.add(INDENT_STR.repeat(indentLevel) + trimmed)
                continue
            }

            val (openCount, closeCount, leadingCloses) = analyzeBraces(trimmed)
            val currentLineIndent = (indentLevel - leadingCloses).coerceAtLeast(0)
            val spacedLine = normalizeLineSpacing(trimmed, isKotlin)
            
            val isContinuation = isContinuationLine(trimmed)
            val continuationIndent = if (isContinuation && leadingCloses == 0) 1 else 0

            val finalIndent = currentLineIndent + continuationIndent
            processedLines.add(INDENT_STR.repeat(finalIndent) + spacedLine)

            indentLevel = (indentLevel + openCount - closeCount).coerceAtLeast(0)
        }

        return processedLines.joinToString("\n").trimEnd() + "\n"
    }

    private data class BraceAnalysis(val openCount: Int, val closeCount: Int, val leadingCloses: Int)

    private fun analyzeBraces(line: String): BraceAnalysis {
        var open = 0
        var close = 0
        var leadingCloses = 0
        var checkingLeading = true

        var inString = false
        var stringChar = ' '
        var inChar = false
        var i = 0

        while (i < line.length) {
            val c = line[i]

            if (!inString && !inChar && c == '/' && i + 1 < line.length && line[i + 1] == '/') {
                break
            }

            if (c == '\\' && (inString || inChar)) {
                i += 2
                continue
            }
            if (c == '"' && !inChar) {
                inString = !inString
                stringChar = if (inString) '"' else ' '
                checkingLeading = false
            } else if (c == '\'' && !inString) {
                inChar = !inChar
                checkingLeading = false
            } else if (!inString && !inChar) {
                when (c) {
                    '{', '(', '[' -> {
                        open++
                        checkingLeading = false
                    }
                    '}', ')', ']' -> {
                        close++
                        if (checkingLeading) {
                            leadingCloses++
                        }
                    }
                    else -> {
                        if (!c.isWhitespace() && c != ',' && c != ';') {
                            checkingLeading = false
                        }
                    }
                }
            }
            i++
        }

        return BraceAnalysis(open, close, leadingCloses)
    }

    private fun isContinuationLine(line: String): Boolean {
        return line.startsWith(".") ||
                line.startsWith("?.") ||
                line.startsWith("?:") ||
                line.startsWith("&&") ||
                line.startsWith("||") ||
                line.startsWith("+ ") ||
                line.startsWith("->")
    }

    private fun normalizeLineSpacing(line: String, isKotlin: Boolean): String {
        val sb = StringBuilder()
        var inString = false
        var stringChar = ' '
        var inChar = false
        var inComment = false
        var i = 0

        while (i < line.length) {
            val c = line[i]

            if (!inString && !inChar && c == '/' && i + 1 < line.length && line[i + 1] == '/') {
                inComment = true
                sb.append(line.substring(i))
                break
            }

            if (c == '\\' && (inString || inChar)) {
                sb.append(c)
                if (i + 1 < line.length) {
                    sb.append(line[i + 1])
                    i += 2
                    continue
                }
            }

            if (c == '"' && !inChar) {
                inString = !inString
                sb.append(c)
                i++
                continue
            }
            if (c == '\'' && !inString) {
                inChar = !inChar
                sb.append(c)
                i++
                continue
            }

            if (inString || inChar) {
                sb.append(c)
                i++
                continue
            }

            when {
                c == '{' -> {
                    if (sb.isNotEmpty() && !sb.endsWith(" ") && !sb.endsWith("(") && !sb.endsWith("$")) {
                        sb.append(" ")
                    }
                    sb.append(c)
                }

                c == ',' -> {
                    sb.append(c)
                    if (i + 1 < line.length && !line[i + 1].isWhitespace() && line[i + 1] != ')') {
                        sb.append(" ")
                    }
                }

                c == ';' -> {
                    sb.append(c)
                    if (i + 1 < line.length && !line[i + 1].isWhitespace()) {
                        sb.append(" ")
                    }
                }

                c == '=' && i + 1 < line.length && line[i + 1] == '=' -> {
                    ensureSpacedOperator(sb, "==")
                    i += 2
                    continue
                }
                c == '!' && i + 1 < line.length && line[i + 1] == '=' -> {
                    ensureSpacedOperator(sb, "!=")
                    i += 2
                    continue
                }
                c == '-' && i + 1 < line.length && line[i + 1] == '>' -> {
                    ensureSpacedOperator(sb, "->")
                    i += 2
                    continue
                }
                c == '+' && i + 1 < line.length && line[i + 1] == '=' -> {
                    ensureSpacedOperator(sb, "+=")
                    i += 2
                    continue
                }
                c == '-' && i + 1 < line.length && line[i + 1] == '=' -> {
                    ensureSpacedOperator(sb, "-=")
                    i += 2
                    continue
                }
                c == '=' -> {
                    if (i + 1 < line.length && line[i + 1] != '=') {
                        ensureSpacedOperator(sb, "=")
                    } else {
                        sb.append(c)
                    }
                }

                else -> sb.append(c)
            }
            i++
        }

        return sb.toString()
    }

    private fun ensureSpacedOperator(sb: StringBuilder, op: String) {
        if (sb.isNotEmpty() && !sb.endsWith(" ")) {
            sb.append(" ")
        }
        sb.append(op)
        sb.append(" ")
    }
}