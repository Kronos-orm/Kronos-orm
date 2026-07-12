package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.annotations.Version
import com.kotlinorm.enums.KColumnType.BIT
import com.kotlinorm.enums.KColumnType.DATETIME
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.interfaces.KPojo
import java.time.LocalDateTime

@Table("kt_integration_strategy_account")
data class IntegrationStrategyAccount(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(VARCHAR, 80)
    var name: String? = null,

    @ColumnType(INT)
    var balance: Int? = null,

    @CreateTime
    @ColumnType(DATETIME)
    var createdAt: LocalDateTime? = null,

    @UpdateTime
    @ColumnType(DATETIME)
    var updatedAt: LocalDateTime? = null,

    @LogicDelete
    @ColumnType(BIT)
    var deleted: Boolean? = null,

    @Version
    @ColumnType(INT)
    var version: Int? = null,
) : KPojo
