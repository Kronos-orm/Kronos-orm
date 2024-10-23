package com.kotlinorm.database.beans

import com.kotlinorm.annotations.*
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.enums.KColumnType.*
import com.kotlinorm.enums.SqlServer
import java.time.LocalDateTime

@Table(name = "tb_user")
@TableIndex("idx_username", ["username"], SqlServer.KIndexType.NONCLUSTERED, SqlServer.KIndexMethod.UNIQUE)
@TableIndex("idx_username_createTime", ["username", "create_time"], SqlServer.KIndexType.NONCLUSTERED)
@TableIndex(name = "idx_multi", columns = ["xml"], type = "XML")
data class SsqlUser(
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
    @NotNull
    var createTime: String? = null,
    @UpdateTime
    @NotNull
    var updateTime: LocalDateTime? = null,
    @LogicDelete
    @NotNull
    var deleted: Boolean? = null,
    @ColumnType(XML)
    var xml: String? = null
) : KPojo