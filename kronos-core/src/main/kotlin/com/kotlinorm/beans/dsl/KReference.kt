package com.kotlinorm.beans.dsl

class KReference(
    val propName: String,
    val referenceColumns: Array<String> = arrayOf(),
    val targetColumns: Array<String> = arrayOf(),
    val cascade: Boolean = false
)