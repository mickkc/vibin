package de.mickkc.vibin.routes

import de.mickkc.vibin.auth.UserPrincipal
import de.mickkc.vibin.config.ApiException
import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.dto.errors.ErrorDto
import de.mickkc.vibin.dto.errors.ErrorDtoType
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PermissionRepo
import de.mickkc.vibin.repos.UserRepo
import io.ktor.openapi.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.enums.enumEntries
import kotlin.reflect.KType
import kotlin.reflect.typeOf

suspend fun ApplicationCall.success(success: Boolean = true) {
    respond(SuccessDto(success))
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

fun noPermission(vararg requiredPermissions: PermissionType): Nothing =
    throw ApiException(ErrorDtoType.NO_PERMISSION, "requiredPermissions" to requiredPermissions.joinToString(",") { it.id })

fun forbidden(reason: String = "forbidden"): Nothing =
    throw ApiException(ErrorDtoType.FORBIDDEN, "reason" to reason)

fun notImplemented(reason: String = "Not implemented"): Nothing =
    throw ApiException(ErrorDtoType.NOT_IMPLEMENTED, "reason" to reason)


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
        // The Ktor OpenAPI code inference copies the argument of headers[...] into the generated docs, which crashes
        // with a null header name when REAL_IP_HEADER is not configured.
        val realIp = request.headers.entries()
            .firstOrNull { it.key.equals(realIpHeader, ignoreCase = true) }
            ?.value?.firstOrNull()

        if (!realIp.isNullOrEmpty()) {
            val clientIp = realIp.split(",", ";").firstOrNull()?.trim()
            if (!clientIp.isNullOrEmpty()) {
                return clientIp
            }
        }
    }

    return request.local.remoteAddress
}

fun Route.getP(path: String, vararg permissions: PermissionType, body: suspend RoutingContext.() -> Unit): Route {
    return get(path) {
        if (permissions.isNotEmpty() && !call.hasPermissions(*permissions)) {
            noPermission(*permissions)
        }
        body()
    }
}

fun Route.postP(path: String, vararg permissions: PermissionType, body: suspend RoutingContext.() -> Unit): Route {
    return post(path) {
        if (permissions.isNotEmpty() && !call.hasPermissions(*permissions)) {
            noPermission(*permissions)
        }
        body()
    }
}

fun Route.putP(path: String, vararg permissions: PermissionType, body: suspend RoutingContext.() -> Unit): Route {
    return put(path) {
        if (permissions.isNotEmpty() && !call.hasPermissions(*permissions)) {
            noPermission(*permissions)
        }
        body()
    }
}

