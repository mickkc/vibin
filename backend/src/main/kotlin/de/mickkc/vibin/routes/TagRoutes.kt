package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.dto.tags.TagDto
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.dto.tags.TagEditDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.TagRepo
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Application.configureTagRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/tags", PermissionType.VIEW_TAGS) {

            val query = call.getStringOrDefault("query", "")
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.takeIf { it > 0 }

            val allTags = TagRepo.getAll(query, limit)
            call.respond(TagRepo.toDto(allTags))
        }.describe {
            operationId = "getAllTags"
            summary = "Get all tags"
            tag("Tags")
            parameters {
                q("query", "pop", required = false)
                q("limit", 10, required = false)
            }
            authResponses(PermissionType.VIEW_TAGS) {
                successResponse<List<TagDto>>("All tags")
            }
        }

        getP("/api/tags/ids", PermissionType.VIEW_TAGS) {
            val idsParam = call.request.queryParameters["ids"] ?: missingParameter("ids")
            val ids = idsParam.split(",").mapNotNull { it.toLongOrNull() }
            val tags = TagRepo.getByIds(ids)
            call.respond(TagRepo.toDto(tags))
        }.describe {
            operationId = "getTagsByIds"
            summary = "Get multiple tags by their Ids"
            tag("Tags")
            parameters {
                q("ids", "1,2,3", required = true,
                    description = "comma-separated list of tag Ids")
            }
            authResponses(PermissionType.VIEW_TAGS) {
                successResponse<List<TagDto>>("Tags sorted by their name")
            }
        }

        postP("/api/tags", PermissionType.CREATE_TAGS) {

            val editDto = call.receive<TagEditDto>()
            editDto.validate()

            if (TagRepo.doesTagExist(editDto.name)) {
                invalidParameter("name")
            }

            val createdTag = TagRepo.create(editDto)
            call.respond(TagRepo.toDto(createdTag))
        }.describe {
            operationId = "createTag"
            summary = "Create a new tag"
            tag("Tags")
            requestBody {
                description = "The tag edit data. At least the name must be non-null."
                schema = jsonSchema<TagEditDto>()
            }
            authResponses(PermissionType.CREATE_TAGS) {
                successResponse<TagDto>("The created tags")
                missingParameterResponse("name")
                invalidParameterResponse("name", "name is longer than 255 characters or already taken by another tag.")
            }
        }

        putP("/api/tags/{id}", PermissionType.MANAGE_TAGS) {
            val tagId = call.getLongParameter("id")
            val editDto = call.receive<TagEditDto>()
            editDto.validate()

            val existingTag = TagRepo.getById(tagId) ?: notFound()

            if (existingTag.name != editDto.name && TagRepo.doesTagExist(editDto.name)) {
                invalidParameter("name")
            }

            val updatedTag = TagRepo.update(existingTag, editDto)
            call.respond(TagRepo.toDto(updatedTag))
        }.describe {
            operationId = "updateTag"
            summary = "Update a tag"
            tag("Tags")
            parameters {
                p("id", 1L, description = "The tag Id")
            }
            requestBody {
                description = "The tag edit data. Null values will not be updated"
                schema = jsonSchema<TagEditDto>()
            }
            authResponses(PermissionType.MANAGE_TAGS) {
                successResponse<TagDto>("The updated tag")
                notFoundResponse("Tag")
                invalidParameterResponse("name", "name is longer than 255 characters or already taken by another tag.")
            }
        }

        deleteP("/api/tags/{id}", PermissionType.DELETE_TAGS) {
            val tagId = call.getLongParameter("id")
            val existingTag = TagRepo.getById(tagId) ?: notFound()
            TagRepo.delete(existingTag)
            call.success()
        }.describe {
            operationId = "deleteTag"
            summary = "Delete a tag"
            tag("Tags")
            parameters {
                p("id", 1L, description = "The tag Id")
            }
            authResponses(PermissionType.DELETE_TAGS) {
                successResponse<SuccessDto>("Success")
                notFoundResponse("Tag")
            }
        }

        getP("/api/tags/autocomplete", PermissionType.VIEW_TAGS) {
            val query = call.getStringParameter("query")
            val limit = call.getIntOrDefault("limit", 10)

            val tagNames = TagRepo.autocomplete(query, limit)
            call.respond(tagNames)
        }.describe {
            operationId = "getTagAutocomplete"
            summary = "Get tag name autocompletions"
            tag("Tags")
            parameters {
                q<String>("query", "pop", required = true)
                q("limit", 10, required = false, description = "Maximum number of completions to return.")
            }
            authResponses(PermissionType.VIEW_TAGS) {
                successResponse<List<String>>("Autocompleted names",
                    exampleValue = listOf("pop", "dance-pop", "synth-pop"))
            }
        }

        getP("/api/tags/named/{name}", PermissionType.VIEW_TAGS) {
            val name = call.getStringParameter("name").trim()
            val tag = TagRepo.getByName(name) ?: notFound()
            call.respond(TagRepo.toDto(tag))
        }.describe {
            operationId = "getTagByName"
            summary = "Get a tag by it's name (case-insensitive)"
            tag("Tags")
            parameters {
                p("name", "pop")
            }
            authResponses(PermissionType.VIEW_TAGS) {
                successResponse<TagDto>("A found tag with that name")
                notFoundResponse("Tag with that name")
            }
        }

        getP("/api/tags/check/{name}", PermissionType.VIEW_TAGS) {
            val name = call.getStringParameter("name").trim()
            val exists = TagRepo.doesTagExist(name)
            call.success(exists)
        }.describe {
            operationId = "checkIfTagExists"
            summary = "Check if a tag with a name exists"
            tag("Tags")
            parameters {
                p("name", "pop")
            }
            authResponses(PermissionType.VIEW_TAGS) {
                successResponse<SuccessDto>("True if a tag with that name exists, false if not")
            }
        }
    }
}