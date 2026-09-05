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

package com.kotlinorm.compiler.fir

import org.jetbrains.kotlin.name.Name

/** Keeps requested projection names stable and suffixes later collisions with `_N`. */
internal fun List<KronosProjectionField>.withUniqueProjectionNames(): List<KronosProjectionField> {
    val reservedNames = mapTo(linkedSetOf()) { it.requestedName.asString() }
    val usedNames = linkedSetOf<String>()
    val nextSuffixByName = mutableMapOf<String, Int>()

    return map { field ->
        val requestedName = field.requestedName.asString()
        if (usedNames.add(requestedName)) {
            field
        } else {
            var suffix = nextSuffixByName.getOrDefault(requestedName, 1)
            var candidate: String
            do {
                candidate = "${requestedName}_${suffix++}"
            } while (candidate in reservedNames || candidate in usedNames)
            nextSuffixByName[requestedName] = suffix
            usedNames += candidate
            field.copy(
                name = Name.identifier(candidate),
                signature = "${field.signature}:output:$candidate"
            )
        }
    }
}
