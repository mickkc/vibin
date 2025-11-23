package utils

import org.junit.jupiter.api.assertNotNull
import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.dto.users.UserEditDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PermissionRepo
import de.mickkc.vibin.repos.SessionRepo
import de.mickkc.vibin.repos.UserRepo

object UserTestUtils {

    suspend fun createTestUser(username: String, password: String): UserEntity {
        val user = UserRepo.updateOrCreateUser(null, UserEditDto(
            username = username,
            password = password,
            email = null,
            isAdmin = false,
            isActive = true,
            displayName = null,
            profilePictureUrl = null,
            oldPassword = null,
            description = null
        ))
        assertNotNull(user)
        return user
    }

    suspend fun createUserWithSession(username: String, password: String): Pair<UserEntity, String> {
        val user = createTestUser(username, password)
        val sessionToken = CryptoUtil.createToken()
        SessionRepo.addSession(user, sessionToken)
        return user to sessionToken
    }

    suspend fun createUserAndSessionWithPermissions(username: String, password: String, vararg permissions: Pair<PermissionType, Boolean>): Pair<UserEntity, String> {
        val (user, token) = createUserWithSession(username, password)
        permissions.forEach { (permission, granted) ->
            if (granted) {
                if (!PermissionRepo.hasPermission(user.id.value, permission))
                    PermissionRepo.addPermission(user.id.value, permission)
            } else {
                if (PermissionRepo.hasPermission(user.id.value, permission))
                    PermissionRepo.removePermission(user.id.value, permission)
            }
        }
        return user to token
    }
}