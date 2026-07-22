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

// Verifies separate KPojo transforms retain their concrete KType and metadata.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.statement.SqlQuery
import kotlin.reflect.typeOf

@Table("tb_symbol_user")
data class SymbolUser(var id: Int? = null, var name: String? = null) : KPojo

@Table("tb_symbol_post")
data class SymbolPost(var id: Int? = null, var userId: Int? = null, var title: String? = null) : KPojo

@Table("tb_symbol_comment")
data class SymbolComment(var id: Int? = null, var postId: Int? = null, var content: String? = null) : KPojo

fun box(): String {
    with(Kronos) {}

    val user = SymbolUser(1, "Ada")
    val post = SymbolPost(2, 1, "Notes")
    val comment = SymbolComment(3, 2, "OK")
    val userSelect = user.select { [it.id, it.name] }.toSqlQuery() as SqlQuery.Select
    val postSelect = post.select { [it.id, it.userId, it.title] }.toSqlQuery() as SqlQuery.Select
    val commentMap = comment.toDataMap()

    return when {
        user.__kType != typeOf<SymbolUser>() -> "Fail: user KType was ${user.__kType}"
        post.__kType != typeOf<SymbolPost>() -> "Fail: post KType was ${post.__kType}"
        comment.__kType != typeOf<SymbolComment>() -> "Fail: comment KType was ${comment.__kType}"
        user.__columns.map { it.name } != ["id", "name"] -> "Fail: user columns were ${user.__columns.map { it.name }}"
        post.__columns.map { it.name } != ["id", "userId", "title"] -> "Fail: post columns were ${post.__columns.map { it.name }}"
        comment.__columns.map { it.name } != ["id", "postId", "content"] -> "Fail: comment columns were ${comment.__columns.map { it.name }}"
        userSelect.select.size != 2 -> "Fail: user select size was ${userSelect.select.size}"
        postSelect.select.size != 3 -> "Fail: post select size was ${postSelect.select.size}"
        commentMap["content"] != "OK" -> "Fail: comment map was $commentMap"
        else -> "OK"
    }
}
