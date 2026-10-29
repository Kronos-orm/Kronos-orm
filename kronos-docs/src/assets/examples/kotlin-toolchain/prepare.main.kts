import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

// Run with the Kotlin compiler: kotlinc -script prepare.main.kts
// Keep these module declarations aligned with project.yaml and module.yaml.
data class Module(
    val name: String,
    val jvmScope: String? = null,
    val native: Boolean = false,
    val nativeDependencies: List<String> = emptyList(),
)

val root = Path.of("").toAbsolutePath().normalize()
require(Files.isRegularFile(root.resolve("project.yaml"))) { "Run this script in the example project root" }
val modules = listOf(
    Module("models", jvmScope = "jvmMain", native = true),
    Module("jvm-app", jvmScope = "main"),
    Module("native-app", native = true, nativeDependencies = listOf("models")),
)

fun coordinate(module: Module): String {
    val yaml = Files.readString(root.resolve("${module.name}/module.yaml"))
    return Regex("(?m)^\\s+module-coordinate:\\s*([A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+)\\s*$")
        .find(yaml)?.groupValues?.get(1)
        ?: error("Missing module-coordinate in ${module.name}/module.yaml")
}

// Mirrors the compiler's module-coordinate identity; names are derived, never copied hashes.
fun identity(module: Module, scope: String): Pair<String, String> {
    val value = "gradle:${coordinate(module)}::${module.name}:$scope"
    val suffix = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        .joinToString("") { "%02x".format(it.toInt() and 255) }.take(16)
    return "$value#$suffix" to suffix
}

fun write(relative: String, text: String) {
    val path = root.resolve(relative)
    Files.createDirectories(path.parent)
    if (!Files.exists(path) || Files.readString(path) != text) Files.writeString(path, text)
}

fun nativeEntry(module: Module, test: Boolean = false): String =
    "com.kotlinorm.generated.native.KronosGeneratedTypeMetadata_${identity(module, if (test) "commonTest" else "commonMain").second}"

modules.forEach { module ->
    for (test in listOf(false, true)) {
        module.jvmScope?.let { scope ->
            val resources = (if (test) "testResources" else "resources") + if (module.native) "@jvm" else ""
            val compilationScope = when {
                !test -> scope
                scope == "main" -> "test"
                else -> scope.removeSuffix("Main") + "Test"
            }
            write("${module.name}/$resources/META-INF/services/com.kotlinorm.utils.GeneratedTypeProvider",
                "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_${identity(module, compilationScope).second}\n")
        }
        if (module.native) {
            val sources = (if (test) "test" else "src") + if (module.jvmScope != null) "@native" else ""
            val entry = nativeEntry(module, test)
            val name = entry.substringAfterLast('.')
            val dependencyCalls = if (test) "    ${nativeEntry(module)}Dependencies(registry)"
            else module.nativeDependencies.joinToString("\n") { dependency ->
                val target = modules.single { it.name == dependency }
                "    ${nativeEntry(target)}Dependencies(registry)"
            }
            write("${module.name}/$sources/GeneratedKronosMetadata.kt", """
                @file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)
                package com.kotlinorm.generated.native

                import com.kotlinorm.utils.GeneratedTypeMetadataRegistry
                import com.kotlinorm.utils.GeneratedTypeProvider
                import com.kotlinorm.utils.GeneratedTypeRegistrar

                // Source-visible skeleton; the Kronos compiler fills its factories and descriptors.
                private class ${name}Provider : GeneratedTypeProvider {
                    override val id: String = "${identity(module, if (test) "commonTest" else "commonMain").first}"
                    override fun contributeTo(registrar: GeneratedTypeRegistrar) {}
                }

                fun $name(registry: GeneratedTypeMetadataRegistry) {
                    registry.register(${name}Provider())
                }

                fun ${name}Dependencies(
                    registry: GeneratedTypeMetadataRegistry = com.kotlinorm.utils.generatedTypeMetadataRegistry
                ) {
                @@DEPENDENCIES@@
                    $name(registry)
                }
            """.trimIndent().replace("@@DEPENDENCIES@@", dependencyCalls) + "\n")
            write("${module.name}/$sources/GeneratedKronosProjectionAnchor.kt", """
                package com.kotlinorm.generated.projection
                internal object ${name}ProjectionMetadataAnchor {}
            """.trimIndent() + "\n")
        }
    }
}
println("Prepared main/test JVM services and Native metadata for ${modules.joinToString { it.name }}")
