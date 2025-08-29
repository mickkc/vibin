package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.db.UserTable
import wtf.ndu.vibin.dto.UserDto

/**
 * Repository for managing [wtf.ndu.vibin.db.UserEntity] instances.
 */
object UserRepo {

    /**
     * Retrieves a user by their username.
     *
     * @param username The username of the user to retrieve.
     * @return The [UserEntity] if found, otherwise null.
     */
    fun getByUsername(username: String): UserEntity? = transaction {
        UserEntity.find { UserTable.username eq username }.firstOrNull()
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id The ID of the user to retrieve.
     * @return The [UserEntity] if found, otherwise null.
     */
    fun getById(id: Long): UserEntity? = transaction {
        UserEntity.findById(id)
    }

    /**
     * Updates the specified user with the provided block of code.
     *
     * @param user The user to update.
     * @param block The block of code to execute on the user for updating fields.
     */
    fun updateUser(user: UserEntity, block: UserEntity.() -> Unit): UserEntity = transaction {
        user.apply {
            this.block()
            this.updatedAt = System.currentTimeMillis()
        }
    }

    /**
     * Converts a UserEntity to a UserDto.
     * Loads all lazy fields within a transaction.
     *
     * @param entity The UserEntity to convert.
     * @return The corresponding UserDto.
     */
    fun toDto(entity: UserEntity): UserDto = transaction {
        UserDto(
            id = entity.id.value,
            username = entity.username,
            displayName = entity.displayName ?: entity.username,
            email = entity.email,
            isActive = entity.isActive,
            isAdmin = entity.isAdmin,
            lastLogin = entity.lastLogin,
            profilePicture = entity.profilePicture?.let { ImageRepo.toDto(it) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}