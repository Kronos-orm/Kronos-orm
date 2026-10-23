package com.kotlinorm.testfixtures.entities

import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.NonNull
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table(name = "tb_user")
data class DialectUser(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var username: String? = null,
    var score: Int? = null,
    @Column("gender")
    @Default("0")
    var gender: Int? = null,
    @LogicDelete
    @NonNull
    @Default("0")
    var deleted: Boolean? = null
) : KPojo
