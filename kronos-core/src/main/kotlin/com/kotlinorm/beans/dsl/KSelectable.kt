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

package com.kotlinorm.beans.dsl

import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper

abstract class KSelectable<T : KPojo>(
    internal open val pojo: T
) {
    open var selectFields: LinkedHashSet<Field> = linkedSetOf()
    open var selectFunctions: LinkedHashSet<KTableForFunction> = linkedSetOf()
    abstract var selectAll: Boolean
    abstract fun build(wrapper: KronosDataSourceWrapper? = null): KronosQueryTask
}