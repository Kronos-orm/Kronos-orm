import com.kotlinorm.buildSrc.SelectFromNGeneratorExtension

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
}

gradlePlugin {
    plugins {
        create("kronosJoinClauseGeneratorPlugin") {
            id = "com.kotlinorm.buildSrc.kronosJoinClauseGeneratorPlugin"
            implementationClass = "com.kotlinorm.buildSrc.KronosJoinClauseGeneratorPlugin"
        }
    }
}

configure<SelectFromNGeneratorExtension> {
    generateSelectFrom.runTask()
}

task("123").