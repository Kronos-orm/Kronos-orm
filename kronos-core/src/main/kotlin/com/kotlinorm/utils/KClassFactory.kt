/**
 * Copyright 2022-2025 kronos-orm
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

package com.kotlinorm.utils

import com.kotlinorm.interfaces.KPojo
import java.util.ServiceLoader
import kotlin.reflect.KClass

typealias KPojoFactory = (KClass<out KPojo>) -> KPojo?

interface KPojoFactoryProvider {
    fun register()
}

/**
 * Reflection-free KPojo factory.
 *
 * The compiler plugin registers factory functions that map KClass values to direct
 * constructor calls.
 */
private val emptyKPojoFactory: KPojoFactory = { null }

private val bootstrapKPojoFactory: KPojoFactory = { kClass ->
    loadGeneratedKPojoFactories()
    registeredKPojoFactory(kClass)
}

private var registeredKPojoFactory: KPojoFactory = emptyKPojoFactory
private var kPojoFactory: KPojoFactory = bootstrapKPojoFactory
private var generatedKPojoFactoriesLoaded: Boolean = false

fun registerKPojoFactory(factory: KPojoFactory) {
    val previous = registeredKPojoFactory
    registeredKPojoFactory = { kClass ->
        previous(kClass) ?: factory(kClass)
    }
    if (generatedKPojoFactoriesLoaded) {
        kPojoFactory = registeredKPojoFactory
    }
}

fun registerKPojo(kClass: KClass<out KPojo>, factory: () -> KPojo) {
    registerKPojoFactory { target ->
        if (target == kClass) factory() else null
    }
}

inline fun <reified T : KPojo> registerKPojo(noinline factory: () -> T) {
    registerKPojo(T::class, factory)
}

private fun loadGeneratedKPojoFactories() {
    if (generatedKPojoFactoriesLoaded) return
    generatedKPojoFactoriesLoaded = true
    ServiceLoader.load(KPojoFactoryProvider::class.java).forEach { provider ->
        provider.register()
    }
    kPojoFactory = registeredKPojoFactory
}

@Suppress("UNCHECKED_CAST")
fun <T : KPojo> KClass<T>.createInstance(): T {
    return kPojoFactory(this) as T? ?: throw NullPointerException(
        "KClass ${qualifiedName ?: simpleName} instantiation failed.\n" +
                "No generated or registered KPojo factory matched this class.\n" +
                "Check that:\n" +
                "1. The Kronos compiler plugin is enabled for the module that declares this KPojo.\n" +
                "2. The KPojo has a no-argument constructor or default values for all constructor parameters.\n" +
                "3. Generated KPojo factory providers are packaged correctly under META-INF/services.\n" +
                "4. External or third-party KPojo classes are registered manually, for example:\n" +
                "   registerKPojo(YourKPojo::class) { YourKPojo() }\n" +
                "   or registerKPojoFactory { kClass -> when (kClass) { YourKPojo::class -> YourKPojo(); else -> null } }"
    )
}

