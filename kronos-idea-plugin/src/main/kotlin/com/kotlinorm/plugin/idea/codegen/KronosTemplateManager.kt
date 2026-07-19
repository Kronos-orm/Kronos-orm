package com.kotlinorm.plugin.idea.codegen

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.nio.charset.StandardCharsets

data class KronosIdeaTemplate(
    val name: String,
    val description: String,
    val projectFile: VirtualFile?,
) {
    val isProject: Boolean = projectFile != null
    val source: String = if (isProject) "Project" else "Built-in"
    val path: String = projectFile?.path ?: "Bundled with Kronos IDEA plugin"
}

class KronosTemplateManager(
    private val project: Project,
) {
    fun templates(): List<KronosIdeaTemplate> {
        val projectTemplates = templateDirectory()
            ?.children
            ?.filter { !it.isDirectory && it.extension == "kts" }
            ?.map { file -> KronosIdeaTemplate(file.nameWithoutExtension, "Project-local generator template", file) }
            .orEmpty()
            .sortedWith(compareBy<KronosIdeaTemplate> { it.name != BuiltInKPojoName }.thenBy { it.name })
        return projectTemplates + KronosIdeaTemplate(BuiltInKPojoName, "Default Kronos KPojo data class renderer", null)
    }

    fun copyBuiltInKPojoToProject(): VirtualFile {
        val baseDir = projectDirectory() ?: error("Project base directory is not available.")
        return WriteCommandAction.writeCommandAction(project).compute<VirtualFile, RuntimeException> {
            val dir = VfsUtil.createDirectoryIfMissing(baseDir, ".kronos/templates")
                ?: error("Could not create .kronos/templates.")
            val file = dir.findOrCreateChildData(this, "KPojo.kts")
            if (file.length == 0L) {
                file.setBinaryContent(BuiltInKPojoTemplate.toByteArray(StandardCharsets.UTF_8))
            }
            file
        }
    }

    fun content(template: KronosIdeaTemplate): String =
        template.projectFile?.let { VfsUtil.loadText(it) } ?: BuiltInKPojoTemplate

    private fun templateDirectory(): VirtualFile? {
        val baseDir = projectDirectory() ?: return null
        return ApplicationManager.getApplication().runReadAction<VirtualFile?> {
            baseDir.findFileByRelativePath(".kronos/templates")
        }
    }

    private fun projectDirectory(): VirtualFile? =
        project.basePath?.let(LocalFileSystem.getInstance()::findFileByPath)
}

const val BuiltInKPojoName = "KPojo"

val BuiltInKPojoTemplate = """
    $IdeaTemplateMarker
    package {{packageName}}

    {{imports}}

    {{tableComment}}
    // @author: Kronos-Codegen
    // @date: {{generatedAt}}

    @Table(name = "{{tableName}}")
    {{tableIndexes}}
    data class {{className}}(
    {{fields}}
    ): KPojo
""".trimIndent()
