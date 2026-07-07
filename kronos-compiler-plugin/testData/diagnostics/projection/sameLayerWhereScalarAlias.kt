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

// Verifies same-layer where uses Source and cannot access scalar subquery aliases.

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_scalar_alias_user")
data class ScalarAliasUser(
    var id: Int? = null,
    var status: Int? = null,
) : KPojo

@Table("tb_scalar_alias_order")
data class ScalarAliasOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
) : KPojo

fun invalidSameLayerScalarAliasWhere() {
    ScalarAliasUser()
        .select {
            [
                it.id,
                (ScalarAliasOrder()
                    .select { order -> order.amount }
                    .where { order -> order.userId == it.id }
                    .limit(1)).alias("lastAmount")
            ]
        }
        .where { it.<!UNRESOLVED_REFERENCE!>lastAmount<!> > 10 }
}
