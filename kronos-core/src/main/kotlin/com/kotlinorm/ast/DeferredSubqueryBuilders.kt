/**
 * Copyright 2022-2026 kronos-orm
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

package com.kotlinorm.ast

import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.Field

internal fun KSelectable<*>.toSelectQueryRef(): SelectQueryRef = KSelectableQueryRef(this)

internal fun SelectQueryRef.toScalarSubqueryExpression(): DeferredSubqueryExpression.Scalar {
    return DeferredSubqueryExpression.Scalar(this)
}

internal fun KSelectable<*>.toScalarSubqueryExpression(): DeferredSubqueryExpression.Scalar {
    return toSelectQueryRef().toScalarSubqueryExpression()
}

internal fun Any?.toBuilderExpression(parameterName: String): Expression {
    return when (this) {
        is Expression -> this
        is Field -> FieldToExpressionConverter.fieldToExpression(this)
        is SelectQueryRef -> toScalarSubqueryExpression()
        is KSelectable<*> -> toScalarSubqueryExpression()
        else -> Parameter.NamedParameter(parameterName)
    }
}

internal fun Any?.requiresBuilderParameter(): Boolean {
    return this !is Expression && this !is Field && this !is SelectQueryRef && this !is KSelectable<*>
}

internal fun SelectQueryRef.toExistsSubqueryExpression(): DeferredSubqueryExpression.Exists {
    return DeferredSubqueryExpression.Exists(this)
}

internal fun KSelectable<*>.toExistsSubqueryExpression(): DeferredSubqueryExpression.Exists {
    return toSelectQueryRef().toExistsSubqueryExpression()
}

internal fun SelectQueryRef.toNotExistsSubqueryExpression(): DeferredSubqueryExpression.Exists {
    return DeferredSubqueryExpression.Exists(this, not = true)
}

internal fun KSelectable<*>.toNotExistsSubqueryExpression(): DeferredSubqueryExpression.Exists {
    return toSelectQueryRef().toNotExistsSubqueryExpression()
}

internal fun Expression.toInSubqueryExpression(query: SelectQueryRef): DeferredSubqueryExpression.In {
    return DeferredSubqueryExpression.In(value = this, query = query)
}

internal fun Expression.toInSubqueryExpression(query: KSelectable<*>): DeferredSubqueryExpression.In {
    return toInSubqueryExpression(query.toSelectQueryRef())
}

internal fun Expression.toNotInSubqueryExpression(query: SelectQueryRef): DeferredSubqueryExpression.In {
    return DeferredSubqueryExpression.In(value = this, query = query, not = true)
}

internal fun Expression.toNotInSubqueryExpression(query: KSelectable<*>): DeferredSubqueryExpression.In {
    return toNotInSubqueryExpression(query.toSelectQueryRef())
}
