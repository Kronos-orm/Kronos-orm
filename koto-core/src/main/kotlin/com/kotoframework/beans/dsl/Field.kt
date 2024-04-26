package com.kotoframework.beans.dsl

class Field(
    val columnName: String,
    val name: String
){
    override fun toString(): String {
        return name
    }
}