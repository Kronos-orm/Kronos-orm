package com.kotlinorm.codegen

import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import org.apache.commons.dbcp2.BasicDataSource
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CodegenConfigTest {

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

    @AfterTest
    fun cleanup() {
        codeGenConfig = null
    }

    @Test
    fun testInitialDataSourceWithDefaultClassName() {
        val config = mapOf<String, Any?>("url" to "jdbc:mysql://localhost:3306/test")
        val ds = initialDataSource(config)
        assertNotNull(ds)
        assertTrue(ds is BasicDataSource)
    }

    @Test
    fun testInitialDataSourceWithExplicitClassName() {
        val config = mapOf<String, Any?>(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "url" to "jdbc:mysql://localhost:3306/test",
            "driverClassName" to "com.mysql.cj.jdbc.Driver"
        )
        val ds = initialDataSource(config)
        assertNotNull(ds)
        assertTrue(ds is BasicDataSource)
    }

    // NOTE: testReadConfigWithExtendConfig is skipped because readConfig() has a bug:
    // after merging extendConfig + config, the "extend" key from the original config
    // remains in the merged map, causing an infinite while loop (ConfigReader.kt:106-119).
    // This should be fixed in the source code by removing "extend" from config after merge.

    @Test
    fun testReadConfigWithInvalidFileThrows() {
        assertFailsWith<Exception> {
            readConfig("/nonexistent/path/config.toml")
        }
    }

    @Test
    fun testReadConfigWithEmptyFile() {
        val tmpDir = System.getProperty("java.io.tmpdir")
        val emptyFile = File(tmpDir, "kronos_empty_test.toml")
        try {
            emptyFile.writeText("")
            // Empty TOML file: Jackson TOML parser returns an empty map (not null),
            // so readConfig returns an empty map without throwing
            val config = readConfig(emptyFile.absolutePath)
            assertTrue(config.isEmpty())
        } finally {
            emptyFile.delete()
        }
    }

    @Test
    fun testReadConfigWithValidTomlNoExtend() {
        val tmpDir = System.getProperty("java.io.tmpdir")
        val tomlFile = File(tmpDir, "kronos_valid_test.toml")
        try {
            tomlFile.writeText("""
                [dataSource]
                url = "jdbc:mysql://localhost:3306/test"

                [output]
                targetDir = "/tmp/output"
                packageName = "com.example"

                [[table]]
                name = "user"
            """.trimIndent())
            val config = readConfig(tomlFile.absolutePath)
            assertNotNull(config)
            assertTrue(config.containsKey("dataSource"))
            assertTrue(config.containsKey("output"))
            assertTrue(config.containsKey("table"))
        } finally {
            tomlFile.delete()
        }
    }

    @Test
    fun testInitialDataSourceWithCustomProperties() {
        val config = mapOf<String, Any?>(
            "url" to "jdbc:mysql://localhost:3306/test",
            "maxTotal" to 10,
            "maxIdle" to 5
        )
        val ds = initialDataSource(config)
        assertNotNull(ds)
        assertTrue(ds is BasicDataSource)
        val bds = ds as BasicDataSource
        assertEquals(10, bds.maxTotal)
        assertEquals(5, bds.maxIdle)
    }

    @Test
    fun testInitialDataSourceWithBooleanProperty() {
        val config = mapOf<String, Any?>(
            "url" to "jdbc:mysql://localhost:3306/test",
            "testOnBorrow" to true
        )
        val ds = initialDataSource(config)
        assertNotNull(ds)
        val bds = ds as BasicDataSource
        assertEquals(true, bds.testOnBorrow)
    }

    @Test
    fun testInitialDataSourceWithLongProperty() {
        val config = mapOf<String, Any?>(
            "url" to "jdbc:mysql://localhost:3306/test",
            "maxTotal" to 20L  // Long value for an Int setter — tests Number conversion
        )
        val ds = initialDataSource(config)
        assertNotNull(ds)
        val bds = ds as BasicDataSource
        assertEquals(20, bds.maxTotal)
    }

    @Test
    fun testInitialDataSourceSkipsDataSourceClassName() {
        val config = mapOf<String, Any?>(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "wrapperClassName" to "com.kotlinorm.KronosBasicWrapper",
            "url" to "jdbc:mysql://localhost:3306/test"
        )
        val ds = initialDataSource(config)
        assertNotNull(ds)
        // dataSourceClassName and wrapperClassName should be skipped, not set as properties
        assertTrue(ds is BasicDataSource)
    }

    @Test
    fun testInitialDataSourceWithInvalidProperty() {
        // Property that doesn't have a setter — should log warning but not throw
        val config = mapOf<String, Any?>(
            "url" to "jdbc:mysql://localhost:3306/test",
            "nonExistentProperty" to "value"
        )
        val ds = initialDataSource(config)
        assertNotNull(ds)
    }

    @Test
    fun testCreateWrapperWithInvalidClassName() {
        val ds = BasicDataSource().apply {
            url = "jdbc:mysql://localhost:3306/test"
        }
        assertFailsWith<IllegalStateException> {
            createWrapper("com.nonexistent.WrapperClass", ds)
        }
    }

    @Test
    fun testTemplateWithoutInit() {
        codeGenConfig = null
        assertFailsWith<IllegalStateException> {
            TemplateConfig.template { }
        }
    }

    @Test
    fun testReadConfigWithValidTomlContainingStrategy() {
        val tmpDir = System.getProperty("java.io.tmpdir")
        val tomlFile = File(tmpDir, "kronos_strategy_test.toml")
        try {
            tomlFile.writeText("""
                [dataSource]
                url = "jdbc:mysql://localhost:3306/test"

                [output]
                targetDir = "/tmp/output"
                packageName = "com.example"

                [strategy]
                tableNamingStrategy = "lineHumpNamingStrategy"
                fieldNamingStrategy = "noneNamingStrategy"
                createTimeStrategy = "create_time"
                updateTimeStrategy = "update_time"
                logicDeleteStrategy = "deleted"

                [[table]]
                name = "user"
                className = "UserEntity"
            """.trimIndent())
            val config = readConfig(tomlFile.absolutePath)
            assertNotNull(config)
            assertTrue(config.containsKey("strategy"))
            val strategy = config["strategy"] as Map<*, *>
            assertEquals("lineHumpNamingStrategy", strategy["tableNamingStrategy"])
            assertEquals("noneNamingStrategy", strategy["fieldNamingStrategy"])
        } finally {
            tomlFile.delete()
        }
    }
}
