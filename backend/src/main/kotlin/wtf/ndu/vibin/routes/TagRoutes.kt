package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.dto.tags.TagEditDto
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.TagRepo

fun Application.configureTagRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/tags", PermissionType.VIEW_TAGS) {
            val allTags = TagRepo.getAll()
            call.respond(TagRepo.toDto(allTags))
        }

        postP("/api/tags", PermissionType.CREATE_TAGS) {

            val editDto = call.receive<TagEditDto>()

            if (TagRepo.doesTagExist(editDto.name)) {
                return@postP call.invalidParameter("name")
            }

            val createdTag = TagRepo.create(editDto)
            call.respond(TagRepo.toDto(createdTag))
        }

        putP("/api/tags/{id}", PermissionType.MANAGE_TAGS) {
            val tagId = call.parameters["id"]?.toLongOrNull() ?: return@putP call.missingParameter("id")
            val editDto = call.receive<TagEditDto>()

            val existingTag = TagRepo.getById(tagId) ?: return@putP call.notFound()

            if (existingTag.name != editDto.name && TagRepo.doesTagExist(editDto.name)) {
                return@putP call.invalidParameter("name")
            }

            val updatedTag = TagRepo.update(existingTag, editDto)
            call.respond(TagRepo.toDto(updatedTag))
        }

        deleteP("/api/tags/{id}", PermissionType.DELETE_TAGS) {
            val tagId = call.parameters["id"]?.toLongOrNull() ?: return@deleteP call.missingParameter("id")
            val existingTag = TagRepo.getById(tagId) ?: return@deleteP call.notFound()
            TagRepo.delete(existingTag)
            call.success()
        }

        getP("/api/tags/autocomplete", PermissionType.VIEW_TAGS) {
            val query = call.request.queryParameters["query"] ?: return@getP call.missingParameter("query")
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

            val tagNames = TagRepo.autocomplete(query, limit)
            call.respond(tagNames)
        }

        getP("/api/tags/named/{name}", PermissionType.VIEW_TAGS) {
            val name = call.parameters["name"]?.trim() ?: return@getP call.missingParameter("name")
            val tag = TagRepo.getByName(name) ?: return@getP call.notFound()
            call.respond(TagRepo.toDto(tag))
        }

        getP("/api/tags/check/{name}", PermissionType.VIEW_TAGS) {
            val name = call.parameters["name"]?.trim() ?: return@getP call.missingParameter("name")
            val exists = TagRepo.doesTagExist(name)
            call.success(exists)
        }
    }
}