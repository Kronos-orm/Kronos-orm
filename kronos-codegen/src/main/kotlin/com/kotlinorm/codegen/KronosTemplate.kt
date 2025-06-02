package com.kotlinorm.codegen

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.primaryKeyStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.PrimaryKeyType

class KronosTemplate(
    val packageName: String,
    val tableName: String,
    val className: String,
    val tableComment: String,
    val fields: List<Field>,
    val indexes: List<KTableIndex>,
    val tableCommentLineWords: Int, // Default value if not specified
) {
    var content = ""

    var imports = linkedSetOf(
        "com.kotlinorm.annotations.Table",
        "com.kotlinorm.interfaces.KPojo"
    )


    fun Field.annotations(): List<String> {
        val annotations = mutableListOf<String>()
        if(primaryKeyStrategy.field.columnName == columnName) {
            primaryKey = PrimaryKeyType.IDENTITY
        }
        when (primaryKey) {
            PrimaryKeyType.IDENTITY -> {
                annotations.add("@PrimaryKey(identity = true)")
                imports.add("com.kotlinorm.annotations.PrimaryKey")
            }

            PrimaryKeyType.DEFAULT -> {
                annotations.add("@PrimaryKey")
                imports.add("com.kotlinorm.annotations.PrimaryKey")
            }

            else -> {}
        }
        if (!nullable && primaryKey == PrimaryKeyType.NOT) {
            annotations.add("@Necessary")
            imports.add("com.kotlinorm.annotations.Necessary")
        }
        if (defaultValue != null) {
            annotations.add("@Default(\"$defaultValue\")")
            imports.add("com.kotlinorm.annotations.Default")
        }
        val isNumber = type in listOf(
            KColumnType.TINYINT,
            KColumnType.INT,
            KColumnType.BIGINT,
            KColumnType.FLOAT,
            KColumnType.DOUBLE,
            KColumnType.DECIMAL
        )
        val isBool = type == KColumnType.BIT
        val isString = type == KColumnType.VARCHAR && length == 255 && scale == 0
        if (
            !isString && !isBool && (length != 0 || scale != 0) && (scale != 10 || !isNumber)
        ) {
            val params = mutableListOf<String>()
            if (length != 0) {
                params.add("length = $length")
            }
            if (scale != 0) {
                params.add("scale = $scale")
            }
            annotations.add(
                "@ColumnType(type = KColumnType.${type}, ${params.joinToString(", ")})"
            )
            imports.add("com.kotlinorm.annotations.ColumnType")
            imports.add("com.kotlinorm.enums.KColumnType")
        }
        if (Kronos.createTimeStrategy.field.columnName == columnName) {
            annotations.add("@CreateTime")
            imports.add("com.kotlinorm.annotations.CreateTime")
        }
        if (Kronos.updateTimeStrategy.field.columnName == columnName) {
            annotations.add("@UpdateTime")
            imports.add("com.kotlinorm.annotations.UpdateTime")
        }
        if (Kronos.logicDeleteStrategy.field.columnName == columnName) {
            annotations.add("@LogicDelete")
            imports.add("com.kotlinorm.annotations.LogicDelete")
        }
        if (Kronos.optimisticLockStrategy.field.columnName == columnName) {
            annotations.add("@Version")
            imports.add("com.kotlinorm.annotations.Version")
        }
        return annotations
    }

    fun KTableIndex.toAnnotations(): String {
        val params = mutableListOf<String>()
        if (name.isNotEmpty()) {
            params.add("name = \"$name\"")
        }
        if (columns.isNotEmpty()) {
            params.add("columns = [${columns.joinToString(", ") { "\"$it\"" }}]")
        }
        if (type.isNotEmpty()) {
            params.add("type = \"$type\"")
        }
        if (method.isNotEmpty()) {
            params.add("method = \"$method\"")
        }
        if (concurrently) {
            params.add("concurrently = $concurrently")
        }
        return "@TableIndex(${params.joinToString(", ")})"
    }

    fun List<KTableIndex>.toAnnotations(): String? {
        if (isNotEmpty()) {
            imports.add("com.kotlinorm.annotations.TableIndex")
        } else {
            return null
        }
        return joinToString("\n") { it.toAnnotations() }
    }

    init {
        fields.forEach { it.annotations() }
        indexes.toAnnotations()
    }

    val formatedComment by lazy {
        if (tableComment.isEmpty()) {
            ""
        } else {
            val words = tableComment.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = ""
            for (word in words) {
                if (currentLine.length + word.length + 1 > tableCommentLineWords) {
                    lines.add(currentLine.trim())
                    currentLine = ""
                }
                currentLine += "$word "
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.trim())
            }
            lines.joinToString("\n") { "// $it" }
        }
    }

    fun indent(num: Int = 1): String {
        return " ".repeat(num)
    }

    operator fun String?.unaryPlus() {
        if (this == null) return
        content += this
        content += "\n"
    }
}