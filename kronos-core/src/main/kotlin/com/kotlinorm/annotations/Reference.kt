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

package com.kotlinorm.annotations

import com.kotlinorm.enums.CascadeAction

/**
 * Annotation to specify the reference between two database tables.
 *
 * This annotation is used to define a reference from one database table to another.
 * It is applied to a property in a data class that represents a database table.
 * The property should be of type Array<String> and should contain the names of the columns in the referencing table.
 *
 * @property referenceColumns The names of the columns in the referencing table.
 * @property targetColumns The names of the columns in the referenced table.
 * @property cascade The cascade action to apply when a referenced row is deleted or updated. The default value is CASCADE.
 * @property defaultValue The default value to use when a referenced row is deleted or updated and the cascade action is SET DEFAULT.
 * @author OUSC
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Reference(
    val referenceColumns: Array<String>,
    val targetColumns: Array<String>,
    val cascade: String = CascadeAction.CASCADE,
    val defaultValue: String = "",
    val mapperBy: Array<String> = []
)