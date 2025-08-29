package utils

import org.jetbrains.exposed.sql.Database
import wtf.ndu.vibin.config.createTables

fun initTestDb() {
    Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
    createTables()
}

