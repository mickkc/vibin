package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.GrantedPermissionTable
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.permissions.PermissionType

object PermissionRepo {

    fun hasPermissions(userId: Long, permissions: List<PermissionType>): Boolean = transaction {

        val user = UserEntity.findById(userId) ?: return@transaction false

        if (user.isAdmin) return@transaction true

        val grantedPermissions = GrantedPermissionTable
            .select(GrantedPermissionTable.permission)
            .where { GrantedPermissionTable.user eq userId }
            .map { it[GrantedPermissionTable.permission] }

        return@transaction permissions.all { it in grantedPermissions }
    }

    fun getPermissions(userId: Long): List<PermissionType> = transaction {
        GrantedPermissionTable
            .select(GrantedPermissionTable.permission)
            .where { GrantedPermissionTable.user eq userId }
            .map { it[GrantedPermissionTable.permission] }
    }

    fun addPermission(userId: Long, permissionType: PermissionType) = transaction {
        GrantedPermissionTable.insert {
            it[GrantedPermissionTable.user] = userId
            it[GrantedPermissionTable.permission] = permissionType
        }
    }

    fun removePermission(userId: Long, permissionType: PermissionType) = transaction {
        GrantedPermissionTable.deleteWhere {
            (GrantedPermissionTable.user eq userId) and (GrantedPermissionTable.permission eq permissionType)
        }
    }

    fun addDefaultPermissions(userId: Long) = transaction {
        val defaultPermissions = PermissionType.values().filter { it.grantedByDefault }
        defaultPermissions.forEach { permission ->
            GrantedPermissionTable.insert {
                it[GrantedPermissionTable.user] = userId
                it[GrantedPermissionTable.permission] = permission
            }
        }
    }
}