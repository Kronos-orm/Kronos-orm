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

class KronosTemplateTest {

    @BeforeTest
    fun setup() {
        with(Kronos) {
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
        val t = template(fields = [field])
        val annotations = with(t) { field.annotations() }
        assertEquals(listOf("@PrimaryKey(identity = true)"), annotations)
        assertEquals(
            linkedSetOf(
                "com.kotlinorm.annotations.Table",
                "com.kotlinorm.interfaces.KPojo",
                "com.kotlinorm.annotations.PrimaryKey"
            ),
            t.imports
        )
    }

    @Test
    fun annotationsWithPrimaryKeyDefault() {
        val field = Field(columnName = "pk_col", type = KColumnType.INT, primaryKey = PrimaryKeyType.DEFAULT)
        val t = template(fields = [field])
        val annotations = with(t) { field.annotations() }
        assertTrue(annotations.any { it == "@PrimaryKey" })
    }

    @Test
    fun annotationsWithDefaultValue() {
        val field = Field(columnName = "status", type = KColumnType.INT, defaultValue = "0")
        val t = template(fields = [field])
        val annotations = with(t) { field.annotations() }
        assertEquals(listOf("@Default(\"0\")"), annotations)
        assertEquals(
            linkedSetOf(
                "com.kotlinorm.annotations.Table",
                "com.kotlinorm.interfaces.KPojo",
                "com.kotlinorm.annotations.Default"
            ),
            t.imports
        )
    }

    @Test
    fun annotationsWithNonNullableField() {
        val field = Field(columnName = "name", type = KColumnType.VARCHAR, nullable = false)
        val t = template(fields = [field])
        val annotations = with(t) { field.annotations() }
        assertEquals(listOf("@NonNull"), annotations)
        assertEquals(
            linkedSetOf(
                "com.kotlinorm.annotations.Table",
                "com.kotlinorm.interfaces.KPojo",
                "com.kotlinorm.annotations.NonNull"
            ),
            t.imports
        )
    }

    @Test
    fun annotationsWithCreateTimeStrategy() {
        val field = Field(columnName = "create_time", type = KColumnType.DATETIME)
        val t = template(fields = [field])
        val annotations = with(t) { field.annotations() }
        assertEquals(listOf("@CreateTime"), annotations)
        assertEquals(
            linkedSetOf(
                "com.kotlinorm.annotations.Table",
                "com.kotlinorm.interfaces.KPojo",
                "com.kotlinorm.annotations.CreateTime"
            ),
            t.imports
        )
    }

    @Test
    fun annotationsWithUpdateTimeStrategy() {
        val field = Field(columnName = "update_time", type = KColumnType.DATETIME)
        val t = template(fields = [field])
        val annotations = with(t) { field.annotations() }
        assertEquals(listOf("@UpdateTime"), annotations)
        assertEquals(
            linkedSetOf(
                "com.kotlinorm.annotations.Table",
                "com.kotlinorm.interfaces.KPojo",
                "com.kotlinorm.annotations.UpdateTime"
            ),
            t.imports
        )
    }

    @Test
    fun annotationsWithLogicDeleteStrategy() {
        val field = Field(columnName = "deleted", type = KColumnType.BIT)
        val t = template(fields = [field])
        val annotations = with(t) { field.annotations() }
        assertEquals(listOf("@LogicDelete"), annotations)
        assertEquals(
            linkedSetOf(
                "com.kotlinorm.annotations.Table",
                "com.kotlinorm.interfaces.KPojo",
                "com.kotlinorm.annotations.LogicDelete"
            ),
            t.imports
        )
    }

    @Test
    fun annotationsWithVersionStrategy() {
        val field = Field(columnName = "version", type = KColumnType.INT)
        val t = template(fields = [field])
        val annotations = with(t) { field.annotations() }
        assertEquals(listOf("@Version"), annotations)
        assertEquals(
            linkedSetOf(
                "com.kotlinorm.annotations.Table",
                "com.kotlinorm.interfaces.KPojo",
                "com.kotlinorm.annotations.Version"
            ),
            t.imports
        )
    }

    @Test
    fun annotationsWithColumnTypeNonDefaultLength() {
        // A DECIMAL field with non-default length/scale triggers @ColumnType
        val field = Field(columnName = "price", type = KColumnType.DECIMAL, length = 10, scale = 2)
        val t = template(fields = [field])
        val annotations = with(t) { field.annotations() }
        assertEquals(listOf("@ColumnType(type = KColumnType.DECIMAL, length = 10, scale = 2)"), annotations)
        assertEquals(
            linkedSetOf(
                "com.kotlinorm.annotations.Table",
                "com.kotlinorm.interfaces.KPojo",
                "com.kotlinorm.annotations.ColumnType",
                "com.kotlinorm.enums.KColumnType"
            ),
            t.imports
        )
    }

    // --- KTableIndex.toAnnotations() tests ---

    @Test
    fun indexToAnnotationsWithNameAndColumns() {
        val index = KTableIndex(name = "idx_user", columns = arrayOf("name", "email"), type = "UNIQUE")
        val t = template(indexes = [index])
        val annotation = with(t) { index.toAnnotations() }
        assertEquals("@TableIndex(name = \"idx_user\", columns = [\"name\", \"email\"], type = \"UNIQUE\")", annotation)
    }

    @Test
    fun indexToAnnotationsWithMethod() {
        val index = KTableIndex(name = "idx_geo", columns = arrayOf("location"), type = "", method = "GIST")
        val t = template(indexes = [index])
        val annotation = with(t) { index.toAnnotations() }
        assertEquals("@TableIndex(name = \"idx_geo\", columns = [\"location\"], method = \"GIST\")", annotation)
    }

    @Test
    fun indexToAnnotationsWithConcurrently() {
        val index = KTableIndex(name = "idx_conc", columns = arrayOf("col1"), concurrently = true)
        val t = template(indexes = [index])
        val annotation = with(t) { index.toAnnotations() }
        assertEquals("@TableIndex(name = \"idx_conc\", columns = [\"col1\"], concurrently = true)", annotation)
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
        val t = template(indexes = [idx1, idx2])
        val result = with(t) { indexes.toAnnotations() }!!
        val lines = result.split("\n")
        assertEquals(
            listOf(
                "@TableIndex(name = \"idx1\", columns = [\"a\"])",
                "@TableIndex(name = \"idx2\", columns = [\"b\"])"
            ),
            lines
        )
        assertEquals(
            linkedSetOf(
                "com.kotlinorm.annotations.Table",
                "com.kotlinorm.interfaces.KPojo",
                "com.kotlinorm.annotations.TableIndex"
            ),
            t.imports
        )
    }
}
