package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.WidgetUtils
import kotlinx.html.*
import kotlinx.html.stream.appendHTML

class UserWidget(ctx: WidgetContext) : BaseWidget(ctx) {

    override fun render(interactive: Boolean): String = buildString {

        val user = UserRepo.getById(ctx.userId) ?: return t("widgets.user.not_found")

        val image = UserRepo.getProfilePicture(user)
        val avatarUrl = WidgetUtils.getImageUrl(image, "user", 192)

        appendHTML(prettyPrint = false).div("widget-body") {

            div("user-container") {
                img(
                    alt = t("widgets.user.avatar.alt"),
                    src = avatarUrl,
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
                    if (interactive) {
                        a(href = "/web/users/${ctx.userId}", target = "_blank", classes = "btn") {
                            style = "background-color: ${WidgetUtils.colorToHex(ctx.accentColor)}; color: ${WidgetUtils.colorToHex(ctx.backgroundColor)};"
                            +t("widgets.user.view_profile")
                        }
                    }
                }
            }
        }
    }
}