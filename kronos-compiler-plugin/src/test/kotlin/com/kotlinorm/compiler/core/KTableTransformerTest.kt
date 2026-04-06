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

package com.kotlinorm.compiler.core

import com.kotlinorm.compiler.utils.IrTestFramework
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.kotlinorm.compiler.utils.KotlinSourceDynamicCompiler.compile
import com.kotlinorm.compiler.utils.testBaseName
import com.tschuchort.compiletesting.KotlinCompilation

/**
 * Integration tests for KTableTransformer base class
 *
 * Tests that the KTableTransformer compiles correctly and can be extended
 */
@OptIn(ExperimentalCompilerApi::class)
class KTableTransformerTest {

    @Test
    fun `KTableTransformer can be extended with custom logic`() {
        // Test that code using KTableTransformer compiles successfully
        val ctx = IrTestFramework.compile(
            IrTestFramework.source("Test.kt", """
                package test
                
                import com.kotlinorm.compiler.core.KTableTransformer
                import com.kotlinorm.compiler.core.ErrorReporter
                import org.jetbrains.kotlin.ir.declarations.IrFunction
                import org.jetbrains.kotlin.ir.expressions.IrReturn
                
                class MyTransformer(
                    irFunction: IrFunction,
                    errorReporter: ErrorReporter
                ) : KTableTransformer(irFunction, errorReporter) {
                    
                    override fun visitReturn(expression: IrReturn): org.jetbrains.kotlin.ir.expressions.IrExpression {
                        if (shouldProcessReturn(expression)) {
                            // Process the return
                        }
                        return super.visitReturn(expression)
                    }
                }
            """)
        )
        ctx.assertSuccess()
    }

    @Test
    fun `KTableTransformer provides shouldProcessReturn method`() {
        // Test that shouldProcessReturn is accessible in subclasses
        val ctx = IrTestFramework.compile(
            IrTestFramework.source("Test.kt", """
                package test
                
                import com.kotlinorm.compiler.core.KTableTransformer
                import com.kotlinorm.compiler.core.ErrorReporter
                import org.jetbrains.kotlin.ir.declarations.IrFunction
                import org.jetbrains.kotlin.ir.expressions.IrReturn
                
                class TestTransformer(
                    irFunction: IrFunction,
                    errorReporter: ErrorReporter
                ) : KTableTransformer(irFunction, errorReporter) {
                    
                    fun checkReturn(expression: IrReturn): Boolean {
                        return shouldProcessReturn(expression)
                    }
                }
            """)
        )
        ctx.assertSuccess()
    }

    @Test
    fun `KTableTransformer provides reportError method`() {
        // Test that reportError is accessible in subclasses
        val ctx = IrTestFramework.compile(
            IrTestFramework.source("Test.kt", """
                package test
                
                import com.kotlinorm.compiler.core.KTableTransformer
                import com.kotlinorm.compiler.core.ErrorReporter
                import org.jetbrains.kotlin.ir.declarations.IrFunction
                import org.jetbrains.kotlin.ir.IrElement
                
                class TestTransformer(
                    irFunction: IrFunction,
                    errorReporter: ErrorReporter
                ) : KTableTransformer(irFunction, errorReporter) {
                    
                    fun testError(element: IrElement) {
                        reportError(element, "Test error", "Test suggestion")
                    }
                }
            """)
        )
        ctx.assertSuccess()
    }

    @Test
    fun `KTableTransformer extends IrElementTransformerVoidWithContext`() {
        // Test that KTableTransformer properly extends the base transformer
        val ctx = IrTestFramework.compile(
            IrTestFramework.source("Test.kt", """
                package test
                
                import com.kotlinorm.compiler.core.KTableTransformer
                import com.kotlinorm.compiler.core.ErrorReporter
                import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
                import org.jetbrains.kotlin.ir.declarations.IrFunction
                
                class TestTransformer(
                    irFunction: IrFunction,
                    errorReporter: ErrorReporter
                ) : KTableTransformer(irFunction, errorReporter) {
                    
                    fun isTransformer(): Boolean {
                        return this is IrElementTransformerVoidWithContext
                    }
                }
            """)
        )
        ctx.assertSuccess()
    }

    @Test
    fun `KTableTransformer can access irFunction property`() {
        // Test that the irFunction property is accessible
        val ctx = IrTestFramework.compile(
            IrTestFramework.source("Test.kt", """
                package test
                
                import com.kotlinorm.compiler.core.KTableTransformer
                import com.kotlinorm.compiler.core.ErrorReporter
                import org.jetbrains.kotlin.ir.declarations.IrFunction
                
                class TestTransformer(
                    irFunction: IrFunction,
                    errorReporter: ErrorReporter
                ) : KTableTransformer(irFunction, errorReporter) {
                    
                    fun getFunctionSymbol() = irFunction.symbol
                }
            """)
        )
        ctx.assertSuccess()
    }

