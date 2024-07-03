package com.kotlinorm.tableOperation.beans

import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KPojo
import java.time.LocalDateTime

@Table(name = "product_log")
data class ProductLog(
    @Column(name = "id")
    var id: Int? = null,

    @Column(name = "type")
    var type: Int? = null,

    @Column(name = "note_no")
    var noteNo: String? = null,

    @Column(name = "tech_module_name")
    var techModuleName: String? = null,

    @Column(name = "images")
    var images: Any? = null, // Assuming it's a JSON object or array

    @Column(name = "files")
    var files: Any? = null, // Assuming it's a JSON object or array

    @Column(name = "author")
    var author: String? = null,

    @Column(name = "create_time")
    var createTime: LocalDateTime? = null,

    @Column(name = "update_time")
    var updateTime: LocalDateTime? = null,

    @Column(name = "deleted")
    var deleted: Byte? = 0.toByte(), // Default to 0 for not deleted

    @Column(name = "status")
    var status: Int? = -1,

    @Column(name = "content")
    var content: String? = null,

    @Column(name = "content_analyzed")
    var contentAnalyzed: String? = null,

    @Column(name = "permission_type")
    var permissionType: Int? = null,

    @Column(name = "permission_value")
    var permissionValue: Any? = null, // Assuming it's a JSON object or array

    @Column(name = "remark_num")
    var remarkNum: Int? = 0,

    @Column(name = "uuid")
    var uuid: String? = null,

    @Column(name = "audit_roles")
    var auditRoles: Any? = null, // Assuming it's a JSON object or array
) : KPojo