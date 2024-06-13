package com.kotlinorm.beans.dsl

data class KTableIndex(
    val name: String,
    val columns: List<String>,
    val type: String,
    val method: String
)