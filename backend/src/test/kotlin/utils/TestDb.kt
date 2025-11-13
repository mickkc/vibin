package utils

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.config.allTables
import wtf.ndu.vibin.config.createDefaultAdminUser
import wtf.ndu.vibin.config.createTables
import wtf.ndu.vibin.module
import wtf.ndu.vibin.parsing.MetadataFetchingType
import wtf.ndu.vibin.repos.SessionRepo
import wtf.ndu.vibin.repos.SettingsRepo
import wtf.ndu.vibin.repos.UserRepo
import wtf.ndu.vibin.settings.server.AlbumMetadataFetchType
import wtf.ndu.vibin.settings.server.ArtistMetadataFetchType
import wtf.ndu.vibin.settings.server.LyricsMetadataSource

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

