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

// Verifies required scalar enum metadata rejects inaccessible types without rejecting serialized enum values.

import com.kotlinorm.annotations.Serialize
import com.kotlinorm.database.SqlExecutor.queryOne
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper

private enum class PrivatePropertyStatus { READY }

internal class PrivateEnumPropertyEntity : KPojo {
    <!KRONOS_INACCESSIBLE_ENUM_METADATA!>private var status: PrivatePropertyStatus = PrivatePropertyStatus.READY<!>
}

private enum class PrivateSerializedStatus { READY }

internal class SerializedPrivateEnumEntity : KPojo {
    @Serialize
    private var status: PrivateSerializedStatus? = null

    @Serialize
    private var statuses: List<PrivateSerializedStatus> = emptyList()
}

private object PrivateResultTypes {
    enum class HiddenStatus { HIDDEN }
}

@Suppress("UNUSED_PARAMETER", "unused")
private fun hiddenScalarResult(wrapper: KronosDataSourceWrapper): PrivateResultTypes.HiddenStatus =
    <!KRONOS_INACCESSIBLE_ENUM_METADATA!>wrapper.queryOne<PrivateResultTypes.HiddenStatus>("select status")<!>
