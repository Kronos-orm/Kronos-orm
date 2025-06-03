/**
 * Copyright 2022-2025 kronos-orm
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

package com.kotlinorm.annotations

import com.kotlinorm.enums.CascadeDeleteAction
import com.kotlinorm.enums.KOperationType

/**
 * Annotation to specify the cascade between two database tables.
 *
 * This annotation is used to define a cascade from one database table to another.
 * It is applied to a property in a data class that represents a database table.
 * The property should be of type Array<String> and should contain the names of the columns in the referencing table.
 *
 * @property properties The names of the columns in the referencing table.
 * @property targetProperties The names of the columns in the referenced table.
 * @property onDelete The cascade action to apply when a referenced row is deleted or updated. The default value is CASCADE.
 * @property defaultValue The default value to use when a referenced row is deleted or updated and the cascade action is SET DEFAULT.
 * @property usage The usage of the cascade: insert, update, delete, select.
 * @author OUSC
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Cascade(
    val properties: Array<String>,
    val targetProperties: Array<String>,
    val onDelete: CascadeDeleteAction = CascadeDeleteAction.NO_ACTION,
    val defaultValue: Array<String> = [],
    val usage: Array<KOperationType> = [KOperationType.INSERT, KOperationType.UPDATE, KOperationType.DELETE, KOperationType.SELECT, KOperationType.UPSERT]
){
    companion object{
        const val RESERVED = "RESERVED"
    }
}