package org.cat.editor.ui.preview

import java.util.regex.Pattern

/**
 * Parses Jetpack Compose source code / function body into an AST tree of ComposeNodes.
 * Supports Layouts, Material 3 Components, Modifiers, variable expansion, and nested Composable function resolution.
 */
object ComposeAstParser {

    private val KNOWN_COMPOSABLES = hashSetOf(
        "Column", "Row", "Box", "Scaffold", "Surface", "Card", "ElevatedCard", "OutlinedCard",
        "Spacer", "Divider", "HorizontalDivider", "VerticalDivider", "LazyColumn", "LazyRow",
        "Text", "BasicText", "Icon", "Image", "Button", "ElevatedButton", "FilledTonalButton", "OutlinedButton",
        "TextButton", "IconButton", "FloatingActionButton", "TextField", "OutlinedTextField",
        "BasicTextField", "Checkbox", "Switch", "RadioButton", "Slider", "LinearProgressIndicator",
        "CircularProgressIndicator", "Badge", "BadgedBox", "Chip", "AssistChip", "FilterChip",
        "InputChip", "SuggestionChip", "TopAppBar", "CenterAlignedTopAppBar", "SmallTopAppBar",
        "MediumTopAppBar", "LargeTopAppBar", "BottomAppBar", "NavigationBar", "NavigationBarItem",
        "FlowRow", "FlowColumn", "LazyVerticalGrid", "LazyHorizontalGrid", "ListItem", "SelectionContainer"
    )

    private val NON_COMPOSABLES = hashSetOf(
        "Color", "Brush", "TextStyle", "FontWeight", "FontFamily", "FontStyle",
        "RoundedCornerShape", "CircleShape", "RectangleShape", "CutCornerShape",
        "PaddingValues", "Dp", "dp", "sp", "em", "Offset", "Size", "IntOffset", "IntSize",
        "Alignment", "Arrangement", "Modifier", "TextOverflow", "LineHeightStyle", "TextAlign",
        "ButtonDefaults", "CardDefaults", "IconButtonDefaults", "TextFieldDefaults",
        "SwitchDefaults", "CheckboxDefaults", "SliderDefaults", "ProgressIndicatorDefaults",
        "BadgeDefaults", "NavigationBarItemDefaults", "SnackbarDefaults", "Colors",
        "String", "Int", "Float", "Double", "Boolean", "Long", "List", "Set", "Map",
        "listOf", "setOf", "mapOf", "arrayOf", "emptyList", "mutableStateOf", "remember",
        "derivedStateOf", "rememberCoroutineScope", "LaunchedEffect", "SideEffect", "DisposableEffect",
        "rememberScrollState", "rememberLazyListState"
    )

    data class DeclaredComposable(
        val name: String,
        val params: List<String>,
        val body: String
    )

    fun parse(bodyCode: String?, fullFileContent: String?): ComposeNode {
        if (bodyCode.isNullOrBlank()) {
            val empty = ComposeNode("Box")
            val text = ComposeNode("Text").apply {
                addAttribute("text", "Empty Composable Preview")
            }
            empty.addChild(text)
            return empty
        }

        val declaredFunctions = extractAllDeclaredFunctions(fullFileContent)
        var resolvedCode = bodyCode.trim()

        val targetFunction = findDelegatedComposable(resolvedCode)
        if (targetFunction != null && declaredFunctions.containsKey(targetFunction)) {
            val target = declaredFunctions[targetFunction]
            if (target != null && target.body.isNotBlank()) {
                resolvedCode = target.body
            }
        }

        val variables = mutableMapOf<String, String>()
        val parsedNodes = parseNodes(resolvedCode, declaredFunctions, variables, emptySet())

        if (parsedNodes.isEmpty()) {
            val root = ComposeNode("Column").apply {
                addModifier("fillMaxWidth()")
                addModifier("padding(16.dp)")
            }
            val text = ComposeNode("Text").apply {
                addAttribute("text", "Compose Preview (" + (targetFunction ?: "Ready") + ")")
            }
            root.addChild(text)
            return root
        }

        if (parsedNodes.size == 1) {
            val single = parsedNodes[0]
            if (single.type in listOf("Scaffold", "Surface", "Column", "Box", "Row")) {
                return single
            }
        }

        val root = ComposeNode("Column").apply {
            addModifier("fillMaxWidth()")
        }
        for (n in parsedNodes) {
            root.addChild(n)
        }
        return root
    }

