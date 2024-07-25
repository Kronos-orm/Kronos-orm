package com.kotlinorm.enums

enum class SqlType(val type: String) {
    INSERT("INSERT");

    companion object {
        val Insert = INSERT
    }
}