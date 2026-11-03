package de.mickkc.vibin.scrobbling.lastfm.settings

import de.mickkc.vibin.settings.user.UserSetting

object LastFmUserCredentialsUsername : UserSetting<String>(
    key = "lastfm_user_credentials_username",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "",
    validationRegex = Regex("^(?=.*\\S).+$")
)

object LastFmUserCredentialsPassword : UserSetting<String>(
    key = "lastfm_user_credentials_password",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "",
    validationRegex = Regex("^(?=.*\\S).+$"),
    secret = true
)