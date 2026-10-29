package de.mickkc.vibin.scrobbling.lastfm.settings

import de.mickkc.vibin.settings.user.UserSetting

object LastFmUserSessionKey : UserSetting<String>(
    key = "lastfm_user_session_key",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = ""
)