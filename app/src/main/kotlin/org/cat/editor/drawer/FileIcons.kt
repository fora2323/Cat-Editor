package org.cat.editor.drawer

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cat.editor.R
import org.cat.editor.ui.preview.ImagePreviewIcon
import org.cat.editor.ui.preview.isImageFile
import java.io.File

@Composable
fun FileIcon(file: File, modifier: Modifier = Modifier) {
    val fileName = file.name
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    when {
        isImageFile(fileName) -> ImagePreviewIcon(file = file, modifier = modifier)
        fileName == "AndroidManifest.xml" -> Icon(painterResource(R.drawable.ic_android), stringResource(id = R.string.manifest), modifier = modifier, tint = iconTint)
        fileName.endsWith(".kt") -> Icon(painterResource(R.drawable.ic_kotlin), stringResource(id = R.string.file_kotlin), modifier = modifier, tint = iconTint)
        fileName.endsWith(".kts") || fileName == "gradlew" || fileName == "gradlew.bat" || fileName.endsWith(".gradle") || fileName == "gradle.properties" || fileName == "local.properties" -> Icon(painterResource(R.drawable.ic_gradle), stringResource(id = R.string.file_gradle), modifier = modifier, tint = iconTint)
        fileName.endsWith(".java") -> Icon(painterResource(R.drawable.ic_java), stringResource(id = R.string.file_java), modifier = modifier, tint = iconTint)
        fileName.endsWith(".xml") -> Icon(painterResource(R.drawable.ic_xml), stringResource(id = R.string.file_xml), modifier = modifier, tint = iconTint)
        fileName.endsWith(".cpp") || fileName.endsWith(".cc") || fileName.endsWith(".cxx") || fileName.endsWith(".hpp") -> Icon(painterResource(R.drawable.ic_cpp), stringResource(id = R.string.file_cpp), modifier = modifier, tint = iconTint)
        fileName.endsWith(".c") || fileName.endsWith(".h") -> Icon(painterResource(R.drawable.ic_c), stringResource(id = R.string.file_c), modifier = modifier, tint = iconTint)
        fileName == "CMakeLists.txt" || fileName.endsWith(".cmake") -> Icon(painterResource(R.drawable.ic_cmake), stringResource(id = R.string.file_cmake), modifier = modifier, tint = iconTint)
        fileName.endsWith(".py") -> Icon(Icons.Default.Code, stringResource(id = R.string.file_python), modifier = modifier, tint = iconTint)
        fileName.endsWith(".md") -> Icon(Icons.Default.Description, stringResource(id = R.string.file_markdown), modifier = modifier, tint = iconTint)
        fileName.endsWith(".json") -> Icon(Icons.Default.DataObject, stringResource(id = R.string.file_json), modifier = modifier, tint = iconTint)
        fileName.startsWith(".git") -> Icon(Icons.Default.Source, stringResource(id = R.string.file_git), modifier = modifier, tint = iconTint)
        else -> Icon(Icons.Default.Description, stringResource(id = R.string.file_generic), modifier = modifier, tint = iconTint)
    }
}

@Composable
fun FolderIcon(
    folderName: String,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(
            if (isExpanded) R.drawable.ic_folder_open
            else R.drawable.ic_folder
        ),
        contentDescription = folderName,
        modifier = modifier,
        tint = MaterialTheme.colorScheme.primary
    )
}