    private fun extractAllDeclaredFunctions(code: String?): Map<String, DeclaredComposable> {
        val map = mutableMapOf<String, DeclaredComposable>()
        if (code.isNullOrEmpty()) return map

        val p = Pattern.compile("\\bfun\\s+([A-Za-z0-9_]+)\\s*\\(")
        val m = p.matcher(code)
        while (m.find()) {
            val name = m.group(1) ?: continue
            val openParen = m.end() - 1
            val closeParen = findMatchingClose(code, openParen, '(', ')')
            if (closeParen != -1) {
                val paramsStr = code.substring(openParen + 1, closeParen).trim()
                val paramsList = if (paramsStr.isNotEmpty()) {
                    splitArgs(paramsStr).mapNotNull { paramDecl ->
                        val paramName = paramDecl.trim().substringBefore(':').trim()
                        if (paramName.isNotEmpty() && Character.isJavaIdentifierStart(paramName[0])) {
                            paramName
                        } else null
                    }
                } else {
                    emptyList()
                }

                val braceStart = code.indexOf('{', closeParen)
                if (braceStart != -1 && braceStart - closeParen < 150) {
                    val braceEnd = findMatchingClose(code, braceStart, '{', '}')
                    if (braceEnd != -1) {
                        val body = code.substring(braceStart + 1, braceEnd).trim()
                        map[name] = DeclaredComposable(name, paramsList, body)
                    }
                }
            }
        }
        return map
    }

    private fun findDelegatedComposable(body: String): String? {
        val p = Pattern.compile("(?:[A-Za-z0-9_]+Theme\\s*\\{\\s*)?([A-Z][A-Za-z0-9_]*)\\s*\\(")
        val m = p.matcher(body)
        if (m.find()) {
            val name = m.group(1) ?: return null
            if (!KNOWN_COMPOSABLES.contains(name) && !NON_COMPOSABLES.contains(name)) {
                return name
            }
        }
        return null
    }

