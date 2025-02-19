package com.kotlinorm.orm.beans.sample

import com.kotlinorm.annotations.*
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.enums.KColumnType.TINYINT
import java.time.LocalDateTime

@Table(name = "tb_user")
@TableIndex("idx_username", ["username"], "UNIQUE")
@TableIndex(name = "idx_multi", columns = ["id", "username"], "UNIQUE")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @Necessary
    var username: String? = null,
    @ColumnType(TINYINT)
    @Default("0")
    var gender: Int? = null,
    @CreateTime
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    var createTime: String? = null,
    @UpdateTime
    var updateTime: LocalDateTime? = null,
    @LogicDelete
    var deleted: Boolean? = null
) : KPojo