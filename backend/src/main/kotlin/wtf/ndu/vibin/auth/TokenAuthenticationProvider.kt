package wtf.ndu.vibin.auth

import io.ktor.server.auth.*
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.repos.SessionRepo

/**
 * Custom authentication provider that checks for a Bearer token in the Authorization header.
 * If the token is valid, it sets the UserPrincipal in the authentication context.
 *
 * @param config Configuration for the authentication provider (doesn't really do anything here, but required by Ktor).
 */
class TokenAuthenticationProvider(config: TokenAuthenticationProviderConfig = TokenAuthenticationProviderConfig()) : AuthenticationProvider(config) {

    private val logger = LoggerFactory.getLogger(TokenAuthenticationProvider::class.java)

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")

        val userId = token?.let { SessionRepo.validateAndUpdateToken(it) }

        if (userId != null) {
            context.principal(UserPrincipal(userId, token))
        } else {
            logger.warn("Authentication failed: invalid or missing token")
            context.challenge("TokenAuth", AuthenticationFailedCause.InvalidCredentials) { challenge, _ ->
                challenge.complete()
            }
        }
    }
}