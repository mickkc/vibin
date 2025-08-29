package wtf.ndu.vibin.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.auth.UserPrincipal
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.repos.UserRepo

suspend fun RoutingCall.missingParameter(param: String) {
    respondText("Missing parameter: $param", status = HttpStatusCode.BadRequest)
}

suspend fun RoutingCall.unauthorized(message: String = "Unauthorized") {
    respondText(message, status = HttpStatusCode.Unauthorized)
}

fun RoutingCall.getUser(): UserEntity? {
    val principal = principal<UserPrincipal>()
    return principal?.let {
        UserRepo.getById(principal.userId)
    }
}