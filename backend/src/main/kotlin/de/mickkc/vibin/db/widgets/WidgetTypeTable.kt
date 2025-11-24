package de.mickkc.vibin.db.widgets

import de.mickkc.vibin.widgets.WidgetType
import org.jetbrains.exposed.sql.Table

object WidgetTypeTable : Table("widget_type") {
    val widget = reference("widget_id", SharedWidgetTable)
    val type = enumerationByName<WidgetType>("type", 50)
    val index = integer("index")

    override val primaryKey = PrimaryKey(widget, type, name = "PK_WidgetType_Widget_Type")
}
