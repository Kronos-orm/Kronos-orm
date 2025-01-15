package com.kotlinorm.orm.cascade.oneToMany

import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.DateTimeFormat
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.Cascade.Companion.RESERVED
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.enums.CascadeDeleteAction

data class Student(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var studentNo: String? = null,
    var schoolName: String? = null,
    var groupClassName: String? = null,
    @Cascade(
        ["groupClassName", "schoolName"],
        ["name", "schoolName"],
        CascadeDeleteAction.SET_DEFAULT,
        defaultValue = [RESERVED, "10"]
    )
    var groupClass: GroupClass? = null,
    @CreateTime
    @DateTimeFormat("yyyy@MM@dd HH:mm:ss")
    val createTime: String? = null
) : KPojo
