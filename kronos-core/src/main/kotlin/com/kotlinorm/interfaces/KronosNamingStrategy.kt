package com.kotlinorm.interfaces

interface KronosNamingStrategy {
    /**
     * Converts a database name to a Kotlin name.
     *
     * @param name the database name to convert
     * @return the converted Kotlin name
     */
    fun db2k(name: String): String

    /**
     * Converts a Kotlin name to a database name.
     *
     * @param name the Kotlin name to convert
     * @return the converted database name
     */
    fun k2db(name: String): String
}