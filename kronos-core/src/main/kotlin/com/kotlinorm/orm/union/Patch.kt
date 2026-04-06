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

package com.kotlinorm.orm.union

import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.interfaces.KPojo

/**
 * Create a UNION clause from multiple selectable queries.
 *
 * Example:
 * ```kotlin
 * val result = union(
 *     User().select().where { it.id == 1 },
 *     User().select().where { it.id == 2 },
 *     Customer().select().limit(1)
 * ).query()
 * ```
 *
 * @param selectables The selectable queries to union
 * @return UnionClause for further configuration and execution
 */
fun union(vararg selectables: KSelectable<out KPojo>): UnionClause {
    return UnionClause(selectables.toList(), initialUnionAll = false)
}

/**
 * Infix function to chain UNION operations (removes duplicates).
 *
 * Example:
 * ```kotlin
 * val result = (User().select().where { it.id == 1 }
 *     union User().select().where { it.id == 2 }
 *     union Customer().select().limit(1)).query()
 * ```
 *
 * @param other The selectable query to union with this one
 * @return UnionClause for further configuration and execution
 */
infix fun <T : KPojo> KSelectable<T>.union(other: KSelectable<out KPojo>): UnionClause {
    return UnionClause(listOf(this, other), initialUnionAll = false)
}

/**
 * Infix function to chain UNION ALL operations (includes duplicates).
 *
 * Example:
 * ```kotlin
 * val result = (User().select().where { it.id == 1 }
 *     unionAll User().select().where { it.id == 2 }
 *     unionAll Customer().select().limit(1)).query()
 * ```
 *
 * @param other The selectable query to union with this one
 * @return UnionClause for further configuration and execution
 */
infix fun <T : KPojo> KSelectable<T>.unionAll(other: KSelectable<out KPojo>): UnionClause {
    return UnionClause(listOf(this, other), initialUnionAll = true)
}

/**
 * Infix function to add another query to an existing union with UNION (removes duplicates).
 *
 * Example:
 * ```kotlin
 * val result = (User().select().where { it.id == 1 }
 *     union User().select().where { it.id == 2 }
 *     union Customer().select().limit(1)).query()
 * ```
 *
 * @param other The selectable query to add to this union
 * @return UnionClause for further configuration and execution
 */
infix fun UnionClause.union(other: KSelectable<out KPojo>): UnionClause {
    return this.addQueryWithUnion(other)
}

/**
 * Infix function to add another query to an existing union with UNION ALL (includes duplicates).
 *
 * Example:
 * ```kotlin
 * val result = (User().select().where { it.id == 1 }
 *     unionAll User().select().where { it.id == 2 }
 *     unionAll Customer().select().limit(1)).query()
 * ```
 *
 * @param other The selectable query to add to this union
 * @return UnionClause for further configuration and execution
 */
infix fun UnionClause.unionAll(other: KSelectable<out KPojo>): UnionClause {
    return this.addQueryWithUnionAll(other)
}
