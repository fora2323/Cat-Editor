package org.cat.editor.ui.preview

import android.content.res.Configuration
import java.util.regex.Pattern

/**
 * Intelligent parser for extracting @Preview annotations and @Composable
 * functions from Kotlin source files. Supports Android Studio multi-previews, device presets,
 * custom parameters, and smart auto-discovery.
 */
object ComposePreviewParser {

    private val PREVIEW_LIGHT_DARK_PATTERN = Pattern.compile("@PreviewLightDark\\b")
    private val PREVIEW_SCREEN_SIZES_PATTERN = Pattern.compile("@PreviewScreenSizes\\b")
    private val PREVIEW_FONT_SCALES_PATTERN = Pattern.compile("@PreviewFontScales\\b")

    private val PREVIEW_PATTERN = Pattern.compile("@Preview(?:\\s*\\(([^)]*)\\))?")
    private val COMPOSABLE_PATTERN = Pattern.compile("@Composable\\s*(?:(?:private|public|internal)\\s+)?fun\\s+([A-Za-z0-9_]+)\\s*\\(")

    /**
     * Checks if the given Kotlin code contains Jetpack Compose indicators.
     */
    fun isComposeSource(code: String?): Boolean {
        if (code.isNullOrBlank()) return false
        return code.contains("@Composable") ||
                code.contains("@Preview") ||
                code.contains("androidx.compose.") ||
                code.contains("Modifier.") ||
                code.contains("MaterialTheme.")
    }

    /**
     * Parses the Kotlin source code and returns all available ComposePreviewModels.
     */
    fun parse(sourceCode: String?): List<ComposePreviewModel> {
        val previews = mutableListOf<ComposePreviewModel>()
        if (sourceCode.isNullOrBlank()) {
            return previews
        }

        val codeWithoutComments = removeComments(sourceCode)

        // Find all function declarations with preceding annotations
        var index = 0
        while (index < codeWithoutComments.length) {
            val composablePos = codeWithoutComments.indexOf("@Composable", index)
            if (composablePos == -1) break

            val lookbackStart = maxOf(0, composablePos - 500)
            val precedingText = codeWithoutComments.substring(lookbackStart, composablePos)

            val funcPos = codeWithoutComments.indexOf("fun ", composablePos)
            if (funcPos != -1 && funcPos - composablePos < 100) {
                var nameStart = funcPos + 4
                while (nameStart < codeWithoutComments.length && codeWithoutComments[nameStart].isWhitespace()) {
                    nameStart++
                }
                var nameEnd = nameStart
                while (nameEnd < codeWithoutComments.length && Character.isJavaIdentifierPart(codeWithoutComments[nameEnd])) {
                    nameEnd++
                }

                if (nameEnd > nameStart) {
                    val functionName = codeWithoutComments.substring(nameStart, nameEnd)
                    val functionBody = extractFunctionBody(codeWithoutComments, nameEnd)

                    val parsedList = parsePreviewsForFunction(precedingText, functionName, functionBody)
                    if (parsedList.isNotEmpty()) {
                        previews.addAll(parsedList)
                    }
                }
            }

            index = composablePos + 11
        }

        // If no @Preview annotation was explicitly declared, auto-discover all @Composable functions
        if (previews.isEmpty()) {
            val m = COMPOSABLE_PATTERN.matcher(codeWithoutComments)
            while (m.find()) {
                val functionName = m.group(1) ?: continue
                val bodyStart = m.end()
                val body = extractFunctionBody(codeWithoutComments, bodyStart)

                val model = ComposePreviewModel(functionName, functionName).apply {
                    this.functionBody = body
                    this.showBackground = true
                    this.isAutoDetected = true
                }
                previews.add(model)
            }
        }

        // If still empty but code has content, generate a fallback model using the whole source
        if (previews.isEmpty()) {
            val defaultModel = ComposePreviewModel("Preview", "Live Preview").apply {
                this.showBackground = true
                this.functionBody = sourceCode
                this.isAutoDetected = true
            }
            previews.add(defaultModel)
        }

        return previews
    }

    private fun parsePreviewsForFunction(
        precedingText: String,
        functionName: String,
        functionBody: String
    ): List<ComposePreviewModel> {
        val list = mutableListOf<ComposePreviewModel>()

        // Check for Multi-Preview annotations
        if (PREVIEW_LIGHT_DARK_PATTERN.matcher(precedingText).find()) {
            list.add(ComposePreviewModel(functionName, "$functionName (Light)").apply {
                this.functionBody = functionBody
                this.showBackground = true
                this.uiMode = Configuration.UI_MODE_NIGHT_NO
            })
            list.add(ComposePreviewModel(functionName, "$functionName (Dark)").apply {
                this.functionBody = functionBody
                this.showBackground = true
                this.uiMode = Configuration.UI_MODE_NIGHT_YES
            })
        }

        if (PREVIEW_SCREEN_SIZES_PATTERN.matcher(precedingText).find()) {
            list.add(ComposePreviewModel(functionName, "$functionName (Phone)").apply {
                this.functionBody = functionBody
                this.widthDp = 412
                this.heightDp = 915
                this.showBackground = true
            })
            list.add(ComposePreviewModel(functionName, "$functionName (Foldable)").apply {
                this.functionBody = functionBody
                this.widthDp = 841
                this.heightDp = 673
                this.showBackground = true
            })
            list.add(ComposePreviewModel(functionName, "$functionName (Tablet)").apply {
                this.functionBody = functionBody
                this.widthDp = 1280
                this.heightDp = 800
                this.showBackground = true
            })
        }

        if (PREVIEW_FONT_SCALES_PATTERN.matcher(precedingText).find()) {
            list.add(ComposePreviewModel(functionName, "$functionName (1.0x Font)").apply {
                this.functionBody = functionBody
                this.fontScale = 1.0f
            })
            list.add(ComposePreviewModel(functionName, "$functionName (1.5x Font)").apply {
                this.functionBody = functionBody
                this.fontScale = 1.5f
            })
        }

        // Standard @Preview annotations
        val matcher = PREVIEW_PATTERN.matcher(precedingText)
        while (matcher.find()) {
            val args = matcher.group(1)
            val model = ComposePreviewModel(functionName, "").apply {
                this.functionBody = functionBody
            }

            if (!args.isNullOrBlank()) {
                parsePreviewArgs(args, model)
            }

            if (model.previewName.isEmpty()) {
                model.previewName = functionName
            }

            list.add(model)
        }

        return list
    }

