package de.mickkc.vibin.config

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.db.*
import org.flywaydb.core.Flyway
import de.mickkc.vibin.db.albums.AlbumTable
import de.mickkc.vibin.db.artists.ArtistTable
import de.mickkc.vibin.db.artists.TrackArtistConnection
import de.mickkc.vibin.db.images.ImageTable
import de.mickkc.vibin.db.playlists.PlaylistCollaborator
import de.mickkc.vibin.db.playlists.PlaylistTable
import de.mickkc.vibin.db.playlists.PlaylistTrackTable
import de.mickkc.vibin.db.tags.TagTable
import de.mickkc.vibin.db.tags.TrackTagConnection
import de.mickkc.vibin.db.tracks.TrackTable
import de.mickkc.vibin.db.widgets.SharedWidgetTable
import de.mickkc.vibin.db.widgets.WidgetTypeTable

private val logger = LoggerFactory.getLogger("Database initialization")

/**
 * Configures the database connection and initializes the schema.
 * Creates a default admin user if no users are found in the database.
 */
fun configureDatabase() {

    val dbHost = EnvUtil.getOrError(EnvUtil.DB_HOST)
    val dbPort = EnvUtil.getOrError(EnvUtil.DB_PORT)
    val dbName = EnvUtil.getOrError(EnvUtil.DB_NAME)

    val dbUser = EnvUtil.getOrError(EnvUtil.DB_USER)
    val dbPassword = EnvUtil.getOrError(EnvUtil.DB_PASSWORD)

    val dbUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"

    Flyway
        .configure()
        .dataSource(dbUrl, dbUser, dbPassword)
        .load()
        .migrate()

    logger.info("Connecting to database at $dbHost:$dbPort, database name: $dbName, user: $dbUser")

    Database.connect(
        url = "jdbc:postgresql://$dbHost:$dbPort/$dbName",
        driver = "org.postgresql.Driver",
        user = dbUser,
        password = dbPassword
    )

    logger.info("Connected to database, creating tables if not existing")

    createTables()
    createDefaultAdminUser()
}

val allTables = arrayOf(
    UserTable, GrantedPermissionTable, SessionTable, MediaTokenTable,
    SettingsTable, UserSettingsTable,
    ImageTable,
    TagTable,
    ArtistTable,
    AlbumTable,
    TrackTable, TrackTagConnection, TrackArtistConnection,
    PlaylistTable, PlaylistTrackTable, PlaylistCollaborator,
    ListenTable,
    LyricsTable,
    TaskSettingTable,
    FavoriteTable,
    SharedWidgetTable, WidgetTypeTable
)

fun createTables() = transaction {
    SchemaUtils.create(*allTables)

    logger.info("Tables created or already existing")
}

/**
 * Creates a default admin user with username "Admin" and password "admin".
 */
fun createDefaultAdminUser() = transaction {

    if (UserEntity.count() > 0L) {
        logger.info("Users found in database, skipping default admin user creation")
        return@transaction
    }

    logger.info("Checking for existing users in database")
    val username = "Admin"
    val salt = CryptoUtil.getSalt()
    val passwordHash = CryptoUtil.hashPassword("admin", salt)

    logger.warn("No users found in database, creating default admin user with username 'Admin' and password 'admin'. Please change the password immediately after logging in.")

    UserEntity.new {
        this.username = username
        this.salt = salt
        this.passwordHash = passwordHash
        this.isAdmin = true
    }
}