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

// Verifies the public filter contract rejects a null predicate.

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.filter
import com.kotlinorm.orm.select.select

@Table("tb_filter_rejects_null")
data class FilterRejectsNullUser(
    var id: Int? = null,
) : KPojo

fun invalidNullFilterPredicate() {
    FilterRejectsNullUser()
        .select()
        .filter(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}
