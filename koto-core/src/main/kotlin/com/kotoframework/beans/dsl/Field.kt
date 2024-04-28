package com.kotoframework.beans.dsl

import com.kotoframework.utils.fieldDb2k

class Field(
    val columnName: String,
    val name: String = fieldDb2k(columnName)
){
    override fun toString(): String {
        return name
    }
}