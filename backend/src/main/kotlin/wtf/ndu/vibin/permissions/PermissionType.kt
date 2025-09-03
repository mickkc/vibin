package wtf.ndu.vibin.permissions

enum class PermissionType(val id: String, val grantedByDefault: Boolean) {

    CHANGE_SERVER_SETTINGS("change_server_settings", false),
    CHANGE_OWN_SETTINGS("change_own_settings", true),
    CHANGE_USER_SETTINGS("change_user_settings", false),

    MANAGE_PERMISSIONS("manage_permissions", false),

    VIEW_TRACKS("view_tracks", true),
    MANAGE_TRACKS("manage_tracks", false),
    UPLOAD_TRACKS("upload_tracks", true),
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
    DELETE_COLLABORATIVE_PLAYLISTS("delete_collaborative_playlists", false),
    ALLOW_COLLABORATION("allow_collaboration", true),

    VIEW_USERS("view_users", false),
    MANAGE_USERS("manage_users", false),
    DELETE_USERS("delete_users", false),
    CREATE_USERS("create_users", false)
}