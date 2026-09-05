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

package com.kotlinorm.beans.subquery

import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.interfaces.KPojo

@Table(name = "tb_subquery_user")
internal data class SubqueryUser(
    @PrimaryKey
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table(name = "tb_subquery_order")
internal data class SubqueryOrder(
    @PrimaryKey
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

@Table(name = "tb_subquery_logic_user")
internal data class SubqueryLogicUser(
    var id: Int? = null,
    @LogicDelete
    var deleted: Boolean? = null,
) : KPojo

@Table(name = "tb_subquery_order_archive")
internal data class SubqueryOrderArchive(
    @PrimaryKey
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

@Table(name = "tb_subquery_identity_archive")
internal data class SubqueryIdentityArchive(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

@Table(name = "tb_subquery_strategy_archive")
internal data class SubqueryStrategyArchive(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
    @Default("1")
    var flag: Int? = null,
    @CreateTime
    var createTime: String? = null,
    @UpdateTime
    var updateTime: String? = null,
    @LogicDelete
    var deleted: Int? = null,
) : KPojo
