package com.kotlinorm

import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.annotations.UseSerializeResolver
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.join.join
import java.util.*

import com.kotlinorm.annotations.*
import com.kotlinorm.enums.KColumnType.CHAR
import com.kotlinorm.enums.SQLite
import java.time.LocalDateTime
@Table(name = "tb_user")
@TableIndex("aaa", ["username"], SQLite.KIndexType.BINARY, SQLite.KIndexMethod.UNIQUE)
@TableIndex(  "bbb",columns = ["username","gender"], type = SQLite.KIndexType.NOCASE)
@TableIndex(  "ccc",columns = ["gender"])
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
) : KPojo()
@Table(name = "tb_user")
@TableIndex(name = "idx_user_id", columns = ["id"], type = "UNIQUE", method = "BTREE")
data class User(
    var id: Int? = null,
    @UseSerializeResolver
    var username: String? = null,
    var gender: Int? = null,
    @CreateTime
    var createTime: Date? = null
) : KPojo()

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