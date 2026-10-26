package de.mickkc.vibin.routes

import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.auth.UserPrincipal
import de.mickkc.vibin.config.ApiException
import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.dto.errors.ErrorDto
import de.mickkc.vibin.dto.errors.ErrorDtoType
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PermissionRepo
import de.mickkc.vibin.repos.UserRepo
import io.ktor.server.application.ApplicationCall
import kotlin.enums.enumEntries

suspend fun ApplicationCall.success(success: Boolean = true) {
    respond(mapOf("success" to success))
}

suspend fun ApplicationCall.respond(error: ErrorDto) {
    respond(error.type.code, error)
}

fun missingParameter(param: String): Nothing =
    throw ApiException(ErrorDtoType.MISSING_PARAMETER, "parameterName" to param)

fun invalidParameter(param: String, vararg allowedParameters: String): Nothing =
    throw ApiException(ErrorDtoType.INVALID_PARAMETER, "parameterName" to param, "reason" to allowedParameters.joinToString(", "))

fun unauthorized(reason: String = "Unauthorized"): Nothing =
    throw ApiException(ErrorDtoType.UNAUTHORIZED, "reason" to reason)

fun conflict(reason: String): Nothing =
    throw ApiException(ErrorDtoType.CONFLICT, "reason" to reason)

fun unavailable(reason: String): Nothing =
    throw ApiException(ErrorDtoType.UNAVAILABLE, "reason" to reason)

fun forbidden(vararg requiredPermissions: PermissionType): Nothing =
    throw ApiException(ErrorDtoType.NO_PERMISSION, "requiredPermissions" to requiredPermissions.joinToString(",") { it.id })

fun notFound(): Nothing =
    throw ApiException(ErrorDtoType.NOT_FOUND)

fun rateLimitExceeded(): Nothing =
    throw ApiException(ErrorDtoType.RATE_LIMIT_EXCEEDED)

fun ApplicationCall.getToken(): String? {
    val principal = principal<UserPrincipal>()
    return principal?.token
}

fun ApplicationCall.getUser(): UserEntity? {
    val principal = principal<UserPrincipal>()
    return principal?.let {
        UserRepo.getById(principal.userId)
    }
}

fun ApplicationCall.getUserId(): Long? {
    val principal = principal<UserPrincipal>()
    return principal?.userId
}

fun ApplicationCall.hasPermissions(vararg permissions: PermissionType): Boolean {
    val userId = getUserId() ?: return false
    if (permissions.isEmpty()) return true
    return PermissionRepo.hasPermissions(userId, permissions.toList())
}

/**
 * Generates a client ID for rate limiting based on the client's IP address.
 * Checks the configured proxy header first, then falls back to remote address.
 *
 * @return A unique identifier for the client
 */
fun ApplicationCall.getClientId(): String {

    val realIpHeader = EnvUtil.getOrNull(EnvUtil.REAL_IP_HEADER)

    // Example values:
    // X-Forwarded-For: 0.0.0.0, 1.1.1.1, 2.2.2.2 (first is the client IP, then potential proxies / load balancers)
    // X-Real-IP: 0.0.0.0
    // Forwarded: for=0.0.0.0;proto=http;by=1.1.1.1 (for is the client IP, by is the proxy)

    if (!realIpHeader.isNullOrEmpty()) {
        val realIp = request.headers[realIpHeader]
        if (realIp != null) {
            val clientIp = realIp.split(",", ";").firstOrNull()?.trim()
            if (!clientIp.isNullOrEmpty()) {
                return clientIp
            }
        }
    }

    return request.local.remoteAddress
}

fun Route.getP(path: String, vararg permissions: PermissionType, body: suspend RoutingContext.() -> Unit) {
    get(path) {
        if (permissions.isNotEmpty() && !call.hasPermissions(*permissions)) {
            forbidden(*permissions)
        }
        body()
    }
}

fun Route.postP(path: String, vararg permissions: PermissionType, body: suspend RoutingContext.() -> Unit) {
    post(path) {
        if (permissions.isNotEmpty() && !call.hasPermissions(*permissions)) {
            forbidden(*permissions)
        }
        body()
    }
}

