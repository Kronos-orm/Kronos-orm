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

import com.kotlinorm.Kronos.fieldNamingStrategy
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KColumnType.UNDEFINED
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KPojo
import kotlin.reflect.KClass

/**
 * Field
 *
 * Basic class of Kronos, which represents the field of a database table.
 *
 * @property columnName the name of the column in the database table
 * @property name the name of the field in Kotlin
 * @property type [KColumnType] of the field, default is UNDEFINED
 * @property primaryKey whether the field is a primary key and primary key type: none, default, identity, uuid, snowflake
 * @property dateFormat the format of the date field
 * @property tableName the name of the table
 * @property cascade the cascade of the field
 * @property cascadeIsCollectionOrArray whether the cascade field is a collection or array
 * @property kClass the class of the cascade field
 * @property isColumn whether the field is a column of database, KPojo/Collection<KPojo> fields are not columns of database
 * @property length the length of the field
 * @property defaultValue the default value of the field
 * @property nullable whether the field is nullable
 * @property ignore whether the field should be ignored in some operations
 *
 * @author: OUSC
 */
open class Field(
    var columnName: String,
    var name: String = fieldNamingStrategy.db2k(columnName),
    val type: KColumnType = UNDEFINED,
    var primaryKey: PrimaryKeyType = PrimaryKeyType.NOT,
    val dateFormat: String? = null,
    var tableName: String = "",
    val cascade: KCascade? = null,
    val cascadeIsCollectionOrArray: Boolean = false,
    val kClass: KClass<KPojo>? = null,
    val superTypes: List<String> = emptyList(),
    val ignore: Array<IgnoreAction>? = null,
    val isColumn: Boolean = true,
    val length: Int = 0,
    val defaultValue: String? = null,
    val nullable: Boolean = true,
    val serializable: Boolean = false,
    val kDoc: String? = null
) {
    // Returns the name of the field as a string
    override fun toString(): String {
        return name
    }

    /**
     * Check if this object is equal to another object.
     *
     * @param other the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        other as Field
        if (columnName != other.columnName) return false
        if (name != other.name) return false
        if (tableName != other.tableName) return false
        return true
    }

    /**
     * Calculates the hash code value for the object.
     *
     * @return the hash code value of the object.
     */
    override fun hashCode(): Int {
        var result = columnName.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + tableName.hashCode()
        return result
    }

    /**
     * Concatenates the current field name with the given string and returns a new Field object.
     *
     * @param other the string to concatenate with the field name
     * @return a new Field object with the concatenated name
     */
    operator fun plus(other: String?): Field = Field(
        columnName,
        name + other
    )

    fun refUseFor(usage: KOperationType): Boolean {
        return cascade != null && cascade.usage.contains(usage)
    }

    fun copy(
        columnName: String = this.columnName,
        name: String = this.name,
        type: KColumnType = this.type,
        primaryKey: PrimaryKeyType = this.primaryKey,
        dateFormat: String? = this.dateFormat,
        tableName: String = this.tableName,
        cascade: KCascade? = this.cascade,
        cascadeIsCollectionOrArray: Boolean = this.cascadeIsCollectionOrArray,
        kClass: KClass<KPojo>? = this.kClass,
        superTypes: List<String> = this.superTypes,
        ignore: Array<IgnoreAction>? = this.ignore,
        isColumn: Boolean = this.isColumn,
        length: Int = this.length,
        defaultValue: String? = this.defaultValue,
        nullable: Boolean = this.nullable
    ): Field {
        return Field(
            columnName,
            name,
            type,
            primaryKey,
            dateFormat,
            tableName,
            cascade,
            cascadeIsCollectionOrArray,
            kClass,
            superTypes,
            ignore,
            isColumn,
            length,
            defaultValue,
            nullable
        )
    }
}