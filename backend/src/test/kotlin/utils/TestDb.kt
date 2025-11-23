package utils

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import de.mickkc.vibin.config.allTables
import de.mickkc.vibin.config.createDefaultAdminUser
import de.mickkc.vibin.config.createTables
import de.mickkc.vibin.module
import de.mickkc.vibin.parsing.MetadataFetchingType
import de.mickkc.vibin.repos.SessionRepo
import de.mickkc.vibin.repos.SettingsRepo
import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.settings.server.AlbumMetadataFetchType
import de.mickkc.vibin.settings.server.ArtistMetadataFetchType
import de.mickkc.vibin.settings.server.LyricsMetadataSource

val testToken = "test-token"

fun initTestDb() {
    Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
    transaction { SchemaUtils.drop(*allTables) }
    createTables()
    createDefaultAdminUser()

    val user = UserRepo.getById(1)!!
    SessionRepo.addSession(user, testToken)

    SettingsRepo.updateServerSetting(AlbumMetadataFetchType, MetadataFetchingType.NONE.name)
    SettingsRepo.updateServerSetting(ArtistMetadataFetchType, MetadataFetchingType.NONE.name)
    SettingsRepo.updateServerSetting(LyricsMetadataSource, "None")

}

fun Application.testModule() {
    module()
}

