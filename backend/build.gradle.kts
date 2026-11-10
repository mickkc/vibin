plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    jacoco
}

group = "de.mickkc"
version = "0.1.0"

application {
    mainClass = "de.mickkc.vibin.ApplicationKt"
}

kotlin {
    jvmToolchain(21)
}


repositories {
    mavenCentral()
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.gson)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.simple.cache)
    implementation(libs.ktor.simple.memory.cache)
    implementation(libs.ktor.server.partial.content)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)
    implementation(libs.swagger.parser)
    implementation(libs.ktor.server.websockets)
    implementation(libs.logback.classic)

    implementation(libs.ktor.server.html.builder)
    implementation(libs.kotlinx.html.jvm)
    implementation(libs.kotlin.css)
    implementation(libs.selenium.java)

    implementation(libs.angus.mail)

    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)

    implementation(libs.jaudiotagger)
    implementation(libs.directory.watcher)
    implementation(libs.caffeine)
    implementation(libs.java.totp)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.jsoup)
    testImplementation(libs.ph.css)
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
}

tasks.test {
    useJUnitPlatform()
    val kotlinTestClasses = sourceSets.test.get().output.classesDirs
    classpath += files(kotlinTestClasses)
    finalizedBy(tasks.jacocoTestReport)
}

val buildInfoDir = layout.buildDirectory.dir("generated/buildInfo/kotlin")

val generateBuildInfo by tasks.registering {

    description = "Generates BuildInfo.kt, which contains the current app and Ktor versions."

    val appVersion = project.version.toString()
    val ktorVersion = libs.versions.ktor.get()

    inputs.properties("appVersion" to appVersion, "ktorVersion" to ktorVersion)
    outputs.dir(buildInfoDir)

    doLast {
        val pkgDir = buildInfoDir.get().asFile.resolve("de/mickkc/vibin/utils").also { it.mkdirs() }
        pkgDir.resolve("BuildInfo.kt").writeText(
            """
                package de.mickkc.vibin.utils
                
                object BuildInfo {
                    const val APP_VERSION = "$appVersion"
                    const val KTOR_VERSION = "$ktorVersion"
                }
            """.trimIndent()
        )
    }
}

kotlin.sourceSets["main"].kotlin.srcDir(generateBuildInfo)