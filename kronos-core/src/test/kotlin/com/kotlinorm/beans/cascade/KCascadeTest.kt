package com.kotlinorm.beans.cascade

import com.kotlinorm.Kronos
import com.kotlinorm.testfixtures.cascade.manytomany.Course
import com.kotlinorm.testfixtures.cascade.manytomany.Student
import com.kotlinorm.testfixtures.cascade.manytomany.StudentCourse
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test cases for [com.kotlinorm.beans.dsl.KCascade].
 *
 * author: OUSC
 */
class KCascadeTest {
    init {
        with(Kronos) {
            fieldNamingStrategy = lineHumpNamingStrategy
            tableNamingStrategy = lineHumpNamingStrategy
        }
    }

    @Test
    fun testManyToManyDelegate() {
        val course = Course().apply {
            students = [
                Student(name = "Alice"),
                Student(name = "Bob")
            ]
        }

        assertEquals(
            Course().apply {
                studentCourse = [
                    StudentCourse(student = Student(name = "Alice")),
                    StudentCourse(student = Student(name = "Bob")),
                ]
            }, course
        )

        assertEquals(
            course.students,
            course.studentCourse?.map { it.student }
        )
    }
}