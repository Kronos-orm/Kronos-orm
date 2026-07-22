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

// Verifies only otherwise factory-eligible inaccessible KPojo declarations fail in FIR.

import com.kotlinorm.interfaces.KPojo

private data class <!KRONOS_INACCESSIBLE_KPOJO_FACTORY!>PrivateTopLevelKPojo<!>(
    var id: Int? = null,
) : KPojo

private class <!KRONOS_INACCESSIBLE_KPOJO_FACTORY!>PrivateImplicitConstructorKPojo<!> : KPojo

private class PrivateRequiredConstructorKPojo(val id: Int) : KPojo

private object PrivateOwner {
    data class <!KRONOS_INACCESSIBLE_KPOJO_FACTORY!>PrivateNestedKPojo<!>(
        var id: Int? = null,
    ) : KPojo

    class <!KRONOS_INACCESSIBLE_KPOJO_FACTORY!>DefaultConstructorKPojo<!>(
        var id: Int = 0,
    ) : KPojo

    class <!KRONOS_INACCESSIBLE_KPOJO_FACTORY!>SecondaryNoArgKPojo<!> private constructor(
        val id: Int,
    ) : KPojo {
        internal constructor() : this(0)
    }

    abstract class AbstractKPojo : KPojo

    sealed class SealedKPojo : KPojo

    object ObjectKPojo : KPojo

    interface KPojoContract : KPojo

    class RequiredConstructorKPojo(val id: Int) : KPojo

    class PartiallyDefaultedKPojo(val id: Int, val name: String = "") : KPojo

    class PrivateConstructorKPojo private constructor() : KPojo

    open class ProtectedConstructorKPojo protected constructor() : KPojo

    class <!KRONOS_GENERIC_KPOJO_NOT_SUPPORTED!>GenericKPojo<!><T>(
        var value: T? = null,
    ) : KPojo
}

private class PrivateClassOwner {
    class PublicMiddle {
        class <!KRONOS_INACCESSIBLE_KPOJO_FACTORY!>DeeplyNestedKPojo<!> : KPojo
    }
}

class PublicOwner {
    private inner class InnerKPojo : KPojo
}

internal object InternalOwner {
    private class <!KRONOS_INACCESSIBLE_KPOJO_FACTORY!>PrivateNestedKPojo<!> : KPojo
}

internal data class InternalKPojo(var id: Int? = null) : KPojo

data class PublicKPojo(var id: Int? = null) : KPojo

fun localKPojoDoesNotRequireProviderFactory() {
    class LocalKPojo(var id: Int? = null) : KPojo
    LocalKPojo()
}
