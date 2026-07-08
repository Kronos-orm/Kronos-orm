package com.kotlinorm.plugin.idea

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.ContentFactory

class MainWinFactory : com.intellij.openapi.wm.ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        val mainPanel = MainPanel(project)
        val content = ContentFactory.getInstance().createContent(mainPanel.component, "", false)
        toolWindow.contentManager.addContent(content)
    }
}