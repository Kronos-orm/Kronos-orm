import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    kotlin("jvm")
}

dependencies {
    api(kotlin("reflect"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

kronosPublishing(
    mavenPublishing,
    publishing,
    KotlinJvm(JavadocJar.Dokka("dokkaHtml"), sourcesJar = true),
    "An easy-to-use, flexible, lightweight ORM framework designed for kotlin."
)