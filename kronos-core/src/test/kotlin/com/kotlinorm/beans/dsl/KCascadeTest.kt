package com.kotlinorm.beans.dsl

import com.kotlinorm.beans.sample.cascade.manyToMany.Course
import com.kotlinorm.beans.sample.cascade.manyToMany.Student
import com.kotlinorm.beans.sample.cascade.manyToMany.StudentCourse
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test cases for [com.kotlinorm.beans.dsl.KCascade].
 *
 * author: OUSC
 */
class KCascadeTest {
    @Test
    fun testManyToManyDelegate() {
        val course = Course().apply {
            students = listOf(
                Student(name = "Alice"),
                Student(name = "Bob")
            )
        }

        assertEquals(
            course,
            Course().apply {
                studentCourse = listOf(
                    StudentCourse(student = Student(name = "Alice")),
                    StudentCourse(student = Student(name = "Bob")),
                )
            }
        )

        assertEquals(
            course.students,
            course.studentCourse?.map { it.student }
        )
    }
}