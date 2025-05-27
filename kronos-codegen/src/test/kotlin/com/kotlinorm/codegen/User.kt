package com.kotlinorm.codegen

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.Necessary
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import java.time.LocalDateTime

// Kotlin Data Class for MysqlUser
@Table(name = "tb_user")
@TableIndex(name = "idx_multi", columns = ["id", "username"], type = "UNIQUE", method = "BTREE")
@TableIndex(name = "idx_username", columns = ["username"], type = "NORMAL", method = "BTREE")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @ColumnType(type = KColumnType.VARCHAR, length = 254)
    var username: String? = null,
    var score: Int? = null,
    @Default("0")
    var gender: Boolean? = null,
    @Necessary
    @CreateTime
    var createTime: String? = null,
    @Necessary
    @UpdateTime
    var updateTime: LocalDateTime? = null,
    @Necessary
    @Default("0")
    @LogicDelete
    var deleted: Boolean? = null
) : KPojo