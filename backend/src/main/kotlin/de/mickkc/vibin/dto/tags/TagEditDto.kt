package de.mickkc.vibin.dto.tags

import de.mickkc.vibin.dto.ValidatableBaseDto
import de.mickkc.vibin.routes.invalidParameter
import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class TagEditDto(

    @JsonSchema.Example("\"pop\"")
    @JsonSchema.MaxLength(255)
    val name: String,

    @JsonSchema.Example("\"Pop-Songs\"")
    val description: String?,

    @JsonSchema.Example("10")
    @JsonSchema.Description("How likely tracks sharing this tag are being recommended, from 0 to 10.")
    @JsonSchema.Minimum(0.0)
    @JsonSchema.Maximum(10.0)
    val importance: Int

) : ValidatableBaseDto {
    override fun validate() {
        if (name.length > 255) invalidParameter("name", "name.length <= 255")
        if (importance !in 0..10) invalidParameter("importance", "0 <= importance <= 10")
    }
}