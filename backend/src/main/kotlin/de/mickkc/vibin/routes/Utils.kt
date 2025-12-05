package de.mickkc.vibin.routes

import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.auth.UserPrincipal
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.dto.errors.ErrorDto
import de.mickkc.vibin.dto.errors.ErrorDtoType
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PermissionRepo
import de.mickkc.vibin.repos.UserRepo

suspend fun RoutingCall.success(success: Boolean = true) {
    respond(mapOf("success" to success))
}

suspend fun RoutingCall.respond(error: ErrorDto) {
    respond(error.type.code, error)
}

suspend fun RoutingCall.missingParameter(param: String) {
    respond(ErrorDto.fromType(ErrorDtoType.MISSING_PARAMETER, "parameterName" to param))
}

suspend fun RoutingCall.invalidParameter(param: String, vararg allowedParameters: String) {
    respond(ErrorDto.fromType(ErrorDtoType.INVALID_PARAMETER, "parameterName" to param, "reason" to allowedParameters.joinToString(", ")))
}

suspend fun RoutingCall.unauthorized(reason: String = "Unauthorized") {
    respond(ErrorDto.fromType(ErrorDtoType.UNAUTHORIZED, "reason" to reason))
}

suspend fun RoutingCall.conflict() {
    respond(ErrorDto.fromType(ErrorDtoType.CONFLICT))
}

suspend fun RoutingCall.forbidden(vararg requiredPermissions: PermissionType) {
    respond(ErrorDto.fromType(
        ErrorDtoType.NO_PERMISSION,
        "requiredPermissions" to requiredPermissions.joinToString(",") { it.id }
    ))
}

suspend fun RoutingCall.notFound() {
    respond(ErrorDto.fromType(ErrorDtoType.NOT_FOUND))
}

suspend fun RoutingCall.rateLimitExceeded() {
    respond(ErrorDto.fromType(ErrorDtoType.RATE_LIMIT_EXCEEDED))
}

fun RoutingCall.getToken(): String? {
    val principal = principal<UserPrincipal>()
    return principal?.token
}

fun RoutingCall.getUser(): UserEntity? {
    val principal = principal<UserPrincipal>()
    return principal?.let {
        UserRepo.getById(principal.userId)
    }
}

fun RoutingCall.getUserId(): Long? {
    val principal = principal<UserPrincipal>()
    return principal?.userId
}

fun RoutingCall.hasPermissions(vararg permissions: PermissionType): Boolean {
    val userId = getUserId() ?: return false
    if (permissions.isEmpty()) return true
    return PermissionRepo.hasPermissions(userId, permissions.toList())
}

/**
 * Generates a client ID for rate limiting based on the client's IP address.
 * Checks proxy headers (X-Forwarded-For, X-Real-IP) first, then falls back to remote address.
 *
 * @return A unique identifier for the client
 */
fun RoutingCall.getClientId(): String {
    // TODO: Add config option to enable/disable proxy header checks
    val forwardedFor = request.headers["X-Forwarded-For"]
    if (forwardedFor != null) {
        val clientIp = forwardedFor.split(",").firstOrNull()?.trim()
        if (!clientIp.isNullOrEmpty()) {
            return clientIp
        }
    }

    val realIp = request.headers["X-Real-IP"]
    if (!realIp.isNullOrEmpty()) {
        return realIp
    }

    return request.local.remoteAddress
}

fun Route.getP(path: String, vararg permissions: PermissionType, body: suspend RoutingContext.() -> Unit) {
    get(path) {
        if (permissions.isNotEmpty() && !call.hasPermissions(*permissions)) {
            return@get call.forbidden(*permissions)
        }
        body()
    }
}

fun Route.postP(path: String, vararg permissions: PermissionType, body: suspend RoutingContext.() -> Unit) {
    post(path) {
        if (permissions.isNotEmpty() && !call.hasPermissions(*permissions)) {
            return@post call.forbidden(*permissions)
        }
        body()
    }
}