    private fun parseNodes(
        code: String?,
        declaredFunctions: Map<String, DeclaredComposable>,
        variables: MutableMap<String, String>,
        expandingFunctions: Set<String>
    ): List<ComposeNode> {
        val nodes = mutableListOf<ComposeNode>()
        if (code == null) return nodes

        var i = 0
        val len = code.length

        while (i < len) {
            while (i < len && code[i].isWhitespace()) i++
            if (i >= len) break

            // Skip line comments
            if (i + 1 < len && code[i] == '/' && code[i + 1] == '/') {
                val nextLine = code.indexOf('\n', i + 2)
                if (nextLine == -1) break
                i = nextLine + 1
                continue
            }

            // Skip block comments
            if (i + 1 < len && code[i] == '/' && code[i + 1] == '*') {
                val endBlock = code.indexOf("*/", i + 2)
                if (endBlock == -1) break
                i = endBlock + 2
                continue
            }

            // Handle local val / var declarations
            if ((code.startsWith("val ", i) || code.startsWith("var ", i)) &&
                (i == 0 || !Character.isJavaIdentifierPart(code[i - 1]))
            ) {
                i += 4
                while (i < len && code[i].isWhitespace()) i++
                val nameStart = i
                while (i < len && Character.isJavaIdentifierPart(code[i])) i++
                val varName = code.substring(nameStart, i).trim()

                while (i < len && code[i].isWhitespace()) i++
                if (i < len && code.startsWith("by ", i)) {
                    i += 3
                } else if (i < len && code[i] == '=') {
                    i++
                }

                while (i < len && code[i].isWhitespace()) i++
                val valStart = i
                var depth = 0
                var inQuotes = false
                while (i < len) {
                    val c = code[i]
                    if (c == '"' && (i == 0 || code[i - 1] != '\\')) {
                        inQuotes = !inQuotes
                    } else if (!inQuotes) {
                        if (c == '(' || c == '{' || c == '[') {
                            depth++
                        } else if (c == ')' || c == '}' || c == ']') {
                            if (depth > 0) depth--
                        } else if (depth == 0 && (c == '\n' || c == ';')) {
                            break
                        }
                    }
                    i++
                }
                val valExpr = code.substring(valStart, i).trim()
                if (varName.isNotEmpty() && valExpr.isNotEmpty()) {
                    variables[varName] = valExpr
                }
                continue
            }

            // Find identifier
            val start = i
            if (!Character.isJavaIdentifierStart(code[i])) {
                i++
                continue
            }
            while (i < len && Character.isJavaIdentifierPart(code[i])) {
                i++
            }
            val identifier = code.substring(start, i)

            while (i < len && code[i].isWhitespace()) i++

            val hasArgs = (i < len && code[i] == '(')
            val hasLambda = (i < len && code[i] == '{')

            if (NON_COMPOSABLES.contains(identifier)) {
                if (hasArgs) {
                    val argEnd = findMatchingClose(code, i, '(', ')')
                    if (argEnd != -1) i = argEnd + 1
                }
                while (i < len && code[i].isWhitespace()) i++
                if (i < len && code[i] == '{') {
                    val bodyEnd = findMatchingClose(code, i, '{', '}')
                    if (bodyEnd != -1) i = bodyEnd + 1
                }
                continue
            }

            if (isKnownOrProbableComposable(identifier)) {
                val node = ComposeNode(identifier)
                var argsString = ""

                if (hasArgs) {
                    val argEnd = findMatchingClose(code, i, '(', ')')
                    if (argEnd != -1) {
                        argsString = code.substring(i + 1, argEnd).trim()
                        parseArguments(argsString, node, variables)
                        i = argEnd + 1
                    }
                }

                while (i < len && code[i].isWhitespace()) i++

                if (i < len && code[i] == '{') {
                    val bodyEnd = findMatchingClose(code, i, '{', '}')
                    if (bodyEnd != -1) {
                        val lambdaBody = code.substring(i + 1, bodyEnd).trim()
                        val childNodes = parseNodes(lambdaBody, declaredFunctions, variables, expandingFunctions)
                        for (child in childNodes) {
                            node.addChild(child)
                        }
                        i = bodyEnd + 1
                    }
                } else if (declaredFunctions.containsKey(identifier) && node.children.isEmpty() && !expandingFunctions.contains(identifier)) {
                    val funcDef = declaredFunctions[identifier]
                    if (funcDef != null && funcDef.body.isNotBlank()) {
                        val innerVariables = variables.toMutableMap()
                        for ((k, v) in node.attributes) {
                            if (k.startsWith("arg_")) {
                                val idx = k.removePrefix("arg_").toIntOrNull()
                                if (idx != null && idx < funcDef.params.size) {
                                    innerVariables[funcDef.params[idx]] = v
                                }
                            } else {
                                innerVariables[k] = v
                            }
                        }
                        val childNodes = parseNodes(funcDef.body, declaredFunctions, innerVariables, expandingFunctions + identifier)
                        for (child in childNodes) {
                            node.addChild(child)
                        }
                    }
                }

                node.rawSource = identifier + (if (hasArgs) "($argsString)" else "")
                nodes.add(node)
            } else {
                i++
            }
        }

        return nodes
    }

    private fun isKnownOrProbableComposable(name: String): Boolean {
        if (NON_COMPOSABLES.contains(name)) return false
        if (KNOWN_COMPOSABLES.contains(name)) return true
        return name.isNotEmpty() && Character.isUpperCase(name[0])
    }

    private fun parseArguments(args: String?, node: ComposeNode, variables: MutableMap<String, String>) {
        if (args.isNullOrBlank()) return

        val argList = splitArgs(args)
        for (a in argList) {
            val trimmed = a.trim()
            if (trimmed.startsWith("modifier") && trimmed.contains("=")) {
                val modValue = trimmed.substring(trimmed.indexOf('=') + 1).trim()
                parseModifiers(modValue, node, variables)
            } else if (trimmed.startsWith("Modifier.") || trimmed.startsWith("Modifier")) {
                parseModifiers(trimmed, node, variables)
            } else if (trimmed.contains("=")) {
                val kv = trimmed.split("=", limit = 2)
                val key = kv[0].trim()
                var value = kv[1].trim()
                if (variables.containsKey(value)) {
                    value = variables[value] ?: value
                }
                node.addAttribute(key, cleanValueExpression(value))
            } else if (trimmed.isNotEmpty()) {
                var value = trimmed
                if (variables.containsKey(value)) {
                    value = variables[value] ?: value
                }
                if ((node.type == "Text" || node.type == "BasicText") && !node.attributes.containsKey("text")) {
                    node.addAttribute("text", cleanValueExpression(value))
                } else if (node.type == "Icon" && !node.attributes.containsKey("imageVector")) {
                    node.addAttribute("imageVector", value)
                } else if (node.type == "BasicTextField" && !node.attributes.containsKey("value")) {
                    node.addAttribute("value", cleanValueExpression(value))
                } else {
                    node.addAttribute("arg_${node.attributes.size}", cleanValueExpression(value))
                }
            }
        }
    }

