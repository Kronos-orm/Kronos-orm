package com.kotlinorm.codegen

import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.PrimaryKeyType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertContains

class KronosTemplateTest {

    @BeforeTest
    fun setup() {
        Kronos.init {
            fieldNamingStrategy = Kronos.lineHumpNamingStrategy
            tableNamingStrategy = Kronos.lineHumpNamingStrategy
            createTimeStrategy = KronosCommonStrategy(true, Field("create_time", "createTime"))
            updateTimeStrategy = KronosCommonStrategy(true, Field("update_time", "updateTime"))
            logicDeleteStrategy = KronosCommonStrategy(true, Field("deleted"))
            optimisticLockStrategy = KronosCommonStrategy(true, Field("version"))
        }
    }

    private fun template(
        tableName: String = "test_table",
        className: String = "TestTable",
        tableComment: String = "",
        fields: List<Field> = emptyList(),
        indexes: List<KTableIndex> = emptyList(),
        tableCommentLineWords: Int = MAX_COMMENT_LINE_WORDS
    ) = KronosTemplate(
        packageName = "com.test",
        tableName = tableName,
        className = className,
        tableComment = tableComment,
        fields = fields,
        indexes = indexes,
        tableCommentLineWords = tableCommentLineWords
    )

    // --- indent tests ---

    @Test
    fun indentDefaultReturnsOneSpace() {
        val t = template()
        assertEquals(" ", t.indent())
    }

    @Test
    fun indentFourReturnsFourSpaces() {
        val t = template()
        assertEquals("    ", t.indent(4))
    }

    @Test
    fun indentZeroReturnsEmpty() {
        val t = template()
        assertEquals("", t.indent(0))
    }

    // --- unaryPlus tests ---

    @Test
    fun unaryPlusAppendsStringAndNewline() {
        val t = template()
        with(t) {
            +"hello"
        }
        assertEquals("hello\n", t.content)
    }

    @Test
    fun unaryPlusWithNullDoesNotAppend() {
        val t = template()
        with(t) {
            val s: String? = null
            +s
        }
        assertEquals("", t.content)
    }

    @Test
    fun unaryPlusMultipleStrings() {
        val t = template()
        with(t) {
            +"line1"
            +"line2"
        }
        assertEquals("line1\nline2\n", t.content)
    }

    // --- formatedComment tests ---

    @Test
    fun formatedCommentWithEmptyString() {
        val t = template(tableComment = "")
        assertEquals("", t.formatedComment)
    }

    @Test
    fun formatedCommentWithShortComment() {
        val t = template(tableComment = "A short comment")
        assertEquals("// A short comment", t.formatedComment)
    }

    @Test
    fun formatedCommentWrapsLongComment() {
        // Use a very small lineWords to force wrapping
        val t = template(tableComment = "word1 word2 word3", tableCommentLineWords = 10)
        val lines = t.formatedComment.split("\n")
        assertTrue(lines.size > 1, "Expected multiple lines for wrapped comment")
        assertTrue(lines.all { it.startsWith("// ") })
    }

    // --- Field.annotations() tests ---

    @Test
    fun annotationsWithPrimaryKeyIdentity() {
        val field = Field(columnName = "id", type = KColumnType.INT, primaryKey = PrimaryKeyType.IDENTITY)
        val t = template(fields = listOf(field))
        val annotations = with(t) { field.annotations() }
        assertTrue(annotations.any { it.contains("@PrimaryKey") })
        assertContains(t.imports, "com.kotlinorm.annotations.PrimaryKey")
    }

    @Test
    fun annotationsWithPrimaryKeyDefault() {
        val field = Field(columnName = "pk_col", type = KColumnType.INT, primaryKey = PrimaryKeyType.DEFAULT)
        val t = template(fields = listOf(field))
        val annotations = with(t) { field.annotations() }
        assertTrue(annotations.any { it == "@PrimaryKey" })
    }

    @Test
    fun annotationsWithDefaultValue() {
        val field = Field(columnName = "status", type = KColumnType.INT, defaultValue = "0")
        val t = template(fields = listOf(field))
        val annotations = with(t) { field.annotations() }
        assertTrue(annotations.any { it.contains("@Default(\"0\")") })
        assertContains(t.imports, "com.kotlinorm.annotations.Default")
    }

