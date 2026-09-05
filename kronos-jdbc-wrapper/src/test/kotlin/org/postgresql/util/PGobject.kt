package org.postgresql.util

class PGobject(private val value: String?) {
    fun getValue(): String? = value
}
