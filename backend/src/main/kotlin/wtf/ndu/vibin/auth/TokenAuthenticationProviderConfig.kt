package wtf.ndu.vibin.auth

import io.ktor.server.auth.AuthenticationProvider

class TokenAuthenticationProviderConfig : AuthenticationProvider.Config(name = "tokenAuth")