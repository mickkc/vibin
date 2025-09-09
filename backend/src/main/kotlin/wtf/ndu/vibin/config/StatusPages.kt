package wtf.ndu.vibin.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.respond
import wtf.ndu.vibin.dto.errors.ErrorDto
import wtf.ndu.vibin.dto.errors.ErrorDtoType

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorDto.fromType(
                    type = ErrorDtoType.INTERNAL_ERROR,
                    "errorMessage" to (cause.message ?: "No message")
                )
            )
        }

        status(HttpStatusCode.Unauthorized) {
            call.respond(
                status = HttpStatusCode.Unauthorized,
                message = ErrorDto.fromType(ErrorDtoType.UNAUTHORIZED)
            )
        }

        status(HttpStatusCode.NotFound) {
            call.respond(
                status = HttpStatusCode.NotFound,
                message = ErrorDto.fromType(ErrorDtoType.NOT_FOUND)
            )
        }

        status(HttpStatusCode.BadRequest) {
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = ErrorDto.fromType(ErrorDtoType.INVALID_PARAMETER)
            )
        }
    }
}