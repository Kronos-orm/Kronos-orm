package com.kotlinorm.orm.relationQuery

import com.kotlinorm.Kronos
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.InsertClause.Companion.execute
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.relationQuery.oneToMany.GroupClass
import com.kotlinorm.orm.relationQuery.oneToMany.School
import com.kotlinorm.orm.relationQuery.oneToMany.Student
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RelationQuery {
    init {
        Kronos.tableNamingStrategy = LineHumpNamingStrategy
        Kronos.fieldNamingStrategy = LineHumpNamingStrategy
    }

    @Test
    fun a() {
        val student = Student().select().where { it.id == 1 && it.groupClass!!.groupNo.isNull }.queryOne()
        val groupClass = student.groupClass
        val students = groupClass!!.students
    }

    @Test
    fun b() {
        val student = GroupClass().update()
            .set {
                it.id = 2
            }
            .where {
                it.id == 1
            }
            .execute()

        // update student set groupClassId = 2 where groupClassId = 1
    }

    @Test
    fun c() {
        val groupClass = GroupClass(1)
        val students = (1..10).map {
            Student(it, groupClassId = 1).apply {
                this.groupClass = groupClass
            }
        }
        groupClass.students = students
        println(students)
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
    fun testCascadeDeleteSetDefault() {
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
            "UPDATE `student` " +
                    "SET `student`.`group_class_id` = :defaultVal0 " +
                    "WHERE `student`.`group_class_id` " +
                    "IN ( " +
                    "SELECT `id` from (" +
                    "SELECT `group_class`.`id` " +
                    "FROM `group_class` " +
                    "WHERE `group_class`.`id` = :id" +
                    ") " +
                    "as KRONOS_TEMP_TABLE_Ufv2)",
            task.component3()[0].sql
        )
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
        val school = School(1)

        val groupClasses = (1..10).map { cId ->
            val c = GroupClass(cId, schoolId = 1)

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
        school.insert().execute()
        school.groupClass?.insert()?.execute()

        val tasks = school.delete().where().build()
        println(tasks.component3().size)
    }

    @Test
    fun testUpdate() {
        val groupClass = GroupClass(
            schoolId = 3
        ).select().where { it.schoolId.eq }.queryOne()

        groupClass.school = School(
            name = "实验小学"
        )
        groupClass.update().execute()

        val school = School(1).select().by { it.id }.queryOne()

        groupClass.school = school

        groupClass.update().execute()
    }
}