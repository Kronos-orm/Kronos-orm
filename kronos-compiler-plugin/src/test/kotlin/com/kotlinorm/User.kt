package com.kotlinorm

import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.delete.DeleteClause.Companion.build
import com.kotlinorm.orm.delete.DeleteClause.Companion.by
import com.kotlinorm.orm.delete.delete

@Table(name = "tb_user")
data class User(
    var id: Int? = null,
    var username: String? = null,
    var gender: Int? = null
) : KPojo()

fun main() {
    Kronos.apply {
        fieldNamingStrategy = LineHumpNamingStrategy
        tableNamingStrategy = LineHumpNamingStrategy
        updateTimeStrategy = KronosCommonStrategy(false, Field("update_time"))
    }

    val user = User(1)
    val testUser = User(1, "test")

    val (sql, paramMap) = listOf(user, testUser).delete().by { it.id }.build()
}