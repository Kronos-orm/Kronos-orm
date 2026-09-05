package com.kotlinorm.plugin.idea.setting

object ExampleSettings {
    val exampleJsonString = """
    {
        "dataSources":[
            {
                "dataSourceName": "myDataSource",
                "dataSourceUrl": "jdbc:mysql://localhost:3306/yourdb",
                "dataSourceUser": "username",
                "dataSourcePassword": "password",
                "dataSourceDriver": "com.mysql.cj.jdbc.Driver",
                "default": true,
                "description": "MySQL Data Source"
            }
        ],
        "templates":[
            "KPojoTemplate.kts",
            "ServiceTemplate.kts"
            "ControllerTemplate.kts"
        ],
        "customVariables": {
            "var1": "value1",
            "var2": "value2"
        }
    }
    """.trimIndent()

}