rootProject.name = "food-delivery-saga"

// Shared kernel module with domain primitives and event contracts
include("shared-kernel")

// Microservices
include("order-service")
include("payment-service")
include("kitchen-service")
include("delivery-service")

// Plugin management
pluginManagement {
    val kotlinVersion: String by settings
    val springBootVersion: String by settings
    val springDependencyManagementVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.spring") version kotlinVersion
        kotlin("plugin.jpa") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.springframework.boot") version springBootVersion
        id("io.spring.dependency-management") version springDependencyManagementVersion
    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
