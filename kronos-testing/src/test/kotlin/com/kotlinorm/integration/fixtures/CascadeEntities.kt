package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.CascadeDeleteAction
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.interfaces.KPojo

@Table("KT_CASCADE_INSERT_DEPT")
data class CascadeInsertDepartment(
    @PrimaryKey(identity = true)
    @Column("ID")
    @ColumnType(INT)
    var id: Int? = null,

    @Column("NAME")
    @ColumnType(VARCHAR, 80)
    var name: String? = null,

    var employees: List<CascadeInsertEmployee> = listOf(),
) : KPojo

@Table("KT_CASCADE_INSERT_EMPLOYEE")
data class CascadeInsertEmployee(
    @PrimaryKey(identity = true)
    @Column("ID")
    @ColumnType(INT)
    var id: Int? = null,

    @Column("DEPT_ID")
    @ColumnType(INT)
    var departmentId: Int? = null,

    @Column("NAME")
    @ColumnType(VARCHAR, 80)
    var name: String? = null,

    @Cascade(["departmentId"], ["id"])
    var department: CascadeInsertDepartment? = null,
) : KPojo

@Table("KT_CASCADE_DEPT")
data class CascadeDepartment(
    @PrimaryKey
    @Column("ID")
    @ColumnType(INT)
    var id: Int? = null,

    @Column("NAME")
    @ColumnType(VARCHAR, 80)
    var name: String? = null,

    var employees: List<CascadeEmployee> = listOf(),
) : KPojo

@Table("KT_CASCADE_EMPLOYEE")
data class CascadeEmployee(
    @PrimaryKey
    @Column("ID")
    @ColumnType(INT)
    var id: Int? = null,

    @Column("DEPT_ID")
    @ColumnType(INT)
    var departmentId: Int? = null,

    @Column("NAME")
    @ColumnType(VARCHAR, 80)
    var name: String? = null,

    @Cascade(["departmentId"], ["id"], onDelete = CascadeDeleteAction.SET_NULL)
    var department: CascadeDepartment? = null,
) : KPojo

@Table("KT_CASCADE_PROJECT")
data class CascadeProject(
    @PrimaryKey
    @Column("ID")
    @ColumnType(INT)
    var id: Int? = null,

    @Column("NAME")
    @ColumnType(VARCHAR, 80)
    var name: String? = null,

    var tasks: List<CascadeTask> = listOf(),
) : KPojo

@Table("KT_CASCADE_TASK")
data class CascadeTask(
    @PrimaryKey
    @Column("ID")
    @ColumnType(INT)
    var id: Int? = null,

    @Column("PROJECT_ID")
    @ColumnType(INT)
    var projectId: Int? = null,

    @Column("NAME")
    @ColumnType(VARCHAR, 80)
    var name: String? = null,

    @Cascade(["projectId"], ["id"], onDelete = CascadeDeleteAction.CASCADE)
    var project: CascadeProject? = null,
) : KPojo

data class CascadeDepartmentRecord(
    val id: Int?,
    val name: String?,
    val employees: List<CascadeEmployeeRecord>,
)

data class CascadeEmployeeRecord(
    val id: Int?,
    val departmentId: Int?,
    val name: String?,
)

data class CascadeProjectRecord(
    val id: Int?,
    val name: String?,
    val tasks: List<CascadeTaskRecord>,
)

data class CascadeTaskRecord(
    val id: Int?,
    val projectId: Int?,
    val name: String?,
)