    private fun parsePreviewArgs(args: String, model: ComposePreviewModel) {
        val pairs = args.split(",")
        for (pair in pairs) {
            val kv = pair.split("=", limit = 2)
            if (kv.size == 2) {
                val key = kv[0].trim()
                val valStr = kv[1].trim()

                when (key) {
                    "name" -> model.previewName = cleanQuotes(valStr)
                    "group" -> model.group = cleanQuotes(valStr)
                    "showBackground" -> model.showBackground = valStr.toBoolean()
                    "backgroundColor" -> {
                        try {
                            if (valStr.startsWith("0x", ignoreCase = true)) {
                                model.backgroundColor = valStr.substring(2).toLong(16).toInt()
                            } else {
                                model.backgroundColor = valStr.toLong().toInt()
                            }
                            model.showBackground = true
                        } catch (_: Exception) {}
                    }
                    "widthDp" -> {
                        try {
                            model.widthDp = valStr.replace(Regex("[^0-9]"), "").toInt()
                        } catch (_: Exception) {}
                    }
                    "heightDp" -> {
                        try {
                            model.heightDp = valStr.replace(Regex("[^0-9]"), "").toInt()
                        } catch (_: Exception) {}
                    }
                    "device" -> {
                        val dev = cleanQuotes(valStr)
                        model.device = dev
                        if (dev.contains("width=") && dev.contains("height=")) {
                            try {
                                val mw = Pattern.compile("width\\s*=\\s*([0-9]+)").matcher(dev)
                                val mh = Pattern.compile("height\\s*=\\s*([0-9]+)").matcher(dev)
                                if (mw.find() && mh.find()) {
                                    model.widthDp = (mw.group(1) ?: "").toInt()
                                    model.heightDp = (mh.group(1) ?: "").toInt()
                                }
                            } catch (_: Exception) {}
                        } else {
                            val dl = dev.lowercase()
                            when {
                                dl.contains("tablet") || dl.contains("nexus 10") || dl.contains("pixel_c") -> {
                                    model.widthDp = 1280
                                    model.heightDp = 800
                                }
                                dl.contains("foldable") || dl.contains("fold") -> {
                                    model.widthDp = 841
                                    model.heightDp = 673
                                }
                                dl.contains("compact") || dl.contains("small") -> {
                                    model.widthDp = 360
                                    model.heightDp = 640
                                }
                                dl.contains("desktop") || dl.contains("1080p") -> {
                                    model.widthDp = 1920
                                    model.heightDp = 1080
                                }
                                dl.contains("phone") || dl.contains("pixel_7") -> {
                                    model.widthDp = 412
                                    model.heightDp = 915
                                }
                            }
                        }
                    }
                    "uiMode" -> {
                        if (valStr.contains("NIGHT_YES") || valStr.contains("32")) {
                            model.uiMode = Configuration.UI_MODE_NIGHT_YES
                        } else if (valStr.contains("NIGHT_NO") || valStr.contains("16")) {
                            model.uiMode = Configuration.UI_MODE_NIGHT_NO
                        }
                    }
                    "fontScale" -> {
                        try {
                            model.fontScale = valStr.replace(Regex("[^0-9.]"), "").toFloat()
                        } catch (_: Exception) {}
                    }
                    "showSystemUi" -> model.showSystemUi = valStr.toBoolean()
                    "locale" -> model.locale = cleanQuotes(valStr)
                }
            } else if (kv.size == 1) {
                val trimmed = kv[0].trim()
                if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                    model.previewName = cleanQuotes(trimmed)
                }
            }
        }
    }

    private fun extractFunctionBody(code: String, searchStart: Int): String {
        val braceStart = code.indexOf('{', searchStart)
        if (braceStart == -1) return ""

        var depth = 1
        var i = braceStart + 1
        while (i < code.length && depth > 0) {
            val c = code[i]
            if (c == '{') {
                depth++
            } else if (c == '}') {
                depth--
            } else if (c == '"') {
                i++
                while (i < code.length && code[i] != '"') {
                    if (code[i] == '\\') i++
                    i++
                }
            }
            if (depth == 0) {
                return code.substring(braceStart + 1, i).trim()
            }
            i++
        }
        return code.substring(braceStart + 1).trim()
    }

    private fun removeComments(code: String): String {
        val withoutBlock = code.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        return withoutBlock.replace(Regex("//.*"), "")
    }

    private fun cleanQuotes(text: String?): String {
        if (text == null) return ""
        val trimmed = text.trim()
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
            return trimmed.substring(1, trimmed.length - 1)
        }
        return trimmed
    }
}
