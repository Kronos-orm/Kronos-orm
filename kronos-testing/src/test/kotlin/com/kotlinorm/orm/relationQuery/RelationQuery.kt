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
        password = "rootroot"
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
    fun a() {
        val student = Student().select().where { it.id == 1 && it.groupClass!!.groupNo.isNull }.queryOne()
        val groupClass = student.groupClass
        val students = groupClass!!.students
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
            id = 1,
            name = "School",
            groupClass = listOf(
                GroupClass(
                    id = 11,
                    name = "一年级",
                    students = listOf(
                        Student(
                            id = 101,
                            name = "张三",
                            studentNo = "2021001"
                        ),
                        Student(
                            id = 102,
                            name = "李四",
                            studentNo = "2021002"
                        )
                    )
                ),
                GroupClass(
                    name = "二年级",
                    students = listOf(
                        Student(
                            id = 103,
                            name = "王五",
                            studentNo = "2022001"
                        ),
                        Student(
                            id = 104,
                            name = "赵六",
                            studentNo = "2022002"
                        )
                    )
                )
            )
        )

        val task2 = school.insert().build()
        val res2 = task2.execute()
        val afterExecute2 = task2.afterExecute?.invoke(res2)
        println(afterExecute2)
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
            "DELETE FROM `group_class` " +
                    "WHERE `group_class`.`id` = :id AND (" +
                    "SELECT count(1) " +
                    "FROM `student` " +
                    "WHERE`student`.`group_class_id` " +
                    "IN ( " +
                    "SELECT `id` from (" +
                    "SELECT `group_class`.`id` " +
                    "FROM `group_class` " +
                    "WHERE `group_class`.`id` = :id" +
                    ") as KRONOS_TEMP_TABLE_1b09) " +
                    "LIMIT 1) = 0",
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
            "DELETE FROM `student` " +
                    "WHERE `student`.`group_class_id` " +
                    "IN ( " +
                    "SELECT `id` from (" +
                    "SELECT `group_class`.`id` " +
                    "FROM `group_class` " +
                    "WHERE `group_class`.`id` = :id) " +
                    "as KRONOS_TEMP_TABLE_xNzU)" +
                    "LIMIT 1) = 0",
            task.component3()[0].sql
        )
    }

    @Test
    fun testMultipleCascade() {
        val school = School(1 , name = "aaa")

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
        val (affectRows, lastInsertId) = task.execute()
        println(lastInsertId)
    }
}