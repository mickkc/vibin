package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.TaskSettingEntity

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