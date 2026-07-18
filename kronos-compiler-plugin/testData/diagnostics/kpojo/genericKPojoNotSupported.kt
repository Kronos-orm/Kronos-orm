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

// Verifies generic KPojo declarations fail in FIR while ordinary generic classes remain valid.

import com.kotlinorm.interfaces.KPojo

data class PlainGeneric<T>(val value: T)

data <!KRONOS_GENERIC_KPOJO_NOT_SUPPORTED!>class GenericDataKPojo<!><T>(
    var value: T? = null,
) : KPojo

<!KRONOS_GENERIC_KPOJO_NOT_SUPPORTED!>class GenericClassKPojo<!><T : Number>(
    var value: T? = null,
) : KPojo

open class ConcreteKPojo(
    var id: Int? = null,
) : KPojo

<!KRONOS_GENERIC_KPOJO_NOT_SUPPORTED!>class IndirectGenericKPojo<!><T>(
    var value: T? = null,
) : ConcreteKPojo()

fun validPlainGenericUsage(): PlainGeneric<String> = PlainGeneric("ok")
