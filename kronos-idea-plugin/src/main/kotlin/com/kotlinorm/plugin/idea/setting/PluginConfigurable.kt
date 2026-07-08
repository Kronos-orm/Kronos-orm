package com.kotlinorm.plugin.idea.setting

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class PluginConfigurable : Configurable {
    private lateinit var settings: PluginSettings
    private lateinit var panel: DialogPanel

    override fun createComponent(): JComponent {
        settings = PluginSettings.getInstance()
        panel = panel {
            row("Config File:") {
                textField()
                    .bindText(settings::configJsonFile)
                    .align(Align.FILL)
                    .comment("Path to the Kronos config JSON file")
            }
        }
        return panel
    }

    override fun isModified(): Boolean = panel.isModified()
    override fun apply() = panel.apply()
    override fun reset() = panel.reset()
    override fun getDisplayName() = "Kronos Plugin Settings"
}