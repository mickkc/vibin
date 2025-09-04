package wtf.ndu.vibin.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.auth.UserPrincipal
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.PermissionRepo
import wtf.ndu.vibin.repos.UserRepo

suspend fun RoutingCall.missingParameter(param: String) {
    respondText("Missing parameter: $param", status = HttpStatusCode.BadRequest)
}

suspend fun RoutingCall.unauthorized(message: String = "Unauthorized") {
    respondText(message, status = HttpStatusCode.Unauthorized)
}

suspend fun RoutingCall.forbidden(message: String = "Forbidden") {
    respondText(message, status = HttpStatusCode.Forbidden)
}

suspend fun RoutingCall.notFound(message: String = "Not Found") {
    respondText(message, status = HttpStatusCode.NotFound)
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
            return@get call.forbidden()
        }
        body()
    }
}

fun Route.postP(path: String, vararg permissions: PermissionType, body: suspend RoutingContext.() -> Unit) {
    post(path) {
        if (permissions.isNotEmpty() && !call.hasPermissions(*permissions)) {
            return@post call.forbidden()
        }
        body()
    }
}

fun Route.putP(path: String, vararg permissions: PermissionType, body: suspend RoutingContext.() -> Unit) {
    put(path) {
        if (permissions.isNotEmpty() && !call.hasPermissions(*permissions)) {
            return@put call.forbidden()
        }
        body()
    }
}

fun Route.deleteP(path: String, vararg permissions: PermissionType, body: suspend RoutingContext.() -> Unit) {
    delete(path) {
        if (permissions.isNotEmpty() && !call.hasPermissions(*permissions)) {
            return@delete call.forbidden()
        }
        body()
    }
}