package com.kotlinorm.utils

import com.kotlinorm.Kronos


fun fieldK2db(str: String): String {
    return Kronos.fieldNamingStrategy.k2db(str)
}

fun fieldDb2k(str: String): String {
    return Kronos.fieldNamingStrategy.db2k(str)
}

fun tableK2db(str: String): String {
    return Kronos.fieldNamingStrategy.k2db(str)
}

fun tableDb2k(str: String): String {
    return Kronos.fieldNamingStrategy.db2k(str)
}