package io.github.mlsgrr.apiviews.codegen

import io.swagger.v3.oas.models.Operation
import org.openapitools.codegen.CodegenOperation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ViewMatchDefinitionTest {

    // --- httpMethods ---

    @Test
    fun `matches GET when httpMethods contains GET`() {
        val match = ViewMatchDefinition(httpMethods = listOf("GET"))
        assertTrue(match.matches(operation(), codegenOp(httpMethod = "GET", operationId = "getCat")))
    }

    @Test
    fun `rejects POST when httpMethods only contains GET`() {
        val match = ViewMatchDefinition(httpMethods = listOf("GET"))
        assertFalse(match.matches(operation(), codegenOp(httpMethod = "POST", operationId = "createCat")))
    }

    @Test
    fun `httpMethods matching is case insensitive`() {
        val match = ViewMatchDefinition(httpMethods = listOf("get"))
        assertTrue(match.matches(operation(), codegenOp(httpMethod = "GET", operationId = "getCat")))
    }

    @Test
    fun `matches when httpMethods contains multiple methods`() {
        val match = ViewMatchDefinition(httpMethods = listOf("POST", "PUT", "PATCH", "DELETE"))
        assertTrue(match.matches(operation(), codegenOp(httpMethod = "PUT", operationId = "updateCat")))
        assertFalse(match.matches(operation(), codegenOp(httpMethod = "GET", operationId = "getCat")))
    }

    // --- operationIds.include ---

    @Test
    fun `matches when operationId is in include list`() {
        val match = ViewMatchDefinition(
            operationIds = OperationIdMatchDefinition(include = listOf("getCat", "listCats"))
        )
        assertTrue(match.matches(operation(), codegenOp(httpMethod = "GET", operationId = "getCat")))
    }

    @Test
    fun `rejects when operationId is not in include list`() {
        val match = ViewMatchDefinition(
            operationIds = OperationIdMatchDefinition(include = listOf("getCat"))
        )
        assertFalse(match.matches(operation(), codegenOp(httpMethod = "GET", operationId = "listCats")))
    }

    // --- operationIds.includeRegex ---

    @Test
    fun `matches when operationId matches includeRegex`() {
        val match = ViewMatchDefinition(
            operationIds = OperationIdMatchDefinition(includeRegex = "get.*")
        )
        assertTrue(match.matches(operation(), codegenOp(httpMethod = "GET", operationId = "getCat")))
    }

    @Test
    fun `rejects when operationId does not match includeRegex`() {
        val match = ViewMatchDefinition(
            operationIds = OperationIdMatchDefinition(includeRegex = "get.*")
        )
        assertFalse(match.matches(operation(), codegenOp(httpMethod = "POST", operationId = "createCat")))
    }

    // --- operationIds.excludeRegex ---

    @Test
    fun `rejects when operationId matches excludeRegex`() {
        val match = ViewMatchDefinition(
            operationIds = OperationIdMatchDefinition(excludeRegex = "delete.*")
        )
        assertFalse(match.matches(operation(), codegenOp(httpMethod = "DELETE", operationId = "deleteCat")))
    }

    @Test
    fun `matches when operationId does not match excludeRegex`() {
        val match = ViewMatchDefinition(
            operationIds = OperationIdMatchDefinition(excludeRegex = "delete.*")
        )
        assertTrue(match.matches(operation(), codegenOp(httpMethod = "GET", operationId = "getCat")))
    }

    // --- extensions ---

    @Test
    fun `matches when operation has all required extensions`() {
        val match = ViewMatchDefinition(extensions = mapOf("x-cqrs" to "command"))
        val op = operation(extensions = mapOf("x-cqrs" to "command"))
        assertTrue(match.matches(op, codegenOp(httpMethod = "POST", operationId = "createCat")))
    }

    @Test
    fun `rejects when operation is missing required extension`() {
        val match = ViewMatchDefinition(extensions = mapOf("x-cqrs" to "command"))
        val op = operation(extensions = mapOf("x-cqrs" to "query"))
        assertFalse(match.matches(op, codegenOp(httpMethod = "GET", operationId = "getCat")))
    }

    @Test
    fun `rejects when operation has no extensions but match requires them`() {
        val match = ViewMatchDefinition(extensions = mapOf("x-cqrs" to "command"))
        assertFalse(match.matches(operation(), codegenOp(httpMethod = "POST", operationId = "createCat")))
    }

    // --- combined criteria ---

    @Test
    fun `all criteria must pass together`() {
        val match = ViewMatchDefinition(
            httpMethods = listOf("POST"),
            operationIds = OperationIdMatchDefinition(include = listOf("createCat")),
            extensions = mapOf("x-cqrs" to "command")
        )
        val op = operation(extensions = mapOf("x-cqrs" to "command"))
        assertTrue(match.matches(op, codegenOp(httpMethod = "POST", operationId = "createCat")))
    }

    @Test
    fun `fails if one criterion does not match`() {
        val match = ViewMatchDefinition(
            httpMethods = listOf("POST"),
            operationIds = OperationIdMatchDefinition(include = listOf("createCat"))
        )
        // wrong httpMethod
        assertFalse(match.matches(operation(), codegenOp(httpMethod = "GET", operationId = "createCat")))
        // wrong operationId
        assertFalse(match.matches(operation(), codegenOp(httpMethod = "POST", operationId = "deleteCat")))
    }

    // --- hasAnyCriteria ---

    @Test
    fun `hasAnyCriteria returns false for empty definition`() {
        assertFalse(ViewMatchDefinition().hasAnyCriteria())
    }

    @Test
    fun `hasAnyCriteria returns true when httpMethods is set`() {
        assertTrue(ViewMatchDefinition(httpMethods = listOf("GET")).hasAnyCriteria())
    }

    @Test
    fun `hasAnyCriteria returns true when operationIds is set`() {
        assertTrue(ViewMatchDefinition(operationIds = OperationIdMatchDefinition(include = listOf("a"))).hasAnyCriteria())
    }

    @Test
    fun `hasAnyCriteria returns true when extensions is set`() {
        assertTrue(ViewMatchDefinition(extensions = mapOf("x-cqrs" to "command")).hasAnyCriteria())
    }

    // --- regex pre-compilation ---

    @Test
    fun `invalid includeRegex throws at construction time`() {
        val ex = kotlin.test.assertFailsWith<IllegalArgumentException> {
            OperationIdMatchDefinition(includeRegex = "[invalid")
        }
        assertTrue(ex.message!!.contains("Invalid includeRegex"))
    }

    @Test
    fun `invalid excludeRegex throws at construction time`() {
        val ex = kotlin.test.assertFailsWith<IllegalArgumentException> {
            OperationIdMatchDefinition(excludeRegex = "(unclosed")
        }
        assertTrue(ex.message!!.contains("Invalid excludeRegex"))
    }

    // --- helpers ---

    private fun operation(extensions: Map<String, Any>? = null): Operation {
        return Operation().apply {
            this.extensions = extensions
        }
    }

    private fun codegenOp(httpMethod: String, operationId: String): CodegenOperation {
        return CodegenOperation().apply {
            this.httpMethod = httpMethod
            this.operationId = operationId
        }
    }
}
