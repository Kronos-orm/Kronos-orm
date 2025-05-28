package com.kotlinorm.beans.sample.codegen

import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table(name = "cg_student")
data class CgStudent(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var studentNo: String? = null,
    var schoolName: String? = null,
    var groupClassName: String? = null,
    @CreateTime
    var createTime: String? = null
) : KPojo