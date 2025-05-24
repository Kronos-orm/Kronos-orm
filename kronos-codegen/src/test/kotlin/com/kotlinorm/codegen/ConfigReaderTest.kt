package com.kotlinorm.codegen

import com.kotlinorm.Kronos
import org.intellij.lang.annotations.Language
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigReaderTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun createTempDir() {
        tempDir = createTempDirectory("kotlinFileTest").toFile()
    }

    @AfterTest
    fun cleanupTempDir() {
        tempDir.deleteRecursively()
    }

    fun File.writeTomlContent(@Language("toml") content: String) {
        this.parentFile.mkdirs()
        this.writeText(
            content.trimIndent()
        )
    }

    @Test
    fun testReadConfig() {
        val configPath = File(tempDir, "testConfig.toml").apply {
            parentFile.mkdirs()
            writeTomlContent(
                """
                [table]
                name = "tb_user"
                tableNamingStrategy = "lineHumpNamingStrategy"
                fieldNamingStrategy = "lineHumpNamingStrategy"
                createTimeStrategy = "createTime"
                updateTimeStrategy = "updateTime"
                logicDeleteStrategy = "deleted"
                className = "User"

                [output]
                targetDir = "./src/main/kotlin/com/kotlinorm/orm/table/"
                packageName = "com.kotlinorm.orm.table"
                tableCommentLineWords = 80

                [dataSource]
                dataSourceClassName = "org.apache.commons.dbcp2.BasicDataSource"
                wrapperClassName = "com.kotlinorm.codegen.SampleMysqlJdbcWrapper"
                url = "jdbc:mysql://localhost:3306/kronos_testing?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&useServerPrepStmts=true&rewriteBatchedStatements=true"
                username = "root"
                password = "password"
                driverClassName = "com.mysql.cj.jdbc.Driver"
                initialSize = 5
                maxActive = 10
                """.trimIndent()
            )
        }.path

//        comment: Sample Table Comment
//        fields:
//        mapOf(
//            "Field" to "id",
//            "Type" to "Int",
//            "Key" to "PRI"
//        ),
//        mapOf(
//            "Field" to "username",
//            "Type" to "Varchar"
//        ),
//        mapOf(
//            "Field" to "gender",
//            "Type" to "Int"
//        ),
//        mapOf(
//            "Field" to "create_time",
//            "Type" to "Datetime"
//        ),
//        mapOf(
//            "Field" to "update_time",
//            "Type" to "Datetime"
//        ),
//        mapOf(
//            "Field" to "deleted",
//            "Type" to "Boolean"
//        )
//        indexes:
//        mapOf(
//            "tableName" to "tb_user",
//            "indexName" to "PRIMARY",
//            "columnName" to "id",
//            "nonUnique" to 0,
//            "indexType" to "BTREE"
//        )
        val config = readConfig(configPath)

        assertEquals("tb_user", config.table.name)
        assertEquals("./src/main/kotlin/com/kotlinorm/orm/table/", config.output.targetDir)
        assertEquals("com.kotlinorm.orm.table", config.output.packageName)
        assertEquals("User", config.table.className)
        assertEquals(Kronos.lineHumpNamingStrategy, config.table.tableNamingStrategy)
        assertEquals(Kronos.lineHumpNamingStrategy, config.table.fieldNamingStrategy)
        assertEquals("createTime", config.table.createTimeStrategy?.field?.name)
        assertEquals("updateTime", config.table.updateTimeStrategy?.field?.name)
        assertEquals("deleted", config.table.logicDeleteStrategy?.field?.name)
        assertEquals("com.kotlinorm.codegen.SampleMysqlJdbcWrapper", config.dataSource::class.java.name)
        assertEquals(
            "jdbc:mysql://localhost:3306/kronos_testing?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&useServerPrepStmts=true&rewriteBatchedStatements=true",
            config.dataSource.url
        )
        assertEquals("root", config.dataSource.userName)
        assertEquals("Mysql", config.dataSource.dbType.name)

        val dataSource = config.dataSource as SampleMysqlJdbcWrapper
        assertEquals("org.apache.commons.dbcp2.BasicDataSource", dataSource.dataSource::class.java.name)

        assertEquals(
            config.formatedKotlinComment, "// Sample Table Comment",
            "Expected comment to contain 'Sample Table Comment', but got '${config.formatedKotlinComment}'"
        )

        //fields:
        assertEquals(
            config.fields.size, 6,
            "Expected 6 fields, but got ${config.fields.size}"
        )
        assertEquals(
            config.fields[0].name, "id",
            "Expected first field name to be 'id', but got '${config.fields[0].name}'"
        )
        assertEquals(
            config.fields[1].name, "username",
            "Expected second field name to be 'username', but got '${config.fields[1].name}'"
        )
        assertEquals(
            config.fields[2].name, "gender",
            "Expected third field name to be 'gender', but got '${config.fields[2].name}'"
        )
        assertEquals(
            config.fields[3].name, "createTime",
            "Expected fourth field name to be 'createTime', but got '${config.fields[3].name}'"
        )
        assertEquals(
            config.fields[4].name, "updateTime",
            "Expected fifth field name to be 'updateTime', but got '${config.fields[4].name}'"
        )
        assertEquals(
            config.fields[5].name, "deleted",
            "Expected sixth field name to be 'deleted', but got '${config.fields[5].name}'"
        )

        //indexes:
        assertEquals(
            config.indexes.size, 1,
            "Expected 1 index, but got ${config.indexes.size}"
        )
        assertEquals(
            config.indexes[0].name, "PRIMARY",
            "Expected index name to be 'PRIMARY', but got '${config.indexes[0].name}'"
        )
        assertEquals(
            config.indexes[0].columns.size, 1,
            "Expected index to have 1 column, but got ${config.indexes[0].columns.size}"
        )
        assertEquals(
            config.indexes[0].columns[0], "id",
            "Expected index column name to be 'id', but got '${config.indexes[0].columns[0]}'"
        )
        assertEquals(
            config.indexes[0].type, "UNIQUE",
            "Expected index type to be 'BTREE', but got '${config.indexes[0].type}'"
        )

    }
}