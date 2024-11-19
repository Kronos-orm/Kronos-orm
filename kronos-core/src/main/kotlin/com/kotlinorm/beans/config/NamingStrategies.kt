/**
 * Copyright 2022-2024 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.beans.config

import com.kotlinorm.interfaces.KronosNamingStrategy
import java.util.*

class LineHumpNamingStrategy : KronosNamingStrategy {
    override fun k2db(name: String): String {
        return humpToLine(name)
    }

    override fun db2k(name: String): String {
        return lineToHump(name)
    }

    /**
     * Converts a line separated string to a hump case string.
     *
     * @param line The line separated string to be converted.
     * @return The hump case string.
     */
    private fun lineToHump(line: String): String {
        val str = line.trim()
        if (str.isEmpty()) return ""
        return str.split("_")
            .mapIndexed { index, it ->
                if (it[0] in 'a'..'z' && index != 0) it[0] - 32 + it.substring(1) else it
            }
            .joinToString("")
    }

    /**
     * Converts a hump case string to a line separated string.
     *
     * @param hump The hump case string to be converted.
     * @return The line separated string.
     */
    private fun humpToLine(hump: String): String {
        val str = hump.trim()
        if (str.isEmpty()) return ""
        val list = mutableListOf<String>()
        var i = 1
        var j = 0
        while (i < str.length) {
            if (str[i] in 'A'..'Z') {
                list.add(str.substring(j, i))
                j = i
            }
            i++
        }
        list.add(str.substring(j))
        return list.joinToString("_") { it.lowercase(Locale.getDefault()) }
    }
}

class NoneNamingStrategy : KronosNamingStrategy {
    override fun k2db(name: String): String = name
    override fun db2k(name: String): String = name
}