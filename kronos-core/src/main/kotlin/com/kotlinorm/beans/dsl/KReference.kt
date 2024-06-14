package com.kotlinorm.beans.dsl

class KReference(
    val tableName: String = "",
    val fields: Array<String> = arrayOf(),
    val referenceFields: Array<String> = arrayOf(),
    val cascade: Boolean = false
)