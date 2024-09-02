package com.kotlinorm.orm.relationQuery

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.database.table
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.relationQuery.manyToMany.AuthUser
import com.kotlinorm.orm.relationQuery.manyToMany.Permission
import com.kotlinorm.orm.relationQuery.manyToMany.Role
import com.kotlinorm.orm.relationQuery.manyToMany.RolePermissionRelation
import com.kotlinorm.orm.relationQuery.oneToMany.GroupClass
import com.kotlinorm.orm.relationQuery.oneToMany.School
import com.kotlinorm.orm.relationQuery.oneToMany.Student
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.utils.GsonResolver
import org.apache.commons.dbcp2.BasicDataSource
import kotlin.test.Test

class RelationQuery {
    private val ds = BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver"
        url = "jdbc:mysql://localhost:3306/test"
        username = "root"
        password = "******"
    }

    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
            dataSource = { KronosBasicWrapper(ds) }
            serializeResolver = GsonResolver
        }
    }

    @Test
    fun testCascadeInsert() {
        dataSource.table.dropTable<School>()
        dataSource.table.dropTable<GroupClass>()
        dataSource.table.dropTable<Student>()
        dataSource.table.createTable<School>()
        dataSource.table.createTable<GroupClass>()
        dataSource.table.createTable<Student>()

        val school = School(
            id = 1, name = "School", groupClass = listOf(
                GroupClass(
                    id = 11, name = "一年级", students = listOf(
                        Student(name = "张三", studentNo = "2021001"), Student(
                            name = "李四", studentNo = "2021002"
                        )
                    )
                ),
                GroupClass(
                    name = "三年级", students = listOf(
                        Student(
                            name = "孙七", studentNo = "2023001"
                        ), Student(
                            name = "周八", studentNo = "2023002"
                        )
                    )
                ),
                GroupClass(
                    id = 42, name = "二年级", students = listOf(
                        Student(
                            name = "王五", studentNo = "2022001"
                        ), Student(
                            name = "赵六", studentNo = "2022002"
                        )
                    )
                ),
            )
        )

        school.insert().execute()
    }

    @Test
    fun testCascadeUpdate() {
        testCascadeInsert()
        val res = School(name = "School")
            .update()
            .set { it.name = "School2" }
            .execute()
        println(res)
    }

    @Test
    fun testCascadeDelete() {
        testCascadeInsert()
        val result = School(name = "School").select().queryList()
        println(result)
        val res = School(name = "School").delete().execute()
        println(res)
    }

    @Test
    fun testSelect() {
        testCascadeInsert()
        val result = School(name = "School").select().queryList()
        println(result)
    }

    @Test
    fun testRevertSelect() {
        dataSource.table.dropTable<School>()
        dataSource.table.dropTable<GroupClass>()
        dataSource.table.dropTable<Student>()
        dataSource.table.createTable<School>()
        dataSource.table.createTable<GroupClass>()
        dataSource.table.createTable<Student>()

        val student = Student(name = "张三", studentNo = "2021001")
        val groupClass = GroupClass(name = "一年级", students = listOf(student))
        val school = School(name = "School", groupClass = listOf(groupClass))

        school.insert().execute()

        val groupClassQ = groupClass.select().queryOne()
        val schoolQ = school.select().queryOne()

        school.delete().execute()

        print(groupClassQ)
    }

    @Test
    fun testManyToMany() {
        dataSource.table.dropTable<Role>()
        dataSource.table.dropTable<RolePermissionRelation>()
        dataSource.table.dropTable<Permission>()
        dataSource.table.createTable<Role>()
        dataSource.table.createTable<RolePermissionRelation>()
        dataSource.table.createTable<Permission>()

        val role = Role(
            name = "admin"
        ).apply {
            permissions = listOf(
                Permission(name = "test"),
                Permission(name = "test2"),
                Permission(name = "test3")
            )
        }

        role.insert().execute()
        val permissions = Role(name = "admin").select().queryOne().permissions

        println(permissions)
    }

    @Test
    fun testAuthUser() {
        dataSource.table.dropTable<AuthUser>()
        dataSource.table.dropTable<Role>()
        dataSource.table.dropTable<RolePermissionRelation>()
        dataSource.table.dropTable<Permission>()
        dataSource.table.createTable<AuthUser>()
        dataSource.table.createTable<Role>()
        dataSource.table.createTable<RolePermissionRelation>()
        dataSource.table.createTable<Permission>()

        val role = Role(
            name = "admin"
        ).apply {
            permissions = listOf(
                Permission(name = "test"),
                Permission(name = "test2"),
                Permission(name = "test3")
            )
        }

        val (lastInsertId) = role.insert().execute()

        val user = AuthUser(
            name = "admin",
            roleId = lastInsertId
        )
        user.insert().execute()
        val authU = AuthUser(name = "admin").select().queryOne()

        println(authU)
    }
}