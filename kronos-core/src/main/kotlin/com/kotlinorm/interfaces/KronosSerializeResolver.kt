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

import kotlin.reflect.KClass

/**
 * Kronos Serialize Resolver
 *
 * Serialize and deserialize object with specific class.
 * @author: OUSC
 */
interface KronosSerializeResolver {
    /**
     * Deserializes a string into an object of type T.
     *
     * @param serializedStr the string to deserialize
     * @param kClass the class of the object to deserialize into
     * @return the deserialized object of type T
     */
    fun <T> deserialize(serializedStr: String, kClass: KClass<*>): T

    /**
     * Serializes an object of type T into a string.
     *
     * @param obj the object to serialize
     * @return the serialized string
     */
    fun serialize(obj: Any): String
}