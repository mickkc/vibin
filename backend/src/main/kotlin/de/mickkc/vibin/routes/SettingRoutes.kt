package de.mickkc.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.dto.KeyValueDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.SettingsRepo
import de.mickkc.vibin.settings.server.serverSettings
import de.mickkc.vibin.settings.user.userSettings

fun Application.configureSettingRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/settings/server", PermissionType.CHANGE_SERVER_SETTINGS) {
            val settings = SettingsRepo.getAllValues(serverSettings)
            call.respond(settings)
        }

        getP("/api/settings/user", PermissionType.CHANGE_OWN_USER_SETTINGS) {
            val userId = call.getUserId() ?: return@getP call.unauthorized()
            val settings = SettingsRepo.getAllValues(userId, userSettings)
            call.respond(settings)
        }

        putP("/api/settings/{settingKey}") {

            val settingKey = call.parameters["settingKey"] ?: return@putP call.missingParameter("settingKey")
            val setting = SettingsRepo.getServerSetting(settingKey)

            if (setting == null) {
                val userSetting = SettingsRepo.getUserSetting(settingKey) ?: return@putP call.notFound()

                if (!call.hasPermissions(PermissionType.CHANGE_OWN_USER_SETTINGS))
                    return@putP call.forbidden()

                val userId = call.getUserId() ?: return@putP call.unauthorized()
                val settingValue = call.receive<String>()
                SettingsRepo.updateUserSetting(userSetting, userId, settingValue)
                return@putP call.respond(KeyValueDto(userSetting.key, userSetting.parser(settingValue)))
            }
            else {
                if (!call.hasPermissions(PermissionType.CHANGE_SERVER_SETTINGS))
                    return@putP call.forbidden()

                val settingValue = call.receive<String>()
                SettingsRepo.updateServerSetting(setting, settingValue)
                call.respond(KeyValueDto(setting.key, setting.parser(settingValue)))
            }

        }
    }
}