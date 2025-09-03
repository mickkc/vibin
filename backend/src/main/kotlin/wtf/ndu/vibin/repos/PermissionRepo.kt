package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.GrantedPermissionTable
import wtf.ndu.vibin.db.UserEntity

object PermissionRepo {

    fun hasPermissions(userId: Long, permissions: List<String>): Boolean = transaction {

        val user = UserEntity.findById(userId) ?: return@transaction false

        if (user.isAdmin) return@transaction true

        val grantedPermissions = GrantedPermissionTable
            .select(GrantedPermissionTable.permission)
            .where { GrantedPermissionTable.user eq userId }
            .map { it[GrantedPermissionTable.permission] }

        return@transaction permissions.all { it in grantedPermissions }
    }
}