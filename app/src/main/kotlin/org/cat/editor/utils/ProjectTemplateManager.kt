package org.cat.editor.utils

import android.content.Context
import java.io.File
import java.io.IOException
import java.nio.file.FileAlreadyExistsException

/**
 * Languages for which the app ships a starter project template.
 */
enum class ProjectLanguage(
    val id: String,
    val displayName: String
) {
    KOTLIN("kotlin", "Kotlin"),
    JAVA("java", "Java"),
    PYTHON("python", "Python")
}

/**
 * Visual illustration style for a project preset card.
 */
enum class PresetIconType {
    COMPOSE_EMPTY,
    VIEWS_EMPTY,
    VIEWS_BASIC_FAB,
    VIEWS_BOTTOM_NAV,
    NO_ACTIVITY,
    CONSOLE,
    PYTHON
}

/**
 * Metadata used to present and select a project starter.
 */
data class ProjectTemplatePreset(
    val language: ProjectLanguage,
    val displayName: String,
    val description: String,
    val assetDirectory: String = "${ProjectTemplateManager.ASSET_ROOT}/${language.id}",
    val id: String = "${language.id}_${displayName.lowercase().replace(Regex("[^a-z0-9]"), "_")}",
    val isBlank: Boolean = false,
    val iconType: PresetIconType = PresetIconType.VIEWS_EMPTY,
    val badge: String? = null
)

/**
 * Lists bundled project starters and creates a new project from one of them.
 *
 * Template assets are text files and may contain these placeholders:
 * - [PROJECT_NAME_PLACEHOLDER]
 * - [PACKAGE_NAME_PLACEHOLDER]
 * - [PACKAGE_PATH_PLACEHOLDER] (the package with dots changed to `/`)
 */
object ProjectTemplateManager {
    const val ASSET_ROOT = "project_templates"
    const val PROJECT_NAME_PLACEHOLDER = "__PROJECT_NAME__"
    const val PACKAGE_NAME_PLACEHOLDER = "__PACKAGE_NAME__"
    const val PACKAGE_PATH_PLACEHOLDER = "__PACKAGE_PATH__"
    private const val ASSET_PACKAGE_PATH_TOKEN = "PACKAGE_PATH"

    /** All languages supported by the bundled templates. */
    val supportedLanguages: List<ProjectLanguage> = ProjectLanguage.entries.toList()

