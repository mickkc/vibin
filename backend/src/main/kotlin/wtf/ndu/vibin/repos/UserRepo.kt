package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.auth.CryptoUtil
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.db.UserTable
import wtf.ndu.vibin.db.images.ImageEntity
import wtf.ndu.vibin.dto.users.UserDto
import wtf.ndu.vibin.dto.users.UserEditDto
import wtf.ndu.vibin.routes.PaginatedSearchParams
import wtf.ndu.vibin.utils.DateTimeUtils

/**
 * Repository for managing [wtf.ndu.vibin.db.UserEntity] instances.
 */
object UserRepo {

    fun count(): Long = transaction {
        UserEntity.all().count()
    }

    fun getProfilePicture(user: UserEntity): ImageEntity? = transaction {
        return@transaction user.profilePicture
    }

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

    suspend fun updateOrCreateUser(id: Long?, editDto: UserEditDto): UserEntity? {
        val user = transaction {
            if (id != null)
                UserEntity.findById(id)
            else
                UserEntity.new {
                    username = editDto.username!!
                    displayName = editDto.displayName
                    description = editDto.description ?: ""
                    email = editDto.email
                    isActive = editDto.isActive ?: true
                    isAdmin = editDto.isAdmin ?: false
                    salt = CryptoUtil.getSalt()
                    passwordHash = CryptoUtil.hashPassword(editDto.password!!, salt)
                }
        }

        if (user == null) return null

        val (imageUpdated, newImage) = ImageRepo.getUpdatedImage(editDto.profilePictureUrl)

        return transaction {
            if (id != null) {
                user.apply {
                    editDto.username?.takeIf { it.isNotBlank() }?.let { this.username = it }
                    editDto.displayName?.let { this.displayName = it.takeIf { it.isNotBlank() } }
                    editDto.email?.let { this.email = it.takeIf { it.isNotBlank() } }
                    editDto.isActive?.let { this.isActive = it }
                    editDto.isAdmin?.let { this.isAdmin = it }
                    editDto.password?.let {
                        this.passwordHash = CryptoUtil.hashPassword(it, this.salt)
                    }
                    editDto.description?.let { this.description = it }
                }
            }
            else {
                PermissionRepo.addDefaultPermissions(user.id.value)
            }

            if (imageUpdated) {
                user.profilePicture = newImage
            }

            user.updatedAt = DateTimeUtils.now()
            user
        }
    }

    fun deleteUser(userId: Long, deleteData: Boolean): Boolean = transaction {

        val user = UserEntity.findById(userId) ?: return@transaction false

        val uploadedTracks = TrackRepo.getUploadedByUser(userId)
        uploadedTracks.forEach { track ->
            if (deleteData) {
                TrackRepo.delete(track)
            } else {
                track.uploader = null
            }
        }

        user.delete()
        return@transaction true
    }

    /**
     * Retrieves all users from the database.
     *
     * @return A list of all [UserEntity] instances.
     */
    fun getAllUsers(params: PaginatedSearchParams): Pair<List<UserEntity>, Int> = transaction {
        val users = UserEntity
            .find {
                (UserTable.displayName.lowerCase() like "%${params.query.lowercase()}%") or
                (UserTable.username.lowerCase() like "%${params.query.lowercase()}%")
            }
            .orderBy(
                (Case()
                    .When(UserTable.displayName like "${params.query.lowercase()}%", intLiteral(2))
                    .When(UserTable.username like "${params.query.lowercase()}%", intLiteral(1))
                    .Else(intLiteral(0))) to SortOrder.DESC,
                UserTable.displayName to SortOrder.ASC,
                UserTable.username to SortOrder.ASC
            )
        val count = users.count().toInt()
        val results = users
            .limit(params.pageSize)
            .offset(params.offset)
            .toList()
        return@transaction results to count
    }

    fun checkUsernameExists(username: String): Boolean = transaction {
        return@transaction UserEntity.find { UserTable.username.lowerCase() eq username.lowercase() }.count() > 0
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

    internal fun toDtoInternal(entity: UserEntity): UserDto {
        return UserDto(
            id = entity.id.value,
            username = entity.username,
            description = entity.description,
            displayName = entity.displayName,
            email = entity.email,
            isActive = entity.isActive,
            isAdmin = entity.isAdmin,
            lastLogin = entity.lastLogin,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}