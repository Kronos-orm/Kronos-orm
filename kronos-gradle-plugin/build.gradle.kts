import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
}

dependencies {
    implementation(libs.kotlin.gradle.plugin.api)
    implementation(libs.kotlin.gradle.plugin)
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

val generatedKronosResourceDir =
    layout.buildDirectory.dir("generated/kronos-version/main/resources")

val generateKronosCompilerPluginVersionResource =
    tasks.register("generateKronosCompilerPluginVersionResource") {
        inputs.property("compilerPluginVersion", project.version.toString())
        outputs.dir(generatedKronosResourceDir)

        doLast {
            generatedKronosResourceDir.get()
                .file("META-INF/kronos/compiler-plugin.properties")
                .asFile
                .apply {
                    parentFile.mkdirs()
                    writeText("version=${project.version}\n")
                }
        }
    }

tasks.named<Copy>("processResources") {
    dependsOn(generateKronosCompilerPluginVersionResource)
    from(generatedKronosResourceDir)
}

val kronosRepositoryRoot = rootProject.projectDir.parentFile

val publishTestKitFixtureArtifacts = tasks.register<Exec>("publishTestKitFixtureArtifacts") {
    group = "verification"
    description = "Publishes the current JVM Kronos artifacts consumed by Gradle TestKit fixtures."
    workingDir = kronosRepositoryRoot
    val wrapper = if (System.getProperty("os.name").startsWith("Windows")) {
        listOf("cmd", "/c", "gradlew.bat")
    } else {
        listOf("./gradlew")
    }
    commandLine(wrapper + listOf(
        ":kronos-syntax:publishKotlinMultiplatformPublicationToMavenLocal",
        ":kronos-syntax:publishJvmPublicationToMavenLocal",
        ":kronos-core:publishJvmPublicationToMavenLocal",
        ":kronos-compiler-plugin:publishToMavenLocal",
        "--no-daemon",
        "--configure-on-demand",
        "--console=plain"
    ))
}

tasks.withType<Test>().configureEach {
    dependsOn(publishTestKitFixtureArtifacts)
    systemProperty("kronos.version", project.version.toString())
    systemProperty("kronos.kotlin.version", libs.versions.kotlin.get())
}


gradlePlugin {
    plugins {
        create("kronosCompilerPlugin") {
            id = "com.kotlinorm.kronos-gradle-plugin"
            implementationClass = "com.kotlinorm.compiler.plugin.KronosGradlePlugin"
        }
    }
}
