plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.1"
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

repositories {
    gradlePluginPortal()
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.openapitools:openapi-generator:7.16.0")
    implementation("org.openapitools:openapi-generator-gradle-plugin:7.16.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")

    testImplementation(kotlin("test"))
}

gradlePlugin {
    website.set("https://github.com/mlsgrr/openapi-views-gradle-plugin")
    vcsUrl.set("https://github.com/mlsgrr/openapi-views-gradle-plugin")

    plugins {
        create("openApiViews") {
            id = providers.gradleProperty("pluginId").get()
            implementationClass = providers.gradleProperty("pluginImplementationClass").get()
            displayName = "OpenAPI API Views"
            description = "Generates additional architecture-oriented Kotlin interfaces from domain-oriented OpenAPI specs. Decouple your public API contract shape from internal architecture boundaries."
            tags.set(listOf("openapi", "kotlin", "spring", "cqrs", "codegen", "api"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
