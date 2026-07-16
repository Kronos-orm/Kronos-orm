package com.kotlinorm.codegen

import com.kotlinorm.wrappers.KronosJdbcWrapper
import org.apache.commons.dbcp2.BasicDataSource
import java.lang.reflect.InvocationTargetException
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DataSourceHelperTest {
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

    @Test
    fun testInitialDataSourceWithUppercaseProperty() {
        val config = mapOf(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "URL" to "jdbc:mysql://localhost:3306/test"
        )
        val dataSource = initialDataSource(config) as BasicDataSource
        assertEquals("jdbc:mysql://localhost:3306/test", dataSource.url)
    }

    @Test
    fun createWrapperWithNullClassNameUsesDefault() {
        val ds = BasicDataSource()
        val error = assertFailsWith<IllegalStateException> {
            createWrapper(null, ds)
        }

        assertEquals(
            "Failed to create wrapper for ${KronosJdbcWrapper::class.java.name}",
            error.message
        )
        assertTrue(error.cause is InvocationTargetException)
    }

    @Test
    fun createWrapperWithValidWrapperClass() {
        val ds = BasicDataSource().apply {
            url = "jdbc:mysql://localhost:3306/test"
            username = "root"
            password = ""
            driverClassName = "com.mysql.cj.jdbc.Driver"
        }
        val wrapper = createWrapper("com.kotlinorm.codegen.SampleMysqlJdbcWrapper", ds)
        assertNotNull(wrapper)
        assertTrue(wrapper is SampleMysqlJdbcWrapper)
    }

    @Test
    fun createWrapperWithClassNotFoundThrows() {
        val ds = BasicDataSource()
        val ex = assertFailsWith<IllegalStateException> {
            createWrapper("com.nonexistent.FakeWrapper", ds)
        }
        assertEquals("Wrapper class not found: com.nonexistent.FakeWrapper", ex.message)
    }

    @Test
    fun createWrapperWithNoCompatibleConstructorThrows() {
        val ds = BasicDataSource()
        val ex = assertFailsWith<IllegalStateException> {
            createWrapper("java.lang.String", ds)
        }
        assertEquals(
            "No compatible constructor found for java.lang.String. Expected (org.apache.commons.dbcp2.BasicDataSource or javax.sql.DataSource).",
            ex.message
        )
    }

    @Test
    fun initialDataSourceWithBooleanConversion() {
        val config = mapOf(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "defaultAutoCommit" to true,
            "defaultReadOnly" to false
        )
        val ds = initialDataSource(config) as BasicDataSource
        assertEquals(true, ds.defaultAutoCommit)
        assertEquals(false, ds.defaultReadOnly)
    }

    @Test
    fun initialDataSourceWithStringConversion() {
        val config = mapOf(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "url" to "jdbc:mysql://localhost:3306/mydb",
            "username" to "admin",
            "password" to "secret123"
        )
        val ds = initialDataSource(config) as BasicDataSource
        assertEquals("jdbc:mysql://localhost:3306/mydb", ds.url)
        assertEquals("admin", ds.username)
        assertEquals("secret123", ds.password)
    }

    @Test
    fun initialDataSourceWithIntFromDoubleConversion() {
        val config = mapOf(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "initialSize" to 3.0,
            "maxTotal" to 20.0
        )
        val ds = initialDataSource(config) as BasicDataSource
        assertEquals(3, ds.initialSize)
        assertEquals(20, ds.maxTotal)
    }

    @Test
    fun initialDataSourceWithLongConversion() {
        val config = mapOf(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "maxWaitMillis" to 5000L
        )
        val ds = initialDataSource(config) as BasicDataSource
        assertEquals(5000L, ds.maxWaitMillis)
    }

    @Test
    fun initialDataSourceSkipsDataSourceClassName() {
        val config = mapOf(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "wrapperClassName" to "com.kotlinorm.codegen.SampleMysqlJdbcWrapper",
            "url" to "jdbc:mysql://localhost/db"
        )
        val ds = initialDataSource(config) as BasicDataSource
        // wrapperClassName and dataSourceClassName should be skipped, url should be set
        assertEquals("jdbc:mysql://localhost/db", ds.url)
    }

    @Test
    fun initialDataSourceWithNonExistentPropertyDoesNotThrow() {
        val config = mapOf(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "thisPropertyDoesNotExist" to "someValue"
        )
        // Should not throw, just log a warning
        val ds = initialDataSource(config)
        assertNotNull(ds)
    }

    @Test
    fun initialDataSourceWithNullValueDoesNotThrow() {
        val config = mapOf<String, Any?>(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "validationQuery" to null
        )
        val ds = initialDataSource(config)
        assertNotNull(ds)
    }

    @Test
    fun initialDataSourceWithIncompatibleValueLogsWarning() {
        // Passing a value that can't be converted to the setter's type
        // triggers the IllegalArgumentException catch branch
        val config = mapOf<String, Any?>(
            "dataSourceClassName" to "org.apache.commons.dbcp2.BasicDataSource",
            "initialSize" to "not-a-number"  // String where Int expected, convertValue throws
        )
        val ds = initialDataSource(config)
        assertNotNull(ds)
    }

    @Test
    fun initialDataSourceSupportsEmptyAndAlternateSetterNames() {
        val config = linkedMapOf<String, Any?>(
            "dataSourceClassName" to SetterProbeDataSource::class.java.name,
            "" to "empty-key",
            "mixedKey" to "second-candidate",
            "upperOnly" to "third-candidate",
            "lowerOnly" to "fourth-candidate",
            "stringValue" to 42,
            "badCount" to "ignored",
            "wrongType" to "ignored",
            "explodes" to "attempted"
        )

        val ds = initialDataSource(config) as SetterProbeDataSource

        assertEquals("empty-key", ds.emptyKeyValue)
        assertEquals("second-candidate", ds.mixedKeyValue)
        assertEquals("third-candidate", ds.upperOnlyValue)
        assertEquals("fourth-candidate", ds.lowerOnlyValue)
        assertEquals("42", ds.observedStringValue)
        assertEquals(0, ds.wrongTypeCalls)
        assertEquals(1, ds.explodeAttempts)
    }

    @Test
    fun createWrapperReportsWrongTypesAndConstructorFailures() {
        val ds = BasicDataSource()
        val wrongType = assertFailsWith<IllegalStateException> {
            createWrapper(NonWrapper::class.java.name, ds)
        }
        assertEquals("Failed to create wrapper for ${NonWrapper::class.java.name}", wrongType.message)
        assertTrue(wrongType.cause is ClassCastException)

        val constructorFailure = assertFailsWith<IllegalStateException> {
            createWrapper(ThrowingWrapper::class.java.name, ds)
        }
        assertEquals(
            "Failed to create wrapper for ${ThrowingWrapper::class.java.name}",
            constructorFailure.message
        )
        assertTrue(constructorFailure.cause is InvocationTargetException)
        assertEquals("constructor failure", constructorFailure.cause?.cause?.message)
    }

    @Test
    fun `conversion helper covers scalar enum and failure results`() {
        val marker = SetterPayload("same-instance")

        assertSame(marker, invokeConversion(marker, SetterPayload::class.java))
        assertEquals(null, invokeConversion(null, String::class.java))
        assertEquals(7, invokeConversion(7.9, Int::class.javaPrimitiveType!!))
        assertEquals(7L, invokeConversion(7.9, Long::class.javaPrimitiveType!!))
        assertEquals(true, invokeConversion("TRUE", Boolean::class.javaPrimitiveType!!))
        assertEquals("42", invokeConversion(42, String::class.java))
        assertEquals(SetterMode.SLOW, invokeConversion("slow", SetterMode::class.java))

        val invalidEnum = conversionFailure("missing", SetterMode::class.java)
        assertEquals(IllegalArgumentException::class, invalidEnum::class)
        assertEquals("Invalid enum value 'missing' for SetterMode", invalidEnum.message)

        val unsupported = conversionFailure("payload", SetterPayload::class.java)
        assertEquals(IllegalArgumentException::class, unsupported::class)
        assertEquals(
            "Unsupported type conversion: class java.lang.String to class com.kotlinorm.codegen.SetterPayload",
            unsupported.message
        )

        val invalidNumber = conversionFailure("not-a-number", Int::class.javaPrimitiveType!!)
        assertEquals(TypeCastException::class, invalidNumber::class)
        assertEquals(
            "Cannot convert not-a-number (class java.lang.String) to int",
            invalidNumber.message
        )
    }

    private fun invokeConversion(value: Any?, targetType: Class<*>): Any? {
        val method = Class.forName("com.kotlinorm.codegen.DataSourceHelperKt")
            .getDeclaredMethod("convertValue", Any::class.java, Class::class.java)
            .apply { isAccessible = true }
        return method.invoke(null, value, targetType)
    }

    private fun conversionFailure(value: Any?, targetType: Class<*>): Throwable =
        assertFailsWith<InvocationTargetException> {
            invokeConversion(value, targetType)
        }.targetException
}

