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

import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToSelect
import com.kotlinorm.utils.createInstance
import kotlin.reflect.KClass
import kotlin.reflect.KType
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

inline fun <reified T : KPojo> T.where(noinline selectCondition: ToFilter<T, Boolean?> = null): SelectClause<T, T, T> {
    return selectWithType(typeOf<T>(), typeOf<T?>()).where(selectCondition)
}

inline fun <reified S : KPojo> KSelectable<S>.select(
    noinline fields: ToSelect<S, Any?> = null
): SelectClause<S, S, S> {
    val selectedClass = selectedType.classifier as KClass<S>
    val source = selectedClass.createInstance()
    return SelectClause(
        source,
        fields,
        selectedType,
        nullableSelectedType,
        sourceQuery = this,
        sourceAlias = DerivedQueryAlias
    )
}

@PublishedApi
internal inline fun <T : KPojo, reified R : KPojo> T.selectGeneratedProjection(
    @Suppress("UNUSED_PARAMETER")
    projectionClass: KClass<R>,
    noinline fields: ToSelect<T, Any?> = null
): SelectClause<T, R, T> {
    return SelectClause(this, fields, typeOf<R>(), typeOf<R?>())
}

@PublishedApi
internal inline fun <T : KPojo, reified R : KPojo, C : KPojo> T.selectGeneratedProjection(
    @Suppress("UNUSED_PARAMETER")
    projectionClass: KClass<R>,
    contextClass: KClass<C>,
    noinline fields: ToSelect<T, Any?> = null
): SelectClause<T, R, C> {
    return SelectClause(this, fields, typeOf<R>(), typeOf<R?>(), contextClass.createInstance())
}

@PublishedApi
internal inline fun <S : KPojo, reified R : KPojo, C : KPojo> KSelectable<S>.selectGeneratedProjection(
    @Suppress("UNUSED_PARAMETER")
    projectionClass: KClass<R>,
    contextClass: KClass<C>,
    noinline fields: ToSelect<S, Any?> = null
): SelectClause<S, R, C> {
    val selectedClass = selectedType.classifier as KClass<S>
    val source = selectedClass.createInstance()
    return SelectClause(
        source,
        fields,
        typeOf<R>(),
        typeOf<R?>(),
        contextClass.createInstance(),
        sourceQuery = this,
        sourceAlias = DerivedQueryAlias
    )
}

@JvmName("selectProjection")
inline fun <reified T : KPojo, reified R : KPojo> T.select(
    @Suppress("UNUSED_PARAMETER")
    projectionClass: KClass<R> = R::class,
    noinline fields: ToSelect<T, Any?> = null
): SelectClause<T, R, T> {
    return SelectClause(this, fields, typeOf<R>(), typeOf<R?>())
}

fun <T : KPojo> T.db(name: String) = this to name

inline fun <reified T : KPojo> Pair<T, String>.select(noinline fields: ToSelect<T, Any?> = null) =
    SelectClause<T, T, T>(this.first, fields, typeOf<T>(), typeOf<T?>()).db(this.second)

inline fun <T : KPojo, reified R : KPojo> Pair<T, String>.select(
    @Suppress("UNUSED_PARAMETER")
    projectionClass: KClass<R>,
    noinline fields: ToSelect<T, Any?> = null
) = SelectClause<T, R, T>(this.first, fields, typeOf<R>(), typeOf<R?>()).db(this.second)
