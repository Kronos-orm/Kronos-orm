package com.kotlinorm.orm.beans

import com.kotlinorm.annotations.*
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.KColumnType.TINYINT
import java.time.LocalDateTime

@Table(name = "tb_user")
@TableIndex("idx_username", ["name"], "UNIQUE")
@TableIndex(name = "idx_multi", columns = ["id", "name"], "UNIQUE")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @Column("name")
    @NotNull
    var username: String? = null,
    @ColumnType(TINYINT)
    @Default("0")
    var age: Int? = null,
    @CreateTime
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    var createTime: String? = null,
    @NotNull
    var companyId: Int? = null,
    @UpdateTime
    var updateTime: LocalDateTime? = null,
    @LogicDelete
    var deleted: Boolean? = null,
    @Version
    var version: Int? = null
) : KPojo