package de.mickkc.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.dto.KeyValueDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.SettingsRepo
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.serverSettings
import de.mickkc.vibin.settings.user.userSettings
import kotlin.to

fun Application.configureSettingRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/settings/server", PermissionType.CHANGE_SERVER_SETTINGS) {
            val userId = call.getUserId() ?: unauthorized()
            call.respond(serverSettings.associate {
                it.key to Settings.toDto(it, userId)
            })
        }

        getP("/api/settings/user", PermissionType.CHANGE_OWN_USER_SETTINGS) {
            val userId = call.getUserId() ?: unauthorized()
            call.respond(userSettings.associate {
                it.key to Settings.toDto(it, userId)
            })
        }

        putP("/api/settings/{settingKey}") {

            val settingKey = call.getStringParameter("settingKey")
            val setting = SettingsRepo.getServerSetting(settingKey)
            val settingValue = call.receive<String>()

            if (setting == null) {
                val userSetting = SettingsRepo.getUserSetting(settingKey) ?: notFound()

                if (!call.hasPermissions(PermissionType.CHANGE_OWN_USER_SETTINGS))
                    forbidden()

                if (userSetting.validationRegex != null && !userSetting.validationRegex.matches(settingValue)) {
                    invalidParameter("value", userSetting.validationRegex.pattern)
                }

                val userId = call.getUserId() ?: unauthorized()
                val parsedValue = userSetting.parser(settingValue)
                    ?: invalidParameter("body")

                SettingsRepo.updateUserSetting(userSetting, userId, settingValue)
                return@putP call.respond(KeyValueDto(userSetting.key, parsedValue))
            }
            else {
                if (!call.hasPermissions(PermissionType.CHANGE_SERVER_SETTINGS))
                    forbidden()

                if (setting.validationRegex != null && !setting.validationRegex.matches(settingValue)) {
                    invalidParameter("value", setting.validationRegex.pattern)
                }

                val parsedValue = setting.parser(settingValue)
                    ?: invalidParameter("body")

                SettingsRepo.updateServerSetting(setting, settingValue)
                call.respond(KeyValueDto(setting.key, parsedValue))
            }
        }
    }
}