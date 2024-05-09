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

package com.kotlinorm.beans.namingStrategy

import com.kotlinorm.interfaces.KronosNamingStrategy

object LineHumpNamingStrategy : KronosNamingStrategy {
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
        return line
            .split("_")
            .joinToString("") { it.replaceFirstChar(Char::uppercase) }
    }

    /**
     * Converts a hump case string to a line separated string.
     *
     * @param hump The hump case string to be converted.
     * @return The line separated string.
     */
    private fun humpToLine(hump: String): String {
        return hump
            .split("(?<=[a-z])(?=[A-Z])".toRegex())
            .joinToString("_") { it.lowercase() }
    }
}

class NoneNamingStrategy : KronosNamingStrategy {
    override fun k2db(name: String): String = name
    override fun db2k(name: String): String = name
}