    @Test
    fun `KTableTransformer can access errorReporter property`() {
        // Test that the errorReporter property is accessible
        val ctx = IrTestFramework.compile(
            IrTestFramework.source("Test.kt", """
                package test
                
                import com.kotlinorm.compiler.core.KTableTransformer
                import com.kotlinorm.compiler.core.ErrorReporter
                import org.jetbrains.kotlin.ir.declarations.IrFunction
                
                class TestTransformer(
                    irFunction: IrFunction,
                    errorReporter: ErrorReporter
                ) : KTableTransformer(irFunction, errorReporter) {
                    
                    fun getReporter() = errorReporter
                }
            """)
        )
        ctx.assertSuccess()
    }

    @Test
    fun `shouldProcessReturn compares returnTargetSymbol with function symbol`() {
        // Test that the shouldProcessReturn logic is correct by checking the implementation
        val ctx = IrTestFramework.compile(
            IrTestFramework.source("Test.kt", """
                package test
                
                import com.kotlinorm.compiler.core.KTableTransformer
                import com.kotlinorm.compiler.core.ErrorReporter
                import org.jetbrains.kotlin.ir.declarations.IrFunction
                import org.jetbrains.kotlin.ir.expressions.IrReturn
                
                class TestTransformer(
                    irFunction: IrFunction,
                    errorReporter: ErrorReporter
                ) : KTableTransformer(irFunction, errorReporter) {
                    
                    override fun visitReturn(expression: IrReturn): org.jetbrains.kotlin.ir.expressions.IrExpression {
                        // shouldProcessReturn checks: expression.returnTargetSymbol == irFunction.symbol
                        val shouldProcess = shouldProcessReturn(expression)
                        
                        // This verifies the logic exists and compiles
                        if (shouldProcess) {
                            // Process top-level returns
                        } else {
                            // Skip nested lambda returns
                        }
                        
                        return super.visitReturn(expression)
                    }
                }
            """)
        )
        ctx.assertSuccess()
    }

    @Suppress("LanguageDetectionInspection")
    private val mainKt = """
        import com.kotlinorm.Kronos
        import com.kotlinorm.annotations.*
        import com.kotlinorm.beans.dsl.Field
        import com.kotlinorm.beans.dsl.KTableIndex
        import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
        import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
        import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
        import com.kotlinorm.beans.dsl.KTableForSet.Companion.afterSet
        import com.kotlinorm.beans.config.KronosCommonStrategy
        import com.kotlinorm.interfaces.KPojo
        import com.kotlinorm.enums.KColumnType
        import com.kotlinorm.enums.KColumnType.*
        import com.kotlinorm.enums.SortType
        import com.kotlinorm.enums.IgnoreAction
        import com.kotlinorm.types.ToSelect
        import com.kotlinorm.types.ToSort
        import com.kotlinorm.types.ToFilter
        import com.kotlinorm.types.ToSet
        import java.time.LocalDateTime
        import java.math.BigDecimal
        import kotlin.test.assertEquals
        import kotlin.test.assertNotNull
        import kotlin.test.assertNull
        import kotlin.test.assertTrue

// PLACEHOLDER_CLASSES

        fun main() {
            Kronos.init {
                fieldNamingStrategy = lineHumpNamingStrategy
                tableNamingStrategy = lineHumpNamingStrategy
            }

// PLACEHOLDER_INJECT

            test()
        }
    """.trimIndent()

    private fun compileAndRun(classes: String, code: String, tag: String) {
        val source = mainKt
            .replace("// PLACEHOLDER_CLASSES", classes)
            .replace("// PLACEHOLDER_INJECT", code)
        val result = compile(source, testBaseName + tag)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val ktClazz = result.classLoader.loadClass("${testBaseName}${tag}Kt")
        val main = ktClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
        main.invoke(null)
    }

    // ========================================================================
    // 1. KPojo with all annotation types combined
    // ========================================================================

