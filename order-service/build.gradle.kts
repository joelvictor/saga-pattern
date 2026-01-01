plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val springKafkaVersion: String by project
val postgresDriverVersion: String by project

dependencies {
    implementation(project(":shared-kernel"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Kafka
    implementation("org.springframework.kafka:spring-kafka:$springKafkaVersion")
    
    // Database
    runtimeOnly("org.postgresql:postgresql:$postgresDriverVersion")
    
    // Jackson Kotlin Module
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test:$springKafkaVersion")
    testImplementation("org.testcontainers:postgresql:1.20.0")
    testImplementation("org.testcontainers:kafka:1.20.0")
}

tasks.bootJar {
    archiveFileName.set("order-service.jar")
}
