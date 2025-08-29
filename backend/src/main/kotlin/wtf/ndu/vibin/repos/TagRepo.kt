package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.TagEntity
import wtf.ndu.vibin.db.TagTable

object TagRepo {

    fun getOrCreateTag(name: String): TagEntity = transaction {
        TagEntity.find { TagTable.name.lowerCase() eq name.lowercase() }.firstOrNull() ?: TagEntity.new {
            this.name = name
        }
    }
}