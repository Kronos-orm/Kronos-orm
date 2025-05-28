package com.kotlinorm.orm.cascade

import com.kotlinorm.GsonProcessor
import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.sample.manyToMany.Permission
import com.kotlinorm.beans.sample.manyToMany.Role
import com.kotlinorm.beans.sample.manyToMany.RolePermissionRelation
import com.kotlinorm.beans.sample.oneToMany.GroupClass
import com.kotlinorm.beans.sample.oneToMany.School
import com.kotlinorm.beans.sample.oneToMany.Student
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import org.apache.commons.dbcp2.BasicDataSource
import kotlin.io.print
import kotlin.io.println
import kotlin.test.Test

class RelationQuery {
    private val wrapper = BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver" // MySQL驱动类名，需根据实际数据库类型调整
        // 数据库URL
        url =
            "jdbc:mysql://localhost:3306/kronos_testing?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowMultiQueries=true&allowPublicKeyRetrieval=true&useServerPrepStmts=false&rewriteBatchedStatements=true"
        username = System.getenv("db.username") // 数据库用户名
        password = System.getenv("db.password") // 数据库密码
        maxIdle = 10 // 最大空闲连接数
    }.let {
        KronosBasicWrapper(it)
    }

    init {
        Kronos.init {
            fieldNamingStrategy = lineHumpNamingStrategy
            tableNamingStrategy = lineHumpNamingStrategy
            createTimeStrategy.enabled = false
            updateTimeStrategy.enabled = false
            logicDeleteStrategy.enabled = false
            optimisticLockStrategy.enabled = false
            dataSource = { wrapper }
            serializeProcessor = GsonProcessor
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
                        Student(name = "张三", studentNo = "2021001"),
                        Student(
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
    fun testReverseCascadeDelete() {
        testCascadeInsert()
        val result = School(name = "School").select().queryList()
        println(result)
        val student = School(name = "School").select().queryOne()
        val res = student.delete().execute()
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
    fun testToMapUseDelegate() {
        dataSource.table.dropTable<Role>()
        dataSource.table.dropTable<RolePermissionRelation>()
        dataSource.table.dropTable<Permission>()
        dataSource.table.createTable<Role>()
        dataSource.table.createTable<RolePermissionRelation>()
        dataSource.table.createTable<Permission>()

        val role = Role(
            name = "admin",
            rolePermissions = listOf(
                RolePermissionRelation(
                    permission = Permission(name = "test")
                ),
                RolePermissionRelation(
                    permission = Permission(name = "test2")
                ),
                RolePermissionRelation(
                    permission = Permission(name = "test3")
                )
            )
        )

        val a = role.toDataMap()
        println(a)
    }

    @Test
    fun testCascadeSetNull() {
        testCascadeInsert()
        School(1).delete().execute()
    }
}