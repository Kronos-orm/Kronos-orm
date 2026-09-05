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

// Verifies class-level and property-level common strategy disable metadata.

import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.annotations.Version
import com.kotlinorm.interfaces.KPojo

@CreateTime(false)
data class StrategyClassDisabledUser(
    var id: Int? = null,
    var createdAt: String? = null,
) : KPojo

data class StrategyPropertyDisabledUser(
    var id: Int? = null,
    @UpdateTime(false)
    var updatedAt: String? = null,
    @LogicDelete(false)
    var deleted: Boolean? = null,
    @Version(false)
    var version: Int? = null,
) : KPojo

fun expectStrategyDisable(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun box(): String {
    val classDisabled = StrategyClassDisabledUser().__createTime
    val propertyDisabled = StrategyPropertyDisabledUser()
    val updateTime = propertyDisabled.__updateTime
    val logicDelete = propertyDisabled.__logicDelete
    val optimisticLock = propertyDisabled.__optimisticLock

    val failures = listOfNotNull(
        expectStrategyDisable(!classDisabled.enabled) { "class createTime enabled" },
        expectStrategyDisable(classDisabled.field.name == "") { "class createTime field was ${classDisabled.field.name}" },
        expectStrategyDisable(!updateTime.enabled) { "updateTime enabled" },
        expectStrategyDisable(updateTime.field.name == "updatedAt") { "updateTime field was ${updateTime.field.name}" },
        expectStrategyDisable(!logicDelete.enabled) { "logicDelete enabled" },
        expectStrategyDisable(logicDelete.field.name == "deleted") { "logicDelete field was ${logicDelete.field.name}" },
        expectStrategyDisable(!optimisticLock.enabled) { "optimisticLock enabled" },
        expectStrategyDisable(optimisticLock.field.name == "version") { "optimisticLock field was ${optimisticLock.field.name}" },
    )

    return failures.firstOrNull() ?: "OK"
}
