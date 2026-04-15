package io.github.mlsgrr.apiviews

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ApiViewsExtension @Inject constructor(
    objects: ObjectFactory,
    layout: ProjectLayout
) {
    val inputSpec: Property<String> = objects.property(String::class.java)
    val viewConfig: RegularFileProperty = objects.fileProperty()
    val outputDir: DirectoryProperty = objects.directoryProperty().convention(
        layout.buildDirectory.dir("generated/openapi/views")
    )

    val apiPackage: Property<String> = objects.property(String::class.java)
    val modelPackage: Property<String> = objects.property(String::class.java)
    val packageName: Property<String> = objects.property(String::class.java)
    val invokerPackage: Property<String> = objects.property(String::class.java)

    val library: Property<String> = objects.property(String::class.java).convention("spring-boot")
    val serializationLibrary: Property<String> = objects.property(String::class.java).convention("jackson")
    val useSpringBoot3: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val dateLibrary: Property<String> = objects.property(String::class.java).convention("java8")
    val interfaceOnly: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val skipDefaultInterface: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val skipDefaultApiInterface: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val useBeanValidation: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val useTags: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val requestMappingMode: Property<String> = objects.property(String::class.java).convention("api_interface")
    val cleanupOutput: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    val failOnUnmatchedOperations: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    val dependsOnTasks: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    val additionalConfigOptions: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java).convention(emptyMap())
    val globalProperties: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java).convention(emptyMap())
}
