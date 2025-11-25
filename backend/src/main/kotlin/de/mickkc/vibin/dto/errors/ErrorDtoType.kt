package de.mickkc.vibin.dto.errors

import io.ktor.http.HttpStatusCode

enum class ErrorDtoType(val code: HttpStatusCode) {
    MISSING_PARAMETER(HttpStatusCode.BadRequest),
    INVALID_PARAMETER(HttpStatusCode.BadRequest),
    NOT_FOUND(HttpStatusCode.NotFound),
    NO_PERMISSION(HttpStatusCode.Forbidden),
    INTERNAL_ERROR(HttpStatusCode.InternalServerError),
    UNAUTHORIZED(HttpStatusCode.Unauthorized),
    RATE_LIMIT_EXCEEDED(HttpStatusCode.TooManyRequests),

    CONFLICT(HttpStatusCode.Conflict),
}