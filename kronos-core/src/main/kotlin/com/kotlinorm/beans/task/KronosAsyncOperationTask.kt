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

package com.kotlinorm.beans.task

import com.kotlinorm.interfaces.KAtomicTask
import com.kotlinorm.interfaces.KronosSQLTask

/**
 * Kronos Async Operation Task
 *
 * Operation Task with async = true
 *
 * @property tasks the atomic tasks list
 * @property async whether the operation is async
 *
 * @author OUSC
 * @create 2024/4/18 23:01
 */
class KronosAsyncOperationTask(
    override val tasks: List<KAtomicTask>,
    override val async: Boolean = true
) : KronosSQLTask