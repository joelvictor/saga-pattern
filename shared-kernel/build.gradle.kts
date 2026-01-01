plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Kotlin Serialization for JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
    
    // Jackson for Spring compatibility
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.0")
}
