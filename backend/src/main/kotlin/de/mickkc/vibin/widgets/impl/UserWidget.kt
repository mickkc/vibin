package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.images.ImageCache
import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.utils.PathUtils
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.WidgetUtils
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class UserWidget(ctx: WidgetContext) : BaseWidget(ctx) {

    @OptIn(ExperimentalEncodingApi::class)
    override fun render(): String = buildString {

        val user = UserRepo.getById(ctx.userId) ?: return t("widgets.user.not_found")

        val image = UserRepo.getProfilePicture(user)
        val avatarFile = image?.let { ImageCache.getImageFile(it, 192) }
            ?: PathUtils.getDefaultImage("user", 192)
            ?: return t("widgets.user.not_found")

        val bytes = avatarFile.readBytes()
        val base64Image = "data:image/${avatarFile.extension};base64,${
            Base64.encode(bytes)
        }"

        appendHTML(prettyPrint = false).div("widget-body") {

            div("user-container") {
                img(
                    alt = t("widgets.user.avatar.alt"),
                    src = base64Image,
                )

                div("user-info") {
                    h1 {
                        +(user.displayName ?: user.username)
                    }
                    p {
                        +"@${user.username}"
                    }
                    p("description") {
                        +user.description
                    }
                    a(href = "/web/users/${ctx.userId}", target = "_blank", classes = "btn") {
                        style = "background-color: ${WidgetUtils.colorToHex(ctx.accentColor)}; color: ${WidgetUtils.colorToHex(ctx.backgroundColor)};"
                        +t("widgets.user.view_profile")
                    }
                }
            }
        }
    }
}