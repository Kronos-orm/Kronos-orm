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

import com.kotlinorm.Kronos
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToSelect
import com.kotlinorm.utils.KTypeKey
import com.kotlinorm.utils.createKPojo
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability
import kotlin.reflect.typeOf

@PublishedApi
internal const val DerivedQueryAlias = "q"

@PublishedApi
internal fun <T : KPojo> T.selectWithType(
    targetType: KType,
    nullableTargetType: KType = targetType,
    fields: ToSelect<T, Any?> = null
): SelectClause<T, T, T> = SelectClause(this, fields, targetType, nullableTargetType)

inline fun <reified T : KPojo> T.select(noinline fields: ToSelect<T, Any?> = null): SelectClause<T, T, T> {
    return selectWithType(typeOf<T>(), typeOf<T?>(), fields)
}

inline fun <reified T : KPojo> T.where(noinline selectCondition: ToFilter<T, Boolean?>? = null): SelectClause<T, T, T> {
    return selectWithType(typeOf<T>(), typeOf<T?>()).where(selectCondition)
}

inline fun <reified S : KPojo> KSelectable<S>.select(
    noinline fields: ToSelect<S, Any?> = null
): SelectClause<S, S, S> {
    val source = Kronos.createKPojo(selectedType) as S
    return SelectClause(
        source,
        fields,
        selectedType,
        nullableSelectedType,
        sourceQuery = this,
        sourceAlias = DerivedQueryAlias
    )
}

/**
 * Filters this query's selected result rows through a derived-query boundary.
 *
 * Unlike [SelectClause.where], whose receiver is the current query layer's source type,
 * this predicate receives only the [KSelectable] result type emitted by the current query.
 */
inline fun <reified Selected : KPojo> KSelectable<Selected>.filter(
    noinline predicate: ToFilter<Selected, Boolean?>
): SelectClause<Selected, Selected, Selected> = select().where(predicate)

@PublishedApi
internal inline fun <T : KPojo, reified R : KPojo, reified C : KPojo> T.selectGeneratedProjection(
    noinline fields: ToSelect<T, Any?> = null
): SelectClause<T, R, C> {
    val projectionType = typeOf<R>()
    @Suppress("UNCHECKED_CAST")
    val contextPojo = if (this is C) {
        this as C
    } else {
        createKPojo<C>()
    }
    return SelectClause(this, fields, projectionType, projectionType.withNullability(true), contextPojo)
}

@PublishedApi
internal inline fun <S : KPojo, reified R : KPojo, reified C : KPojo> KSelectable<S>.selectGeneratedProjection(
    noinline fields: ToSelect<S, Any?> = null
): SelectClause<S, R, C> {
    val source = Kronos.createKPojo(selectedType) as S
    val projectionType = typeOf<R>()
    return SelectClause(
        source,
        fields,
        projectionType,
        projectionType.withNullability(true),
        createKPojo<C>(),
        sourceQuery = this,
        sourceAlias = DerivedQueryAlias
    )
}

/**
 * Selects rows as [R] using a complete projection [KType].
 *
 * [projectionType] must equal `typeOf<R>()`; the nullable selected type is derived from it.
 *
 * @throws IllegalArgumentException when [projectionType] does not match the reified [R]
 */
@JvmName("selectProjection")
inline fun <reified T : KPojo, reified R : KPojo> T.select(
    projectionType: KType = typeOf<R>(),
    noinline fields: ToSelect<T, Any?> = null
): SelectClause<T, R, T> {
    val validatedType = requireProjectionType(projectionType, typeOf<R>())
    return SelectClause(this, fields, validatedType, validatedType.withNullability(true))
}

fun <T : KPojo> T.db(name: String) = this to name

inline fun <reified T : KPojo> Pair<T, String>.select(noinline fields: ToSelect<T, Any?> = null) =
    SelectClause<T, T, T>(this.first, fields, typeOf<T>(), typeOf<T?>()).db(this.second)

/**
 * Selects rows from a named database as [R] using a complete projection [KType].
 *
 * [projectionType] must equal `typeOf<R>()`; the nullable selected type is derived from it.
 *
 * @throws IllegalArgumentException when [projectionType] does not match the reified [R]
 */
inline fun <T : KPojo, reified R : KPojo> Pair<T, String>.select(
    projectionType: KType = typeOf<R>(),
    noinline fields: ToSelect<T, Any?> = null
) = requireProjectionType(projectionType, typeOf<R>()).let { validatedType ->
    SelectClause<T, R, T>(this.first, fields, validatedType, validatedType.withNullability(true)).db(this.second)
}

/**
 * Validates an explicit projection type against its reified result type.
 *
 * @return [projectionType] unchanged when both complete KTypes match
 * @throws IllegalArgumentException when the types differ
 */
@PublishedApi
internal fun requireProjectionType(projectionType: KType, reifiedType: KType): KType {
    require(sameProjectionType(projectionType, reifiedType)) {
        "Projection type $projectionType does not match reified result type $reifiedType"
    }
    return projectionType
}

@PublishedApi
internal fun sameProjectionType(
    first: KType,
    second: KType
): Boolean = KTypeKey.from(first) == KTypeKey.from(second)
