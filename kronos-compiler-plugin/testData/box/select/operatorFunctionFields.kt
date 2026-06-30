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

// Verifies that legal binary operator select expressions become FunctionField projections.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSelect

@Table(name = "tb_select_operator")
data class OperatorSelectUser(
    var id: Int? = null,
    var score: Int = 0,
) : KPojo

fun OperatorSelectUser.collectOperatorSelect(block: ToSelect<OperatorSelectUser, Any?>): List<Field> {
    val result = mutableListOf<Field>()
    afterSelect {
        block!!(it)
        result += fields
    }
    return result
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val fields = OperatorSelectUser().collectOperatorSelect { [it.score + 10 + 20, it.score % 2] }
    val add = fields.getOrNull(0) as? FunctionField
    val nestedAdd = add?.fields?.getOrNull(0)?.first as? FunctionField
    val mod = fields.getOrNull(1) as? FunctionField

    val failures = listOfNotNull(
        expect(fields.size == 2) { "size was ${fields.size}" },
        expect(add?.functionName == "add") { "first function was ${add?.functionName}" },
        expect(nestedAdd?.functionName == "add") { "nested function was ${nestedAdd?.functionName}" },
        expect(nestedAdd?.fields?.getOrNull(0)?.first?.name == "score") {
            "nested first field was ${nestedAdd?.fields?.getOrNull(0)?.first?.name}"
        },
        expect(mod?.functionName == "mod") { "second function was ${mod?.functionName}" },
        expect(mod?.fields?.getOrNull(0)?.first?.name == "score") {
            "mod first field was ${mod?.fields?.getOrNull(0)?.first?.name}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
