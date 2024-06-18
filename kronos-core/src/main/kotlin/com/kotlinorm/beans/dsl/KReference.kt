package com.kotlinorm.beans.dsl

import com.kotlinorm.enums.CascadeAction.CASCADE

class KReference(
    val referenceColumns: Array<String> = arrayOf(),
    val targetColumns: Array<String> = arrayOf(),
    val cascade: String = CASCADE,
    val defaultValue: String = "",
    val mapperBy: String = ""
)