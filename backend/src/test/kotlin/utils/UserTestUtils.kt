package utils

import org.junit.jupiter.api.assertNotNull
import wtf.ndu.vibin.auth.CryptoUtil
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.dto.users.UserEditDto
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.PermissionRepo
import wtf.ndu.vibin.repos.SessionRepo
import wtf.ndu.vibin.repos.UserRepo

object UserTestUtils {

    fun createTestUser(username: String, password: String): UserEntity {
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

    fun createUserWithSession(username: String, password: String): Pair<UserEntity, String> {
        val user = createTestUser(username, password)
        val sessionToken = CryptoUtil.createToken()
        SessionRepo.addSession(user, sessionToken)
        return user to sessionToken
    }

    fun createUserAndSessionWithPermissions(username: String, password: String, vararg permissions: Pair<PermissionType, Boolean>): Pair<UserEntity, String> {
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