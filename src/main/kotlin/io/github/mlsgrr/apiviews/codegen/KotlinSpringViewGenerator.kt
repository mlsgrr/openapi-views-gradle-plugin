package io.github.mlsgrr.apiviews.codegen

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.swagger.v3.oas.models.Operation
import org.openapitools.codegen.CodegenOperation
import org.openapitools.codegen.languages.KotlinSpringServerCodegen
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Locale
import java.util.regex.PatternSyntaxException

private const val API_VIEWS_CONFIG = "apiViewsConfig"
private const val FAIL_ON_UNMATCHED = "failOnUnmatchedOperations"

class KotlinSpringViewGenerator : KotlinSpringServerCodegen() {
    private val logger = LoggerFactory.getLogger(KotlinSpringViewGenerator::class.java)

    private val yamlMapper = ObjectMapper(YAMLFactory())
        .registerModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)

    private lateinit var viewsConfig: ApiViewsConfig
    private var knownViewNames: Set<String> = emptySet()
    private var failOnUnmatchedOperations: Boolean = false

    init {
        setUseTags(true)
        addOption(
            API_VIEWS_CONFIG,
            "Path to the YAML file describing how source tags should be split into derived API interfaces.",
            ""
        )
        addOption(
            FAIL_ON_UNMATCHED,
            "If true, fail the build when an operation from a configured sourceTag does not match any view output.",
            "false"
        )
    }

    override fun getName(): String = "kotlin-spring-views"

    override fun processOpts() {
        super.processOpts()
        setUseTags(true)

        val configPath = additionalProperties[API_VIEWS_CONFIG]?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required option '$API_VIEWS_CONFIG'")

        viewsConfig = yamlMapper.readValue(File(configPath), ApiViewsConfig::class.java)
        viewsConfig.validate()
        knownViewNames = viewsConfig.apiViews
            .flatMap { view -> view.outputs.map { it.name } }
            .toSet()

        failOnUnmatchedOperations = additionalProperties[FAIL_ON_UNMATCHED]?.toString()?.toBoolean() ?: false
    }

    override fun toApiName(name: String): String {
        return if (knownViewNames.contains(name)) sanitizeName(name) else super.toApiName(name)
    }

    override fun addOperationToGroup(
        tag: String,
        resourcePath: String,
        operation: Operation,
        co: CodegenOperation,
        operations: MutableMap<String, MutableList<CodegenOperation>>
    ) {
        val matchingOutputs = viewsConfig.apiViews
            .filter { it.sourceTag == tag }
            .flatMap { view ->
                view.outputs.filter { output ->
                    output.match.matches(operation = operation, codegenOperation = co)
                }
            }

        require(matchingOutputs.size <= 1) {
            "Operation '${co.operationId}' from tag '$tag' matched multiple view outputs: ${
                matchingOutputs.map { it.name }
            }. Each operation must resolve to at most one derived interface."
        }

        if (matchingOutputs.isEmpty()) {
            val message = "Operation '${co.operationId}' (${co.httpMethod}) from tag '$tag' " +
                "did not match any view output and will be excluded from generated views."
            if (failOnUnmatchedOperations) {
                throw IllegalStateException(message)
            } else {
                logger.warn(message)
            }
            return
        }

        super.addOperationToGroup(matchingOutputs.first().name, resourcePath, operation, co, operations)
    }
}

data class ApiViewsConfig(
    val apiViews: List<ApiViewDefinition> = emptyList()
) {
    fun validate() {
        require(apiViews.isNotEmpty()) {
            "apiViews must contain at least one view definition"
        }

        val allOutputNames = mutableListOf<String>()

        apiViews.forEach { view ->
            require(view.sourceTag.isNotBlank()) {
                "Each apiViews entry must define a non-blank sourceTag"
            }
            require(view.outputs.isNotEmpty()) {
                "View '${view.sourceTag}' must define at least one output"
            }

            view.outputs.forEach { output ->
                require(output.name.isNotBlank()) {
                    "Each output in view '${view.sourceTag}' must have a non-blank name"
                }
                require(output.match.hasAnyCriteria()) {
                    "Output '${output.name}' in view '${view.sourceTag}' has an empty match block. " +
                        "At least one match criterion (httpMethods, operationIds, or extensions) must be specified."
                }
            }

            val duplicateNames = view.outputs
                .groupBy { it.name }
                .filterValues { it.size > 1 }
                .keys
            require(duplicateNames.isEmpty()) {
                "View '${view.sourceTag}' contains duplicate output names: $duplicateNames"
            }

            allOutputNames.addAll(view.outputs.map { it.name })
        }

        val globalDuplicates = allOutputNames
            .groupBy { it }
            .filterValues { it.size > 1 }
            .keys
        require(globalDuplicates.isEmpty()) {
            "Output names must be unique across all views. Duplicates: $globalDuplicates"
        }
    }
}

data class ApiViewDefinition(
    val sourceTag: String = "",
    val outputs: List<ViewOutputDefinition> = emptyList()
)

data class ViewOutputDefinition(
    val name: String = "",
    val match: ViewMatchDefinition = ViewMatchDefinition()
)

data class OperationIdMatchDefinition(
    val include: List<String> = emptyList(),
    val includeRegex: String? = null,
    val excludeRegex: String? = null
) {
    val compiledIncludeRegex: Regex? = includeRegex?.let { pattern ->
        try {
            Regex(pattern)
        } catch (e: PatternSyntaxException) {
            throw IllegalArgumentException("Invalid includeRegex pattern '$pattern': ${e.message}", e)
        }
    }

    val compiledExcludeRegex: Regex? = excludeRegex?.let { pattern ->
        try {
            Regex(pattern)
        } catch (e: PatternSyntaxException) {
            throw IllegalArgumentException("Invalid excludeRegex pattern '$pattern': ${e.message}", e)
        }
    }
}

data class ViewMatchDefinition(
    val httpMethods: List<String> = emptyList(),
    val operationIds: OperationIdMatchDefinition? = null,
    val extensions: Map<String, String> = emptyMap()
) {
    private val normalizedMethods: Set<String> = httpMethods.map { it.uppercase(Locale.ROOT) }.toSet()

    fun hasAnyCriteria(): Boolean =
        httpMethods.isNotEmpty() || operationIds != null || extensions.isNotEmpty()

    fun matches(operation: Operation, codegenOperation: CodegenOperation): Boolean {
        if (normalizedMethods.isNotEmpty() && codegenOperation.httpMethod.uppercase(Locale.ROOT) !in normalizedMethods) {
            return false
        }

        operationIds?.let { ids ->
            if (ids.include.isNotEmpty() && codegenOperation.operationId !in ids.include) {
                return false
            }
            ids.compiledIncludeRegex?.let { regex ->
                if (!regex.matches(codegenOperation.operationId)) {
                    return false
                }
            }
            ids.compiledExcludeRegex?.let { regex ->
                if (regex.matches(codegenOperation.operationId)) {
                    return false
                }
            }
        }

        if (extensions.isNotEmpty()) {
            val operationExtensions = operation.extensions ?: emptyMap()
            val hasAllExtensions = extensions.all { (key, expectedValue) ->
                operationExtensions[key]?.toString() == expectedValue
            }
            if (!hasAllExtensions) {
                return false
            }
        }

        return true
    }
}
