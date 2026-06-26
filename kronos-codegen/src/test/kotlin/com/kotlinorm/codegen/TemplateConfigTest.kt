package com.kotlinorm.codegen

import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.KColumnType
import org.apache.commons.dbcp2.BasicDataSource
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TemplateConfigTest {

    private fun createMockWrapper(): SampleMysqlJdbcWrapper {
        val ds = BasicDataSource().apply {
            url = "jdbc:mysql://localhost:3306/test"
            username = "root"
            password = ""
            driverClassName = "com.mysql.cj.jdbc.Driver"
        }
        return SampleMysqlJdbcWrapper(ds)
    }

    @BeforeTest
    fun setup() {
        codeGenConfig = null
        Kronos.init {
            fieldNamingStrategy = Kronos.lineHumpNamingStrategy
            tableNamingStrategy = Kronos.lineHumpNamingStrategy
            createTimeStrategy = KronosCommonStrategy(true, Field("create_time", "createTime"))
            updateTimeStrategy = KronosCommonStrategy(true, Field("update_time", "updateTime"))
            logicDeleteStrategy = KronosCommonStrategy(true, Field("deleted"))
            optimisticLockStrategy = KronosCommonStrategy(true, Field("version"))
        }
    }

    @Test
    fun getPackageNameReturnsOutputPackageName() {
        val wrapper = createMockWrapper()
        val config = TemplateConfig(
            table = listOf(TableConfig("tb_user", "User")),
            strategy = StrategyConfig(
                Kronos.lineHumpNamingStrategy, Kronos.lineHumpNamingStrategy,
                null, null, null, null, null
            ),
            output = OutputConfig(
                targetDir = "/src/main/kotlin/com/example/tables",
                packageName = "com.example.tables",
                tableCommentLineWords = 80
            ),
            dataSource = wrapper
        )
        assertEquals("com.example.tables", config.packageName)
    }

    @Test
    fun getTargetDirReturnsOutputTargetDir() {
        val wrapper = createMockWrapper()
        val config = TemplateConfig(
            table = listOf(TableConfig("tb_user", "User")),
            strategy = StrategyConfig(
                Kronos.lineHumpNamingStrategy, Kronos.lineHumpNamingStrategy,
                null, null, null, null, null
            ),
            output = OutputConfig(
                targetDir = "/my/target/dir",
                packageName = "com.test",
                tableCommentLineWords = null
            ),
            dataSource = wrapper
        )
        assertEquals("/my/target/dir", config.targetDir)
    }

    @Test
    fun getTableCommentLineWordsDefaultsToMax() {
        val wrapper = createMockWrapper()
        val config = TemplateConfig(
            table = listOf(TableConfig("tb_user", "User")),
            strategy = StrategyConfig(
                Kronos.lineHumpNamingStrategy, Kronos.lineHumpNamingStrategy,
                null, null, null, null, null
            ),
            output = OutputConfig(
                targetDir = "/dir",
                packageName = "com.test",
                tableCommentLineWords = null
            ),
            dataSource = wrapper
        )
        assertEquals(MAX_COMMENT_LINE_WORDS, config.tableCommentLineWords)
    }

    @Test
    fun toKronosConfigsReturnsCorrectSize() {
        val wrapper = createMockWrapper()
        val config = TemplateConfig(
            table = listOf(
                TableConfig("tb_user", "User"),
                TableConfig("tb_order", "Order")
            ),
            strategy = StrategyConfig(
                Kronos.lineHumpNamingStrategy, Kronos.lineHumpNamingStrategy,
                null, null, null, null, null
            ),
            output = OutputConfig(
                targetDir = "/dir",
                packageName = "com.test",
                tableCommentLineWords = 80
            ),
            dataSource = wrapper
        )
        val templates = config.toKronosConfigs()
        assertEquals(2, templates.size)
        assertEquals("User", templates[0].className)
        assertEquals("Order", templates[1].className)
        assertEquals("tb_user", templates[0].tableName)
        assertEquals("tb_order", templates[1].tableName)
    }

    @Test
    fun templateThrowsWhenCodeGenConfigIsNull() {
        codeGenConfig = null
        assertFailsWith<IllegalStateException> {
            TemplateConfig.template { }
        }
    }

    @Test
    fun packageNameFallsBackToTargetDirPath() {
        val wrapper = createMockWrapper()
        val config = TemplateConfig(
            table = listOf(TableConfig("tb_user", "User")),
            strategy = StrategyConfig(
                Kronos.lineHumpNamingStrategy, Kronos.lineHumpNamingStrategy,
                null, null, null, null, null
            ),
            output = OutputConfig(
                targetDir = "/src/main/kotlin/com/example/model",
                packageName = null,
                tableCommentLineWords = null
            ),
            dataSource = wrapper
        )
        assertEquals("com.example.model", config.packageName)
    }

    @Test
    fun packageNameFallsBackToDefaultWhenNoMainKotlin() {
        val wrapper = createMockWrapper()
        val config = TemplateConfig(
            table = listOf(TableConfig("tb_user", "User")),
            strategy = StrategyConfig(
                Kronos.lineHumpNamingStrategy, Kronos.lineHumpNamingStrategy,
                null, null, null, null, null
            ),
            output = OutputConfig(
                targetDir = "/some/random/path",
                packageName = null,
                tableCommentLineWords = null
            ),
            dataSource = wrapper
        )
        // Falls back to "com.kotlinorm.orm.table" since no "main/kotlin/" in path
        assertNotNull(config.packageName)
    }

    @Test
    fun classNameDerivedFromTableNameWhenNotSpecified() {
        val wrapper = createMockWrapper()
        val config = TemplateConfig(
            table = listOf(TableConfig("tb_user", null)),
            strategy = StrategyConfig(
                Kronos.lineHumpNamingStrategy, Kronos.lineHumpNamingStrategy,
                null, null, null, null, null
            ),
            output = OutputConfig(
                targetDir = "/dir",
                packageName = "com.test",
                tableCommentLineWords = 80
            ),
            dataSource = wrapper
        )
        // lineHumpNamingStrategy converts "tb_user" -> "tbUser", then titlecase -> "TbUser"
        assertTrue(config.classNames[0].isNotEmpty())
        assertEquals(config.classNames[0][0], config.classNames[0][0].uppercaseChar())
    }

    @Test
    fun templateHappyPathReturnsKronosConfigs() {
        val wrapper = createMockWrapper()
        codeGenConfig = TemplateConfig(
            table = listOf(TableConfig("tb_user", "User")),
            strategy = StrategyConfig(
                Kronos.lineHumpNamingStrategy, Kronos.lineHumpNamingStrategy,
                null, null, null, null, null
            ),
            output = OutputConfig(
                targetDir = "/dir",
                packageName = "com.test",
                tableCommentLineWords = 80
            ),
            dataSource = wrapper
        )
        val results = TemplateConfig.template { }
        assertEquals(1, results.size)
        assertTrue(results[0].outputPath.endsWith("User.kt"))
    }
}