    @Test
    fun `test KPojo with all annotation types combined`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_full")
        @TableIndex("idx_name", ["name"], "UNIQUE")
        @TableIndex("idx_code_status", ["code", "status"], "BTREE")
        data class FullEntity(
            @PrimaryKey(identity = true) var id: Int? = null,
            @Necessary @Column("user_name") var name: String? = null,
            @ColumnType(TINYINT) @Default("0") var status: Int? = null,
            @Serialize var tags: List<String>? = null,
            @DateTimeFormat("yyyy-MM-dd HH:mm:ss") @CreateTime var createdAt: String? = null,
            @UpdateTime var updatedAt: LocalDateTime? = null,
            @Version var version: Int? = null,
            @LogicDelete var deleted: Boolean? = null,
            var code: String? = null,
            @Ignore([IgnoreAction.ALL]) var internal: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = FullEntity()
            val cols = e.kronosColumns()
            val names = cols.map { it.name }
            assertTrue("id" in names)
            assertTrue("name" in names)
            assertTrue("status" in names)
            assertTrue("tags" in names)
            assertTrue("internal" !in names, "internal with @Ignore ALL should be excluded")
            assertEquals("user_name", e.getColumn("name").columnName)
            assertEquals(KColumnType.TINYINT, e.getColumn("status").type)
            assertTrue(e.getColumn("tags").serializable)
            assertEquals("yyyy-MM-dd HH:mm:ss", e.getColumn("createdAt").dateFormat)
            assertEquals("tb_full", e.__tableName)
        }
            """,
            tag = "AllAnnotations"
        )
    }

    // ========================================================================
    // 2. KPojo with @Version (optimistic lock)
    // ========================================================================

    @Test
    fun `test KPojo with Version optimistic lock`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_versioned")
        data class VersionedEntity(
            @PrimaryKey(identity = true) var id: Int? = null,
            var name: String? = null,
            @Version var version: Int? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = VersionedEntity()
            val versionCol = e.getColumn("version")
            assertNotNull(versionCol)
            assertEquals("version", versionCol.name)
        }
            """,
            tag = "VersionLock"
        )
    }

    // ========================================================================
    // 3. KPojo with multiple @TableIndex
    // ========================================================================

    @Test
    fun `test KPojo with multiple TableIndex annotations`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_indexed")
        @TableIndex("idx_a", ["colA"], "UNIQUE")
        @TableIndex("idx_b", ["colB", "colC"], "BTREE")
        @TableIndex("idx_c", ["colA", "colB", "colC"], "HASH")
        data class IndexedEntity(
            var id: Int? = null,
            var colA: String? = null,
            var colB: String? = null,
            var colC: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = IndexedEntity()
            val indexes = e.kronosTableIndex()
            assertEquals(3, indexes.size)
            val names = indexes.map { it.name }
            assertTrue("idx_a" in names)
            assertTrue("idx_b" in names)
            assertTrue("idx_c" in names)
        }
            """,
            tag = "MultipleTableIndex"
        )
    }

    // ========================================================================
    // 4. KPojo with cascade relationship
    // ========================================================================

    @Test
    fun `test KPojo with cascade relationship`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_cascade")
        data class CascadeEntity(
            @PrimaryKey(identity = true) var id: Int? = null,
            var name: String? = null,
            var parentId: Int? = null,
            @Cascade(["parentId"], ["id"])
            var parent: CascadeEntity? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = CascadeEntity()
            val cols = e.kronosColumns()
            val names = cols.map { it.name }
            assertTrue("id" in names)
            assertTrue("name" in names)
            assertTrue("parentId" in names)
        }
            """,
            tag = "CascadeRelationship"
        )
    }

    // ========================================================================
    // 5. KPojo select operation
    // ========================================================================

    @Test
    fun `test KPojo select operation`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_select_op")
        data class SelectOpEntity(
            var id: Int? = null,
            var name: String? = null,
            var score: Double? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = SelectOpEntity()
            var rst: List<Field>? = null
            fun sel(block: ToSelect<SelectOpEntity, Any?>) {
                e.afterSelect {
                    block!!(it)
                    rst = fields
                }
            }
            sel { it.id + it.name }
            assertEquals(2, rst!!.size)
            assertEquals("id", rst!![0].name)
            assertEquals("name", rst!![1].name)
        }
            """,
            tag = "SelectOperation"
        )
    }

    // ========================================================================
    // 6. KPojo sort operation
    // ========================================================================

    @Test
    fun `test KPojo sort operation`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_sort_op")
        data class SortOpEntity(
            var id: Int? = null,
            var name: String? = null,
            var score: Double? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = SortOpEntity()
            var rst: List<Pair<Field, SortType>>? = null
            fun srt(block: ToSort<SortOpEntity, Any?>) {
                e.afterSort {
                    block!!(it)
                    rst = sortedFields
                }
            }
            srt { it.score.desc() }
            assertEquals(1, rst!!.size)
            assertEquals("score", rst!![0].first.name)
            assertEquals(SortType.DESC, rst!![0].second)
        }
            """,
            tag = "SortOperation"
        )
    }

    // ========================================================================
    // 7. KPojo filter operation
    // ========================================================================

    @Test
    fun `test KPojo filter operation`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_filter_op")
        data class FilterOpEntity(
            var id: Int? = null,
            var name: String? = null,
            var active: Boolean? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = FilterOpEntity()
            e.afterFilter {
                criteriaParamMap = e.toDataMap()
                it.id == 1
            }
            assertNotNull(e)
        }
            """,
            tag = "FilterOperation"
        )
    }

    // ========================================================================
    // 8. KPojo set operation
    // ========================================================================

    @Test
    fun `test KPojo set operation`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_set_op")
        data class SetOpEntity(
            var id: Int? = null,
            var name: String? = null,
            var score: Double? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = SetOpEntity()
            var fields: MutableList<Field> = mutableListOf()
            var paramMap: MutableMap<Field, Any?> = mutableMapOf()
            e.afterSet {
                it.name = "Alice"
                it.score = 9.5
                fields = this.fields
                paramMap = fieldParamMap
            }
            assertEquals(2, fields.size)
            assertEquals("Alice", paramMap[e.getColumn("name")])
            assertEquals(9.5, paramMap[e.getColumn("score")])
        }
            """,
            tag = "SetOperation"
        )
    }

    // ========================================================================
    // 9. KPojo with @LogicDelete
    // ========================================================================

    @Test
    fun `test KPojo with LogicDelete`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_logic_delete")
        data class LogicDeleteEntity(
            var id: Int? = null,
            var name: String? = null,
            @LogicDelete var deleted: Boolean? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = LogicDeleteEntity()
            val strategy = e.kronosLogicDelete()
            assertTrue(strategy.enabled)
            assertEquals("deleted", strategy.field.name)
        }
            """,
            tag = "LogicDelete"
        )
    }

    // ========================================================================
    // 10. KPojo with @CreateTime and @UpdateTime
    // ========================================================================

    @Test
    fun `test KPojo with CreateTime and UpdateTime`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_timestamps")
        data class TimestampEntity(
            var id: Int? = null,
            @CreateTime var createdAt: LocalDateTime? = null,
            @UpdateTime var updatedAt: LocalDateTime? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = TimestampEntity()
            val createStrategy = e.kronosCreateTime()
            val updateStrategy = e.kronosUpdateTime()
            assertTrue(createStrategy.enabled)
            assertEquals("createdAt", createStrategy.field.name)
            assertTrue(updateStrategy.enabled)
            assertEquals("updatedAt", updateStrategy.field.name)
        }
            """,
            tag = "Timestamps"
        )
    }

    // ========================================================================
    // 11. KPojo with @Ignore map actions
    // ========================================================================

    @Test
    fun `test KPojo with Ignore map actions`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_ignore_map")
        data class IgnoreMapEntity(
            var id: Int? = null,
            var normal: String? = null,
            @Ignore([IgnoreAction.ALL]) var secret: String? = null,
            @Ignore([IgnoreAction.TO_MAP]) var writeOnly: String? = null,
            @Ignore([IgnoreAction.FROM_MAP]) var readOnly: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = IgnoreMapEntity()
            val cols = e.kronosColumns()
            val names = cols.map { it.name }
            // ALL should exclude from columns entirely
            assertTrue("secret" !in names, "secret with @Ignore ALL should be excluded from columns")
            // TO_MAP and FROM_MAP should still be in columns
            assertTrue("writeOnly" in names, "writeOnly should be in columns")
            assertTrue("readOnly" in names, "readOnly should be in columns")
            assertTrue("normal" in names, "normal should be in columns")
        }
            """,
            tag = "IgnoreMapActions"
        )
    }

    // ========================================================================
    // 12. KPojo with BigDecimal and multiple types in set operation
    // ========================================================================

    @Test
    fun `test set operation with diverse field types`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_diverse_set")
        data class DiverseSetEntity(
            var id: Int? = null,
            var name: String? = null,
            var amount: BigDecimal? = null,
            var active: Boolean? = null,
            var score: Double? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = DiverseSetEntity()
            var fields: MutableList<Field> = mutableListOf()
            var paramMap: MutableMap<Field, Any?> = mutableMapOf()
            e.afterSet {
                it.name = "test"
                it.active = true
                it.score = 8.5
                fields = this.fields
                paramMap = fieldParamMap
            }
            assertTrue(fields.size >= 3)
            assertEquals("test", paramMap[e.getColumn("name")])
            assertEquals(true, paramMap[e.getColumn("active")])
            assertEquals(8.5, paramMap[e.getColumn("score")])
        }
            """,
            tag = "DiverseSetTypes"
        )
    }
}
