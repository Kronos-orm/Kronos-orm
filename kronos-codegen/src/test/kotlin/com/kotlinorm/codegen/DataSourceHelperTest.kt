package com.kotlinorm.codegen

import org.apache.commons.dbcp2.BasicDataSource
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DataSourceHelperTest {

    @Ignore("Temporarily disabled - needs investigation")
    @Test
    fun testCreateWrapperWithValidClassName() {
        val dataSource = BasicDataSource()
        val wrapper = createWrapper("com.kotlinorm.codegen.SampleMysqlJdbcWrapper", dataSource)
        assertNotNull(wrapper)
        assertEquals("com.kotlinorm.codegen.SampleMysqlJdbcWrapper", wrapper::class.java.name)
    }

    @Ignore("Temporarily disabled - needs investigation")
    @Test
    fun testCreateWrapperWithNullClassName() {
        val dataSource = BasicDataSource()
        val wrapper = createWrapper(null, dataSource)
        assertNotNull(wrapper)
        assertEquals("com.kotlinorm.KronosBasicWrapper", wrapper::class.java.name)
    }

    @Test
    fun testCreateWrapperWithInvalidClassName() {
        val dataSource = BasicDataSource()
        assertFailsWith<IllegalStateException> {
            createWrapper("com.invalid.NonExistentWrapper", dataSource)
        }
    }

    @Test
    fun testCreateWrapperWithNoCompatibleConstructor() {
        val dataSource = BasicDataSource()
        assertFailsWith<IllegalStateException> {
            createWrapper("java.lang.String", dataSource)
        }
    }

    @Test
    fun testInitialDataSourceWithBasicConfig() {
        val config = mapOf(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "url" to "jdbc:mysql://localhost:3306/test",
            "username" to "root",
            "password" to "password",
            "driverClassName" to "com.mysql.cj.jdbc.Driver",
            "initialSize" to 5,
            "maxTotal" to 10
        )
        val dataSource = initialDataSource(config) as BasicDataSource
        assertNotNull(dataSource)
        assertEquals("jdbc:mysql://localhost:3306/test", dataSource.url)
        assertEquals("root", dataSource.username)
        assertEquals("password", dataSource.password)
        assertEquals("com.mysql.cj.jdbc.Driver", dataSource.driverClassName)
        assertEquals(5, dataSource.initialSize)
        assertEquals(10, dataSource.maxTotal)
    }

    @Test
    fun testInitialDataSourceWithDefaultClassName() {
        val config = mapOf(
            "url" to "jdbc:mysql://localhost:3306/test",
            "username" to "testuser"
        )
        val dataSource = initialDataSource(config)
        assertNotNull(dataSource)
        assertTrue(dataSource is BasicDataSource)
    }

    @Test
    fun testInitialDataSourceWithBooleanProperty() {
        val config = mapOf(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "defaultAutoCommit" to true
        )
        val dataSource = initialDataSource(config) as BasicDataSource
        assertEquals(true, dataSource.defaultAutoCommit)
    }

    @Test
    fun testInitialDataSourceWithLongProperty() {
        val config = mapOf(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "maxWaitMillis" to 30000L
        )
        val dataSource = initialDataSource(config) as BasicDataSource
        assertEquals(30000L, dataSource.maxWaitMillis)
    }

    @Test
    fun testInitialDataSourceSkipsWrapperClassName() {
        val config = mapOf(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "wrapperClassName" to "com.kotlinorm.codegen.SampleMysqlJdbcWrapper",
            "url" to "jdbc:mysql://localhost:3306/test"
        )
        val dataSource = initialDataSource(config) as BasicDataSource
        assertNotNull(dataSource)
        assertEquals("jdbc:mysql://localhost:3306/test", dataSource.url)
    }

    @Test
    fun testInitialDataSourceWithInvalidProperty() {
        val config = mapOf(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "nonExistentProperty" to "value"
        )
        // Should not throw, just log warning
        val dataSource = initialDataSource(config)
        assertNotNull(dataSource)
    }

    @Test
    fun testInitialDataSourceWithNullValue() {
        val config = mapOf<String, Any?>(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "password" to null
        )
        val dataSource = initialDataSource(config) as BasicDataSource
        assertNotNull(dataSource)
    }

    @Test
    fun testInitialDataSourceWithNumberConversion() {
        val config = mapOf(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "initialSize" to 5.0 // Double to Int conversion
        )
        val dataSource = initialDataSource(config) as BasicDataSource
        assertEquals(5, dataSource.initialSize)
    }

    @Ignore("Temporarily disabled - needs investigation")
    @Test
    fun testInitialDataSourceWithUppercaseProperty() {
        val config = mapOf(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "URL" to "jdbc:mysql://localhost:3306/test"
        )
        val dataSource = initialDataSource(config) as BasicDataSource
        assertEquals("jdbc:mysql://localhost:3306/test", dataSource.url)
    }
}
