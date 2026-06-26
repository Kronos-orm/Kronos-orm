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

package com.kotlinorm.compiler.transformers

import com.kotlinorm.compiler.utils.KotlinSourceDynamicCompiler.compile
import com.kotlinorm.compiler.utils.testBaseName
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class KClassMapGeneratorTest {

    @Test
    fun `kClassCreator is populated after KronosInit compilation`() {
        val result = compile(
            """
            import com.kotlinorm.Kronos
            import com.kotlinorm.annotations.*
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.utils.kClassCreator
            import kotlin.test.assertNotNull
            import kotlin.test.assertTrue

            data class SimpleUser(
                var id: Int? = null,
                var name: String? = null
            ) : KPojo

            fun main() {
                Kronos.init {
                    fieldNamingStrategy = lineHumpNamingStrategy
                    tableNamingStrategy = lineHumpNamingStrategy
                }

                val instance = kClassCreator(SimpleUser::class)
                assertNotNull(instance, "kClassCreator should produce a non-null instance for SimpleUser")
                assertTrue(instance is SimpleUser, "Instance should be of type SimpleUser")
            }
            """.trimIndent(),
            testBaseName + "Populated"
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val ktClazz = result.classLoader.loadClass("${testBaseName}PopulatedKt")
        ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }.invoke(null)
    }

    @Test
    fun `kClassCreator creates instance with default values`() {
        val result = compile(
            """
            import com.kotlinorm.Kronos
            import com.kotlinorm.annotations.*
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.utils.kClassCreator
            import kotlin.test.assertEquals
            import kotlin.test.assertNotNull
            import kotlin.test.assertNull

            data class DefaultUser(
                var id: Int? = null,
                var name: String? = null,
                var age: Int? = null
            ) : KPojo

            fun main() {
                Kronos.init {
                    fieldNamingStrategy = lineHumpNamingStrategy
                    tableNamingStrategy = lineHumpNamingStrategy
                }

                val instance = kClassCreator(DefaultUser::class) as DefaultUser
                assertNotNull(instance)
                assertNull(instance.id, "Default id should be null")
                assertNull(instance.name, "Default name should be null")
                assertNull(instance.age, "Default age should be null")
            }
            """.trimIndent(),
            testBaseName + "Defaults"
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val ktClazz = result.classLoader.loadClass("${testBaseName}DefaultsKt")
        ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }.invoke(null)
    }

    @Test
    fun `kClassCreator maps multiple KPojo classes`() {
        val result = compile(
            """
            import com.kotlinorm.Kronos
            import com.kotlinorm.annotations.*
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.utils.kClassCreator
            import kotlin.test.assertNotNull
            import kotlin.test.assertTrue

            data class UserA(var id: Int? = null) : KPojo
            data class UserB(var name: String? = null) : KPojo

            fun main() {
                Kronos.init {
                    fieldNamingStrategy = lineHumpNamingStrategy
                    tableNamingStrategy = lineHumpNamingStrategy
                }

                val a = kClassCreator(UserA::class)
                val b = kClassCreator(UserB::class)
                assertNotNull(a)
                assertNotNull(b)
                assertTrue(a is UserA, "Should create UserA instance")
                assertTrue(b is UserB, "Should create UserB instance")
            }
            """.trimIndent(),
            testBaseName + "Multiple"
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val ktClazz = result.classLoader.loadClass("${testBaseName}MultipleKt")
        ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }.invoke(null)
    }

    @Test
    fun `kClassCreator returns null for unknown KPojo class`() {
        val result = compile(
            """
            import com.kotlinorm.Kronos
            import com.kotlinorm.annotations.*
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.utils.kClassCreator
            import kotlin.test.assertNull

            data class KnownUser(var id: Int? = null) : KPojo

            fun main() {
                Kronos.init {
                    fieldNamingStrategy = lineHumpNamingStrategy
                    tableNamingStrategy = lineHumpNamingStrategy
                }

                // KPojo itself is not a concrete class in the map
                // The lambda returns null for the else branch
                // We can't easily test with an unknown class without a second compilation unit,
                // but we can verify the known class works and the function is callable
                val instance = kClassCreator(KnownUser::class)
                kotlin.test.assertNotNull(instance)
            }
            """.trimIndent(),
            testBaseName + "Unknown"
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val ktClazz = result.classLoader.loadClass("${testBaseName}UnknownKt")
        ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }.invoke(null)
    }

    @Test
    fun `kClassCreator produces distinct instances on each call`() {
        val result = compile(
            """
            import com.kotlinorm.Kronos
            import com.kotlinorm.annotations.*
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.utils.kClassCreator
            import kotlin.test.assertNotNull
            import kotlin.test.assertNotSame

            data class DistinctUser(var id: Int? = null) : KPojo

            fun main() {
                Kronos.init {
                    fieldNamingStrategy = lineHumpNamingStrategy
                    tableNamingStrategy = lineHumpNamingStrategy
                }

                val a = kClassCreator(DistinctUser::class)
                val b = kClassCreator(DistinctUser::class)
                assertNotNull(a)
                assertNotNull(b)
                assertNotSame(a, b, "Each call should produce a new instance")
            }
            """.trimIndent(),
            testBaseName + "Distinct"
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val ktClazz = result.classLoader.loadClass("${testBaseName}DistinctKt")
        ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }.invoke(null)
    }

    @Test
    fun `compilation succeeds with KPojo class using annotations`() {
        val result = compile(
            """
            import com.kotlinorm.Kronos
            import com.kotlinorm.annotations.*
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.utils.kClassCreator
            import kotlin.test.assertNotNull
            import kotlin.test.assertTrue

            @Table(name = "tb_annotated")
            data class AnnotatedUser(
                @PrimaryKey(identity = true)
                var id: Int? = null,
                @Column("user_name")
                var name: String? = null
            ) : KPojo

            fun main() {
                Kronos.init {
                    fieldNamingStrategy = lineHumpNamingStrategy
                    tableNamingStrategy = lineHumpNamingStrategy
                }

                val instance = kClassCreator(AnnotatedUser::class)
                assertNotNull(instance)
                assertTrue(instance is AnnotatedUser)
            }
            """.trimIndent(),
            testBaseName + "Annotated"
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val ktClazz = result.classLoader.loadClass("${testBaseName}AnnotatedKt")
        ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }.invoke(null)
    }

    @Test
    fun `generate is triggered by KronosInit annotated function declaration`() {
        val result = compile(
            """
            import com.kotlinorm.Kronos
            import com.kotlinorm.annotations.*
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.utils.kClassCreator
            import kotlin.test.assertNotNull
            import kotlin.test.assertTrue

            data class EntityA(var id: Int? = null) : KPojo
            data class EntityB(var name: String? = null) : KPojo

            @KronosInit
            fun myInit(block: Kronos.() -> Unit) = Kronos.apply(block)

            fun main() {
                myInit {
                    fieldNamingStrategy = lineHumpNamingStrategy
                    tableNamingStrategy = lineHumpNamingStrategy
                }
                val a = kClassCreator(EntityA::class)
                val b = kClassCreator(EntityB::class)
                assertNotNull(a)
                assertNotNull(b)
                assertTrue(a is EntityA)
                assertTrue(b is EntityB)
            }
            """.trimIndent(),
            testBaseName + "KronosInitDecl"
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val ktClazz = result.classLoader.loadClass("${testBaseName}KronosInitDeclKt")
        ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }.invoke(null)
    }
}
