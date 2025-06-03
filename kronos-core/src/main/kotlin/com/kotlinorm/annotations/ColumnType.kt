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

import com.kotlinorm.enums.KColumnType

/**
 * Column
 *
 * Annotation to specify the name of a column in a database table.
 *
 * @property type The type of the column, if empty, inferred by Kotlin Type
 * @property length The length of the column in the database table.
 * @property scale The scale of the column in the database table.
 * @author OUSC
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ColumnType(val type: KColumnType, val length: Int = 0, val scale: Int = 0)