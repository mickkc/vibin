package parsers

import de.mickkc.vibin.config.combineOpenApiSpec
import io.swagger.v3.parser.OpenAPIV3Parser
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenApiBundleTest {
    @Test
    fun mergeOpenApiConfigToSingleFile() {

        val combined = combineOpenApiSpec()

        val yaml = combined.readText()

        // No unresolved external file refs should remain in the bundled output.
        assertFalse(yaml.contains("methods/"))
        assertFalse(yaml.contains("models/"))

        val result = OpenAPIV3Parser().readContents(yaml)
        assertNotNull(result?.openAPI)
        val openApi = result.openAPI
        assertFalse(openApi.paths.isEmpty())

        assertTrue(result.messages.isEmpty())

        println("Bundled ${openApi.paths.size} paths, ${openApi.components?.schemas?.size ?: 0} schemas")
    }
}