package de.mickkc.vibin.repos

import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.db.widgets.SharedWidgetEntity
import de.mickkc.vibin.db.widgets.SharedWidgetTable
import de.mickkc.vibin.db.widgets.WidgetTypeTable
import de.mickkc.vibin.dto.widgets.WidgetDto
import de.mickkc.vibin.widgets.WidgetType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object WidgetRepo {

    fun shareWidget(
        user: UserEntity,
        types: List<WidgetType>,
        bgColor: Int?,
        fgColor: Int?,
        accentColor: Int?
    ): SharedWidgetEntity = transaction {

        val id = CryptoUtil.createToken()
        val widget = SharedWidgetEntity.new(id) {
            this.user = user
            this.bgColor = bgColor
            this.fgColor = fgColor
            this.accentColor = accentColor
        }
        types.distinct().forEach { type ->
            WidgetTypeTable.insert {
                it[WidgetTypeTable.widget] = widget.id
                it[WidgetTypeTable.type] = type
            }
        }
        return@transaction widget
    }

    fun deleteWidget(widget: SharedWidgetEntity, userId: Long? = null): Boolean = transaction {
        if (userId != null && widget.user.id.value != userId) {
            return@transaction false
        }
        widget.delete()
        return@transaction true
    }

    fun getWidget(id: String): SharedWidgetEntity? = transaction {
        return@transaction SharedWidgetEntity.findById(id)
    }

    fun getTypes(widget: SharedWidgetEntity): List<WidgetType> = transaction {
        return@transaction WidgetTypeTable
            .select(WidgetTypeTable.type)
            .where(WidgetTypeTable.widget eq widget.id)
            .map { it[WidgetTypeTable.type] }
    }

    fun getUserId(widget: SharedWidgetEntity): Long = transaction {
        return@transaction widget.user.id.value
    }

    fun getAllForUser(user: UserEntity): List<SharedWidgetEntity> = transaction {
        return@transaction SharedWidgetEntity.find { SharedWidgetTable.userId eq user.id.value }.toList()
    }

    fun toDto(widget: SharedWidgetEntity): WidgetDto = transaction {
        return@transaction toDtoInternal(widget)
    }

    fun toDto(widgets: List<SharedWidgetEntity>): List<WidgetDto> = transaction {
        return@transaction widgets.map { toDtoInternal(it) }
    }

    internal fun toDtoInternal(widget: SharedWidgetEntity): WidgetDto {
        val types = getTypes(widget).map { it.name.lowercase() }
        return WidgetDto(
            id = widget.id.value,
            types = types
        )
    }
}