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

// Verifies that function-local KPojo classes are included in the generated KClass creator map.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.KronosInit
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.kClassCreator

@KronosInit
fun customInit(block: Kronos.() -> Unit) = Kronos.apply(block)

fun box(): String {
    data class LocalCreator(var id: Int? = null) : KPojo

    customInit {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val local = kClassCreator(LocalCreator::class)

    return when (local) {
        is LocalCreator -> {
            local.id = 7
            if (local.id == 7) "OK" else "Fail: local id was ${local.id}"
        }
        else -> "Fail: local was ${local?.let { it::class.qualifiedName }}"
    }
}
