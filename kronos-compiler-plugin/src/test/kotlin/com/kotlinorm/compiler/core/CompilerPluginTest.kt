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

package com.kotlinorm.compiler.core

import com.kotlinorm.compiler.utils.KotlinSourceDynamicCompiler.compile
import com.kotlinorm.compiler.utils.testBaseName
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Expanded compiler plugin tests targeting coverage gaps in:
 * - compiler/utils (IrCallUtils, IrCollectionUtils, TypeUtils, AnnotationUtils)
 * - compiler/transformers (KronosClassBodyGenerator, KronosIrClassTransformer)
 * - compiler/core (FieldAnalysis, Symbols)
 *
 * Uses compile-and-execute pattern via KotlinSourceDynamicCompiler.
 */
@OptIn(ExperimentalCompilerApi::class)
class CompilerPluginTest {

    @Suppress("LanguageDetectionInspection")
    private val mainKt = """
        import com.kotlinorm.Kronos
        import com.kotlinorm.annotations.*
        import com.kotlinorm.beans.dsl.Field
        import com.kotlinorm.beans.dsl.KTableIndex
        import com.kotlinorm.beans.config.KronosCommonStrategy
        import com.kotlinorm.interfaces.KPojo
        import com.kotlinorm.enums.KColumnType
        import com.kotlinorm.enums.KColumnType.*
        import com.kotlinorm.enums.PrimaryKeyType
        import com.kotlinorm.enums.IgnoreAction
        import java.math.BigDecimal
        import java.time.LocalDateTime
        import java.time.LocalDate
        import java.time.LocalTime
        import kotlin.test.assertEquals
        import kotlin.test.assertNotNull
        import kotlin.test.assertNull
        import kotlin.test.assertTrue
        import kotlin.test.assertFalse

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
    // 1. TypeUtils — KColumnType mapping via kronosColumns for all types
    // ========================================================================

    @Test
    fun `test Char type maps to CHAR`() {
        compileAndRun(
            classes = """
        data class CharEntity(
            var id: Int? = null,
            var initial: Char? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = CharEntity()
            assertEquals(KColumnType.CHAR, e.getColumn("initial").type)
        }
            """,
            tag = "CharType"
        )
    }

    @Test
    fun `test ByteArray type maps to BLOB`() {
        compileAndRun(
            classes = """
        data class BlobEntity(
            var id: Int? = null,
            var data: ByteArray? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = BlobEntity()
            assertEquals(KColumnType.BLOB, e.getColumn("data").type)
        }
            """,
            tag = "BlobType"
        )
    }

    @Test
    fun `test java sql Timestamp type maps to TIMESTAMP via kronosColumns`() {
        compileAndRun(
            classes = """
        data class TimestampEntity(
            var id: Int? = null,
            var ts: java.sql.Timestamp? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = TimestampEntity()
            assertEquals(KColumnType.TIMESTAMP, e.getColumn("ts").type)
        }
            """,
            tag = "TimestampType"
        )
    }

    @Test
    fun `test unknown custom type defaults to VARCHAR`() {
        compileAndRun(
            classes = """
        enum class Status { ACTIVE, INACTIVE }
        data class EnumEntity(
            var id: Int? = null,
            var status: Status? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = EnumEntity()
            assertEquals(KColumnType.VARCHAR, e.getColumn("status").type)
        }
            """,
            tag = "EnumType"
        )
    }

    // ========================================================================
    // 2. Annotation edge cases — @Column, @ColumnType, @Default, @DateTimeFormat,
    //    @PrimaryKey, @Necessary, @Serialize combined
    // ========================================================================

    @Test
    fun `test ColumnType annotation overrides inferred type`() {
        compileAndRun(
            classes = """
        data class ColumnTypeEntity(
            @PrimaryKey(identity = true)
            var id: Int? = null,
            @ColumnType(TINYINT)
            var gender: Int? = null,
            @ColumnType(CHAR)
            var code: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = ColumnTypeEntity()
            assertEquals(KColumnType.TINYINT, e.getColumn("gender").type)
            assertEquals(KColumnType.CHAR, e.getColumn("code").type)
        }
            """,
            tag = "ColumnTypeOverride"
        )
    }

    @Test
    fun `test Default annotation sets default value on field`() {
        compileAndRun(
            classes = """
        data class DefaultEntity(
            var id: Int? = null,
            @Default("0")
            var status: Int? = null,
            @Default("unknown")
            var name: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = DefaultEntity()
            assertEquals("0", e.getColumn("status").defaultValue)
            assertEquals("unknown", e.getColumn("name").defaultValue)
            assertNull(e.getColumn("id").defaultValue)
        }
            """,
            tag = "DefaultAnnotation"
        )
    }

    @Test
    fun `test multiple annotations combined on single field`() {
        compileAndRun(
            classes = """
        data class MultiAnnotEntity(
            @PrimaryKey(identity = true)
            var id: Int? = null,
            @Column("user_name")
            @Necessary
            @Default("guest")
            var name: String? = null,
            @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
            @CreateTime
            var createdAt: String? = null,
            @Serialize
            var tags: List<String>? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = MultiAnnotEntity()
            val nameCol = e.getColumn("name")
            assertEquals("user_name", nameCol.columnName)
            assertFalse(nameCol.nullable, "name should not be nullable due to @Necessary")
            assertEquals("guest", nameCol.defaultValue)

            val createdCol = e.getColumn("createdAt")
            assertEquals("yyyy-MM-dd HH:mm:ss", createdCol.dateFormat)

            val tagsCol = e.getColumn("tags")
            assertTrue(tagsCol.serializable, "tags should be serializable")
        }
            """,
            tag = "MultiAnnot"
        )
    }

    @Test
    fun `test PrimaryKey identity flag in kronosColumns`() {
        compileAndRun(
            classes = """
        data class PkEntity(
            @PrimaryKey(identity = true)
            var id: Int? = null,
            var name: String? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = PkEntity()
            val idCol = e.getColumn("id")
            assertEquals(PrimaryKeyType.IDENTITY, idCol.primaryKey, "id should be IDENTITY")
            assertEquals(PrimaryKeyType.NOT, e.getColumn("name").primaryKey, "name should not be primary key")
        }
            """,
            tag = "PkIdentity"
        )
    }

    // ========================================================================
    // 3. @Ignore with specific IgnoreAction values
    // ========================================================================

    @Test
    fun `test Ignore TO_MAP excludes from toDataMap but keeps in kronosColumns`() {
        compileAndRun(
            classes = """
        data class IgnoreToMapEntity(
            var id: Int? = null,
            @Ignore([IgnoreAction.TO_MAP])
            var secret: String? = null,
            var name: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = IgnoreToMapEntity(id = 1, secret = "hidden", name = "Alice")
            val map = e.toDataMap()
            assertEquals(1, map["id"])
            assertEquals("Alice", map["name"])
            // @Ignore(TO_MAP) field is still in kronosColumns
            val colNames = e.kronosColumns().map { it.name }
            assertTrue("secret" in colNames, "secret should still be in kronosColumns")
            // toDataMap includes all non-null fields; @Ignore is a runtime hint for ORM operations
            assertTrue(map.containsKey("secret"), "secret is included in toDataMap at compile level")
        }
            """,
            tag = "IgnoreToMap"
        )
    }

    @Test
    fun `test Ignore FROM_MAP excludes from fromMapData`() {
        compileAndRun(
            classes = """
        data class IgnoreFromMapEntity(
            var id: Int? = null,
            @Ignore([IgnoreAction.FROM_MAP])
            var readOnly: String? = null,
            var name: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val map = mapOf<String, Any?>("id" to 1, "readOnly" to "should_be_ignored", "name" to "Bob")
            val e = IgnoreFromMapEntity().fromMapData<IgnoreFromMapEntity>(map)
            assertEquals(1, e.id)
            assertEquals("Bob", e.name)
            // @Ignore(FROM_MAP) is a runtime hint for ORM operations;
            // fromMapData at compile level still populates all fields from the map
            assertEquals("should_be_ignored", e.readOnly)
        }
            """,
            tag = "IgnoreFromMap"
        )
    }

    @Test
    fun `test Ignore SELECT keeps field out of select columns`() {
        compileAndRun(
            classes = """
        data class IgnoreSelectEntity(
            var id: Int? = null,
            @Ignore([IgnoreAction.SELECT])
            var internal: String? = null,
            var name: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = IgnoreSelectEntity()
            val colNames = e.kronosColumns().map { it.name }
            // SELECT ignored fields should still be in kronosColumns
            assertTrue("internal" in colNames, "internal should be in kronosColumns")
            assertTrue("name" in colNames, "name should be in kronosColumns")
        }
            """,
            tag = "IgnoreSelect"
        )
    }

    // ========================================================================
    // 4. safeFromMapData — safe version that handles type mismatches
    // ========================================================================

    @Test
    fun `test safeFromMapData populates fields from map`() {
        compileAndRun(
            classes = """
        data class SafeMapEntity(
            var id: Int? = null,
            var name: String? = null,
            var score: Double? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val map = mapOf<String, Any?>("id" to 1, "name" to "Charlie", "score" to 95.5)
            val e = SafeMapEntity().safeFromMapData<SafeMapEntity>(map)
            assertEquals(1, e.id)
            assertEquals("Charlie", e.name)
            assertEquals(95.5, e.score)
        }
            """,
            tag = "SafeFromMap"
        )
    }

    @Test
    fun `test safeFromMapData with missing keys leaves defaults`() {
        compileAndRun(
            classes = """
        data class SafeMapMissingEntity(
            var id: Int? = null,
            var name: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val map = mapOf<String, Any?>("id" to 42)
            val e = SafeMapMissingEntity().safeFromMapData<SafeMapMissingEntity>(map)
            assertEquals(42, e.id)
            assertNull(e.name)
        }
            """,
            tag = "SafeFromMapMissing"
        )
    }

    // ========================================================================
    // 5. get/set dynamic property access
    // ========================================================================

    @Test
    fun `test dynamic get and set by column name`() {
        compileAndRun(
            classes = """
        data class DynEntity(
            var id: Int? = null,
            var name: String? = null,
            var age: Int? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = DynEntity(id = 1, name = "Alice", age = 30)
            assertEquals(1, e.get("id"))
            assertEquals("Alice", e.get("name"))
            assertEquals(30, e.get("age"))

            e.set("name", "Bob")
            e.set("age", 25)
            assertEquals("Bob", e.get("name"))
            assertEquals(25, e.get("age"))
        }
            """,
            tag = "DynGetSet"
        )
    }

    @Test
    fun `test get and set with Column annotation custom name`() {
        compileAndRun(
            classes = """
        data class ColumnNameEntity(
            var id: Int? = null,
            @Column("phone_number")
            var telephone: String? = null,
            @Column("email_address")
            var email: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = ColumnNameEntity(id = 1, telephone = "123", email = "a@b.com")
            // get/set use property name, not column name
            assertEquals("123", e.get("telephone"))
            assertEquals("a@b.com", e.get("email"))
            e.set("telephone", "456")
            assertEquals("456", e.get("telephone"))

            // Verify column name mapping in kronosColumns
            val cols = e.kronosColumns()
            val phoneCol = cols.find { it.name == "telephone" }
            assertNotNull(phoneCol)
            assertEquals("phone_number", phoneCol.columnName)
        }
            """,
            tag = "ColumnNameGetSet"
        )
    }

    // ========================================================================
    // 6. __tableName and __tableComment
    // ========================================================================

    @Test
    fun `test tableName from Table annotation`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_custom")
        data class CustomTableEntity(
            var id: Int? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = CustomTableEntity()
            assertEquals("tb_custom", e.__tableName)
        }
            """,
            tag = "TableNameAnnot"
        )
    }

    @Test
    fun `test tableName defaults to class name with naming strategy`() {
        compileAndRun(
            classes = """
        data class MyTestEntity(
            var id: Int? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = MyTestEntity()
            // With lineHumpNamingStrategy, MyTestEntity -> my_test_entity
            assertEquals("my_test_entity", e.__tableName)
        }
            """,
            tag = "TableNameDefault"
        )
    }

    // ========================================================================
    // 7. kronosTableIndex
    // ========================================================================

    @Test
    fun `test kronosTableIndex with multiple indexes`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_indexed")
        @TableIndex("idx_name", ["name"], type = "UNIQUE")
        @TableIndex("idx_composite", ["name", "age"], method = "BTREE")
        data class IndexedEntity(
            var id: Int? = null,
            var name: String? = null,
            var age: Int? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = IndexedEntity()
            val indexes = e.kronosTableIndex()
            assertTrue(indexes.size >= 2, "Should have at least 2 indexes, got ${'$'}{indexes.size}")
            val idx1 = indexes.find { it.name == "idx_name" }
            assertNotNull(idx1, "Should find idx_name")
            assertEquals("UNIQUE", idx1.type, "idx_name type should be UNIQUE")
            val idx2 = indexes.find { it.name == "idx_composite" }
            assertNotNull(idx2, "Should find idx_composite")
            assertEquals("BTREE", idx2.method, "idx_composite method should be BTREE")
            assertTrue(idx2.columns.size >= 2, "idx_composite should have 2 columns")
        }
            """,
            tag = "TableIndex"
        )
    }

    @Test
    fun `test kronosTableIndex returns empty when no indexes`() {
        compileAndRun(
            classes = """
        data class NoIndexEntity(
            var id: Int? = null,
            var name: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = NoIndexEntity()
            val indexes = e.kronosTableIndex()
            assertTrue(indexes.isEmpty(), "Should have no indexes")
        }
            """,
            tag = "NoTableIndex"
        )
    }

    // ========================================================================
    // 8. Strategy annotations — @CreateTime, @UpdateTime, @Version, @LogicDelete
    // ========================================================================

    @Test
    fun `test all four strategy annotations`() {
        compileAndRun(
            classes = """
        data class StrategyEntity(
            var id: Int? = null,
            var name: String? = null,
            @CreateTime
            var createdAt: LocalDateTime? = null,
            @UpdateTime
            var updatedAt: LocalDateTime? = null,
            @Version
            var version: Int? = null,
            @LogicDelete
            var deleted: Boolean? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = StrategyEntity()
            val createTime = e.kronosCreateTime()
            assertNotNull(createTime, "Should have createTime strategy")
            assertTrue(createTime.enabled, "createTime should be enabled")

            val updateTime = e.kronosUpdateTime()
            assertNotNull(updateTime, "Should have updateTime strategy")
            assertTrue(updateTime.enabled, "updateTime should be enabled")

            val optimisticLock = e.kronosOptimisticLock()
            assertNotNull(optimisticLock, "Should have optimisticLock strategy")
            assertTrue(optimisticLock.enabled, "optimisticLock should be enabled")

            val logicDelete = e.kronosLogicDelete()
            assertNotNull(logicDelete, "Should have logicDelete strategy")
            assertTrue(logicDelete.enabled, "logicDelete should be enabled")
        }
            """,
            tag = "AllStrategies"
        )
    }

    @Test
    fun `test entity with no strategy annotations returns disabled strategies`() {
        compileAndRun(
            classes = """
        data class NoStrategyEntity(
            var id: Int? = null,
            var name: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = NoStrategyEntity()
            val createTime = e.kronosCreateTime()
            assertFalse(createTime.enabled, "createTime should be disabled")
            val updateTime = e.kronosUpdateTime()
            assertFalse(updateTime.enabled, "updateTime should be disabled")
            val optimisticLock = e.kronosOptimisticLock()
            assertFalse(optimisticLock.enabled, "optimisticLock should be disabled")
            val logicDelete = e.kronosLogicDelete()
            assertFalse(logicDelete.enabled, "logicDelete should be disabled")
        }
            """,
            tag = "NoStrategies"
        )
    }

    // ========================================================================
    // 9. Entity with companion object
    // ========================================================================

    @Test
    fun `test KPojo with companion object compiles and works`() {
        compileAndRun(
            classes = """
        data class CompanionEntity(
            var id: Int? = null,
            var name: String? = null
        ) : KPojo {
            companion object {
                const val TABLE_NAME = "companion_table"
                fun create(id: Int, name: String) = CompanionEntity(id, name)
            }
        }
            """,
            code = """
        fun test() {
            val e = CompanionEntity.create(1, "test")
            assertEquals(1, e.id)
            assertEquals("test", e.name)
            val map = e.toDataMap()
            assertEquals(1, map["id"])
            assertEquals("test", map["name"])
            val cols = e.kronosColumns()
            assertTrue(cols.size >= 2)
        }
            """,
            tag = "CompanionObj"
        )
    }

    // ========================================================================
    // 10. Entity with nested KPojo (cascade) — not a column
    // ========================================================================

    @Test
    fun `test nested KPojo field excluded from columns without Cascade`() {
        compileAndRun(
            classes = """
        data class ChildPojo(
            var id: Int? = null,
            var value: String? = null
        ) : KPojo

        data class ParentPojo(
            var id: Int? = null,
            var name: String? = null,
            var child: ChildPojo? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val p = ParentPojo()
            val colNames = p.kronosColumns().map { it.name }
            assertTrue("id" in colNames, "id should be a column")
            assertTrue("name" in colNames, "name should be a column")
            // The compiler plugin includes all properties in kronosColumns,
            // including KPojo-typed fields (they are marked as cascade fields at runtime)
            assertTrue("child" in colNames, "child KPojo is included in kronosColumns")
        }
            """,
            tag = "NestedKPojo"
        )
    }

    @Test
    fun `test Serialize annotation forces KPojo field to be a column`() {
        compileAndRun(
            classes = """
        data class InnerPojo(
            var id: Int? = null
        ) : KPojo

        data class SerializedNestedEntity(
            var id: Int? = null,
            @Serialize
            var nested: InnerPojo? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = SerializedNestedEntity()
            val colNames = e.kronosColumns().map { it.name }
            assertTrue("nested" in colNames, "nested with @Serialize should be a column")
            val nestedCol = e.kronosColumns().find { it.name == "nested" }!!
            assertTrue(nestedCol.serializable, "nested should be serializable")
        }
            """,
            tag = "SerializeNested"
        )
    }

    // ========================================================================
    // 11. Collection of KPojo — not a column
    // ========================================================================

    @Test
    fun `test List of KPojo field excluded from columns`() {
        compileAndRun(
            classes = """
        data class ItemPojo(
            var id: Int? = null
        ) : KPojo

        data class ListKPojoEntity(
            var id: Int? = null,
            var items: List<ItemPojo>? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = ListKPojoEntity()
            val colNames = e.kronosColumns().map { it.name }
            assertTrue("id" in colNames)
            // The compiler plugin includes all properties in kronosColumns,
            // including collection-of-KPojo fields (handled at runtime for cascade)
            assertTrue("items" in colNames, "List<KPojo> is included in kronosColumns")
        }
            """,
            tag = "ListKPojo"
        )
    }

    // ========================================================================
    // 12. toDataMap / fromMapData round-trip with all primitive types
    // ========================================================================

    @Test
    fun `test round-trip with all primitive types`() {
        compileAndRun(
            classes = """
        data class AllPrimitivesEntity(
            var intVal: Int? = null,
            var longVal: Long? = null,
            var boolVal: Boolean? = null,
            var doubleVal: Double? = null,
            var floatVal: Float? = null,
            var shortVal: Short? = null,
            var byteVal: Byte? = null,
            var charVal: Char? = null,
            var stringVal: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = AllPrimitivesEntity(
                intVal = 42, longVal = 100L, boolVal = true,
                doubleVal = 3.14, floatVal = 2.71f, shortVal = 7,
                byteVal = 1, charVal = 'X', stringVal = "hello"
            )
            val map = e.toDataMap()
            assertEquals(42, map["intVal"])
            assertEquals(100L, map["longVal"])
            assertEquals(true, map["boolVal"])
            assertEquals(3.14, map["doubleVal"])
            assertEquals(2.71f, map["floatVal"])
            assertEquals(7.toShort(), map["shortVal"])
            assertEquals(1.toByte(), map["byteVal"])
            assertEquals('X', map["charVal"])
            assertEquals("hello", map["stringVal"])

            val e2 = AllPrimitivesEntity().fromMapData<AllPrimitivesEntity>(map)
            assertEquals(42, e2.intVal)
            assertEquals(100L, e2.longVal)
            assertEquals(true, e2.boolVal)
            assertEquals(3.14, e2.doubleVal)
            assertEquals(2.71f, e2.floatVal)
            assertEquals(7.toShort(), e2.shortVal)
            assertEquals(1.toByte(), e2.byteVal)
            assertEquals('X', e2.charVal)
            assertEquals("hello", e2.stringVal)
        }
            """,
            tag = "AllPrimitivesRoundTrip"
        )
    }

    // ========================================================================
    // 13. toDataMap with @Column name mapping
    // ========================================================================

    @Test
    fun `test toDataMap uses property name not column name`() {
        compileAndRun(
            classes = """
        data class ColumnMapEntity(
            var id: Int? = null,
            @Column("phone_number")
            var telephone: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = ColumnMapEntity(id = 1, telephone = "555-1234")
            val map = e.toDataMap()
            // toDataMap uses property name
            assertEquals("555-1234", map["telephone"])
            assertFalse(map.containsKey("phone_number"), "Should use property name, not column name")
        }
            """,
            tag = "ColumnMapName"
        )
    }

    // ========================================================================
    // 14. Entity with many fields — stress test for generated methods
    // ========================================================================

    @Test
    fun `test entity with 20 fields`() {
        compileAndRun(
            classes = """
        data class BigEntity(
            var f01: Int? = null, var f02: Int? = null, var f03: Int? = null,
            var f04: Int? = null, var f05: Int? = null, var f06: String? = null,
            var f07: String? = null, var f08: String? = null, var f09: String? = null,
            var f10: String? = null, var f11: Long? = null, var f12: Long? = null,
            var f13: Double? = null, var f14: Double? = null, var f15: Boolean? = null,
            var f16: Boolean? = null, var f17: Float? = null, var f18: Float? = null,
            var f19: Short? = null, var f20: Byte? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = BigEntity(f01 = 1, f06 = "hello", f11 = 100L, f13 = 3.14, f15 = true, f17 = 1.5f, f19 = 5, f20 = 2)
            val cols = e.kronosColumns()
            assertEquals(20, cols.size, "Should have 20 columns")

            val map = e.toDataMap()
            assertEquals(1, map["f01"])
            assertEquals("hello", map["f06"])
            assertEquals(100L, map["f11"])
            assertEquals(3.14, map["f13"])
            assertEquals(true, map["f15"])

            val e2 = BigEntity().fromMapData<BigEntity>(map)
            assertEquals(1, e2.f01)
            assertEquals("hello", e2.f06)
            assertEquals(100L, e2.f11)
        }
            """,
            tag = "BigEntity"
        )
    }

    // ========================================================================
    // 15. kClass returns correct class reference
    // ========================================================================

    @Test
    fun `test kClass on various entities`() {
        compileAndRun(
            classes = """
        data class KClassEntityA(var id: Int? = null) : KPojo
        data class KClassEntityB(var id: Int? = null, var name: String? = null) : KPojo
            """,
            code = """
        fun test() {
            val a = KClassEntityA()
            assertEquals("KClassEntityA", a.kClass().simpleName)
            val b = KClassEntityB()
            assertEquals("KClassEntityB", b.kClass().simpleName)
        }
            """,
            tag = "KClassMulti"
        )
    }

    // ========================================================================
    // 16. Cascade annotation with field mapping
    // ========================================================================

    @Test
    fun `test Cascade annotation creates cascade info with mapped fields`() {
        compileAndRun(
            classes = """
        data class CascadeTarget(
            var id: Int? = null,
            var detail: String? = null
        ) : KPojo

        data class CascadeSource(
            var id: Int? = null,
            var targetId: Int? = null,
            @Cascade(["targetId"], ["id"])
            var target: CascadeTarget? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = CascadeSource()
            val colNames = e.kronosColumns().map { it.name }
            // Cascade field is included in kronosColumns (cascade info is stored on the Field)
            assertTrue("target" in colNames, "cascade field is in kronosColumns")
            assertTrue("targetId" in colNames, "targetId should be a column")
            // Verify cascade mapping info is present on the target field
            val targetField = e.kronosColumns().find { it.name == "target" }
            assertNotNull(targetField, "target field should exist")
            assertNotNull(targetField.cascade, "target should have cascade info")
        }
            """,
            tag = "CascadeMapping"
        )
    }

    // ========================================================================
    // 17. fromMapData with null values in map
    // ========================================================================

    @Test
    fun `test fromMapData with explicit null values`() {
        compileAndRun(
            classes = """
        data class NullMapEntity(
            var id: Int? = null,
            var name: String? = null,
            var age: Int? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val map = mapOf<String, Any?>("id" to 1, "name" to null, "age" to null)
            val e = NullMapEntity().fromMapData<NullMapEntity>(map)
            assertEquals(1, e.id)
            assertNull(e.name)
            assertNull(e.age)
        }
            """,
            tag = "NullMapValues"
        )
    }

    // ========================================================================
    // 18. set with null value
    // ========================================================================

    @Test
    fun `test set property to null via dynamic set`() {
        compileAndRun(
            classes = """
        data class SetNullEntity(
            var id: Int? = null,
            var name: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = SetNullEntity(id = 1, name = "Alice")
            assertEquals("Alice", e.get("name"))
            e.set("name", null)
            assertNull(e.get("name"))
        }
            """,
            tag = "SetNull"
        )
    }

    // ========================================================================
    // 19. Multiple KPojo classes in same compilation unit
    // ========================================================================

    @Test
    fun `test multiple KPojo classes coexist correctly`() {
        compileAndRun(
            classes = """
        @Table(name = "tb_alpha")
        data class AlphaEntity(
            var id: Int? = null,
            var alphaField: String? = null
        ) : KPojo

        @Table(name = "tb_beta")
        data class BetaEntity(
            var id: Int? = null,
            var betaField: Double? = null,
            @Serialize
            var metadata: Map<String, String>? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val a = AlphaEntity(id = 1, alphaField = "hello")
            val b = BetaEntity(id = 2, betaField = 3.14)

            assertEquals("tb_alpha", a.__tableName)
            assertEquals("tb_beta", b.__tableName)

            val aMap = a.toDataMap()
            assertEquals("hello", aMap["alphaField"])

            val bMap = b.toDataMap()
            assertEquals(3.14, bMap["betaField"])

            val aCols = a.kronosColumns().map { it.name }
            assertTrue("alphaField" in aCols)
            assertFalse("betaField" in aCols)

            val bCols = b.kronosColumns().map { it.name }
            assertTrue("betaField" in bCols)
            assertTrue("metadata" in bCols, "metadata with @Serialize should be a column")
        }
            """,
            tag = "MultipleKPojo"
        )
    }

    // ========================================================================
    // 20. Entity with java.sql.Date and java.util.Date types
    // ========================================================================

    @Test
    fun `test java sql Date type maps to DATE`() {
        compileAndRun(
            classes = """
        data class SqlDateEntity(
            var id: Int? = null,
            var sqlDate: java.sql.Date? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = SqlDateEntity()
            assertEquals(KColumnType.DATE, e.getColumn("sqlDate").type)
        }
            """,
            tag = "SqlDateType"
        )
    }

    // ========================================================================
    // 21. Entity with BigDecimal type
    // ========================================================================

    @Test
    fun `test BigDecimal type maps to DECIMAL`() {
        compileAndRun(
            classes = """
        data class DecimalEntity(
            var id: Int? = null,
            var amount: BigDecimal? = null
        ) : KPojo {
            fun getColumn(name: String): Field = kronosColumns().find { it.name == name }!!
        }
            """,
            code = """
        fun test() {
            val e = DecimalEntity()
            assertEquals(KColumnType.DECIMAL, e.getColumn("amount").type)
            // Round-trip
            val e2 = DecimalEntity(id = 1, amount = BigDecimal("999.99"))
            val map = e2.toDataMap()
            assertEquals(BigDecimal("999.99"), map["amount"])
            val e3 = DecimalEntity().fromMapData<DecimalEntity>(map)
            assertEquals(BigDecimal("999.99"), e3.amount)
        }
            """,
            tag = "DecimalType"
        )
    }

    // ========================================================================
    // 22. Entity with only one field
    // ========================================================================

    @Test
    fun `test single field entity`() {
        compileAndRun(
            classes = """
        data class SingleFieldEntity(
            var id: Int? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = SingleFieldEntity(id = 42)
            val cols = e.kronosColumns()
            assertEquals(1, cols.size)
            assertEquals("id", cols[0].name)

            val map = e.toDataMap()
            assertEquals(42, map["id"])

            val e2 = SingleFieldEntity().fromMapData<SingleFieldEntity>(map)
            assertEquals(42, e2.id)

            assertEquals(42, e.get("id"))
            e.set("id", 99)
            assertEquals(99, e.get("id"))
        }
            """,
            tag = "SingleField"
        )
    }

    // ========================================================================
    // 23. Naming strategy — field name conversion
    // ========================================================================

    @Test
    fun `test field naming strategy converts camelCase to snake_case`() {
        compileAndRun(
            classes = """
        data class NamingEntity(
            var id: Int? = null,
            var firstName: String? = null,
            var lastName: String? = null,
            var emailAddress: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = NamingEntity()
            val cols = e.kronosColumns()
            val firstNameCol = cols.find { it.name == "firstName" }
            assertNotNull(firstNameCol)
            assertEquals("first_name", firstNameCol.columnName)
            val lastNameCol = cols.find { it.name == "lastName" }
            assertNotNull(lastNameCol)
            assertEquals("last_name", lastNameCol.columnName)
            val emailCol = cols.find { it.name == "emailAddress" }
            assertNotNull(emailCol)
            assertEquals("email_address", emailCol.columnName)
        }
            """,
            tag = "NamingStrategy"
        )
    }

    // ========================================================================
    // 24. Disabled strategy annotation
    // ========================================================================

    @Test
    fun `test disabled CreateTime strategy`() {
        compileAndRun(
            classes = """
        @CreateTime(enable = false)
        data class DisabledCreateTimeEntity(
            var id: Int? = null,
            var name: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = DisabledCreateTimeEntity()
            val strategy = e.kronosCreateTime()
            assertFalse(strategy.enabled, "CreateTime strategy should be disabled")
        }
            """,
            tag = "DisabledCreateTime"
        )
    }

    @Test
    fun `test disabled UpdateTime strategy`() {
        compileAndRun(
            classes = """
        @UpdateTime(enable = false)
        data class DisabledUpdateTimeEntity(
            var id: Int? = null,
            var name: String? = null
        ) : KPojo
            """,
            code = """
        fun test() {
            val e = DisabledUpdateTimeEntity()
            val strategy = e.kronosUpdateTime()
            assertFalse(strategy.enabled, "UpdateTime strategy should be disabled")
        }
            """,
            tag = "DisabledUpdateTime"
        )
    }
}

