package utils

import org.junit.jupiter.api.assertNotNull
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.dto.users.UserEditDto
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
            profilePictureUrl = null
        ))
        assertNotNull(user)
        return user
    }

}