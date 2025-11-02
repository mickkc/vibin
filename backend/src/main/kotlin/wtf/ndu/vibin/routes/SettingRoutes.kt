package wtf.ndu.vibin.routes

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import wtf.ndu.vibin.dto.KeyValueDto
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.SettingsRepo
import wtf.ndu.vibin.settings.serverSettings

fun Application.configureSettingRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/settings/server", PermissionType.CHANGE_SERVER_SETTINGS) {
            val settings = SettingsRepo.getAllValues(serverSettings)
            call.respond(settings)
        }

        putP("/api/settings/server/{settingKey}", PermissionType.CHANGE_SERVER_SETTINGS) {

            val settingKey = call.parameters["settingKey"] ?: return@putP call.missingParameter("settingKey")
            val setting = SettingsRepo.getServerSetting(settingKey) ?: return@putP call.notFound()

            val settingValue = call.receive<String>()
            SettingsRepo.updateSetting(setting, settingValue)
            call.respond(KeyValueDto(setting.key, setting.parser(settingValue)))
        }

    }
}