package com.kotlinorm.codegen

import com.kotlinorm.codegen.KronosConfig.Companion.write
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinFileTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun createTempDir() {
        tempDir = createTempDirectory("kotlinFileTest").toFile()
    }

    @AfterTest
    fun cleanupTempDir() {
        tempDir.deleteRecursively()
    }

    @Test
    fun writesFileSuccessfullyWhenDirectoryExists() {
        val content = "Hello, World!"
        val filePath = File(tempDir, "existingDir/testFile.kt").apply { parentFile.mkdirs() }.path
        listOf(KronosConfig(content, filePath)).write()

        val writtenFile = File(filePath)
        assertTrue(writtenFile.exists())
        assertEquals(content, writtenFile.readText())
    }

    @Test
    fun createsParentDirectoriesIfNotExist() {
        val content = "Hello, Kotlin!"
        val filePath = File(tempDir, "nonExistingDir/testFile.kt").path
        listOf(KronosConfig(content, filePath)).write()

        val writtenFile = File(filePath)
        assertTrue(writtenFile.exists())
        assertEquals(content, writtenFile.readText())
    }

    @Test
    fun overwritesExistingFileContent() {
        val initialContent = "Initial Content"
        val updatedContent = "Updated Content"
        val filePath = File(tempDir, "overwriteDir/testFile.kt").apply {
            parentFile.mkdirs()
            writeText(initialContent)
        }.path
        listOf(KronosConfig(updatedContent, filePath)).write()

        val writtenFile = File(filePath)
        assertTrue(writtenFile.exists())
        assertEquals(updatedContent, writtenFile.readText())
    }

    @Test
    fun handlesEmptyContentGracefully() {
        val content = ""
        val filePath = File(tempDir, "emptyContentDir/testFile.kt").path
        listOf(KronosConfig(content, filePath)).write()

        val writtenFile = File(filePath)
        assertTrue(writtenFile.exists())
        assertEquals(content, writtenFile.readText())
    }
}