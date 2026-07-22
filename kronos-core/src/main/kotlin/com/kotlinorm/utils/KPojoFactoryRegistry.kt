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

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.utils

import com.kotlinorm.Kronos
import com.kotlinorm.exceptions.InvalidKPojoFactoryResult
import com.kotlinorm.exceptions.KPojoConstructionException
import com.kotlinorm.exceptions.MissingKPojoFactory
import com.kotlinorm.interfaces.KPojo
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Creates a fresh, empty instance for one complete logical KPojo [KType].
 *
 * A factory only constructs the object; field assignment and value conversion
 * remain in the normal mapper/ValueCodec flow. Implementations must return a
 * type-compatible fresh instance and may not use reflection as a fallback.
 */
fun interface KPojoFactory {
    /**
     * Creates one fresh instance for the exact complete [type].
     *
     * @param type exact requested KPojo type; generic KPojo types are rejected
     * @return fresh KPojo matching [type], ignoring only top-level nullability
     */
    fun create(type: KType): KPojo
}

/**
 * Registration handle for one exact KPojo factory override.
 *
 * Closing is idempotent and removes only this registration. The next older
 * user registration or generated factory becomes visible after close.
 */
interface KPojoFactoryRegistration : AutoCloseable {
    /**
     * Removes this override from future exact-type lookups.
     * Repeated calls have no effect.
     */
    override fun close()
}

private data class UserFactoryEntry(
    val id: Long,
    val factory: KPojoFactory
)

private object KPojoFactories {
    private val lock = Any()
    private val userFactories = mutableMapOf<KTypeKey, MutableList<UserFactoryEntry>>()
    private var nextId = 0L

    fun register(type: KType, factory: KPojoFactory): KPojoFactoryRegistration {
        val key = type.normalizedKPojoType()
        val entry = synchronized(lock) {
            UserFactoryEntry(++nextId, factory).also { userFactories.getOrPut(key, ::mutableListOf) += it }
        }
        return object : KPojoFactoryRegistration {
            private val closed = AtomicBoolean(false)

            override fun close() {
                if (!closed.compareAndSet(false, true)) return
                synchronized(lock) {
                    userFactories[key]?.let { entries ->
                        entries.removeAll { it.id == entry.id }
                        if (entries.isEmpty()) userFactories.remove(key)
                    }
                }
            }
        }
    }

    fun create(type: KType): KPojo {
        val key = type.normalizedKPojoType()
        val userFactory = synchronized(lock) { userFactories[key]?.lastOrNull()?.factory }
        val result = if (userFactory != null) {
            invokeFactory(type, userFactory)
        } else {
            val generatedFactory = generatedTypeMetadata().kPojoFactory(type)?.factory
                ?: throw MissingKPojoFactory(type)
            invokeFactory(type, generatedFactory)
        }
        return result.validateFactoryResult(type)
    }

    private fun invokeFactory(type: KType, factory: KPojoFactory): KPojo = try {
        factory.create(type)
    } catch (cause: CancellationException) {
        throw cause
    } catch (cause: Error) {
        throw cause
    } catch (cause: Throwable) {
        throw KPojoConstructionException(type, cause)
    }
}

private fun KPojo.validateFactoryResult(requestedType: KType): KPojo {
    val actualType = __kType
    val requestedKey = KTypeKey.from(requestedType, ignoreTopLevelNullability = true)
    val actualKey = KTypeKey.from(actualType, ignoreTopLevelNullability = true)
    if (actualKey != requestedKey) {
        throw InvalidKPojoFactoryResult(
            requestedType,
            actualType,
            "expected complete KType $requestedType, ignoring only top-level nullability, got $actualType"
        )
    }
    return this
}

/**
 * Registers one exact KPojo factory override in the process-wide registry.
 *
 * @param type complete, concrete and currently non-generic KPojo type
 * @param factory fresh-instance constructor for [type]
 * @return idempotent registration handle scoped to this override
 */
internal fun registerKPojoFactory(type: KType, factory: KPojoFactory): KPojoFactoryRegistration =
    KPojoFactories.register(type, factory)

/**
 * Creates a fresh KPojo through the highest-priority exact factory.
 *
 * @param type complete, concrete and currently non-generic KPojo type
 * @return type-validated fresh instance
 */
internal fun createKPojo(type: KType): KPojo = KPojoFactories.create(type)

/**
 * Registers a factory override for exact [T]. Later registrations win, and
 * closing the returned handle restores the previous user or generated factory.
 *
 * @param factory fresh-instance constructor for [T]
 * @return idempotent exact-type registration handle
 * @throws com.kotlinorm.exceptions.UnsupportedType when [T] is unsupported
 */
inline fun <reified T : KPojo> registerKPojo(noinline factory: () -> T): KPojoFactoryRegistration =
    Kronos.registerKPojoFactory(typeOf<T>(), KPojoFactory { factory() })

/**
 * Creates a fresh [T] through the exact generated or highest-priority user
 * factory registered for its complete reified KType.
 *
 * @return fresh type-validated [T]
 * @throws com.kotlinorm.exceptions.KPojoFactoryException when construction fails
 */
inline fun <reified T : KPojo> createKPojo(): T = Kronos.createKPojo(typeOf<T>()) as T