@Suppress("FunctionName", "FunctionNaming")
class SetterProbeDataSource : BasicDataSource() {
    var emptyKeyValue: String? = null
    var mixedKeyValue: String? = null
    var upperOnlyValue: String? = null
    var lowerOnlyValue: String? = null
    var observedStringValue: String? = null
    var wrongTypeCalls: Int = 0
    var explodeAttempts: Int = 0

    fun set(value: String) {
        emptyKeyValue = value
    }

    fun setMixedkey(value: String) {
        mixedKeyValue = value
    }

    fun setUPPERONLY(value: String) {
        upperOnlyValue = value
    }

    fun setloweronly(value: String) {
        lowerOnlyValue = value
    }

    fun setStringValue(value: String) {
        observedStringValue = value
    }

    fun setBadCount() = Unit

    fun setBadCount(
        @Suppress("UNUSED_PARAMETER") first: String,
        @Suppress("UNUSED_PARAMETER") second: String
    ) = Unit

    fun setWrongType(@Suppress("UNUSED_PARAMETER") value: Int) {
        wrongTypeCalls++
    }

    fun setExplodes(@Suppress("UNUSED_PARAMETER") value: String) {
        explodeAttempts++
        error("setter failure")
    }
}

class NonWrapper(@Suppress("UNUSED_PARAMETER") dataSource: BasicDataSource)

class ThrowingWrapper(@Suppress("UNUSED_PARAMETER") dataSource: BasicDataSource) {
    init {
        error("constructor failure")
    }
}

enum class SetterMode {
    FAST,
    SLOW
}

data class SetterPayload(val value: String)
