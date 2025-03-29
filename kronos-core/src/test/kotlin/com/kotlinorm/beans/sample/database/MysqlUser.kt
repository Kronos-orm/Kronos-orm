package com.kotlinorm.beans.sample.database

import com.kotlinorm.annotations.*
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.enums.KColumnType.TINYINT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.enums.Mysql
import java.time.LocalDateTime

@Table(name = "tb_user")
@TableIndex("idx_username", ["username"], Mysql.KIndexType.NORMAL, Mysql.KIndexMethod.BTREE)
@TableIndex(name = "idx_multi", columns = ["id", "username"], type = "UNIQUE", method = "BTREE")
data class MysqlUser(
    @PrimaryKey(identity = true)
    var id: Int? = null,

    @ColumnType(VARCHAR, 254)
    var username: String? = null,

    var score: Int? = null,

    @Column("gender")
    @ColumnType(TINYINT)
    @Default("0")
    var gender: Int? = null,

//    @ColumnType(INT)
//    var age: Int? = null,
    @CreateTime
    @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
    @Necessary
    var createTime: String? = null,

    @UpdateTime
    @Necessary
    var updateTime: LocalDateTime? = null,

    @LogicDelete
    @Necessary
    @Default("0")
    var deleted: Boolean? = null
) : KPojo