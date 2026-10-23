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

package com.kotlinorm.orm.update

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSelect
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@PublishedApi
internal fun <T : KPojo> T.updateWithType(
    targetType: KType,
    fields: ToSelect<T, Any?> = null
): UpdateClause<T> {
    return UpdateClause(this, targetType, fields)
}

inline fun <reified T : KPojo> T.update(noinline fields: ToSelect<T, Any?> = null): UpdateClause<T> {
    return updateWithType(typeOf<T>(), fields)
}
