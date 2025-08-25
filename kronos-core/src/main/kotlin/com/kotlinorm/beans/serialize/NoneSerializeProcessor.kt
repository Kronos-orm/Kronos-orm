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

package com.kotlinorm.beans.serialize

import com.kotlinorm.interfaces.KronosSerializeProcessor
import kotlin.reflect.KClass

/**
 * None Serialize Resolver
 *
 * Default implement of [KronosSerializeProcessor], throw [UnsupportedOperationException]
 *
 * @author OUSC
 */
object NoneSerializeProcessor : KronosSerializeProcessor {
    override fun deserialize(serializedStr: String, kClass: KClass<*>): Any {
        throw UnsupportedOperationException()
    }

    override fun serialize(obj: Any): String {
        throw UnsupportedOperationException()
    }
}