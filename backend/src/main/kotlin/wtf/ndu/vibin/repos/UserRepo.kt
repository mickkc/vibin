package wtf.ndu.vibin.repos

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.auth.CryptoUtil
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.db.UserTable
import wtf.ndu.vibin.dto.UserDto
import wtf.ndu.vibin.dto.UserEditDto
import wtf.ndu.vibin.parsing.Parser
import wtf.ndu.vibin.processing.ThumbnailProcessor
import wtf.ndu.vibin.utils.DateTimeUtils

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

    fun updateOrCreateUser(id: Long?, editDto: UserEditDto): UserEntity? = transaction {
        val user = if (id != null) UserEntity.findById(id) else UserEntity.new {
            username = editDto.username!!
            displayName = editDto.displayName
            email = editDto.email
            isActive = editDto.isActive ?: true
            isAdmin = editDto.isAdmin ?: false
            salt = CryptoUtil.getSalt()
            passwordHash = CryptoUtil.hashPassword(editDto.password!!, salt)
        }

        if (user == null) return@transaction null

        if (id != null) {
            user.apply {
                editDto.username?.takeIf { it.isNotBlank() }?.let { this.username = it }
                editDto.displayName?.let { this.displayName = it.takeIf { it.isNotBlank() } }
                editDto.email?.let { this.email = it.takeIf { it.isNotBlank() } }
                editDto.isActive?.let { this.isActive = it }
                editDto.isAdmin?.let { this.isAdmin = it }
            }
        }

        if (editDto.profilePictureUrl != null) {
            val data = runBlocking { Parser.downloadCoverImage(editDto.profilePictureUrl) }
            val image = data?.let { ThumbnailProcessor.getImage(data, ThumbnailProcessor.ThumbnailType.USER, user.id.value.toString()) }
            user.profilePicture?.delete()
            user.profilePicture = image
        }

        user.updatedAt = DateTimeUtils.now()
        return@transaction user
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
            this.updatedAt = DateTimeUtils.now()
        }
    }

    fun deleteUser(user: UserEntity) = transaction {
        user.delete()
    }

    /**
     * Retrieves all users from the database.
     *
     * @return A list of all [UserEntity] instances.
     */
    fun getAllUsers(): List<UserEntity> = transaction {
        UserEntity.all().toList()
    }

    /**
     * Converts a UserEntity to a UserDto.
     * Loads all lazy fields within a transaction.
     *
     * @param entity The UserEntity to convert.
     * @return The corresponding UserDto.
     */
    fun toDto(entity: UserEntity): UserDto = transaction {
        toDtoInternal(entity)
    }

    fun toDto(entities: List<UserEntity>): List<UserDto> = transaction {
        entities.map { toDtoInternal(it) }
    }

    fun toDtoInternal(entity: UserEntity): UserDto {
        return UserDto(
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