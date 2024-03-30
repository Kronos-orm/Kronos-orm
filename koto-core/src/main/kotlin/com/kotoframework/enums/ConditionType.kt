package com.kotoframework.enums

enum class ConditionType {
    LIKE,
    EQUAL,
    IN,
    ISNULL,
    SQL,
    GT,
    GE,
    LT,
    LE,
    BETWEEN,
    AND,
    OR,
    NOT,
    ROOT
}

internal fun toConditionType(str: String): ConditionType {
    return ConditionType.valueOf(str.uppercase())
}