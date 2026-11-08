package de.mickkc.vibin.scrobbling.listenbrainz.settings

import de.mickkc.vibin.settings.user.UserSetting

object ListenBrainzToken : UserSetting<String>(
    key = "listenbrainz_user_token",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "",
    validationRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"),
    secret = true
)