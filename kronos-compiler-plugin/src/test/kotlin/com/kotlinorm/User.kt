package com.kotlinorm

import com.kotlinorm.annotations.ColumnDeserialize
import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.Reference
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.join.join
import java.util.*

@Table(name = "tb_user")
data class User(
    var id: Int? = null,
    @ColumnDeserialize
    var username: String? = null,
    var gender: Int? = null,
    @CreateTime
    var createTime: Date? = null,
    var friendId: Int? = null,
) : KPojo() {
    @Reference(["friendId"], ["id"])
    lateinit var friend: User
}

data class UserRelation(
    var id: Int? = null,
    var username: String? = null,
    var gender: Int? = null,
    var id2: Int? = null
) : KPojo()


fun main() {
    Kronos.apply {
        fieldNamingStrategy = LineHumpNamingStrategy
        tableNamingStrategy = LineHumpNamingStrategy
    }

    data class C(val id: Int? = null)

    val c = C(1)
    val (sql, paramMap) =
        User(1).join(
            UserRelation(1, "123", 1, 1),
        ) { user, relation ->
            leftJoin(relation) {
                user.id > 1 &&
                        user.id <= 1 &&
                        1 > user.id &&
                        1 <= user.id &&
                        user.id == 1 &&
                        user.id != 1 &&
                        c.id >= user.id &&
                        user.id > relation.id2.value &&
                        relation.id2.value >= user.id
            }
            select {
                user.id + relation.gender
            }
            where { user.id == 1 }
            orderBy { user.id.desc() }
        }.build()
}