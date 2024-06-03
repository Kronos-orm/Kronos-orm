package com.kotlinorm.enums

enum class JoinType(val value: String) {
    INNER_JOIN("INNER JOIN"),
    LEFT_JOIN("LEFT JOIN"),
    RIGHT_JOIN("RIGHT JOIN"),
    FULL_JOIN("FULL JOIN"),
    CROSS_JOIN("CROSS JOIN")
}