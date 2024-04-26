package com.kotoframework.beans.dsl

class Field(
    val columnName: String,
    val propertyName: String
){
    override fun toString(): String {
        return propertyName
    }
}