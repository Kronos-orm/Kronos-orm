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

package com.kotlinorm.orm.upsert

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSelect


inline fun <reified T : KPojo> T.upsert(noinline setUpdateFields: ToSelect<T, Any?> = null): UpsertClause<T> {
    return UpsertClause(this, setUpdateFields)
}


// 添加批量upsert功能
inline fun <reified T : KPojo> Iterable<T>.upsert(noinline setUpdateFields: ToSelect<T, Any?> = null): List<UpsertClause<T>> {
    return map { entity ->
        UpsertClause(entity, setUpdateFields)
    }
}

// 对于Array类型的批量upsert功能
inline fun <reified T : KPojo> Array<T>.upsert(noinline setUpdateFields: ToSelect<T, Any?> = null): List<UpsertClause<T>> {
    return map { entity ->
        UpsertClause(entity, setUpdateFields)
    }
}