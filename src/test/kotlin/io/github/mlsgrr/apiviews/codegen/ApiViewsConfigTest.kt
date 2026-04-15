package io.github.mlsgrr.apiviews.codegen

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ApiViewsConfigTest {

    @Test
    fun `valid config passes validation`() {
        val config = ApiViewsConfig(
            apiViews = listOf(
                ApiViewDefinition(
                    sourceTag = "Cats",
                    outputs = listOf(
                        ViewOutputDefinition(
                            name = "CatsCommandsApi",
                            match = ViewMatchDefinition(httpMethods = listOf("POST", "PUT"))
                        ),
                        ViewOutputDefinition(
                            name = "CatsQueriesApi",
                            match = ViewMatchDefinition(httpMethods = listOf("GET"))
                        )
                    )
                )
            )
        )
        config.validate() // should not throw
    }

    @Test
    fun `empty apiViews list fails validation`() {
        val config = ApiViewsConfig(apiViews = emptyList())
        val ex = assertFailsWith<IllegalArgumentException> { config.validate() }
        assertTrue(ex.message!!.contains("at least one view definition"))
    }

    @Test
    fun `blank sourceTag fails validation`() {
        val config = ApiViewsConfig(
            apiViews = listOf(
                ApiViewDefinition(
                    sourceTag = "  ",
                    outputs = listOf(
                        ViewOutputDefinition(
                            name = "SomeApi",
                            match = ViewMatchDefinition(httpMethods = listOf("GET"))
                        )
                    )
                )
            )
        )
        val ex = assertFailsWith<IllegalArgumentException> { config.validate() }
        assertTrue(ex.message!!.contains("non-blank sourceTag"))
    }

    @Test
    fun `empty outputs list fails validation`() {
        val config = ApiViewsConfig(
            apiViews = listOf(
                ApiViewDefinition(sourceTag = "Cats", outputs = emptyList())
            )
        )
        val ex = assertFailsWith<IllegalArgumentException> { config.validate() }
        assertTrue(ex.message!!.contains("at least one output"))
    }

    @Test
    fun `blank output name fails validation`() {
        val config = ApiViewsConfig(
            apiViews = listOf(
                ApiViewDefinition(
                    sourceTag = "Cats",
                    outputs = listOf(
                        ViewOutputDefinition(
                            name = "",
                            match = ViewMatchDefinition(httpMethods = listOf("GET"))
                        )
                    )
                )
            )
        )
        val ex = assertFailsWith<IllegalArgumentException> { config.validate() }
        assertTrue(ex.message!!.contains("non-blank name"))
    }

    @Test
    fun `empty match block fails validation`() {
        val config = ApiViewsConfig(
            apiViews = listOf(
                ApiViewDefinition(
                    sourceTag = "Cats",
                    outputs = listOf(
                        ViewOutputDefinition(name = "CatsAllApi", match = ViewMatchDefinition())
                    )
                )
            )
        )
        val ex = assertFailsWith<IllegalArgumentException> { config.validate() }
        assertTrue(ex.message!!.contains("empty match block"))
    }

    @Test
    fun `duplicate output names within same view fails validation`() {
        val config = ApiViewsConfig(
            apiViews = listOf(
                ApiViewDefinition(
                    sourceTag = "Cats",
                    outputs = listOf(
                        ViewOutputDefinition(
                            name = "CatsApi",
                            match = ViewMatchDefinition(httpMethods = listOf("GET"))
                        ),
                        ViewOutputDefinition(
                            name = "CatsApi",
                            match = ViewMatchDefinition(httpMethods = listOf("POST"))
                        )
                    )
                )
            )
        )
        val ex = assertFailsWith<IllegalArgumentException> { config.validate() }
        assertTrue(ex.message!!.contains("duplicate output names"))
    }

    @Test
    fun `duplicate output names across different views fails validation`() {
        val config = ApiViewsConfig(
            apiViews = listOf(
                ApiViewDefinition(
                    sourceTag = "Cats",
                    outputs = listOf(
                        ViewOutputDefinition(
                            name = "SharedApi",
                            match = ViewMatchDefinition(httpMethods = listOf("GET"))
                        )
                    )
                ),
                ApiViewDefinition(
                    sourceTag = "Dogs",
                    outputs = listOf(
                        ViewOutputDefinition(
                            name = "SharedApi",
                            match = ViewMatchDefinition(httpMethods = listOf("GET"))
                        )
                    )
                )
            )
        )
        val ex = assertFailsWith<IllegalArgumentException> { config.validate() }
        assertTrue(ex.message!!.contains("unique across all views"))
    }

    @Test
    fun `multiple valid views pass validation`() {
        val config = ApiViewsConfig(
            apiViews = listOf(
                ApiViewDefinition(
                    sourceTag = "Cats",
                    outputs = listOf(
                        ViewOutputDefinition(
                            name = "CatsCommandsApi",
                            match = ViewMatchDefinition(httpMethods = listOf("POST"))
                        )
                    )
                ),
                ApiViewDefinition(
                    sourceTag = "Dogs",
                    outputs = listOf(
                        ViewOutputDefinition(
                            name = "DogsCommandsApi",
                            match = ViewMatchDefinition(httpMethods = listOf("POST"))
                        )
                    )
                )
            )
        )
        config.validate() // should not throw
    }
}