fun Route.deleteP(path: String, vararg permissions: PermissionType, body: suspend RoutingContext.() -> Unit): Route {
    return delete(path) {
        if (permissions.isNotEmpty() && !call.hasPermissions(*permissions)) {
            noPermission(*permissions)
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

/**
 * One schema per response DTO type to prevent duplicate schemas in the generated OpenAPI docs
 */
@PublishedApi
internal val successSchemas = mutableMapOf<KType, JsonSchema>()

inline fun<reified T : Any> Responses.Builder.successResponse(description: String, exampleValue: T? = null) {
    response(200) {
        this.description = description
        content {
            schema = successSchemas.getOrPut(typeOf<T>()) { jsonSchema<T>() }
            if (exampleValue != null) {
                example(T::class.simpleName ?: "success", ExampleObject(
                    summary = description,
                    value = GenericElement(exampleValue)
                ))
            }
        }
    }
}

/**
 * Creates a new [responses] block and automatically adds [missingPermissionsResponse] for all permission types.
 * Because multiple [responses] blocks can't be defined without overwriting each other, all responses must be be passed
 * to [otherResponses].
 *
 * Also appends a list of all required permissions to the description.
 */
fun Operation.Builder.authResponses(vararg permissions: PermissionType, otherResponses: Responses.Builder.() -> Unit = {}) {
    security {
        requirement("tokenAuth")
    }
    if (permissions.any()) {
        description = buildString {
            if (description != null) {
                append(description)
                append("\n\n")
            }
            appendLine("Required permissions:")
            permissions.forEach { permission ->
                appendLine("- `${permission.id}`")
            }
        }
    }
    responses {
        otherResponses()
        if (permissions.any())
            missingPermissionsResponse(*permissions)
    }
}

fun Operation.Builder.optionalPermissions(vararg permissions: Pair<PermissionType, String>) {
    description = buildString {
        if (description != null) {
            append(description)
            append("\n\n")
        }
        appendLine("Optional permissions:")
        permissions.forEach { (permission, desc) ->
            appendLine("- `$permission`: $desc")
        }
    }
}

fun Parameters.Builder.paginatedSearch() {
    q<String>("query", required = false)
    q("page", 1, false, "Page index, where 1 is the first page.")
    q("pageSize", 50, false, "Maximum amount of items per page.")
}

inline fun<reified T : Any> Parameters.Builder.q(name: String, example: T? = null, required: Boolean = true, description: String? = null) {
    query(name) {
        this.description = description
        this.example = example?.let { GenericElement(it) }
        this.schema = jsonSchema<T>()
        this.required = required
    }
}

inline fun<reified T : Any> Parameters.Builder.p(name: String, example: T? = null, description: String? = null) {
    path(name) {
        this.description = description
        this.example = example?.let { GenericElement(it) }
        this.schema = jsonSchema<T>()
        this.required = true
    }
}

private val errorSchema: JsonSchema = KotlinxSerializerJsonSchemaInference.Default.jsonSchema<ErrorDto>()

fun Responses.Builder.errorResponse(errorDtoType: ErrorDtoType, description: String, vararg data: Pair<String, String>) {
    response(errorDtoType.code.value) {
        this.description = description
        content {
            schema = errorSchema
            example(errorDtoType.name, ExampleObject(
                summary = description,
                value = GenericElement.encodeToElement(ErrorDto.serializer(), ErrorDto(
                    type = errorDtoType,
                    data = data.toMap()
                ))
            ))
        }
    }
}

fun Responses.Builder.notFoundResponse(name: String)
    = errorResponse(ErrorDtoType.NOT_FOUND, "$name not found")
fun Responses.Builder.missingParameterResponse(name: String)
    = errorResponse(ErrorDtoType.NOT_FOUND, "Parameter '$name' is missing", "parameterName" to name)
fun Responses.Builder.invalidParameterResponse(name: String, explanation: String)
    = errorResponse(ErrorDtoType.INVALID_PARAMETER, "Parameter '$name' is invalid: $explanation", "parameterName" to name)
fun Responses.Builder.unauthorizedResponse(reason: String)
    = errorResponse(ErrorDtoType.UNAUTHORIZED, "Authorization failed: $reason", "reason" to reason)
fun Responses.Builder.conflictResponse(reason: String)
    = errorResponse(ErrorDtoType.CONFLICT, "Conflict: $reason", "reason" to reason)
fun Responses.Builder.unavailableResponse(reason: String)
    = errorResponse(ErrorDtoType.UNAVAILABLE, "Service unavailable: $reason", "reason" to reason)
fun Responses.Builder.rateLimitedResponse()
    = errorResponse(ErrorDtoType.RATE_LIMIT_EXCEEDED, "A rate-limit has been exceeded.")
fun Responses.Builder.forbiddenResponse(desc: String = "forbidden")
    = errorResponse(ErrorDtoType.FORBIDDEN, desc)
fun Responses.Builder.missingPermissionsResponse(vararg permissions: PermissionType)
    = errorResponse(ErrorDtoType.NO_PERMISSION, "Missing a required permission",
        "requiredPermissions" to permissions.joinToString(",") { it.id })

fun Responses.Builder.redirectResponse(target: String, permanent: Boolean = false) {
    response(if (permanent) 301 else 302) {
        description = "Redirect to $target"
    }
}
