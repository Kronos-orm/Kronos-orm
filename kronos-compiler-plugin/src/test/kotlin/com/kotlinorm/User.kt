package com.kotlinorm

import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UseSerializeResolver
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.Extensions.mapperTo

@Table(name = "tb_user")
data class User(
    var id: Int? = null,
    var username: String? = null,
    @UseSerializeResolver
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
    val m = mapOf("id" to 2)
    val u = m.mapperTo<User>()

    val (sql, paramMap) = user.select { it.id + it.username + it.gender }.build()
}