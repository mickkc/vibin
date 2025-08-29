package wtf.ndu.vibin.auth

data class UserPrincipal(
    val userId: Long,
    val token: String
)