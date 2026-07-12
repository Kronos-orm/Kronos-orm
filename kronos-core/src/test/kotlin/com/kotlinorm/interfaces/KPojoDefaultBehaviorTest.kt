package com.kotlinorm.interfaces

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import com.kotlinorm.beans.config.KronosCommonStrategy
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

        assertEquals(KPojo::class, invokeDefault("get__kClass", pojo))
        assertEquals(emptyMap<String, Any?>(), invokeDefault("toDataMap", pojo))
        assertSame(pojo, invokeDefault("safeFromMapData", pojo, mapOf("id" to 1)))
        assertSame(pojo, invokeDefault("fromMapData", pojo, mapOf("id" to 1)))
        assertNull(invokeDefault("get", pojo, "id"))
        invokeDefault("set", pojo, "id", 1)
        invokeDefault("set__kClass", pojo, KPojo::class)
        invokeDefault("set__tableName", pojo, "ignored")
        invokeDefault("set__tableComment", pojo, "ignored")
        invokeDefault("set__columns", pojo, mutableListOf<Field>())
        invokeDefault("set__tableIndexes", pojo, mutableListOf<KTableIndex>())
        invokeDefault("set__createTime", pojo, invokeDefault("get__createTime", pojo))
        invokeDefault("set__updateTime", pojo, invokeDefault("get__updateTime", pojo))
        invokeDefault("set__logicDelete", pojo, invokeDefault("get__logicDelete", pojo))
        invokeDefault("set__optimisticLock", pojo, invokeDefault("get__optimisticLock", pojo))
        assertEquals(emptyList<KTableIndex>(), invokeDefault("get__tableIndexes", pojo))
        assertEquals(emptyList<Field>(), invokeDefault("get__columns", pojo))
        assertEquals("createTime", (invokeDefault("get__createTime", pojo) as KronosCommonStrategy).field.name)
        assertEquals("updateTime", (invokeDefault("get__updateTime", pojo) as KronosCommonStrategy).field.name)
        assertEquals("deleted", (invokeDefault("get__logicDelete", pojo) as KronosCommonStrategy).field.name)
        assertEquals("version", (invokeDefault("get__optimisticLock", pojo) as KronosCommonStrategy).field.name)
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
