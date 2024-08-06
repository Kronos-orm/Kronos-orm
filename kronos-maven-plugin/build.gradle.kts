import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

plugins {
    kotlin("jvm")
    kotlin("kapt")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

dependencies {
//    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
    implementation(kotlin("maven-plugin"))
    implementation(project(":kronos-compiler-plugin"))
    implementation("org.apache.maven:maven-core:3.8.1")
}

// A bit of a hack to copy over the META-INF services information so that Maven knows about the NullDefaultsComponentRegistrar
val servicesDirectory = "META-INF/services"
val copyServices = tasks.register<Copy>("copyServices") {
    val nativePlugin = project(":kronos-compiler-plugin")
    from(nativePlugin.kaptGeneratedServicesDir)
    into(kaptGeneratedServicesDir)
}

kotlin {
    jvmToolchain(8)
}

tasks.withType<KotlinCompile> {
    dependsOn(copyServices)
    compilerOptions {
        freeCompilerArgs.add("-Xopt-in=kotlin.RequiresOptIn")
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

mavenPublishing {
    configure(
        KotlinJvm(
            // configures the -javadoc artifact, possible values:
            // - `JavadocJar.None()` don't publish this artifact
            // - `JavadocJar.Empty()` publish an emprt jar
            // - `JavadocJar.Dokka("dokkaHtml")` when using Kotlin with Dokka, where `dokkaHtml` is the name of the Dokka task that should be used as input
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
            // whether to publish a sources jar
            sourcesJar = true,
        )
    )
    coordinates(project.group.toString(), project.name, project.version.toString())
    pom {
        name.set("${project.group}:${project.name}")
        description.set("A maven plugin provided by kronos for parsing SQL Criteria expressions at compile time.")
        inceptionYear.set("2024")
        url.set("https://www.kotlinorm.com")
        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developers {
                developer {
                    id.set("ousc")
                    name.set("ousc")
                    email.set("sundaiyue@foxmail.com")
                }
                developer {
                    id.set("FOYU")
                    name.set("FOYU")
                    email.set("2456416562@qq.com")
                }
                developer {
                    id.set("yf")
                    name.set("yf")
                    email.set("1661264104@qq.com")
                }
            }
        }
        scm {
            url.set("https://github.com/Kronos-orm/Kronos-orm")
            connection.set("scm:git:https://github.com/Kronos-orm/Kronos-orm.git")
            developerConnection.set("scm:git:ssh://git@github.com:Kronos-orm/Kronos-orm.git")
        }
    }
    if (!version.toString().endsWith("-SNAPSHOT")) {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    }

    publishing {
        repositories {
            if (providers.gradleProperty("aliyunMvnPackages").isPresent) {
                maven {
                    name = "aliyun"
                    url = uri(providers.gradleProperty("aliyunMvnPackages").get())
                    credentials {
                        username = providers.gradleProperty("aliyunUsername").get()
                        password = providers.gradleProperty("aliyunPassword").get()
                    }
                }
            }
            mavenLocal()
        }
    }

    signAllPublications()
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs = listOf("-Xinline-classes")
}

val Project.kaptGeneratedServicesDir: File
    get() =
        Kapt3GradleSubplugin.getKaptGeneratedClassesDir(this, sourceSets.main.get().name).resolve(
            servicesDirectory
        )