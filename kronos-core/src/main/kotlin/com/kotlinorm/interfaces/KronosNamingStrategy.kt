package com.kotlinorm.interfaces

interface KronosNamingStrategy {
    fun db2k(name: String): String
    fun k2db(name: String): String
}