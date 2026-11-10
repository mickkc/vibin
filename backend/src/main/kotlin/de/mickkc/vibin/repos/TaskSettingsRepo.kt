package de.mickkc.vibin.repos

import de.mickkc.vibin.db.TaskSettingEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object TaskSettingsRepo {

    fun getById(taskId: String): TaskSettingEntity? = transaction {
        TaskSettingEntity.findById(taskId)
    }

    fun setTaskEnabled(taskId: String, enabled: Boolean) = transaction {
        val setting = TaskSettingEntity.findById(taskId)
        if (setting != null) {
            setting.enabled = enabled
        } else {
            TaskSettingEntity.new(taskId) {
                this.enabled = enabled
            }
        }
    }
}