    /** Presets available to a project-creation flow. */
    val presets: List<ProjectTemplatePreset> = listOf(
        // Kotlin Presets
        ProjectTemplatePreset(
            language = ProjectLanguage.KOTLIN,
            displayName = "Empty Activity (Compose)",
            description = "Jetpack Compose Hello World with Material 3 theme and preview support.",
            assetDirectory = "${ASSET_ROOT}/kotlin_compose_empty",
            id = "kotlin_compose_empty",
            isBlank = false,
            iconType = PresetIconType.COMPOSE_EMPTY,
            badge = "Compose"
        ),
        ProjectTemplatePreset(
            language = ProjectLanguage.KOTLIN,
            displayName = "Empty Views Activity",
            description = "Classic Views activity with Material 3 Toolbar, Card and Hello World.",
            assetDirectory = "${ASSET_ROOT}/kotlin_views_empty",
            id = "kotlin_views_empty",
            isBlank = false,
            iconType = PresetIconType.VIEWS_EMPTY,
            badge = "Views"
        ),
        ProjectTemplatePreset(
            language = ProjectLanguage.KOTLIN,
            displayName = "Basic Views Activity",
            description = "CoordinatorLayout with Material 3 Toolbar, Floating Action Button and Menu.",
            assetDirectory = "${ASSET_ROOT}/kotlin_views_basic_fab",
            id = "kotlin_views_basic_fab",
            isBlank = false,
            iconType = PresetIconType.VIEWS_BASIC_FAB,
            badge = "Material 3"
        ),
        ProjectTemplatePreset(
            language = ProjectLanguage.KOTLIN,
            displayName = "Bottom Navigation Activity",
            description = "Multi-section app with Material 3 BottomNavigationView and tab switching.",
            assetDirectory = "${ASSET_ROOT}/kotlin_views_bottom_nav",
            id = "kotlin_views_bottom_nav",
            isBlank = false,
            iconType = PresetIconType.VIEWS_BOTTOM_NAV,
            badge = "Navigation"
        ),
        ProjectTemplatePreset(
            language = ProjectLanguage.KOTLIN,
            displayName = "No Activity (Blank)",
            description = "Clean Android project structure with Gradle and Manifest, ready for code.",
            assetDirectory = "${ASSET_ROOT}/kotlin_no_activity",
            id = "kotlin_no_activity",
            isBlank = true,
            iconType = PresetIconType.NO_ACTIVITY,
            badge = "Blank"
        ),
        ProjectTemplatePreset(
            language = ProjectLanguage.KOTLIN,
            displayName = "Kotlin Application",
            description = "A lightweight Kotlin/JVM console application with Gradle build.",
            assetDirectory = "${ASSET_ROOT}/kotlin",
            id = "kotlin_console",
            isBlank = false,
            iconType = PresetIconType.CONSOLE,
            badge = "CLI"
        ),

        // Java Presets
        ProjectTemplatePreset(
            language = ProjectLanguage.JAVA,
            displayName = "Empty Views Activity",
            description = "Classic Java Views activity with Material 3 Toolbar and Hello World.",
            assetDirectory = "${ASSET_ROOT}/java_views_empty",
            id = "java_views_empty",
            isBlank = false,
            iconType = PresetIconType.VIEWS_EMPTY,
            badge = "Views"
        ),
        ProjectTemplatePreset(
            language = ProjectLanguage.JAVA,
            displayName = "Basic Views Activity",
            description = "CoordinatorLayout with Material 3 Toolbar, Floating Action Button and Menu in Java.",
            assetDirectory = "${ASSET_ROOT}/java_views_basic_fab",
            id = "java_views_basic_fab",
            isBlank = false,
            iconType = PresetIconType.VIEWS_BASIC_FAB,
            badge = "Material 3"
        ),
        ProjectTemplatePreset(
            language = ProjectLanguage.JAVA,
            displayName = "Bottom Navigation Activity",
            description = "Multi-section app with Material 3 BottomNavigationView in Java.",
            assetDirectory = "${ASSET_ROOT}/java_views_bottom_nav",
            id = "java_views_bottom_nav",
            isBlank = false,
            iconType = PresetIconType.VIEWS_BOTTOM_NAV,
            badge = "Navigation"
        ),
        ProjectTemplatePreset(
            language = ProjectLanguage.JAVA,
            displayName = "No Activity (Blank)",
            description = "Clean Java Android project structure with Gradle and Manifest.",
            assetDirectory = "${ASSET_ROOT}/java_no_activity",
            id = "java_no_activity",
            isBlank = true,
            iconType = PresetIconType.NO_ACTIVITY,
            badge = "Blank"
        ),
        ProjectTemplatePreset(
            language = ProjectLanguage.JAVA,
            displayName = "Java Application",
            description = "A lightweight Java console application with Gradle build.",
            assetDirectory = "${ASSET_ROOT}/java",
            id = "java_console",
            isBlank = false,
            iconType = PresetIconType.CONSOLE,
            badge = "CLI"
        ),

        // Python Presets
        ProjectTemplatePreset(
            language = ProjectLanguage.PYTHON,
            displayName = "Blank Project",
            description = "Empty Python project structure ready for your files and scripts.",
            assetDirectory = "${ASSET_ROOT}/python_blank",
            id = "python_blank",
            isBlank = true,
            iconType = PresetIconType.NO_ACTIVITY,
            badge = "Blank"
        ),
        ProjectTemplatePreset(
            language = ProjectLanguage.PYTHON,
            displayName = "Python Application",
            description = "A small Python application with pyproject configuration.",
            assetDirectory = "${ASSET_ROOT}/python",
            id = "python_app",
            isBlank = false,
            iconType = PresetIconType.PYTHON,
            badge = "Script"
        )
    )

    /**
     * Creates a new project under [targetDirectory] (defaults to [ConfigPath.VAR_PROJECTS_PATH]).
     *
     * The project name becomes the directory name and the package name is used in
     * source files and package directories. Both values are deliberately limited to
     * conventional identifier-like forms so a caller cannot escape the projects root.
     * The operation fails if the destination already exists. If copying fails after
     * creation has started, the newly-created destination is removed.
     *
     * @return the canonical directory of the newly-created project.
     * @throws IllegalArgumentException when a name, package, or preset is invalid.
     * @throws FileAlreadyExistsException when the destination already exists.
     * @throws IOException when the projects directory or template cannot be read.
     */
    @Throws(IOException::class, IllegalArgumentException::class)
    fun createProject(
        context: Context,
        projectName: String,
        packageName: String,
        preset: ProjectTemplatePreset,
        targetDirectory: File = File(ConfigPath.VAR_PROJECTS_PATH)
    ): File {
        validateProjectName(projectName)
        validatePackageName(packageName)

        require(presets.any { it.id == preset.id || it == preset }) {
            "The selected project template is not one of the bundled presets"
        }

        val projectsRoot = targetDirectory.canonicalFile
        if (projectsRoot.exists() && !projectsRoot.isDirectory) {
            throw IOException("Projects path is not a directory: ${projectsRoot.path}")
        }
        if (!projectsRoot.exists() && !projectsRoot.mkdirs() && !projectsRoot.isDirectory) {
            throw IOException("Unable to create projects directory: ${projectsRoot.path}")
        }

        val destination = File(projectsRoot, projectName).canonicalFile
        val rootPrefix = projectsRoot.path + File.separator
        require(destination.path.startsWith(rootPrefix)) {
            "Project destination must be inside ${projectsRoot.path}"
        }
        if (destination.exists()) {
            throw FileAlreadyExistsException(destination.path)
        }
        if (!destination.mkdirs()) {
            if (destination.exists()) {
                throw FileAlreadyExistsException(destination.path)
            }
            throw IOException("Unable to create project directory: ${destination.path}")
        }

        val replacements = mapOf(
            PROJECT_NAME_PLACEHOLDER to projectName,
            PACKAGE_NAME_PLACEHOLDER to packageName,
            PACKAGE_PATH_PLACEHOLDER to packageName.replace('.', '/')
        )
        val pathReplacements = replacements + mapOf(
            // The asset directory uses this additional token because Android's
            // asset merger can ignore directory names beginning with an underscore.
            ASSET_PACKAGE_PATH_TOKEN to packageName.replace('.', '/')
        )

        return try {
            copyAssetTree(
                assetManager = context.assets,
                assetPath = preset.assetDirectory,
                destination = destination,
                projectRoot = destination,
                replacements = replacements,
                pathReplacements = pathReplacements
            )
            destination
        } catch (error: IOException) {
            destination.deleteRecursively()
            throw error
        } catch (error: RuntimeException) {
            destination.deleteRecursively()
            throw error
        }
    }

