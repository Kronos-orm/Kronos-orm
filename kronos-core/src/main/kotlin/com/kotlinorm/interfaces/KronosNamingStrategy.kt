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