/**
 * Copyright 2022-2024 kronos-orm
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
package com.kotlinorm.codegen

import com.kotlinorm.Kronos
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.PrimaryKeyType

val imports = linkedSetOf(
    "com.kotlinorm.annotations.Table",
    "com.kotlinorm.interfaces.KPojo"
)

val Field.kotlinType
    get(): String = when (type) {
        // 基础类型映射
        KColumnType.BIT -> "Boolean"
        KColumnType.TINYINT -> "Byte"
        KColumnType.SMALLINT -> "Short"
        KColumnType.INT, KColumnType.SERIAL, KColumnType.YEAR, KColumnType.MEDIUMINT -> "Int"
        KColumnType.BIGINT -> "Long"
        KColumnType.REAL, KColumnType.FLOAT -> "Float"
        KColumnType.DOUBLE -> "Double"
        KColumnType.DECIMAL, KColumnType.NUMERIC -> "java.math.BigDecimal"

        // 字符串类型（统一返回 String）
        KColumnType.VARCHAR, KColumnType.TEXT, KColumnType.LONGTEXT,
        KColumnType.CLOB, KColumnType.JSON, KColumnType.ENUM,
        KColumnType.NVARCHAR, KColumnType.NCHAR, KColumnType.NCLOB,
        KColumnType.MEDIUMTEXT, KColumnType.SET, KColumnType.GEOMETRY,
        KColumnType.POINT, KColumnType.LINESTRING, KColumnType.XML,
        KColumnType.UNDEFINED -> "String"

        // 二进制类型（统一返回 ByteArray）
        KColumnType.BINARY, KColumnType.VARBINARY, KColumnType.LONGVARBINARY,
        KColumnType.BLOB, KColumnType.MEDIUMBLOB, KColumnType.LONGBLOB -> "ByteArray"

        // 日期时间类型
        KColumnType.DATE -> "java.time.LocalDate"
        KColumnType.TIME -> "java.time.LocalTime"
        KColumnType.DATETIME -> "java.time.LocalDateTime"
        KColumnType.TIMESTAMP -> "java.time.Instant"

        // 特殊类型
        KColumnType.CHAR -> "Char"
        KColumnType.UUID -> "java.util.UUID"

        // 默认兜底
        else -> "String"
    }

fun Field.annotations(): List<String> {
    val annotations = mutableListOf<String>()
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
    if (!nullable) {
        annotations.add("@Necessary")
        imports.add("com.kotlinorm.annotations.Necessary")
    }
    if (defaultValue != null) {
        annotations.add("@Default(\"$defaultValue\")")
        imports.add("com.kotlinorm.annotations.Default")
    }

    if (length != 0 || scale != 0) {
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

fun List<KTableIndex>.toAnnotations(): String {
    if (isNotEmpty()) {
        imports.add("com.kotlinorm.annotations.TableIndex")
    }
    return joinToString("\n") { it.toAnnotations() }
}