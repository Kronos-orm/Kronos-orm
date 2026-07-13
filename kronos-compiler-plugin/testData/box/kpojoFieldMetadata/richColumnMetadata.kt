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

// Verifies richer column metadata including primary-key variants, scale, and cascade exclusion.

import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KPojo

data class RichColumnAddress(
    var id: Int? = null,
    var city: String? = null,
) : KPojo

data class RichColumnUser(
    @PrimaryKey(uuid = true)
    var uuid: String? = null,
    @PrimaryKey(snowflake = true)
    var snowflakeId: Long? = null,
    @PrimaryKey(custom = true)
    var customId: String? = null,
    @ColumnType(KColumnType.DECIMAL, length = 12, scale = 4)
    var balance: java.math.BigDecimal? = null,
    @Cascade(["id"], ["userId"], usage = [KOperationType.SELECT])
    var address: RichColumnAddress? = null,
) : KPojo

fun box(): String {
    val columns = RichColumnUser().__columns
    fun column(name: String): com.kotlinorm.beans.dsl.Field {
        return columns.singleOrNull { it.name == name }
            ?: error("missing column $name")
    }

    val names = columns.map { it.name }
    val uuid = column("uuid")
    val snowflake = column("snowflakeId")
    val custom = column("customId")
    val balance = column("balance")
    val address = column("address")

    return when {
        uuid.primaryKey != PrimaryKeyType.UUID -> "Fail: uuid primary key was ${uuid.primaryKey}"
        snowflake.primaryKey != PrimaryKeyType.SNOWFLAKE -> "Fail: snowflake primary key was ${snowflake.primaryKey}"
        custom.primaryKey != PrimaryKeyType.CUSTOM -> "Fail: custom primary key was ${custom.primaryKey}"
        balance.type != KColumnType.DECIMAL -> "Fail: balance type was ${balance.type}"
        balance.length != 12 -> "Fail: balance length was ${balance.length}"
        balance.scale != 4 -> "Fail: balance scale was ${balance.scale}"
        address.cascade == null -> "Fail: cascade metadata was null"
        address.cascade?.usage?.toList() != listOf(KOperationType.SELECT) -> "Fail: cascade usage was ${address.cascade?.usage?.toList()}"
        else -> "OK"
    }
}
