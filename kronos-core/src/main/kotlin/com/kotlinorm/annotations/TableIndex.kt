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

/**
 * Table Index
 *
 * Annotation to specify the index of a table in a database.
 *
 * @property name The name of the index in the database.
 * @property columns The columns of the index in the database.
 * @property type The type of the index in the database.
 * @property method The method of the index in the database.
 * @property concurrently Whether to create the index concurrently, **only for PostgreSQL**.
 * @author OUSC
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class TableIndex(
    val name: String,
    val columns: Array<String>,
    val type: String = "",
    val method: String = "",
    val concurrently: Boolean = false
)