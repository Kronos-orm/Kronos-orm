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

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.wrappers

import com.kotlinorm.utils.EnumFactory
import com.kotlinorm.utils.GeneratedTypeProvider
import com.kotlinorm.utils.GeneratedTypeRegistrar
import kotlin.reflect.typeOf

class JdbcGeneratedTypeProvider : GeneratedTypeProvider {
    override val id: String = "kronos-jdbc-wrapper-tests"

    override fun contributeTo(registrar: GeneratedTypeRegistrar) {
        registrar.registerEnum(
            typeOf<JdbcStatus>(),
            listOf("READY", "ARCHIVED"),
            EnumFactory { name ->
                when (name) {
                    "READY" -> JdbcStatus.READY
                    "ARCHIVED" -> JdbcStatus.ARCHIVED
                    else -> null
                }
            }
        )
    }
}