fun Route.putP(path: String, vararg permissions: PermissionType, body: suspend RoutingContext.() -> Unit) {
    put(path) {
        if (permissions.isNotEmpty() && !call.hasPermissions(*permissions)) {
            forbidden(*permissions)
        }
        body()
    }
}

fun Route.deleteP(path: String, vararg permissions: PermissionType, body: suspend RoutingContext.() -> Unit) {
    delete(path) {
        if (permissions.isNotEmpty() && !call.hasPermissions(*permissions)) {
            forbidden(*permissions)
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

fun ApplicationCall.getPaginatedSearchParams(): PaginatedSearchParams {
    val query = request.queryParameters["query"] ?: ""
    val page = request.queryParameters["page"]?.toIntOrNull() ?: 1
    val pageSize = request.queryParameters["pageSize"]?.toIntOrNull() ?: 50

    if (page <= 0) {
        invalidParameter("page", "page > 0")
    }

    if (pageSize !in 1..100) {
        invalidParameter("pageSize", "0 < pageSize <= 100")
    }

    return PaginatedSearchParams(
        query = query,
        page = page,
        pageSize = pageSize
    )
}

fun ApplicationCall.getBooleanOrDefault(paramName: String, default: Boolean): Boolean {
    val paramValue = this.parameters[paramName] ?: return default

    return when (paramValue.lowercase()) {
        "true", "1", "yes" -> true
        "false", "0", "no" -> false
        else -> {
            invalidParameter(paramName, "false")
        }
    }
}

fun ApplicationCall.getBooleanParameter(paramName: String): Boolean {
    val paramValue = this.parameters[paramName]
        ?: missingParameter(paramName)

    return when (paramValue.lowercase()) {
        "true", "1", "yes" -> true
        "false", "0", "no" -> false
        else -> {
            invalidParameter(paramName, "false")
        }
    }
}

fun ApplicationCall.getStringOrDefault(paramName: String, default: String): String {
    return this.parameters[paramName] ?: default
}

fun ApplicationCall.getStringParameter(paramName: String): String {
    val paramValue = this.parameters[paramName]
        ?: missingParameter(paramName)

    return paramValue
}

fun ApplicationCall.getLongOrDefault(paramName: String, default: Long): Long {
    val paramValue = this.parameters[paramName] ?: return default

    val longValue = paramValue.toLongOrNull()
        ?: invalidParameter(paramName)

    return longValue
}

fun ApplicationCall.getLongParameter(paramName: String): Long {
    val paramValue = this.parameters[paramName]
        ?: missingParameter(paramName)

    val longValue = paramValue.toLongOrNull()
        ?: invalidParameter(paramName)

    return longValue
}

fun ApplicationCall.getIntOrDefault(paramName: String, default: Int): Int {
    val paramValue = this.parameters[paramName] ?: return default

    val intValue = paramValue.toIntOrNull()
        ?: invalidParameter(paramName)

    return intValue
}

fun ApplicationCall.getIntParameter(paramName: String): Int {
    val paramValue = this.parameters[paramName]
        ?: missingParameter(paramName)

    val intValue = paramValue.toIntOrNull()
        ?: invalidParameter(paramName)

    return intValue
}

fun ApplicationCall.getFloatOrDefault(paramName: String, default: Float): Float {
    val paramValue = this.parameters[paramName] ?: return default

    val intValue = paramValue.toFloatOrNull() ?: invalidParameter(paramName)

    return intValue
}

fun ApplicationCall.getFloatParameter(paramName: String): Float {
    val paramValue = this.parameters[paramName]
        ?: missingParameter(paramName)

    val intValue = paramValue.toFloatOrNull()
        ?: invalidParameter(paramName)

    return intValue
}

inline fun<reified E : Enum<E>> ApplicationCall.getEnum(paramName: String, default: E?): E {
    val strParam = if (default != null) getStringOrDefault(paramName, default.name) else getStringParameter(paramName)

    try {
        return enumValueOf<E>(strParam)
    }
    catch (_: IllegalArgumentException) {
        invalidParameter(paramName, *enumEntries<E>().map { it.name }.toTypedArray())
    }
}