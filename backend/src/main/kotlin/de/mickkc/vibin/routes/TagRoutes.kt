package de.mickkc.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.dto.tags.TagEditDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.TagRepo

fun Application.configureTagRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/tags", PermissionType.VIEW_TAGS) {

            val query = call.getStringOrDefault("query", "")
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.takeIf { it > 0 }

            val allTags = TagRepo.getAll(query, limit)
            call.respond(TagRepo.toDto(allTags))
        }

        getP("/api/tags/ids", PermissionType.VIEW_TAGS) {
            val idsParam = call.request.queryParameters["ids"] ?: missingParameter("ids")
            val ids = idsParam.split(",").mapNotNull { it.toLongOrNull() }
            val tags = TagRepo.getByIds(ids)
            call.respond(TagRepo.toDto(tags))
        }

        postP("/api/tags", PermissionType.CREATE_TAGS) {

            val editDto = call.receive<TagEditDto>()
            editDto.validate()

            if (TagRepo.doesTagExist(editDto.name)) {
                invalidParameter("name")
            }

            val createdTag = TagRepo.create(editDto)
            call.respond(TagRepo.toDto(createdTag))
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
        }

        deleteP("/api/tags/{id}", PermissionType.DELETE_TAGS) {
            val tagId = call.getLongParameter("id")
            val existingTag = TagRepo.getById(tagId) ?: notFound()
            TagRepo.delete(existingTag)
            call.success()
        }

        getP("/api/tags/autocomplete", PermissionType.VIEW_TAGS) {
            val query = call.getStringParameter("query")
            val limit = call.getIntOrDefault("limit", 10)

            val tagNames = TagRepo.autocomplete(query, limit)
            call.respond(tagNames)
        }

        getP("/api/tags/named/{name}", PermissionType.VIEW_TAGS) {
            val name = call.getStringParameter("name").trim()
            val tag = TagRepo.getByName(name) ?: notFound()
            call.respond(TagRepo.toDto(tag))
        }

        getP("/api/tags/check/{name}", PermissionType.VIEW_TAGS) {
            val name = call.getStringParameter("name").trim()
            val exists = TagRepo.doesTagExist(name)
            call.success(exists)
        }
    }
}