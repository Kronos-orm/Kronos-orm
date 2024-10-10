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

package com.kotlinorm.orm.insert

import com.kotlinorm.interfaces.KPojo


fun <T : KPojo> T.insert(): InsertClause<T> {
    return InsertClause(this)
}

fun <T : KPojo> Array<T>.insert(): List<InsertClause<T>> {
    return map { InsertClause(it) }
}

fun <T : KPojo> Iterable<T>.insert(): List<InsertClause<T>> {
    return map { InsertClause(it) }
}