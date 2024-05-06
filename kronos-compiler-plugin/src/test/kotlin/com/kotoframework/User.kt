package com.kotoframework

import com.kotoframework.annotations.Table
import com.kotoframework.beans.namingStrategy.LineHumpNamingStrategy
import com.kotoframework.interfaces.KPojo
import com.kotoframework.orm.update.UpdateClause.Companion.by
import com.kotoframework.orm.update.UpdateClause.Companion.execute
import com.kotoframework.orm.update.update

@Table(name = "tb_user")
data class User(
    var id: Int? = null,
    var username: String? = null,
    var gender: Int? = null
) : KPojo

fun main() {
    Kronos.apply {
        fieldNamingStrategy = LineHumpNamingStrategy
        tableNamingStrategy = LineHumpNamingStrategy
    }

    val user = User(1)
    val testUser = User(1, "test")

    arrayOf(user, testUser).update { it.username }
        .by{ it.id }.execute()
}