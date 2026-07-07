package com.kotlinorm.interfaces

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class KPojoDefaultBehaviorTest {

    @Test
    fun `default KPojo methods return compiler-plugin placeholders`() {
        val pojo = defaultReceiver()

        assertEquals(KPojo::class, invokeDefault("kClass", pojo))
        assertEquals(emptyMap<String, Any?>(), invokeDefault("toDataMap", pojo))
        assertSame(pojo, invokeDefault("safeFromMapData", pojo, mapOf("id" to 1)))
        assertSame(pojo, invokeDefault("fromMapData", pojo, mapOf("id" to 1)))
        assertNull(invokeDefault("get", pojo, "id"))
        invokeDefault("set", pojo, "id", 1)
        invokeDefault("set__tableName", pojo, "ignored")
        invokeDefault("set__tableComment", pojo, "ignored")
        assertEquals(emptyList<KTableIndex>(), invokeDefault("kronosTableIndex", pojo))
        assertEquals(emptyList<Field>(), invokeDefault("kronosColumns", pojo))
        assertEquals("id", (invokeDefault("kronosPrimaryKey", pojo) as com.kotlinorm.beans.config.KronosCommonStrategy).field.name)
        assertEquals("createTime", (invokeDefault("kronosCreateTime", pojo) as com.kotlinorm.beans.config.KronosCommonStrategy).field.name)
        assertEquals("updateTime", (invokeDefault("kronosUpdateTime", pojo) as com.kotlinorm.beans.config.KronosCommonStrategy).field.name)
        assertEquals("deleted", (invokeDefault("kronosLogicDelete", pojo) as com.kotlinorm.beans.config.KronosCommonStrategy).field.name)
        assertEquals("version", (invokeDefault("kronosOptimisticLock", pojo) as com.kotlinorm.beans.config.KronosCommonStrategy).field.name)
    }

    @Test
    fun `default KPojo table metadata getters require compiler generated overrides`() {
        val pojo = defaultReceiver()

        assertEquals(
            "__tableName must be overridden by the compiler plugin",
            assertFailsWith<IllegalStateException> { invokeDefault("get__tableName", pojo) }.message
        )
        assertEquals(
            "__tableComment must be overridden by the compiler plugin",
            assertFailsWith<IllegalStateException> { invokeDefault("get__tableComment", pojo) }.message
        )
    }

    private fun defaultReceiver(): KPojo =
        Proxy.newProxyInstance(KPojo::class.java.classLoader, arrayOf(KPojo::class.java)) { _, method, args ->
            when (method.name) {
                "toString" -> "DefaultKPojoReceiver"
                "hashCode" -> 1
                "equals" -> args?.firstOrNull() === this
                else -> null
            }
        } as KPojo

    private fun invokeDefault(name: String, receiver: KPojo, vararg args: Any?): Any? {
        val method = Class.forName("com.kotlinorm.interfaces.KPojo\$DefaultImpls")
            .methods
            .single { it.name == name && it.parameterCount == args.size + 1 }
        return try {
            method.invoke(null, receiver, *args)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }
}
