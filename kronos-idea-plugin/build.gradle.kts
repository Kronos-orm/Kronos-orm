import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import java.io.File

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
}

val stableIdeaVersion = "2026.2"
val stableIdeaBuild = "262.8665.258"
val macIdeaHomes = listOf(
    "/Applications/IntelliJ IDEA.app/Contents",
    "${System.getProperty("user.home")}/Applications/IntelliJ IDEA.app/Contents"
)
val windowsIdeaExecutable = "C:/Program Files/JetBrains/IntelliJ IDEA 2026.2/bin/idea64.exe"
val kronosIdeaPluginVersion = providers.gradleProperty("kronos.idea.plugin.version")
    .orElse(providers.provider { rootProject.version.toString() })

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
            macIdeaHomes.firstOrNull { file(it).isDirectory }?.let(::normalizeIdeaHome)
                ?: windowsIdeaExecutable.takeIf { file(it).isFile }?.let(::normalizeIdeaHome)
        }
    )
val ideaVersion = providers.gradleProperty("kronos.idea.version")
    .orElse(stableIdeaVersion)
val ideaJavaCompilerVersion = providers.gradleProperty("kronos.idea.java.compiler.version")
    .orElse(stableIdeaBuild)
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
        javaCompiler(ideaJavaCompilerVersion.get())
        pluginVerifier()
        zipSigner()
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named("verifyPluginSignature") {
    dependsOn("signPlugin")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
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

    publishing {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
        channels.set(listOf("default"))
        hidden.set(false)
    }

    signing {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        certificateChainFile.set(
            layout.file(providers.environmentVariable("CERTIFICATE_CHAIN_FILE").map(::File))
        )
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        privateKeyFile.set(
            layout.file(providers.environmentVariable("PRIVATE_KEY_FILE").map(::File))
        )
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }

    pluginConfiguration {
        id.set("com.kotlinorm.kronos-idea-plugin")
        name.set("Kronos-ORM")
        version.set(kronosIdeaPluginVersion)
        description.set(
            """
            <p><b>Kronos IDEA plugin</b> brings Kronos compiler-plugin information into IntelliJ IDEA.</p>
            <p>
              <a href="https://www.kotlinorm.com/">Website</a>
              &nbsp;|&nbsp;
              <a href="https://www.kotlinorm.com/#/documentation/en/resources/idea-plugin">Documentation</a>
            </p>
            <p>Use it with the Gradle or Maven compiler plugin to get editor support for:</p>
            <ul>
              <li>generated KPojo members;</li>
              <li>projection result types;</li>
              <li>subquery shapes;</li>
              <li>database-first code generation.</li>
            </ul>
            <p><b>Project model</b><br>Loads the bundled Kronos FIR compiler plugin during IDEA analysis.</p>
            <p><b>Projection docs</b><br>Shows generated projection result and context shapes in quick documentation.</p>
            <p><b>Editor diagnostics</b><br>Reports projection, scalar subquery, predicate subquery, and INSERT SELECT shape errors in the editor.</p>
            <p><b>Code Generator</b><br>Reads IDEA Database data sources and previews or writes KPojo files.</p>
            <p><b>Templates</b><br>Copies the built-in KPojo template into .kronos/templates for project customization.</p>
            """.trimIndent()
        )
        vendor {
            name.set("Kronos-ORM")
            url.set("https://www.kotlinorm.com")
        }
    }

    pluginVerification {
        ides {
            local(ideaHome)
        }
    }
}