    private fun parseModifiers(modChain: String?, node: ComposeNode, variables: MutableMap<String, String>) {
        if (modChain.isNullOrEmpty()) return
        val parts = splitModifierChain(modChain)
        for (part in parts) {
            var trimmed = part.trim()
            if (trimmed.isNotEmpty() && trimmed != "Modifier") {
                for ((varName, varVal) in variables) {
                    if (trimmed.contains(varName)) {
                        trimmed = trimmed.replace(varName, varVal)
                    }
                }
                node.addModifier(trimmed)
            }
        }
    }

    private fun splitModifierChain(chain: String): List<String> {
        val parts = mutableListOf<String>()
        val len = chain.length
        var start = 0
        var depth = 0
        var inQuotes = false

        for (i in 0 until len) {
            val c = chain[i]
            if (c == '"' && (i == 0 || chain[i - 1] != '\\')) {
                inQuotes = !inQuotes
            } else if (!inQuotes) {
                if (c == '(' || c == '{' || c == '[') {
                    depth++
                } else if (c == ')' || c == '}' || c == ']') {
                    if (depth > 0) depth--
                } else if (c == '.' && depth == 0) {
                    if (i > start) {
                        parts.add(chain.substring(start, i).trim())
                    }
                    start = i + 1
                }
            }
        }
        if (start < len) {
            val last = chain.substring(start).trim()
            if (last.isNotEmpty()) {
                parts.add(last)
            }
        }
        return parts
    }

    private fun splitArgs(args: String): List<String> {
        val list = mutableListOf<String>()
        val len = args.length
        var start = 0
        var depth = 0
        var inQuotes = false

        for (i in 0 until len) {
            val c = args[i]
            if (c == '"' && (i == 0 || args[i - 1] != '\\')) {
                inQuotes = !inQuotes
            } else if (!inQuotes) {
                if (c == '(' || c == '{' || c == '[') {
                    depth++
                } else if (c == ')' || c == '}' || c == ']') {
                    depth--
                } else if (c == ',' && depth == 0) {
                    list.add(args.substring(start, i).trim())
                    start = i + 1
                }
            }
        }
        if (start < len) {
            list.add(args.substring(start).trim())
        }
        return list
    }

    private fun findMatchingClose(s: String, openIndex: Int, open: Char, close: Char): Int {
        var depth = 1
        val len = s.length
        var i = openIndex + 1
        var inQuotes = false

        while (i < len && depth > 0) {
            val c = s[i]
            if (c == '"' && (i == 0 || s[i - 1] != '\\')) {
                inQuotes = !inQuotes
            } else if (!inQuotes) {
                if (c == open) {
                    depth++
                } else if (c == close) {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }
        return -1
    }

    fun cleanValueExpression(expr: String?): String {
        if (expr == null) return ""
        val s = expr.trim()
        if (s.contains("mutableStateOf")) {
            val idx = s.indexOf("mutableStateOf")
            val openParen = s.indexOf('(', idx)
            if (openParen != -1) {
                val closeParen = findMatchingClose(s, openParen, '(', ')')
                if (closeParen != -1) {
                    return cleanQuotes(s.substring(openParen + 1, closeParen).trim())
                }
            }
        }
        if (s.contains("Text(") || s.contains("Text (")) {
            val idx = s.indexOf("Text")
            val openParen = s.indexOf('(', idx)
            if (openParen != -1) {
                val closeParen = findMatchingClose(s, openParen, '(', ')')
                if (closeParen != -1) {
                    var inside = s.substring(openParen + 1, closeParen).trim()
                    if (inside.contains("=")) {
                        inside = inside.substring(inside.indexOf('=') + 1).trim()
                    }
                    return cleanQuotes(inside)
                }
            }
        }
        return cleanQuotes(s)
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
