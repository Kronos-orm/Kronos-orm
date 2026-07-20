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

// Verifies projection refinement only claims the Kronos query APIs it owns.

private class LookalikeQuery {
    fun select(block: (Int) -> Int): Int = block(1)

    fun select(vararg values: Int): Int = values.sum()

    fun <T> toList(): List<T> = emptyList()

    fun first(): Int = 7

    fun firstOrNull(): Int? = null

    fun contains(value: Int): Boolean = value == 1

    fun limit(value: Int): LookalikeQuery = this

    fun insert(vararg values: Int): Int = values.size
}

fun box(): String {
    val lookalike = LookalikeQuery()
    val lambdaResult = lookalike.select { it + 1 }
    val varargResult = lookalike.select(2, 3)
    val genericRows = lookalike.toList<String>()
    val first = lookalike.first()
    val firstOrNull = lookalike.firstOrNull()
    val contains = lookalike.contains(1)
    val inserted = lookalike.insert(1, 2)
    lookalike.limit(1).firstOrNull()

    return if (
        lambdaResult == 2 &&
        varargResult == 5 &&
        genericRows.isEmpty() &&
        first == 7 &&
        firstOrNull == null &&
        contains &&
        inserted == 2
    ) {
        "OK"
    } else {
        "Fail: lookalike API results were $lambdaResult/$varargResult/$first/$firstOrNull/$contains/$inserted"
    }
}
