package wtf.ndu.vibin.config

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.auth.CryptoUtil
import wtf.ndu.vibin.db.*
import org.flywaydb.core.Flyway
import wtf.ndu.vibin.db.albums.AlbumTable
import wtf.ndu.vibin.db.artists.ArtistTable
import wtf.ndu.vibin.db.artists.TrackArtistConnection
import wtf.ndu.vibin.db.images.ImageTable
import wtf.ndu.vibin.db.playlists.PlaylistCollaborator
import wtf.ndu.vibin.db.playlists.PlaylistTable
import wtf.ndu.vibin.db.playlists.PlaylistTrackTable
import wtf.ndu.vibin.db.tags.TagTable
import wtf.ndu.vibin.db.tags.TrackTagConnection
import wtf.ndu.vibin.db.tracks.TrackTable

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
    UserTable, GrantedPermissionTable, SessionTable, MediaTokenTable, SettingsTable,
    ImageTable,
    TagTable,
    ArtistTable,
    AlbumTable,
    TrackTable, TrackTagConnection, TrackArtistConnection,
    PlaylistTable, PlaylistTrackTable, PlaylistCollaborator,
    ListenTable,
    LyricsTable
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