fun Route.putP(path: String, vararg permissions: PermissionType, body: suspend RoutingContext.() -> Unit) {
    put(path) {
        if (permissions.isNotEmpty() && !call.hasPermissions(*permissions)) {
            return@put call.forbidden(*permissions)
        }
        body()
    }
}

fun Route.deleteP(path: String, vararg permissions: PermissionType, body: suspend RoutingContext.() -> Unit) {
    delete(path) {
        if (permissions.isNotEmpty() && !call.hasPermissions(*permissions)) {
            return@delete call.forbidden(*permissions)
        }
        body()
    }
}

data class PaginatedSearchParams(
    val query: String,
    val page: Int,
    val pageSize: Int
) {
    val offset: Long
        get() = ((page - 1) * pageSize).toLong()
}

suspend fun RoutingCall.getPaginatedSearchParams(): PaginatedSearchParams? {
    val query = request.queryParameters["query"] ?: ""
    val page = request.queryParameters["page"]?.toIntOrNull() ?: 1
    val pageSize = request.queryParameters["pageSize"]?.toIntOrNull() ?: 50

    if (page <= 0) {
        invalidParameter("page", "page > 0")
        return null
    }

    if (pageSize !in 1..100) {
        invalidParameter("pageSize", "0 < pageSize <= 100")
        return null
    }

    return PaginatedSearchParams(
        query = query,
        page = page,
        pageSize = pageSize
    )
}

suspend fun RoutingCall.getBooleanOrDefault(paramName: String, default: Boolean): Boolean? {
    val paramValue = this.parameters[paramName]

    if (paramValue == null) {
        return default
    }

    return when (paramValue.lowercase()) {
        "true", "1", "yes" -> true
        "false", "0", "no" -> false
        else -> {
            invalidParameter(paramName, "true", "false")
            null
        }
    }
}

suspend fun RoutingCall.getBooleanParameter(paramName: String): Boolean? {
    val paramValue = this.parameters[paramName]

    if (paramValue == null) {
        missingParameter(paramName)
        return null
    }

    return when (paramValue.lowercase()) {
        "true", "1", "yes" -> true
        "false", "0", "no" -> false
        else -> {
            invalidParameter(paramName, "true", "false")
            null
        }
    }
}

suspend fun RoutingCall.getStringOrDefault(paramName: String, default: String): String {
    return this.parameters[paramName] ?: default
}

suspend fun RoutingCall.getStringParameter(paramName: String): String? {
    val paramValue = this.parameters[paramName]

    if (paramValue == null) {
        missingParameter(paramName)
        return null
    }

    return paramValue
}

suspend fun RoutingCall.getLongOrDefault(paramName: String, default: Long): Long? {
    val paramValue = this.parameters[paramName] ?: return default

    val longValue = paramValue.toLongOrNull()
    if (longValue == null) {
        invalidParameter(paramName)
        return null
    }

    return longValue
}

suspend fun RoutingCall.getLongParameter(paramName: String): Long? {
    val paramValue = this.parameters[paramName]

    if (paramValue == null) {
        missingParameter(paramName)
        return null
    }

    val longValue = paramValue.toLongOrNull()
    if (longValue == null) {
        invalidParameter(paramName)
        return null
    }

    return longValue
}

suspend fun RoutingCall.getIntOrDefault(paramName: String, default: Int): Int? {
    val paramValue = this.parameters[paramName] ?: return default

    val intValue = paramValue.toIntOrNull()
    if (intValue == null) {
        invalidParameter(paramName)
        return null
    }

    return intValue
}

suspend fun RoutingCall.getIntParameter(paramName: String): Int? {
    val paramValue = this.parameters[paramName]

    if (paramValue == null) {
        missingParameter(paramName)
        return null
    }

    val intValue = paramValue.toIntOrNull()
    if (intValue == null) {
        invalidParameter(paramName)
        return null
    }

    return intValue
}