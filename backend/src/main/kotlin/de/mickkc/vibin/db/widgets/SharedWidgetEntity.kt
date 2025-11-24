package de.mickkc.vibin.db.widgets

import de.mickkc.vibin.db.StringEntityClass
import de.mickkc.vibin.db.StringIdTable
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.db.UserTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere

object SharedWidgetTable : StringIdTable("shared_widget", columnLength = 64) {
    val userId = reference("user_id", UserTable)
    val bgColor = integer("bg_color").nullable()
    val fgColor = integer("fg_color").nullable()
    val accentColor = integer("accent_color").nullable()
}

class SharedWidgetEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : StringEntityClass<SharedWidgetEntity>(SharedWidgetTable)

    var user by UserEntity referencedOn SharedWidgetTable.userId
    var bgColor by SharedWidgetTable.bgColor
    var fgColor by SharedWidgetTable.fgColor
    var accentColor by SharedWidgetTable.accentColor

    override fun delete() {

        val id = this.id.value
        WidgetTypeTable.deleteWhere { WidgetTypeTable.widget eq id }

        super.delete()
    }
}
