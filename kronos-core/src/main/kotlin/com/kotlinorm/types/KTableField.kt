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

package com.kotlinorm.types

import com.kotlinorm.beans.dsl.KTableForCondition
import com.kotlinorm.beans.dsl.KTableForReference
import com.kotlinorm.beans.dsl.KTableForSelect
import com.kotlinorm.beans.dsl.KTableForSet
import com.kotlinorm.beans.dsl.KTableForSort


typealias ToSelect<T, R> = (KTableForSelect<T>.(it: T) -> R)?
typealias ToSet<T, R> = (KTableForSet<T>.(it: T) -> R)?
typealias ToSort<T, R> = (KTableForSort<T>.(it: T) -> R)?
typealias ToFilter<T, R> = (KTableForCondition<T>.(it: T) -> R)?
typealias ToReference<T, R> = (KTableForReference<T>.(it: T) -> R)?