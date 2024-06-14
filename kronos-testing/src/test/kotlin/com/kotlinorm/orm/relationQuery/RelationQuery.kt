package com.kotlinorm.orm.relationQuery

import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.relationQuery.oneToMany.GroupClass
import com.kotlinorm.orm.relationQuery.oneToMany.Student
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties

class RelationQuery {
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
    fun testCascadeDelete() {
        val groupClass = GroupClass(1)
        val students = (1..10).map {
            Student(it, groupClassId = 1).apply {
                this.groupClass = groupClass
            }
        }
        groupClass.students = students
        groupClass.delete().where().execute()
    }

    @Test
    fun testCascadeUpdate() {
        val groupClass = GroupClass(1)
        val ans = groupClass::class.memberProperties.find { it.name == "students" }!!.annotations.first()
        val value = ans.annotationClass.members.first().call(ans)
        print(value.toString())
    }
}