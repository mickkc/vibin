package wtf.ndu.vibin.db

import org.jetbrains.exposed.sql.Table

object GrantedPermissionTable : Table() {
    val user = reference("user_id", UserTable)
    val permission = varchar("permission", 100)

    override val primaryKey = PrimaryKey(user, permission, name = "PK_GrantedPermission_user_permission")
}