package com.kotlinorm.beans.sample.databases

import com.kotlinorm.annotations.*
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.enums.KColumnType.TINYINT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.enums.Oracle
import java.time.LocalDateTime

@Table(name = "TB_USER") // Oracle通常建议表名大写
@TableIndex("idx_username", ["username"], Oracle.KIndexType.UNIQUE)
@TableIndex(name = "idx_multi", columns = ["id", "username"], type = "BITMAP")
data class OracleUser(
    @PrimaryKey(identity = true)
    var id: Int? = null, // 移除了identity=true，因为Oracle使用序列和触发器实现自增主键
    @ColumnType(VARCHAR, 254)
    var username: String? = null,
    @Column("gender1")
    @ColumnType(TINYINT)
    @Default("0")
    var gender: Int? = null,
    // 注释掉了age字段，如果需要可取消注释并调整类型
    // @ColumnType(INT)
    // var age: Int? = null,
    @CreateTime
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss") // 格式调整为通用格式，具体需根据实际库处理逻辑
    @Necessary
    var createTime: LocalDateTime? = null, // 更改类型为LocalDateTime以匹配Oracle的日期时间处理
    @UpdateTime
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @Necessary
    var updateTime: LocalDateTime? = null,
    @LogicDelete
    @Necessary
    var deleted: Boolean? = null
) : KPojo