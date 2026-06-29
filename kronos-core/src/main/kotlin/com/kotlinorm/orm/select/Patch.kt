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

package com.kotlinorm.orm.select

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSelect
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun <T : KPojo> T.select(fields: ToSelect<T, Any?> = null): SelectClause<T, T> {
    return SelectClause(this, fields, kClass() as KClass<T>)
}

@PublishedApi
internal fun <T : KPojo, R : KPojo> T.selectGeneratedProjection(
    projectionClass: KClass<R>,
    fields: ToSelect<T, Any?> = null
): SelectClause<T, R> {
    return SelectClause(this, fields, projectionClass)
}

@JvmName("selectProjection")
inline fun <reified T : KPojo, reified R : KPojo> T.select(
    projectionClass: KClass<R> = R::class,
    noinline fields: ToSelect<T, Any?> = null
): SelectClause<T, R> {
    return SelectClause(this, fields, projectionClass)
}

fun <T : KPojo> T.db(name: String) = this to name

@Suppress("UNCHECKED_CAST")
fun <T : KPojo> Pair<T, String>.select(fields: ToSelect<T, Any?> = null) =
    SelectClause(this.first, fields, this.first.kClass() as KClass<T>).db(this.second)

fun <T : KPojo, R : KPojo> Pair<T, String>.select(
    projectionClass: KClass<R>,
    fields: ToSelect<T, Any?> = null
) = SelectClause(this.first, fields, projectionClass).db(this.second)
