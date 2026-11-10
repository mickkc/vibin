package de.mickkc.vibin.db

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity

object TaskSettingTable : StringIdTable("task_setting", "task_id", 255) {

    val enabled: Column<Boolean> = bool("enabled").default(true)
}

class TaskSettingEntity(taskId: EntityID<String>) : Entity<String>(taskId) {
    companion object : StringEntityClass<TaskSettingEntity>(TaskSettingTable)

    var enabled by TaskSettingTable.enabled
}