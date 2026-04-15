# OpenAPI Views Gradle Plugin

A Kotlin/Spring post-generation codegen plugin, delivered as a Gradle plugin, that decouples your public API contract shape from internal architecture boundaries.

Keep your OpenAPI spec domain-oriented. Get architecture-oriented interfaces for free.

## The problem

OpenAPI Generator groups operations by tag — one tag, one interface. A `Cats` tag produces:

```kotlin
interface CatsApi {
    fun createCat(...)
    fun getCat(...)
    fun deleteCat(...)
}
```

Your controller must implement everything. You can't separate reads from writes, commands from queries, or split by any other architectural concern without changing the spec.

## The solution

This plugin generates additional derived interfaces from a simple YAML config, while leaving the original generated APIs untouched:

```kotlin
interface CatsCommandsApi {       interface CatsQueriesApi {
    fun createCat(...)                fun getCat(...)
    fun deleteCat(...)            }
}
```

Your spec stays clean. Your architecture stays flexible.

## Quick setup

### 1. Add the plugin

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.25"
    id("org.openapi.generator") version "7.16.0"
    id("io.github.mlsgrr.openapi-views") version "0.1.0"
}
```

### 2. Configure your normal OpenAPI generation (unchanged)

```kotlin
val generateDomainApi by tasks.registering(GenerateTask::class) {
    generatorName.set("kotlin-spring")
    inputSpec.set(layout.projectDirectory.file("src/main/openapi/pets.yaml").asFile.absolutePath)
    outputDir.set(layout.buildDirectory.dir("generated/openapi/domain").get().asFile.absolutePath)
    apiPackage.set("com.acme.generated.api")
    modelPackage.set("com.acme.generated.model")
    configOptions.set(mapOf(
        "library" to "spring-boot",
        "useSpringBoot3" to "true",
        "interfaceOnly" to "true",
        "skipDefaultInterface" to "true",
        "skipDefaultApiInterface" to "true",
        "useBeanValidation" to "true",
        "useTags" to "true",
        "requestMappingMode" to "api_interface"
    ))
}
```

### 3. Configure the views

```kotlin
apiViews {
    inputSpec.set(layout.projectDirectory.file("src/main/openapi/pets.yaml").asFile.absolutePath)
    viewConfig.set(layout.projectDirectory.file("config/api-views.yaml"))
    apiPackage.set("com.acme.generated.api")
    modelPackage.set("com.acme.generated.model")
    invokerPackage.set("com.acme.generated.invoker")
    dependsOnTasks.add(generateDomainApi.name)
}
```

### 4. Create the view config

```yaml
# config/api-views.yaml
apiViews:
  - sourceTag: Cats
    outputs:
      - name: CatsCommandsApi
        match:
          httpMethods: [POST, PUT, PATCH, DELETE]
      - name: CatsQueriesApi
        match:
          httpMethods: [GET]
```

### 5. Add generated sources to compilation

```kotlin
kotlin {
    sourceSets.named("main") {
        kotlin.srcDir(layout.buildDirectory.dir("generated/openapi/domain/src/main/kotlin"))
    }
}
```

The plugin automatically adds its own output directory. Run `./gradlew generateApiViews` or just build — it hooks into `compileKotlin`.

## Matching options

Every output must have at least one match criterion:

| Criterion | Example | Description |
|-----------|---------|-------------|
| `httpMethods` | `[GET, POST]` | Match by HTTP method |
| `operationIds.include` | `[getCat, listCats]` | Match exact operation IDs |
| `operationIds.includeRegex` | `get.*` | Match operation IDs by regex |
| `operationIds.excludeRegex` | `delete.*` | Exclude operation IDs by regex |
| `extensions` | `x-cqrs: command` | Match by vendor extension value |

Criteria within a single match block are AND-ed together.

**Extension-based example:**

```yaml
apiViews:
  - sourceTag: Cats
    outputs:
      - name: CatsCommandsApi
        match:
          extensions:
            x-cqrs: command
      - name: CatsQueriesApi
        match:
          extensions:
            x-cqrs: query
```