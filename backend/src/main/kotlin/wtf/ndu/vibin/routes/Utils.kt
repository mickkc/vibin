package wtf.ndu.vibin.routes

import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.auth.UserPrincipal
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.dto.errors.ErrorDto
import wtf.ndu.vibin.dto.errors.ErrorDtoType
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.PermissionRepo
import wtf.ndu.vibin.repos.UserRepo
import wtf.ndu.vibin.settings.PageSize
import wtf.ndu.vibin.settings.Settings

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

suspend fun RoutingCall.forbidden(vararg requiredPermissions: PermissionType) {
    respond(ErrorDto.fromType(
        ErrorDtoType.NO_PERMISSION,
        "requiredPermissions" to requiredPermissions.joinToString(",") { it.id }
    ))
}

suspend fun RoutingCall.notFound() {
    respond(ErrorDto.fromType(ErrorDtoType.NOT_FOUND))
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
    val pageSize = request.queryParameters["pageSize"]?.toIntOrNull() ?: Settings.get(PageSize)

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