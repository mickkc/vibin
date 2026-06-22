package de.mickkc.vibin.config

import io.ktor.server.application.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.routing.*
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import java.io.File

fun Application.configureOpenApi() {

    // Combine documentation split into multiple files into a single file
    // because the OpenAPI plugin only supports one file.
    val combinedSpec = combineOpenApiSpec()

    routing {
        openAPI(path = "/api/docs", swaggerFile = combinedSpec.absolutePath)
    }
}

private fun combineOpenApiSpec(): File {
    val options = ParseOptions().apply {
        isResolve = true
        isResolveFully = false
    }

    val result = OpenAPIV3Parser()
        .readLocation("openapi/documentation.yaml", null, options)

    val openApi = result.openAPI
        ?: error("Failed to parse OpenAPI spec: ${result.messages}")

    return File.createTempFile("vibin-openapi", ".yaml").apply {
        deleteOnExit()
        writeText(Yaml.pretty(openApi))
    }
}
