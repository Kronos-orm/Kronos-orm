package com.kotlinorm

import com.kotlinorm.annotations.*
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.strategies.LineHumpNamingStrategy
import com.kotlinorm.enums.KColumnType.CHAR
import com.kotlinorm.enums.SQLite
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.update.update
import java.time.LocalDateTime
import java.util.*

@Table(name = "tb_user")
@TableIndex("aaa", ["username"], SQLite.KIndexType.UNIQUE)
@TableIndex("bbb", columns = ["username", "gender"])
@TableIndex("ccc", columns = ["gender"])
data class SqlliteUser(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var username: String? = null,
    @Column("gender")
    @ColumnType(CHAR)
    @Default("0")
    var gender: Int? = null,
//    @ColumnType(INT)
//    var age: Int? = null,
    @CreateTime
    @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
    @NotNull
    var createTime: String? = null,
    @UpdateTime
    @NotNull
    var updateTime: LocalDateTime? = null,
    @LogicDelete
    @NotNull
    var deleted: Boolean? = null
) : KPojo

@Table(name = "tb_user")
@TableIndex(name = "idx_user_id", columns = ["id"], type = "UNIQUE", method = "BTREE")
data class User(
    var id: Int? = null,
    var username: String? = null,
    var gender: Int? = null,
    @CreateTime
    var createTime: Date? = null
) : KPojo

data class UserRelation(
    var id: Int? = null,
    var username: String? = null,
    var gender: Int? = null,
    var id2: Int? = null
) : KPojo


fun main() {
    Kronos.apply {
        fieldNamingStrategy = LineHumpNamingStrategy
        tableNamingStrategy = LineHumpNamingStrategy
    }

    data class C(val id: Int? = null)

    val c = C(1)
    val (sql, paramMap) =
        User(1).update().set {
            it["id"] += 1
        }.build()
}