package de.mickkc.vibin.db

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column

object TaskSettingTable : StringIdTable("task_setting", "task_id", 255) {

    val enabled: Column<Boolean> = bool("enabled").default(true)
}

class TaskSettingEntity(taskId: EntityID<String>) : Entity<String>(taskId) {
    companion object : StringEntityClass<TaskSettingEntity>(TaskSettingTable)

    var enabled by TaskSettingTable.enabled
}