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

package com.kotlinorm.annotations

import com.kotlinorm.beans.dsl.KPojo
import kotlin.reflect.KClass

/**
 * Annotation to specify the type of referenced entity in a database table.
 *
 * This annotation is used to define the type of referenced entity from one database table to another.
 * It is applied to a property in a data class that represents a database table.
 * The property should be of type KClass<KPojo> and should represent the class of the referenced entity.
 *
 * @property className The class of the referenced entity.
 * @author OUSC
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReferenceType(
    val className: KClass<out KPojo>
)