    /** Convenience overload for callers selecting a language directly. */
    @Throws(IOException::class, IllegalArgumentException::class)
    fun createProject(
        context: Context,
        projectName: String,
        packageName: String,
        language: ProjectLanguage,
        targetDirectory: File = File(ConfigPath.VAR_PROJECTS_PATH)
    ): File {
        val preset = presets.firstOrNull { it.language == language }
            ?: throw IllegalArgumentException("No bundled template for ${language.id}")
        return createProject(context, projectName, packageName, preset, targetDirectory)
    }

    private fun copyAssetTree(
        assetManager: android.content.res.AssetManager,
        assetPath: String,
        destination: File,
        projectRoot: File,
        replacements: Map<String, String>,
        pathReplacements: Map<String, String>
    ) {
        val children = assetManager.list(assetPath)
        if (children != null && children.isNotEmpty()) {
            if (!destination.exists() && !destination.mkdirs()) {
                throw IOException("Unable to create template directory: ${destination.path}")
            }
            for (child in children) {
                val sourceChildPath = "$assetPath/$child"
                val outputName = replacePlaceholders(child, pathReplacements)
                require(isSafeAssetName(outputName)) {
                    "Template contains an unsafe path: $child"
                }
                val output = File(destination, outputName).canonicalFile
                ensureInside(output, projectRoot)
                copyAssetTree(
                    assetManager = assetManager,
                    assetPath = sourceChildPath,
                    destination = output,
                    projectRoot = projectRoot,
                    replacements = replacements,
                    pathReplacements = pathReplacements
                )
            }
            return
        }

        val output = destination.canonicalFile
        ensureInside(output, projectRoot)
        output.parentFile?.let { parent ->
            if (!parent.exists() && !parent.mkdirs() && !parent.isDirectory) {
                throw IOException("Unable to create template parent: ${parent.path}")
            }
        }

        try {
            assetManager.open(assetPath).bufferedReader(Charsets.UTF_8).use { reader ->
                output.writeText(replacePlaceholders(reader.readText(), replacements), Charsets.UTF_8)
            }
        } catch (error: IOException) {
            throw IOException("Unable to copy template asset $assetPath", error)
        }
    }

    private fun ensureInside(candidate: File, root: File) {
        val rootPrefix = root.canonicalPath + File.separator
        require(candidate.canonicalPath.startsWith(rootPrefix)) {
            "Template output must remain inside ${root.canonicalPath}"
        }
    }

    private fun replacePlaceholders(value: String, replacements: Map<String, String>): String {
        return replacements.entries.fold(value) { result, (placeholder, replacement) ->
            result.replace(placeholder, replacement)
        }
    }

    private fun isSafeAssetName(name: String): Boolean {
        val path = name.replace('\\', '/')
        return path.isNotEmpty() && path.split('/').all { segment ->
            segment.isNotEmpty() && segment != "." && segment != ".." &&
                segment.none { it == '\u0000' }
        }
    }

    private fun validateProjectName(projectName: String) {
        require(projectName.matches(PROJECT_NAME_PATTERN)) {
            "Project name must start with a letter or number and contain only letters, numbers, '.', '_' or '-'"
        }
    }

    private fun validatePackageName(packageName: String) {
        require(packageName.matches(PACKAGE_NAME_PATTERN)) {
            "Package name must contain dot-separated identifier segments"
        }
    }

    private val PROJECT_NAME_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")
    private val PACKAGE_NAME_PATTERN = Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*")
}
