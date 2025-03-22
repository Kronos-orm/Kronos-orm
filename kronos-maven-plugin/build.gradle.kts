import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kronos.publishing)
}

dependencies {
    api(project(":kronos-compiler-plugin"))
    implementation(libs.kotlin.maven.plugin)
    implementation(libs.maven.core)
}

// A bit of a hack to copy over the META-INF services information so that Maven knows about the NullDefaultsComponentRegistrar
val servicesDirectory = "META-INF/services"
val Project.kaptGeneratedServicesDir: File
    get() =
        Kapt3GradleSubplugin.getKaptGeneratedClassesDir(this, sourceSets.main.get().name).resolve(
            servicesDirectory
        )
val copyServices =
    tasks.register<Copy>("copyServices") {
        dependsOn(":kronos-compiler-plugin:kaptKotlin")
        val kotlinPlugin = project(":kronos-compiler-plugin")
        from(kotlinPlugin.kaptGeneratedServicesDir)
        into(kaptGeneratedServicesDir)
    }

tasks.withType<KotlinCompile> {
    dependsOn(copyServices)
    compilerOptions {
        freeCompilerArgs.add("-Xopt-in=kotlin.RequiresOptIn")
    }
}
