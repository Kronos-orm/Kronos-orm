import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":kronos-core"))
    implementation(kotlin("reflect"))
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
    "Kronos 's built-in database operation plug-in based on the original jdbc supports variable templates and multiple databases."
)