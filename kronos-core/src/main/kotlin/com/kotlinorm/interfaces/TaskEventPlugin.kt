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

package com.kotlinorm.interfaces

import com.kotlinorm.types.ActionTaskEvent
import com.kotlinorm.types.QueryTaskEvent

/**
 * Task Event Plugin
 *
 * Interface for Task Event Plugin
 *
 * @author OUSC
 */
interface TaskEventPlugin {
    val doBeforeQuery: QueryTaskEvent?
    val doAfterQuery: QueryTaskEvent?
    val doBeforeAction: ActionTaskEvent?
    val doAfterAction: ActionTaskEvent?
}