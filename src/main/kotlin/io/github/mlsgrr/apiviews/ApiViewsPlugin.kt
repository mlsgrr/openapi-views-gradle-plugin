package io.github.mlsgrr.apiviews

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

class ApiViewsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("org.openapi.generator")

        val extension = project.extensions.create<ApiViewsExtension>("apiViews")

        val generateApiViews = project.tasks.register("generateApiViews", GenerateTask::class.java)

        generateApiViews.configure {
            group = "openapi tools"
            description = "Generates additional architecture-oriented API interfaces (views) from a domain-oriented OpenAPI contract."

            generatorName.set("kotlin-spring-views")
            outputDir.set(extension.outputDir.map { it.asFile.absolutePath })
            cleanupOutput.set(extension.cleanupOutput)
            inputSpec.set(extension.inputSpec)
            apiPackage.set(extension.apiPackage)
            modelPackage.set(extension.modelPackage)
            packageName.set(extension.packageName)
            invokerPackage.set(extension.invokerPackage)
            configOptions.set(defaultConfigOptions(project, extension))
            globalProperties.set(defaultGlobalProperties(project, extension))
            dependsOn(extension.dependsOnTasks)
        }

        project.pluginManager.withPlugin("java") {
            project.extensions.configure<SourceSetContainer> {
                getByName("main").java.srcDir(extension.outputDir.map { it.dir("src/main/kotlin") })
            }
        }

        project.tasks.matching { it.name == "compileKotlin" }.configureEach {
            dependsOn(generateApiViews)
        }
    }

    private fun defaultConfigOptions(
        project: Project,
        extension: ApiViewsExtension
    ): Provider<Map<String, String>> {
        return project.provider {
            mapOf(
                "library" to extension.library.get(),
                "serializationLibrary" to extension.serializationLibrary.get(),
                "useSpringBoot3" to extension.useSpringBoot3.get().toString(),
                "dateLibrary" to extension.dateLibrary.get(),
                "interfaceOnly" to extension.interfaceOnly.get().toString(),
                "skipDefaultInterface" to extension.skipDefaultInterface.get().toString(),
                "skipDefaultApiInterface" to extension.skipDefaultApiInterface.get().toString(),
                "useBeanValidation" to extension.useBeanValidation.get().toString(),
                "useTags" to extension.useTags.get().toString(),
                "requestMappingMode" to extension.requestMappingMode.get(),
                "apiViewsConfig" to extension.viewConfig.get().asFile.absolutePath,
                "failOnUnmatchedOperations" to extension.failOnUnmatchedOperations.get().toString()
            ) + extension.additionalConfigOptions.get()
        }
    }

    private fun defaultGlobalProperties(
        project: Project,
        extension: ApiViewsExtension
    ): Provider<Map<String, String>> {
        return project.provider {
            mapOf(
                "models" to "false",
                "apis" to "",
                "supportingFiles" to "false",
                "modelDocs" to "false",
                "modelTests" to "false",
                "apiDocs" to "false",
                "apiTests" to "false"
            ) + extension.globalProperties.get()
        }
    }
}
