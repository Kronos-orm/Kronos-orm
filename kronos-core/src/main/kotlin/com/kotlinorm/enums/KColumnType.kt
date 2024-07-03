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

package com.kotlinorm.enums

/**
 * Column types.
 */
enum class KColumnType(val type: String) {
    UNDEFINED("UNDEFINED"), // 未定义
    BIT("BIT"), // 存储 0/1
    TINYINT("TINYINT"), // 整数值（没有小数点），精度 3。
    SMALLINT("SMALLINT"), // 整数值（没有小数点）。精度 5。
    INT("INT"), // 整数值（没有小数点）。精度 p。
    BIGINT("BIGINT"), // 整数值（没有小数点）。精度 19。
    REAL("REAL"), // 近似数值，尾数精度 7。
    FLOAT("FLOAT"), // 近似数值，尾数精度 p。一个采用以 10 为基数的指数计数法的浮点数。该类型的 size 参数由一个指定最小精度的单一数字组成。
    DOUBLE("DOUBLE"), // 近似数值，尾数精度 16。
    DECIMAL("DECIMAL"), // 精确数值，精度 p，小数点后位数 s。例如：decimal(5,2) 是一个小数点前有 3 位数，小数点后有 2 位数的数字。
    CHAR("CHAR"), // 字符/字符串。固定长度 n。
    VARCHAR("VARCHAR"), // 字符/字符串。可变长度。最大长度 n。
    TEXT("TEXT"), // 字符/字符串。可变长度。
    LONGTEXT("LONGTEXT"), // 字符/字符串。可变长度。
    DATE("DATE"), // 存储年、月、日的值。
    TIME("TIME"), // 存储小时、分、秒的值。
    DATETIME("DATETIME"), // 存储年、月、日、小时、分、秒的值。
    TIMESTAMP("TIMESTAMP"), // 存储年、月、日、小时、分、秒的值。
    BINARY("BINARY"), // 二进制串。固定长度 n。
    VARBINARY("VARBINARY"), // 二进制串。可变长度。最大长度 n。
    LONGVARBINARY("LONGVARBINARY"), // 二进制串。可变长度。
    BLOB("BLOB"), // 二进制串。可变长度。
    CLOB("CLOB"), // 二进制串。可变长度。
    JSON("JSON"), // 存储 JSON 数据
    ENUM("ENUM"), // 存储枚举值
    NVARCHAR("NVARCHAR"), // 字符/字符串。可变长度。最大长度 n。
    NCHAR("NCHAR"), // 字符/字符串。固定长度 n。
    NCLOB("NCLOB"), // 二进制串。可变长度。
    UUID("UUID"), // 二进制串。可变长度。
    SERIAL("SERIAL"), // 整数值（没有小数点），精度 p。
    YEAR("YEAR"), // 存储年份
    MEDIUMINT("MEDIUMINT"), // 整数值（没有小数点），精度 3。
    NUMERIC("NUMERIC"), // 精确数值，精度 p，小数点后位数 s。（与 DECIMAL 相同）
    MEDIUMTEXT("MEDIUMTEXT"), // 字符/字符串。可变长度。
    MEDIUMBLOB("MEDIUMBLOB"), // 二进制串。可变长度。
    LONGBLOB("LONGBLOB"), // 二进制串。可变长度。
    SET("SET"), // 一个元素的固定长度的有序集合
    GEOMETRY("GEOMETRY"), // 二进制串。可变长度。
    POINT("POINT"), // 二进制串。可变长度。
    LINESTRING("LINESTRING"), // 二进制串。可变长度。
    XML("XML"), // 存储 XML 数据
    CUSTOM_CRITERIA_SQL("CUSTOM_CRITERIA_SQL"); // 用于where查询的由String.asSql()产生的自定义SQL Field类型

    companion object {
        fun fromString(type: String): KColumnType {
            return entries.firstOrNull { it.name == type.uppercase() } ?: UNDEFINED
        }
    }
}