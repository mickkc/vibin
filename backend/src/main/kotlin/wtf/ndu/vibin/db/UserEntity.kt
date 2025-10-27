package wtf.ndu.vibin.db

import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import wtf.ndu.vibin.db.images.ImageEntity
import wtf.ndu.vibin.db.images.ImageTable
import wtf.ndu.vibin.repos.PlaylistRepo
import wtf.ndu.vibin.repos.SessionRepo

object UserTable : ModifiableLongIdTable("user") {
    val username = varchar("username", 255).uniqueIndex()
    val displayName = varchar("display_name", 255).nullable()
    val description = text("description").default("")
    val passwordHash = binary("password_hash", 256)
    val salt = binary("salt", 16)
    val email = varchar("email", 255).nullable()
    val isActive = bool("is_active").default(true)
    val isAdmin = bool("is_admin").default(false)
    val lastLogin = long("last_login").nullable()
    val profilePictureId = reference("profile_picture_id", ImageTable).nullable()
}

/**
 * Entity class representing a user in the system.
 *
 * @property username The unique username of the user.
 * @property displayName The display name of the user, (null means to use username).
 * @property passwordHash The hashed password of the user.
 * @property salt The salt used for hashing the user's password.
 * @property email The unique email address of the user. (optional)
 * @property isActive Indicates if the user's account is active.
 * @property isAdmin Indicates if the user has administrative privileges.
 * @property lastLogin The timestamp of the user's last login. (optional)
 * @property profilePicture The profile picture of the user. (optional)
 */
class UserEntity(id: EntityID<Long>) : ModifiableLongIdEntity(id, UserTable) {

    companion object : LongEntityClass<UserEntity>(UserTable)

    var username by UserTable.username
    var displayName by UserTable.displayName
    var description by UserTable.description
    var passwordHash by UserTable.passwordHash
    var salt by UserTable.salt
    var email by UserTable.email
    var isActive by UserTable.isActive
    var isAdmin by UserTable.isAdmin
    var lastLogin by UserTable.lastLogin
    var profilePicture by ImageEntity optionalReferencedOn UserTable.profilePictureId

    override fun delete() {

        val userId = this.id.value

        // Delete any granted permissions associated with this user
        GrantedPermissionTable.deleteWhere { GrantedPermissionTable.user eq userId }

        // Remove any playlists owned by this user
        PlaylistRepo.getOwnedByUser(userId).forEach { it.delete() }

        // Remove sessions and media tokens associated with this user
        SessionRepo.invalidateAllSessionsForUser(userId)
        SessionRepo.invalidateAllMediaTokensForUser(userId)

        super.delete()
    }
}