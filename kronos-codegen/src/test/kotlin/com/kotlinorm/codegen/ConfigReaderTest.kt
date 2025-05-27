package com.kotlinorm.codegen

import com.kotlinorm.Kronos
import org.intellij.lang.annotations.Language
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

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
                [[table]]
                name = "tb_user"
                className = "User"

                [[table]]
                name = "student"
                className = "Student"

                [strategy]
                tableNamingStrategy = "lineHumpNamingStrategy"
                fieldNamingStrategy = "lineHumpNamingStrategy"
                createTimeStrategy = "createTime"
                updateTimeStrategy = "updateTime"
                logicDeleteStrategy = "deleted"

                [output]
                targetDir = "./src/main/kotlin/com/kotlinorm/orm/table/"
                packageName = "com.kotlinorm.orm.table"
                tableCommentLineWords = 80

                [dataSource]
                dataSourceClassName = "org.apache.commons.dbcp2.BasicDataSource"
                wrapperClassName = "com.kotlinorm.codegen.SampleMysqlJdbcWrapper"
                url = "jdbc:mysql://localhost:3306/kronos_testing?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&useServerPrepStmts=true&rewriteBatchedStatements=true"
                username = "root"
                password = "******"
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
        init(configPath)
        val config = codeGenConfig!!

        assertEquals("tb_user", config.table[0].name)
        assertEquals("student", config.table[1].name)
        assertEquals("User", config.table[0].className)
        assertEquals("Student", config.table[1].className)
        assertEquals("./src/main/kotlin/com/kotlinorm/orm/table/", config.output.targetDir)
        assertEquals("com.kotlinorm.orm.table", config.output.packageName)
        assertEquals(Kronos.lineHumpNamingStrategy, config.strategy.tableNamingStrategy)
        assertEquals(Kronos.lineHumpNamingStrategy, config.strategy.fieldNamingStrategy)
        assertEquals("createTime", config.strategy.createTimeStrategy?.field?.name)
        assertEquals("updateTime", config.strategy.updateTimeStrategy?.field?.name)
        assertEquals("deleted", config.strategy.logicDeleteStrategy?.field?.name)
        assertEquals("com.kotlinorm.codegen.SampleMysqlJdbcWrapper", config.dataSource::class.java.name)
        assertEquals(
            "jdbc:mysql://localhost:3306/kronos_testing?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&useServerPrepStmts=true&rewriteBatchedStatements=true",
            config.dataSource.url
        )
        assertEquals("root", config.dataSource.userName)
        assertEquals("Mysql", config.dataSource.dbType.name)

        val dataSource = config.dataSource as SampleMysqlJdbcWrapper
        assertEquals("org.apache.commons.dbcp2.BasicDataSource", dataSource.dataSource::class.java.name)

        val kronosConfig = config.toKronosConfigs()
        val tbUserConfig = kronosConfig[0]
        assertEquals(
            tbUserConfig.formatedComment, "// Sample Table Comment",
            "Expected comment to contain 'Sample Table Comment', but got '${tbUserConfig.formatedComment}'"
        )

        //fields:
        assertEquals(
            tbUserConfig.fields.size, 6,
            "Expected 6 fields, but got ${tbUserConfig.fields.size}"
        )
        assertEquals(
            tbUserConfig.fields[0].name, "id",
            "Expected first field name to be 'id', but got '${tbUserConfig.fields[0].name}'"
        )
        assertEquals(
            tbUserConfig.fields[1].name, "username",
            "Expected second field name to be 'username', but got '${tbUserConfig.fields[1].name}'"
        )
        assertEquals(
            tbUserConfig.fields[2].name, "gender",
            "Expected third field name to be 'gender', but got '${tbUserConfig.fields[2].name}'"
        )
        assertEquals(
            tbUserConfig.fields[3].name, "createTime",
            "Expected fourth field name to be 'createTime', but got '${tbUserConfig.fields[3].name}'"
        )
        assertEquals(
            tbUserConfig.fields[4].name, "updateTime",
            "Expected fifth field name to be 'updateTime', but got '${tbUserConfig.fields[4].name}'"
        )
        assertEquals(
            tbUserConfig.fields[5].name, "deleted",
            "Expected sixth field name to be 'deleted', but got '${tbUserConfig.fields[5].name}'"
        )

        //indexes:
        assertEquals(
            tbUserConfig.indexes.size, 1,
            "Expected 1 index, but got ${tbUserConfig.indexes.size}"
        )
        assertEquals(
            tbUserConfig.indexes[0].name, "PRIMARY",
            "Expected index name to be 'PRIMARY', but got '${tbUserConfig.indexes[0].name}'"
        )
        assertEquals(
            tbUserConfig.indexes[0].columns.size, 1,
            "Expected index to have 1 column, but got ${tbUserConfig.indexes[0].columns.size}"
        )
        assertEquals(
            tbUserConfig.indexes[0].columns[0], "id",
            "Expected index column name to be 'id', but got '${tbUserConfig.indexes[0].columns[0]}'"
        )
        assertEquals(
            tbUserConfig.indexes[0].type, "UNIQUE",
            "Expected index type to be 'BTREE', but got '${tbUserConfig.indexes[0].type}'"
        )
    }

    @Test
    fun noTableConfig() {
        val configPath = File(tempDir, "noTableConfig.toml").apply {
            parentFile.mkdirs()
            writeTomlContent(
                """
                [strategy]
                [output]
                [dataSource]
                """.trimIndent()
            )
        }.path

        assertFailsWith<IllegalArgumentException>("Table configuration is required in config: $configPath") {
            init(configPath)
        }
    }

    @Test
    fun noTableListConfig() {
        val configPath = File(tempDir, "noTableListConfig.toml").apply {
            parentFile.mkdirs()
            writeTomlContent(
                """
                [table]
                name = "tb_user"
                className = "User"

                [strategy]
                [output]
                [dataSource]
                """.trimIndent()
            )
        }.path

        assertFailsWith<IllegalArgumentException>("Table configuration must be a list of table definitions in config: $configPath") {
            init(configPath)
        }
    }

    @Test
    fun noOutputConfig() {
        val configPath = File(tempDir, "noOutputConfig.toml").apply {
            parentFile.mkdirs()
            writeTomlContent(
                """
                [[table]]
                name = "tb_user"
                className = "User"

                [strategy]
                [dataSource]
                """.trimIndent()
            )
        }.path

        assertFailsWith<IllegalArgumentException>("Output configuration is required in config: $configPath") {
            init(configPath)
        }
    }

    @Test
    fun noDataSourceConfig() {
        val configPath = File(tempDir, "noDataSourceConfig.toml").apply {
            parentFile.mkdirs()
            writeTomlContent(
                """
                [[table]]
                name = "tb_user"
                className = "User"

                [strategy]
                [output]
                """.trimIndent()
            )
        }.path

        assertFailsWith<IllegalArgumentException>("DataSource configuration is required in config: $configPath") {
            init(configPath)
        }
    }

    @Test
    fun noOutputDir(){
        val configPath = File(tempDir, "noOutputDir.toml").apply {
            parentFile.mkdirs()
            writeTomlContent(
                """
                [[table]]
                name = "tb_user"
                className = "User"

                [strategy]
                [output]
                packageName = "com.kotlinorm.orm.table"
                """.trimIndent()
            )
        }.path

        assertFailsWith<IllegalArgumentException>("Target directory is required in output config: $configPath") {
            init(configPath)
        }
    }

    @Test
    fun noStrategyConfig() {
        val configPath = File(tempDir, "noStrategyConfig.toml").apply {
            parentFile.mkdirs()
            writeTomlContent(
                """
                [[table]]
                name = "tb_user"
                className = "User"

                [output]
                targetDir = "./src/main/kotlin/com/kotlinorm/orm/table/"
                [dataSource]
                wrapperClassName = "com.kotlinorm.codegen.SampleMysqlJdbcWrapper"
                username = "root"
                url = "jdbc:mysql://localhost:3306/kronos_testing?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&useServerPrepStmts=true&rewriteBatchedStatements=true"
                driverClassName = "com.mysql.cj.jdbc.Driver"
                """.trimIndent()
            )
        }.path

        init(configPath)
        assertNotNull(codeGenConfig!!.strategy)
    }
}