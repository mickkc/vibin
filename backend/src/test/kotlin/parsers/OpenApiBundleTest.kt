package parsers

import de.mickkc.vibin.config.combineOpenApiSpec
import io.swagger.v3.parser.OpenAPIV3Parser
import junit.framework.TestCase
import kotlin.test.Test
import kotlin.test.assertNotNull

class OpenApiBundleTest {
    @Test
    fun mergeOpenApiConfigToSingleFile() {

        val combined = combineOpenApiSpec()

        val yaml = combined.readText()

        // No unresolved external file refs should remain in the bundled output.
        TestCase.assertFalse(yaml.contains("methods/"))
        TestCase.assertFalse(yaml.contains("models/"))

        val result = OpenAPIV3Parser().readContents(yaml)
        assertNotNull(result?.openAPI)
        val openApi = result.openAPI
        TestCase.assertFalse(openApi.paths.isEmpty())

        println("Bundled ${openApi.paths.size} paths, ${openApi.components?.schemas?.size ?: 0} schemas; messages=${result.messages}")
    }
}