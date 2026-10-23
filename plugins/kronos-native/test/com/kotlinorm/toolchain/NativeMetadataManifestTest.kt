/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.toolchain

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NativeMetadataManifestTest {
    @Test
    fun readsPublishedMetadataArchivesInStableOrderAndDeduplicatesDiamondDependencies() {
        val directory = Files.createTempDirectory("kronos-manifests")
        try {
            val first = archive(directory.resolve("first.jar"), "z\tfixture.installZ\na\tfixture.installA\n")
            val second = archive(directory.resolve("second.jar"), "a\tfixture.installA\n")
            assertEquals(
                listOf(
                    NativeMetadataManifestEntry("a", "fixture.installA"),
                    NativeMetadataManifestEntry("z", "fixture.installZ")
                ),
                readNativeMetadataManifests(linkedSetOf(second, first))
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun conflictingIdsOrEntrySymbolsFailBeforeGeneratingApplicationCode() {
        val directory = Files.createTempDirectory("kronos-manifest-conflict")
        try {
            val first = archive(directory.resolve("first.jar"), "a\tfixture.installA\n")
            val second = archive(directory.resolve("second.jar"), "a\tfixture.installB\n")
            assertFailsWith<IllegalArgumentException> { readNativeMetadataManifests(setOf(first, second)) }
            val third = archive(directory.resolve("third.jar"), "b\tfixture.installA\n")
            assertFailsWith<IllegalArgumentException> { readNativeMetadataManifests(setOf(first, third)) }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun archivesWithoutKronosContributionsAreAllowedButMalformedManifestsFail() {
        val directory = Files.createTempDirectory("kronos-manifest-malformed")
        try {
            val plain = directory.resolve("plain.jar").toFile()
            ZipOutputStream(plain.outputStream()).use { }
            assertEquals(emptyList(), readNativeMetadataManifests(setOf(plain)))
            val malformed = archive(directory.resolve("malformed.jar"), "missing-entry\n")
            assertFailsWith<IllegalArgumentException> { readNativeMetadataManifests(setOf(malformed)) }
            val injection = archive(directory.resolve("injection.jar"), "a\tfixture.install();error(1)\n")
            assertFailsWith<IllegalArgumentException> { readNativeMetadataManifests(setOf(injection)) }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun archive(path: java.nio.file.Path, manifest: String): java.io.File = path.toFile().also { file ->
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(NATIVE_METADATA_MANIFEST_PATH))
            zip.write(manifest.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }
}
