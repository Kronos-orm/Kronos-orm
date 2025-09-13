package com.kotlinorm.beans.cascade

import com.kotlinorm.Kronos
import com.kotlinorm.beans.sample.manyToMany.Course
import com.kotlinorm.beans.sample.cascade.manyToMany.Student
import com.kotlinorm.beans.sample.manyToMany.StudentCourse
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test cases for [com.kotlinorm.beans.dsl.KCascade].
 *
 * author: OUSC
 */
class KCascadeTest {
    init {
        Kronos.init {
            fieldNamingStrategy = lineHumpNamingStrategy
            tableNamingStrategy = lineHumpNamingStrategy
        }
    }

    @Test
    fun testManyToManyDelegate() {
        val course = Course().apply {
            students = listOf(
                Student(name = "Alice"),
                Student(name = "Bob")
            )
        }

        assertEquals(
            Course().apply {
                studentCourse = listOf(
                    StudentCourse(student = Student(name = "Alice")),
                    StudentCourse(student = Student(name = "Bob")),
                )
            }, course
        )

        assertEquals(
            course.students,
            course.studentCourse?.map { it.student }
        )
    }
}