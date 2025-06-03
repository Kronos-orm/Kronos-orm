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

package com.kotlinorm.annotations

import com.kotlinorm.enums.IgnoreAction

/**
 * Ignore
 *
 * Annotation to specify the property in KPojo to be ignored in some scenarios, such as cascade query.
 * The priority is lower than the .cascade(vararg [kotlin.reflect.KProperty]<*>) method.
 *
 * 注解用于指定KPojo中的某个属性在某些场景下是否被忽略，例如级联查询。
 * 优先级低于.cascade(vararg [kotlin.reflect.KProperty]<*>)方法。
 *
 * @author OUSC
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Ignore(val targets: Array<IgnoreAction> = [IgnoreAction.ALL])