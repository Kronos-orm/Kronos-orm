/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import kotlin.reflect.typeOf

class KPojoDefaultBehaviorTest {

    @Test
    fun `default KPojo methods return compiler-plugin placeholders`() {
        val pojo = defaultReceiver()

        assertEquals(typeOf<KPojo>(), invokeDefault("get__kType", pojo))
        assertEquals(emptyMap<String, Any?>(), invokeDefault("toDataMap", pojo))
        assertSame(pojo, invokeDefault("safeFromMapData", pojo, mapOf("id" to 1)))
        assertSame(pojo, invokeDefault("fromMapData", pojo, mapOf("id" to 1)))
        assertNull(invokeDefault("get", pojo, "id"))
        invokeDefault("set", pojo, "id", 1)
        invokeDefault("set__kType", pojo, typeOf<KPojo>())
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
