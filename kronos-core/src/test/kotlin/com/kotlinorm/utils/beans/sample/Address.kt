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

package com.kotlinorm.utils.beans.sample

import com.kotlinorm.annotations.*
import com.kotlinorm.interfaces.KPojo
import java.time.LocalDateTime

@Table("tb_address")
data class Address(
    val id: Int? = null,
    val userId: Int? = null,
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    @Column("zip") val zipCode: String? = null,
    val country: String? = null,
    @UpdateTime
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val updateTime: String? = null,
    @CreateTime
    val createTime: LocalDateTime? = null,
    @Version
    val version: Int? = null,
    @LogicDelete
    val deleted: Boolean? = null,
) : KPojo