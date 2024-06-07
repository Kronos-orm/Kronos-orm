package com.kotlinorm.enums

object KColumnType {
    const val CHARACTER = "CHARACTER" // 字符/字符串。固定长度 n。
    const val VARCHAR = "VARCHAR" // 字符/字符串。可变长度。最大长度 n。
    const val BINARY = "BINARY" // 二进制串。固定长度 n。
    const val BOOLEAN = "BOOLEAN" // 存储 TRUE 或 FALSE 值
    const val VARBINARY = "VARBINARY" // 二进制串。可变长度。最大长度 n。
    const val INTEGER = "INTEGER" // 整数值（没有小数点）。精度 p。
    const val SMALLINT = "SMALLINT" // 整数值（没有小数点）。精度 5。
    const val BIGINT = "BIGINT" // 整数值（没有小数点）。精度 19。
    const val DECIMAL = "DECIMAL" // 精确数值，精度 p，小数点后位数 s。例如：decimal(5,2) 是一个小数点前有 3 位数，小数点后有 2 位数的数字。
    const val NUMERIC = "NUMERIC" // 精确数值，精度 p，小数点后位数 s。（与 DECIMAL 相同）
    const val FLOAT = "FLOAT" // 近似数值，尾数精度 p。一个采用以 10 为基数的指数计数法的浮点数。该类型的 size 参数由一个指定最小精度的单一数字组成。
    const val REAL = "REAL" // 近似数值，尾数精度 7。
    const val DOUBLE_PRECISION = "DOUBLE PRECISION" // 近似数值，尾数精度 16。
    const val DATE = "DATE" // 存储年、月、日的值。
    const val TIME = "TIME" // 存储小时、分、秒的值。
    const val DATETIME = "DATETIME" // 存储年、月、日、小时、分、秒的值。
    const val TIMESTAMP = "TIMESTAMP" // 存储年、月、日、小时、分、秒的值。
    const val INTERVAL = "INTERVAL" // 由一些整数字段组成，代表一段时间，取决于区间的类型。
    const val ARRAY = "ARRAY" // 元素的固定长度的有序集合
    const val MULTISET = "MULTISET" // 元素的可变长度的无序集合
    const val XML = "XML" // 存储 XML 数据

    const val CUSTOM_CRITERIA_SQL = "CUSTOM_CRITERIA_SQL" // 用于where查询的由String.asSql()产生的自定义SQL Field类型
}