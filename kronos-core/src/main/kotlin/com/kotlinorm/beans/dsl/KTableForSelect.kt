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

package com.kotlinorm.beans.dsl

import com.kotlinorm.interfaces.KPojo

/**
 * KTable
 *
 * DSL Class of Kronos, which the compiler plugin use to generate the `select` code.
 * to add Fields, you can use following:
 * 1. `it.<field1> + it.<field2>`
 * 2. `it.<field1> + it.<field2>.as("<alias>")`
 * 3. `addField(Field(columnName, optionalName))`
 * 4. `Field(columnName, optionalName).setAlias("<alias>")`
 *
 * @param T the type of the table
 */
open class KTableForSelect<T : KPojo> {
    val fields: MutableList<Field> = mutableListOf()
    /**
     * Overloaded operator function that adds two objects of type Any?.
     *
     * @param other the object to be added to this object.
     * @return an integer value of 1.
     */
    operator fun Any?.plus(@Suppress("UNUSED_PARAMETER") other: Any?): Int = 1
    /**
     * Overloaded operator function that minus two objects of type Any?.
     *
     * @param other the object to be added to this object.
     * @return an integer value of 1.
     */
    operator fun KPojo?.minus(@Suppress("UNUSED_PARAMETER") other: Any?) = this

    /**
     * Sets an alias for the given object.
     *
     * @param alias the alias to set for the object
     * @return the provided alias
     */
    @Suppress("UNUSED")
    infix fun Any?.as_(alias: String): String = alias

    /**
     * Adds a field to the collection of fields.
     *
     * @param property the field to be added
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun addField(property: Field) {
        fields += property
    }

    fun Field.setAlias(alias: String): Field{
        this.name = alias
        return this
    }

    fun count(field: Any?): String = ""

    fun average(field: Any?): String = ""

    fun min(field: Any?): String = ""

    fun max(field: Any?): String = ""

    fun sum(field: Any?): String = ""

    fun abs(x: Any?): String = ""

    fun bin(x: Any?): String = ""

    fun ceiling(x: Any?): String = ""

    fun exp(x: Any?): String = ""

    fun floor(x: Any?): String = ""

    fun greatest(vararg xs: Any?): String = ""

    fun least(vararg xs: Any?): String = ""

    fun ln(x: Any?): String = ""

    fun log(x: Any?, y: Any?): String = ""

    fun mod(x: Any?, y: Any?): String = ""

    fun pi(): String = ""

    fun rand(seed: Any? = null): String = ""

    fun round(x: Any?, y: Any?): String = ""

    fun sign(x: Any?): String = ""

    fun sqrt(x: Any?): String = ""

    fun truncate(x: Any?, y: Any?): String = ""

    fun groupConcat(x: Any?): String = ""

    fun ascii(char: Any?): String = ""

    fun bitLength(str: Any?): String = ""

    fun concat(vararg fields: Any?): String = ""

    fun concatWs(sep: Any?, vararg fields: Any?): String = ""

    fun insert(str: Any?, x: Any?, y: Any?, instr: Any?): String = ""

    fun findInSet(str: Any?, list: Any?): String = ""

    fun lcase(str: Any?): String = ""

    fun left(str: Any?, x: Any?): String = ""

    fun length(s: Any?): String = ""

    fun ltrim(str: Any?): String = ""

    fun position(substr: Any?, str: Any?): String = ""

    fun quote(str: Any?): String = ""

    fun repeat(str: Any?, times: Any?): String = ""

    fun reverse(str: Any?): String = ""

    fun right(str: Any?, x: Any?): String = ""

    fun rtrim(str: Any?): String = ""

    fun strcmp(s1: Any?, s2: Any?): String = ""

    fun trim(str: Any?): String = ""

    fun ucase(str: Any?): String = ""

    fun curdate(): String = ""

    fun curtime(): String = ""

    fun dateAdd(date: Any?, interval: String): String = ""

    fun dateFormat(date: Any?, fmt: Any?): String = ""

    fun dateSub(date: Any?, interval: String): String = ""

    fun dayOfWeek(date: Any?): String = ""

    fun dayOfMonth(date: Any?): String = ""

    fun dayOfYear(date: Any?): String = ""

    fun dayName(date: Any?): String = ""

    fun fromUnixTime(ts: Any?, fmt: Any?): String = ""

    fun hour(time: Any?): String = ""

    fun minute(time: Any?): String = ""

    fun month(date: Any?): String = ""

    fun monthName(date: Any?): String = ""

    fun now(): String = ""

    fun quarter(date: Any?): String = ""

    fun week(date: Any?): String = ""

    fun year(date: Any?): String = ""

    fun periodDiff(p1: Any?, p2: Any?): String = ""

    fun calculateAge(birthday: Any?): String = ""

    fun aesEncrypt(str: Any?, key: Any?): String = ""

    fun aesDecrypt(str: Any?, key: Any?): String = ""

    fun decode(str: Any?, key: Any?): String = ""

    fun encrypt(str: Any?, salt: Any?): String = ""

    fun encode(str: Any?, key: Any?): String = ""

    fun md5(str: Any?): String = ""

    fun password(str: Any?): String = ""

    fun sha(str: Any?): String = ""

    companion object {
        /**
         * Creates a KTable instance with the given KPojo object as the data source and applies the given block to it.
         *
         * @param T The type of the KPojo object.
         * @param block The block of code to be applied to the KTable instance.
         * @return The resulting KTable instance after applying the block.
         */
        fun <T : KPojo> T.afterSelect(block: KTableForSelect<T>.(T) -> Unit) = KTableForSelect<T>().block(this)
    }
}