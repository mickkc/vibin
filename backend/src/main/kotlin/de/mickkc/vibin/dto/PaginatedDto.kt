package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedDto<T>(

    val items: List<T>,

    @JsonSchema.Example("1")
    val total: Int,

    @JsonSchema.Example("50")
    val pageSize: Int,

    @JsonSchema.Example("1")
    val currentPage: Int,

    @JsonSchema.Example("false")
    val hasNext: Boolean = total > currentPage * pageSize,

    @JsonSchema.Example("false")
    val hasPrevious: Boolean = currentPage > 1
)