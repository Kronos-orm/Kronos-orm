package com.kotoframework.beans.namingStrategy

import com.kotoframework.interfaces.KotoNamingStrategy

object LineHumpNamingStrategy : KotoNamingStrategy {
    override fun k2db(name: String): String {
        return humpToLine(name)
    }

    override fun db2k(name: String): String {
        return lineToHump(name)
    }

    private fun lineToHump(line: String): String {
        return line
            .split("_")
            .joinToString("") { it.replaceFirstChar { it.uppercase() } }
    }

    private fun humpToLine(hump: String): String {
        return hump
            .split("(?<=[a-z])(?=[A-Z])".toRegex())
            .joinToString("_") { it.lowercase() }
    }
}

class NoneNamingStrategy : KotoNamingStrategy {
    override fun k2db(name: String): String = name
    override fun db2k(name: String): String = name
}