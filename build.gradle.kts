import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm") apply false
    kotlin("plugin.spring") apply false
    kotlin("plugin.jpa") apply false
    kotlin("plugin.serialization") apply false
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
}

val javaVersion: String by project
val kotlinVersion: String by project
val coroutinesVersion: String by project

allprojects {
    group = "com.fooddelivery"
    version = "1.0.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    // Java 25 toolchain configuration
    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt()))
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(javaVersion))
            languageVersion.set(KotlinVersion.KOTLIN_2_3)
            apiVersion.set(KotlinVersion.KOTLIN_2_3)
            freeCompilerArgs.addAll(
                "-Xjsr305=strict",
                "-Xcontext-receivers"
            )
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        // Enable Virtual Threads for tests
        jvmArgs("--enable-preview")
    }

    dependencies {
        // Kotlin standard library
        "implementation"(kotlin("stdlib"))
        "implementation"(kotlin("reflect"))

        // Kotlin Coroutines (Virtual Threads are native in Java 25+)
        "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

        // Testing
        "testImplementation"(kotlin("test"))
        "testImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    }
}
