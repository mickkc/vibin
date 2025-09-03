package wtf.ndu.vibin.db

import org.jetbrains.exposed.sql.Table
import wtf.ndu.vibin.permissions.PermissionType

object GrantedPermissionTable : Table("granted_permission") {
    val user = reference("user_id", UserTable)
    val permission = enumeration<PermissionType>("permission")

    override val primaryKey = PrimaryKey(user, permission, name = "PK_GrantedPermission_user_permission")
}