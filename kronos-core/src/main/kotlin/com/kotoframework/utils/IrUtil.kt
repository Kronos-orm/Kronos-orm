package com.kotoframework.utils

import com.kotoframework.KotoApp


fun fieldK2db(str: String): String {
    return KotoApp.fieldNamingStrategy.k2db(str)
}

fun fieldDb2k(str: String): String {
    return KotoApp.fieldNamingStrategy.db2k(str)
}

fun tableK2db(str: String): String {
    return KotoApp.fieldNamingStrategy.k2db(str)
}

fun tableDb2k(str: String): String {
    return KotoApp.fieldNamingStrategy.db2k(str)
}