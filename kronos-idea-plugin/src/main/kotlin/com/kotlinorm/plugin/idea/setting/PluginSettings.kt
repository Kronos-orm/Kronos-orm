package com.kotlinorm.plugin.idea.setting

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "KronosPluginSettings", storages = [Storage("kronos-plugin-settings.xml")]
)
class PluginSettings : PersistentStateComponent<PluginSettings> {
    var configJsonFile: String = ""
    var activeTemplateName: String = "KPojo"

    /**
     * {
     *     "dataSources":[
     *         {
     *             "dataSourceName": "myDataSource",
     *             "dataSourceUrl": "jdbc:mysql://localhost:3306/yourdb",
     *             "dataSourceUser": "username",
     *             "dataSourcePassword": "password",
     *             "dataSourceDriver": "com.mysql.cj.jdbc.Driver",
     *             "default": true,
     *             "description": "MySQL Data Source"
     *         }
     *     ],
     *     "templates":[
     *         "KPojoTemplate.kts",
     *         "ServiceTemplate.kts"
     *         "ControllerTemplate.kts"
     *     ],
     *     "customVariables": {
     *         "var1": "value1",
     *         "var2": "value2"
     *     }
     * }
     */

    override fun getState() = this

    override fun loadState(state: PluginSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val settingInstance by lazy { PluginSettings() }
        fun getInstance() = settingInstance
    }
}
