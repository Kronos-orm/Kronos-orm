package com.kotlinorm.orm.relationQuery

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.dataSource
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.database.table
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.InsertClause.Companion.execute
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.relationQuery.oneToMany.GroupClass
import com.kotlinorm.orm.relationQuery.oneToMany.School
import com.kotlinorm.orm.relationQuery.oneToMany.Student
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.utils.GsonResolver
import org.apache.commons.dbcp.BasicDataSource
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RelationQuery {
    private val ds = BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver"
        url = "jdbc:mysql://localhost:3306/test"
        username = "root"
        password = "Leinbo2103221541@"
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

        val task2 = school.insert().execute()
    }

    @Test
    fun testCascadeUpdate() {
        School(name = "School").update().set {
            it.name = "School1"
        }.where().execute()
    }

    @Test
    fun testCascadeDelete() {
        val res = School(name = "School1").delete().where().build()
        println(res)
    }

    @Test
    fun testCascadeDeleteNoAction() {
        val groupClass = GroupClass(1)
        val students = (1..10).map {
            Student(it, groupClassId = 1).apply {
                this.groupClass = groupClass
            }
        }
        groupClass.students = students
        val task = groupClass.delete().where().build()
        println(task.component3().size)
        assertEquals(task.component3().size, 1)
    }

    @Test
    fun testCascadeDeleteRestrict() {
        val groupClass = GroupClass(1)
        val students = (1..10).map {
            Student(it, groupClassId = 1).apply {
                this.groupClass = groupClass
            }
        }
        groupClass.students = students
        val task = groupClass.delete().where().build()
        println(task.component3().size)
        assertEquals(
            "DELETE FROM `group_class` " + "WHERE `group_class`.`id` = :id AND (" + "SELECT count(1) " + "FROM `student` " + "WHERE`student`.`group_class_id` " + "IN ( " + "SELECT `id` from (" + "SELECT `group_class`.`id` " + "FROM `group_class` " + "WHERE `group_class`.`id` = :id" + ") as KRONOS_TEMP_TABLE_1b09) " + "LIMIT 1) = 0",
            task.component3()[0].sql
        )
    }

    @Test
    fun testCascadeDeleteCascade() {
        val groupClass = GroupClass(1)
        val students = (1..10).map {
            Student(it, groupClassId = 1).apply {
                this.groupClass = groupClass
            }
        }
        groupClass.students = students
        val task = groupClass.delete().where().build()
        println(task.component3().size)
        assertEquals(
            "DELETE FROM `student` " + "WHERE `student`.`group_class_id` " + "IN ( " + "SELECT `id` from (" + "SELECT `group_class`.`id` " + "FROM `group_class` " + "WHERE `group_class`.`id` = :id) " + "as KRONOS_TEMP_TABLE_xNzU)" + "LIMIT 1) = 0",
            task.component3()[0].sql
        )
    }

    @Test
    fun testMultipleCascade() {
        val school = School(1, name = "aaa")

        val groupClasses = (1..10).map { cId ->
            val c = GroupClass(cId, schoolName = "aaa")

            val students = (1..10).map { stuId ->
                Student(stuId, groupClassId = cId).apply {
                    this.groupClass = c
                }
            }

            c.apply {
                c.school = school
                c.students = students
            }
        }

        school.groupClass = groupClasses
        val tasks = school.delete().where().build()
        println(tasks.component3().size)
    }

    @Test
    fun testUpdate() {

        dataSource.table.dropTable<School>()
        dataSource.table.dropTable<GroupClass>()
        dataSource.table.dropTable<Student>()
        dataSource.table.createTable<School>()
        dataSource.table.createTable<GroupClass>()
        dataSource.table.createTable<Student>()

        val groupClass = GroupClass(1)
        val stus = (1..10).map {
            Student(it, groupClassId = 1)
        }
        groupClass.students = stus
        groupClass.insert().execute()

        val task = groupClass.update().set { it.id = 999 }.by { it.id }.build()
        val (_, lastInsertId) = task.execute()
        println(lastInsertId)
    }

    @Test
    fun testSelect() {
        dataSource.table.dropTable<School>()
        dataSource.table.dropTable<GroupClass>()
        dataSource.table.dropTable<Student>()
        dataSource.table.createTable<School>()
        dataSource.table.createTable<GroupClass>()
        dataSource.table.createTable<Student>()

        val listOfGroupClass = (1..100).map { gl ->
            val groupClass = GroupClass(gl)
            val stus = (1..10).map {
                Student(gl * 1000 + it, "name${gl * 1000 + it}", groupClassId = gl)
            }
            groupClass.students = stus
            groupClass
        }
        listOfGroupClass.insert().execute()

        val result = GroupClass().select().queryList()
        println(result)
    }
}