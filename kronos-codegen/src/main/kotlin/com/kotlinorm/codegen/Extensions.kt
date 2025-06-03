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
package com.kotlinorm.codegen

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.KColumnType

/**
 * The maximum number of words per line in generated Kotlin comments.
 * 生成的 Kotlin 注释格式化时，每行最多的单词数。
 */
const val MAX_COMMENT_LINE_WORDS = 80

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