    @Test
    fun annotationsWithNonNullableField() {
        val field = Field(columnName = "name", type = KColumnType.VARCHAR, nullable = false)
        val t = template(fields = listOf(field))
        val annotations = with(t) { field.annotations() }
        assertTrue(annotations.any { it == "@Necessary" })
        assertContains(t.imports, "com.kotlinorm.annotations.Necessary")
    }

    @Test
    fun annotationsWithCreateTimeStrategy() {
        val field = Field(columnName = "create_time", type = KColumnType.DATETIME)
        val t = template(fields = listOf(field))
        val annotations = with(t) { field.annotations() }
        assertTrue(annotations.any { it == "@CreateTime" })
        assertContains(t.imports, "com.kotlinorm.annotations.CreateTime")
    }

    @Test
    fun annotationsWithUpdateTimeStrategy() {
        val field = Field(columnName = "update_time", type = KColumnType.DATETIME)
        val t = template(fields = listOf(field))
        val annotations = with(t) { field.annotations() }
        assertTrue(annotations.any { it == "@UpdateTime" })
        assertContains(t.imports, "com.kotlinorm.annotations.UpdateTime")
    }

    @Test
    fun annotationsWithLogicDeleteStrategy() {
        val field = Field(columnName = "deleted", type = KColumnType.BIT)
        val t = template(fields = listOf(field))
        val annotations = with(t) { field.annotations() }
        assertTrue(annotations.any { it == "@LogicDelete" })
        assertContains(t.imports, "com.kotlinorm.annotations.LogicDelete")
    }

    @Test
    fun annotationsWithVersionStrategy() {
        val field = Field(columnName = "version", type = KColumnType.INT)
        val t = template(fields = listOf(field))
        val annotations = with(t) { field.annotations() }
        assertTrue(annotations.any { it == "@Version" })
        assertContains(t.imports, "com.kotlinorm.annotations.Version")
    }

    @Test
    fun annotationsWithColumnTypeNonDefaultLength() {
        // A DECIMAL field with non-default length/scale triggers @ColumnType
        val field = Field(columnName = "price", type = KColumnType.DECIMAL, length = 10, scale = 2)
        val t = template(fields = listOf(field))
        val annotations = with(t) { field.annotations() }
        assertTrue(annotations.any { it.contains("@ColumnType") })
        assertContains(t.imports, "com.kotlinorm.annotations.ColumnType")
    }

    // --- KTableIndex.toAnnotations() tests ---

    @Test
    fun indexToAnnotationsWithNameAndColumns() {
        val index = KTableIndex(name = "idx_user", columns = arrayOf("name", "email"), type = "UNIQUE")
        val t = template(indexes = listOf(index))
        val annotation = with(t) { index.toAnnotations() }
        assertContains(annotation, "@TableIndex")
        assertContains(annotation, "name = \"idx_user\"")
        assertContains(annotation, "\"name\"")
        assertContains(annotation, "\"email\"")
        assertContains(annotation, "type = \"UNIQUE\"")
    }

    @Test
    fun indexToAnnotationsWithMethod() {
        val index = KTableIndex(name = "idx_geo", columns = arrayOf("location"), type = "", method = "GIST")
        val t = template(indexes = listOf(index))
        val annotation = with(t) { index.toAnnotations() }
        assertContains(annotation, "method = \"GIST\"")
    }

    @Test
    fun indexToAnnotationsWithConcurrently() {
        val index = KTableIndex(name = "idx_conc", columns = arrayOf("col1"), concurrently = true)
        val t = template(indexes = listOf(index))
        val annotation = with(t) { index.toAnnotations() }
        assertContains(annotation, "concurrently = true")
    }

    @Test
    fun emptyIndexListToAnnotationsReturnsNull() {
        val t = template(indexes = emptyList())
        val result = with(t) { indexes.toAnnotations() }
        assertNull(result)
    }

    @Test
    fun multipleIndexesToAnnotationsJoinsWithNewline() {
        val idx1 = KTableIndex(name = "idx1", columns = arrayOf("a"))
        val idx2 = KTableIndex(name = "idx2", columns = arrayOf("b"))
        val t = template(indexes = listOf(idx1, idx2))
        val result = with(t) { indexes.toAnnotations() }!!
        val lines = result.split("\n")
        assertEquals(2, lines.size)
        assertContains(t.imports, "com.kotlinorm.annotations.TableIndex")
    }
}
