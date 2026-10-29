package com.kotlinorm.testfixtures.ddl

import com.kotlinorm.annotations.*
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.enums.KColumnType.*
import java.time.LocalDateTime

@Table(name = "tb_user")
@TableIndex("idx_username", ["username"], "NONCLUSTERED", "UNIQUE")
@TableIndex("idx_username_createTime", ["username", "create_time"], "NONCLUSTERED")
@TableIndex(name = "idx_multi", columns = ["xml"], type = "XML")
data class MssqlDdlUser(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @ColumnType(VARCHAR, 254)
    var username: String? = null,
    @Column("gender1")
    @ColumnType(TINYINT, 1)
    @Default("0")
    var gender: Int? = null,
//    @ColumnType(INT)
//    var age: Int? = null,
    @CreateTime
    @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
    @NonNull
    var createTime: String? = null,
    @UpdateTime
    @NonNull
    var updateTime: LocalDateTime? = null,
    @LogicDelete
    @NonNull
    var deleted: Boolean? = null,
    @ColumnType(XML)
    var xml: String? = null
) : KPojo