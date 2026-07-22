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

// Verifies concrete KType and metadata bodies generated for a basic KPojo.

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import kotlin.reflect.typeOf

@Table("tb_official_user")
data class OfficialUser(
    @PrimaryKey
    var id: Int? = null,
    var name: String? = null,
) : KPojo

fun box(): String {
    val user = OfficialUser(7, "Ada")
    val map = user.toDataMap()
    val columnNames = user.__columns.map { it.name }.toSet()

    return when {
        user.__kType != typeOf<OfficialUser>() -> "Fail: KType was ${user.__kType}"
        user.__tableName != "tb_official_user" -> "Fail: table name was ${user.__tableName}"
        map["id"] != 7 -> "Fail: id was ${map["id"]}"
        map["name"] != "Ada" -> "Fail: name was ${map["name"]}"
        "id" !in columnNames -> "Fail: missing id column"
        "name" !in columnNames -> "Fail: missing name column"
        else -> "OK"
    }
}
