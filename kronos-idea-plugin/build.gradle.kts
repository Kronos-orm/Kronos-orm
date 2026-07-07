import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
}

val macIdeaEapHome = "/Applications/IntelliJ IDEA 2026.2 EAP.app/Contents"
val windowsIdeaEapExecutable = "C:/Program Files/JetBrains/IntelliJ IDEA 262.8377.35/bin/idea64.exe"
val localIdeaEapVersion = "2026.2"
val kronosIdeaPluginVersion = providers.gradleProperty("kronos.idea.plugin.version")
    .orElse("0.1.1-SNAPSHOT")

version = kronosIdeaPluginVersion.get()

fun normalizeIdeaHome(path: String): String {
    val localPath = file(path)
    return if (localPath.isFile && localPath.name.equals("idea64.exe", ignoreCase = true)) {
        localPath.parentFile.parentFile.absolutePath
    } else {
        localPath.absolutePath
    }
}

val ideaHome = providers.gradleProperty("kronos.idea.localPath")
    .orElse(providers.environmentVariable("IDEA_HOME"))
    .map(::normalizeIdeaHome)
    .orElse(
        providers.provider {
            when {
                file(macIdeaEapHome).isDirectory -> normalizeIdeaHome(macIdeaEapHome)
                file(windowsIdeaEapExecutable).isFile -> normalizeIdeaHome(windowsIdeaEapExecutable)
                else -> null
            }
        }
    )
val ideaVersion = providers.gradleProperty("kronos.idea.version")
    .orElse(localIdeaEapVersion)
val ideaSandboxContainer = providers.gradleProperty("kronos.idea.sandbox")
    .map { rootProject.layout.projectDirectory.dir(it) }
    .orElse(rootProject.layout.projectDirectory.dir("../Kronos-orm-idea-sandbox"))

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation(project(":kronos-core"))
    implementation(project(":kronos-codegen"))
    implementation(libs.kotlinpoet)
    compileOnly(project(":kronos-compiler-plugin"))
    runtimeOnly(project(":kronos-compiler-plugin"))

    testImplementation(libs.kotlin.test)
    testImplementation(project(":kronos-compiler-plugin"))
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.13.4")

    intellijPlatform {
        if (ideaHome.isPresent) {
            local(ideaHome)
        } else {
            intellijIdea(ideaVersion)
        }
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("com.intellij.database")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Kotlin)
        javaCompiler()
        pluginVerifier()
        zipSigner()
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xsuppress-version-warnings")
        allWarningsAsErrors.set(false)
    }
}

intellijPlatform {
    instrumentCode.set(true)
    sandboxContainer.set(ideaSandboxContainer)

    pluginConfiguration {
        id.set("com.kotlinorm.kronos-idea-plugin")
        name.set("Kronos-Orm")
        version.set(kronosIdeaPluginVersion)
        description.set(
            """
            Kronos ORM IDEA plugin.
            <br>
            <em>Add support for code generation and K2 analysis.</em>
            """.trimIndent()
        )
        vendor {
            name.set("Kronos ORM")
            url.set("https://www.kotlinorm.com")
        }
    }

    pluginVerification {
        ides {
            local(ideaHome)
        }
    }
}
