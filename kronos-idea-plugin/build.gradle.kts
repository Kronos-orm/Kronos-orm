import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PublishPluginTask
import org.jetbrains.intellij.platform.gradle.tasks.SignPluginTask
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
}

val stableIdeaVersion = "2026.2"
val stableIdeaBuild = "262.8665.258"
val stableIdeaSinceBuild = "262"
val stableIdeaUntilBuild = "262.*"
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

val ideaFixtureCompilerPluginArtifact = configurations.create("ideaFixtureCompilerPluginArtifact") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}
val ideaFixtureCoreArtifact = configurations.create("ideaFixtureCoreArtifact") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}
val ideaFixtureSyntaxArtifact = configurations.create("ideaFixtureSyntaxArtifact") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
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
    add(
        ideaFixtureCompilerPluginArtifact.name,
        project(path = ":kronos-compiler-plugin", configuration = "runtimeElements"),
    )
    add(
        ideaFixtureCoreArtifact.name,
        project(path = ":kronos-core", configuration = "runtimeElements"),
    )
    add(
        ideaFixtureSyntaxArtifact.name,
        project(path = ":kronos-syntax", configuration = "runtimeElements"),
    )

    intellijPlatform {
        if (ideaHome.isPresent) {
            local(ideaHome)
        } else {
            intellijIdea(ideaVersion)
        }
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("com.intellij.database")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Kotlin)
        testBundledPlugins(
            "com.intellij.java",
            "org.jetbrains.kotlin",
            "com.intellij.database",
            "intellij.todo.plugin",
            "intellij.testRunner.plugin",
            "intellij.libraries.misc.plugin",
            "intellij.structureView.plugin",
            "intellij.grid.core.plugin",
            "intellij.structuralSearch.plugin",
            "intellij.execution.serviceView.plugin",
            "intellij.navbar.plugin",
        )
        testBundledModules(
            "intellij.grid",
            "intellij.platform.execution.serviceView",
            "intellij.platform.structuralSearch",
            "intellij.platform.structureView",
            "intellij.platform.structureView.backend",
        )
        javaCompiler(ideaJavaCompilerVersion.get())
        pluginVerifier()
        zipSigner()
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    dependsOn(
        ideaFixtureCompilerPluginArtifact,
        ideaFixtureCoreArtifact,
        ideaFixtureSyntaxArtifact,
    )
    doFirst {
        systemProperty(
            "kronos.idea.test.compilerPluginJar",
            ideaFixtureCompilerPluginArtifact.singleFile.absolutePath,
        )
        systemProperty(
            "kronos.idea.test.coreJar",
            ideaFixtureCoreArtifact.singleFile.absolutePath,
        )
        systemProperty(
            "kronos.idea.test.syntaxJar",
            ideaFixtureSyntaxArtifact.singleFile.absolutePath,
        )
    }
}

tasks.named("verifyPluginSignature") {
    dependsOn("signPlugin")
}

val signPluginTask = tasks.named<SignPluginTask>("signPlugin")

tasks.named<PublishPluginTask>("publishPlugin") {
    dependsOn(signPluginTask)
    archiveFile.set(signPluginTask.flatMap { it.signedArchiveFile })
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.removeAll(listOf("--release", "8"))
    options.release.set(25)
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
        ideaVersion {
            sinceBuild.set(stableIdeaSinceBuild)
            untilBuild.set(stableIdeaUntilBuild)
        }
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
            current()
        }
    }
}
