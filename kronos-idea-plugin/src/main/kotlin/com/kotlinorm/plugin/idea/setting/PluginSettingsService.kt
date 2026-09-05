package com.kotlinorm.plugin.idea.setting

import com.intellij.openapi.components.Service
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.PROJECT)
class PluginSettingsService {
    private val settings = PluginSettings.getInstance()

    fun configJsonFile() = settings.configJsonFile
    fun updateSettings(newSettings: PluginSettings) {
        XmlSerializerUtil.copyBean(newSettings, settings)
    }
}