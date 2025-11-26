package de.mickkc.vibin.permissions

enum class PermissionType(val id: String, val grantedByDefault: Boolean) {


    CHANGE_SERVER_SETTINGS("change_server_settings", false),
    CHANGE_OWN_USER_SETTINGS("change_own_user_settings", true),
    MANAGE_PERMISSIONS("manage_permissions", false),

    VIEW_TRACKS("view_tracks", true),
    STREAM_TRACKS("stream_tracks", true),
    MANAGE_TRACKS("manage_tracks", false),
    UPLOAD_TRACKS("upload_tracks", true),
    DOWNLOAD_TRACKS("download_tracks", true),
    DELETE_TRACKS("delete_tracks", false),

    VIEW_ALBUMS("view_albums", true),
    MANAGE_ALBUMS("manage_albums", false),
    DELETE_ALBUMS("delete_albums", false),

    VIEW_ARTISTS("view_artists", true),
    MANAGE_ARTISTS("manage_artists", false),
    DELETE_ARTISTS("delete_artists", false),

    VIEW_PLAYLISTS("view_playlists", true),
    MANAGE_PLAYLISTS("manage_playlists", true),
    CREATE_PRIVATE_PLAYLISTS("create_private_playlists", true),
    CREATE_PUBLIC_PLAYLISTS("create_public_playlists", true),
    DELETE_OWN_PLAYLISTS("delete_own_playlists", true),
    EDIT_COLLABORATIVE_PLAYLISTS("edit_collaborative_playlists", false),
    DELETE_COLLABORATIVE_PLAYLISTS("delete_collaborative_playlists", false),
    ALLOW_COLLABORATION("allow_collaboration", true),

    VIEW_USERS("view_users", false),
    MANAGE_USERS("manage_users", false),
    MANAGE_OWN_USER("edit_own_user", true),
    DELETE_USERS("delete_users", false),
    DELETE_OWN_USER("delete_own_user", false),
    CREATE_USERS("create_users", false),

    VIEW_TAGS("view_tags", true),
    MANAGE_TAGS("manage_tags", false),
    DELETE_TAGS("delete_tags", false),
    CREATE_TAGS("create_tags", false),

    MANAGE_SESSIONS("manage_sessions", true),
    MANAGE_TASKS("manage_tasks", false),
    MANAGE_WIDGETS("manage_widgets", true),

    CREATE_TRACK_RELATIONS("create_track_relations", true),
    DELETE_TRACK_RELATIONS("delete_track_relations", true);

    companion object {
        private val map = entries.associateBy(PermissionType::id)
        fun valueOfId(id: String): PermissionType? = map[id]
    }
}