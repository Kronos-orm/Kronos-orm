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

package com.kotlinorm.interfaces

import kotlin.reflect.KType

/**
 * Kronos Serialize Resolver
 *
 * Serialize and deserialize object with full Kotlin declaration type metadata.
 * @author: OUSC
 */
interface KronosSerializeProcessor {
    /**
     * Deserializes a string using the full Kotlin declaration type.
     *
     * @param serializedStr the string to deserialize
     * @param kType the Kotlin declaration type of the object to deserialize into
     * @return the deserialized object
     */
    fun deserialize(serializedStr: String, kType: KType): Any

    /**
     * Serializes an object using the field declaration type.
     *
     * @param obj the object to serialize
     * @param kType the Kotlin declaration type of the object to serialize
     * @return the serialized string
     */
    fun serialize(obj: Any, kType: KType): String
}
