package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.SettingsEntity
import wtf.ndu.vibin.db.SettingsTable

/**
 * Repository for managing [wtf.ndu.vibin.db.SettingsEntity] instances.
 */
object SettingsRepo {

    /**
     * Retrieves a settings value by its key.
     *
     * @param key The key of the setting to retrieve.
     * @return The value of the setting if found, otherwise null.
     */
    fun getSetting(key: String): String?  = transaction {
        SettingsEntity.find { SettingsTable.key eq key }.firstOrNull()?.value
    }
}