package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.KeyValueDto
import de.mickkc.vibin.dto.SettingDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.SettingsRepo
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.serverSettings
import de.mickkc.vibin.settings.user.userSettings
import io.ktor.openapi.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.*

@OptIn(ExperimentalKtorApi::class)
fun Application.configureSettingRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/settings/server", PermissionType.CHANGE_SERVER_SETTINGS) {
            val userId = call.getUserId() ?: unauthorized()
            call.respond(serverSettings.associate {
                it.key to Settings.toDto(it, userId)
            })
        }.describe {
            operationId = "getServerSettings"
            summary = "Get all server settings"
            description = "Returns all server settings: ${serverSettings.joinToString { "`${it.key}`" }}"
            tag("Settings")
            authResponses(PermissionType.CHANGE_SERVER_SETTINGS) {
                successResponse<Map<String, SettingDto<String>>>("List of all server settings and their values")
            }
        }

        getP("/api/settings/user", PermissionType.CHANGE_OWN_USER_SETTINGS) {
            val userId = call.getUserId() ?: unauthorized()
            call.respond(userSettings.associate {
                it.key to Settings.toDto(it, userId)
            })
        }.describe {
            operationId = "getUserSettings"
            summary = "Get all user settings"
            description = "Returns all user settings: ${userSettings.joinToString { "`${it.key}`" }}"
            tag("Settings")
            authResponses(PermissionType.CHANGE_OWN_USER_SETTINGS) {
                successResponse<Map<String, SettingDto<String>>>("List of all user settings and their " +
                        "values for the current user")
            }
        }

        putP("/api/settings/{settingKey}") {

            val settingKey = call.getStringParameter("settingKey")
            val setting = SettingsRepo.getServerSetting(settingKey)
            val settingValue = call.receive<String>()

            if (setting == null) {
                val userSetting = SettingsRepo.getUserSetting(settingKey) ?: notFound()

                if (!call.hasPermissions(PermissionType.CHANGE_OWN_USER_SETTINGS))
                    noPermission(PermissionType.CHANGE_OWN_USER_SETTINGS)

                if (settingValue.isNotEmpty() && userSetting.validationRegex != null && !userSetting.validationRegex.matches(settingValue)) {
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
                    noPermission(PermissionType.CHANGE_SERVER_SETTINGS)

                if (settingValue.isNotEmpty() && setting.validationRegex != null && !setting.validationRegex.matches(settingValue)) {
                    invalidParameter("value", setting.validationRegex.pattern)
                }

                val parsedValue = setting.parser(settingValue)
                    ?: invalidParameter("body")

                SettingsRepo.updateServerSetting(setting, settingValue)
                call.respond(KeyValueDto(setting.key, parsedValue))
            }
        }.describe {
            operationId = "updateSetting"
            summary = "Update a setting's value"
            description = "This endpoint works for both server settings and user settings."
            tag("Settings")
            parameters {
                p("settingsKey", "lastfm_api_key")
            }
            requestBody {
                description = "The setting's new value, represented as a string"
                schema = jsonSchema<String>()
            }
            authResponses {
                successResponse<KeyValueDto<String, String>>("A key-value pair, where the key is the key of the setting, and the value is the setting's new value.")
                invalidParameterResponse("value", "Invalid settings value")
                notFoundResponse("Setting")
                missingPermissionsResponse(PermissionType.CHANGE_SERVER_SETTINGS, PermissionType.CHANGE_OWN_USER_SETTINGS)
            }
            optionalPermissions(
                PermissionType.CHANGE_OWN_USER_SETTINGS to "Change user settings",
                PermissionType.CHANGE_SERVER_SETTINGS to "Change server settings"
            )
        }
    }
}