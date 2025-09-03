package wtf.ndu.vibin.config

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.auth.CryptoUtil
import wtf.ndu.vibin.db.*
import org.flywaydb.core.Flyway

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
}

fun createTables() = transaction {
    SchemaUtils.create(
        UserTable, SessionTable, SettingsTable,
        ImageTable,
        TagTable,
        ArtistTable, ArtistTagConnection,
        AlbumTable,
        TrackTable, TrackTagConnection, TrackArtistConnection,
        PlaylistTable, PlaylistTrackTable, PlaylistCollaborator
    )

    logger.info("Checking for existing users in database")

    if (UserEntity.count() == 0L) {
        createDefaultAdminUser()
    }

    logger.info("Database setup complete")
}

/**
 * Creates a default admin user with username "Admin" and password "admin".
 */
fun createDefaultAdminUser() {

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