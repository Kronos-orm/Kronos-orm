plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    alias(libs.plugins.kronos.publishing)
}

dependencies {
    implementation(libs.kotlin.gradle.plugin.api)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

kotlin {
    jvmToolchain(8)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}


gradlePlugin {
    plugins {
        create("kronosCompilerPlugin") {
            id = "com.kotlinorm.kronos-gradle-plugin"
            implementationClass = "com.kotlinorm.compiler.plugin.KronosGradlePlugin"
        }
    }
}