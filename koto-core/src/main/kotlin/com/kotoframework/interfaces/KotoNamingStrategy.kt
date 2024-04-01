package com.kotoframework.interfaces

interface KotoNamingStrategy {
    fun db2k(name: String): String
    fun k2db(name: String): String
}