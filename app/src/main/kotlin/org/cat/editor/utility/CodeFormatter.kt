package org.cat.editor.utility

import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.xml.sax.InputSource

/**
 * Intelligent code formatter supporting Java, Kotlin, XML, and Gradle.
 * Formats messy or pasted code to adhere to Android Studio and Material standards.
 */
object CodeFormatter {

    private const val INDENT_SIZE = 4
    private val INDENT_STR = " ".repeat(INDENT_SIZE)

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

        // First attempt standard XML DOM formatting if valid well-formed XML
        val domFormatted = formatXmlWithDom(rawXml)
        val sourceToFormat = domFormatted ?: rawXml

        // Clean and align tags and Android attributes
        return formatXmlTags(sourceToFormat)
    }

    private fun formatXmlWithDom(xml: String): String? {
        return try {
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            // Disable DTD entity expansion for security
            try {
                dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
                dbf.setFeature("http://xml.org/sax/features/external-general-entities", false)
                dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            } catch (_: Throwable) {}

            val db = dbf.newDocumentBuilder()
            val doc = db.parse(InputSource(StringReader(xml)))
            doc.normalizeDocument()

            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            transformer.setOutputProperty(
                OutputKeys.OMIT_XML_DECLARATION,
                if (xml.trim().startsWith("<?xml")) "no" else "yes"
            )
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "$INDENT_SIZE")

            val writer = StringWriter()
            transformer.transform(DOMSource(doc), StreamResult(writer))
            writer.toString()
        } catch (_: Throwable) {
            null
        }
    }

    private fun formatXmlTags(xml: String): String {
        val lines = xml.lines()
        val result = StringBuilder()
        var indentLevel = 0

        var inComment = false
        var inCdata = false

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                if (result.isNotEmpty() && !result.endsWith("\n\n")) {
                    result.append("\n")
                }
                continue
            }

            // Handle multi-line comment state
            if (inComment) {
                result.append(INDENT_STR.repeat(indentLevel.coerceAtLeast(0))).append(line).append("\n")
                if (line.contains("-->")) inComment = false
                continue
            }
            if (line.startsWith("<!--") && !line.contains("-->")) {
                inComment = true
                result.append(INDENT_STR.repeat(indentLevel.coerceAtLeast(0))).append(line).append("\n")
                continue
            }

            // Handle CDATA state
            if (inCdata) {
                result.append(line).append("\n")
                if (line.contains("]]>")) inCdata = false
                continue
            }
            if (line.contains("<![CDATA[") && !line.contains("]]>")) {
                inCdata = true
                result.append(INDENT_STR.repeat(indentLevel.coerceAtLeast(0))).append(line).append("\n")
                continue
            }

            // XML Declaration <?xml ... ?>
            if (line.startsWith("<?xml") || line.startsWith("<!DOCTYPE")) {
                result.append(line).append("\n")
                continue
            }

            // Closing tag on its own: </tag>
            if (line.startsWith("</")) {
                indentLevel = (indentLevel - 1).coerceAtLeast(0)
                result.append(INDENT_STR.repeat(indentLevel)).append(line).append("\n")
                continue
            }

            // Closing of a multi-line tag: /> or >
            if (line == "/>" || line == ">") {
                val currentIndent = (indentLevel - 1).coerceAtLeast(0)
                result.append(INDENT_STR.repeat(currentIndent)).append(line).append("\n")
                if (line == "/>") {
                    indentLevel = (indentLevel - 1).coerceAtLeast(0)
                }
                continue
            }

            // Tag that opens and closes on the same line: <tag ...>text</tag> or <tag .../>
            val isSelfClosing = line.endsWith("/>")
            val isSingleLineTag = isSelfClosing || (line.startsWith("<") && line.contains("</") && line.endsWith(">"))

            if (isSingleLineTag) {
                // If the single-line tag has multiple attributes, format attributes nicely
                if (shouldSplitAttributes(line)) {
                    val formattedTag = splitTagAttributes(line, indentLevel)
                    result.append(formattedTag)
                } else {
                    result.append(INDENT_STR.repeat(indentLevel)).append(line).append("\n")
                }
                continue
            }

            // Multi-line Opening tag: <tag ...
            if (line.startsWith("<") && !line.startsWith("</")) {
                if (shouldSplitAttributes(line)) {
                    val formattedTag = splitTagAttributes(line, indentLevel)
                    result.append(formattedTag)
                    if (!line.endsWith("/>")) {
                        indentLevel++
                    }
                } else {
                    result.append(INDENT_STR.repeat(indentLevel)).append(line).append("\n")
                    if (!line.endsWith("/>") && !line.endsWith(">")) {
                        // Tag header continues onto next line
                    } else if (line.endsWith(">")) {
                        indentLevel++
                    }
                }
                continue
            }

            // Attribute on continuation line: android:layout_width="..."
            if (line.contains("=\"") || line.contains("='")) {
                result.append(INDENT_STR.repeat(indentLevel + 1)).append(line).append("\n")
                if (line.endsWith("/>")) {
                    indentLevel = (indentLevel - 1).coerceAtLeast(0)
                }
                continue
            }

            // Normal text content inside tag
            result.append(INDENT_STR.repeat(indentLevel)).append(line).append("\n")
        }

        return result.toString().trimEnd() + "\n"
    }

    private fun shouldSplitAttributes(tagLine: String): Boolean {
        if (!tagLine.startsWith("<") || tagLine.startsWith("</")) return false
        val attrCount = countOccurrences(tagLine, "=\"") + countOccurrences(tagLine, "='")
        return attrCount >= 2 && tagLine.length > 70
    }

    private fun splitTagAttributes(tagLine: String, indentLevel: Int): String {
        val baseIndent = INDENT_STR.repeat(indentLevel)
        val attrIndent = INDENT_STR.repeat(indentLevel + 1)

        val firstSpace = tagLine.indexOf(' ')
        if (firstSpace == -1) return baseIndent + tagLine + "\n"

        val tagName = tagLine.substring(0, firstSpace).trim()
        val rest = tagLine.substring(firstSpace).trim()

        val isSelfClosing = rest.endsWith("/>")
        val cleanRest = if (isSelfClosing) rest.removeSuffix("/>").trim() else rest.removeSuffix(">").trim()

        val attrs = extractAttributes(cleanRest)
        val sb = StringBuilder()
        sb.append(baseIndent).append(tagName).append("\n")

        for (attr in attrs) {
            sb.append(attrIndent).append(attr).append("\n")
        }

        if (isSelfClosing) {
            sb.append(baseIndent).append("/>\n")
        } else {
            sb.append(baseIndent).append(">\n")
        }
        return sb.toString()
    }

    private fun extractAttributes(text: String): List<String> {
        val list = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuote = false
        var quoteChar = '"'

        for (i in text.indices) {
            val c = text[i]
            if (c == '"' || c == '\'') {
                if (!inQuote) {
                    inQuote = true
                    quoteChar = c
                } else if (quoteChar == c) {
                    inQuote = false
                }
            }

            if (c.isWhitespace() && !inQuote) {
                if (sb.isNotBlank()) {
                    list.add(sb.toString().trim())
                    sb.clear()
                }
            } else {
                sb.append(c)
            }
        }
        if (sb.isNotBlank()) {
            list.add(sb.toString().trim())
        }
        return list
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

            // Handle blank lines: collapse consecutive blank lines
            if (trimmed.isEmpty()) {
                if (processedLines.isNotEmpty() && processedLines.last().isNotEmpty()) {
                    processedLines.add("")
                }
                continue
            }

            // Triple-quote Kotlin multiline string handling: """ ... """
            if (inTripleQuote) {
                processedLines.add(rawLine) // preserve raw indentation inside multiline strings
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

            // Block comment handling: /* ... */
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

            // Compute change in braces/parentheses for this line
            val (openCount, closeCount, leadingCloses) = analyzeBraces(trimmed)

            // Adjust indent for leading close braces on this line (e.g., "}", ")", "},")
            val currentLineIndent = (indentLevel - leadingCloses).coerceAtLeast(0)

            // Normalize spacing outside of strings and comments
            val spacedLine = normalizeLineSpacing(trimmed, isKotlin)

            // Check if line is a continuation line (starts with ., ?., ->, &&, ||, +, :, ?)
            val isContinuation = isContinuationLine(trimmed)
            val continuationIndent = if (isContinuation && leadingCloses == 0) 1 else 0

            val finalIndent = currentLineIndent + continuationIndent
            processedLines.add(INDENT_STR.repeat(finalIndent) + spacedLine)

            // Update indent for subsequent lines
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

            // Check for line comments
            if (!inString && !inChar && c == '/' && i + 1 < line.length && line[i + 1] == '/') {
                break
            }

            // String & Char literals
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

            // Single line comment
            if (!inString && !inChar && c == '/' && i + 1 < line.length && line[i + 1] == '/') {
                inComment = true
                sb.append(line.substring(i))
                break
            }

            // String escape
            if (c == '\\' && (inString || inChar)) {
                sb.append(c)
                if (i + 1 < line.length) {
                    sb.append(line[i + 1])
                    i += 2
                    continue
                }
            }

            // Strings
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

            // Outside strings & comments: normalize spacing
            when {
                // Ensure space before opening curly brace: fun foo(){ -> fun foo() {
                c == '{' -> {
                    if (sb.isNotEmpty() && !sb.endsWith(" ") && !sb.endsWith("(") && !sb.endsWith("$")) {
                        sb.append(" ")
                    }
                    sb.append(c)
                }

                // Ensure space after comma: foo(a,b) -> foo(a, b)
                c == ',' -> {
                    sb.append(c)
                    if (i + 1 < line.length && !line[i + 1].isWhitespace() && line[i + 1] != ')') {
                        sb.append(" ")
                    }
                }

                // Ensure space after semicolon in for loops: for (int i=0;i<10;i++) -> for (int i=0; i<10; i++)
                c == ';' -> {
                    sb.append(c)
                    if (i + 1 < line.length && !line[i + 1].isWhitespace()) {
                        sb.append(" ")
                    }
                }

                // Spacing around binary operators: =, +=, -=, *=, /=, ==, !=, <=, >=, ->
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
                    // Check if it is a default argument or assignment, avoiding == or !=
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
