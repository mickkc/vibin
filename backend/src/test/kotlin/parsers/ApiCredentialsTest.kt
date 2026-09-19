package parsers

import de.mickkc.vibin.parsing.Parser
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.SettingsRepo
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.LastFmApiKey
import de.mickkc.vibin.settings.server.LastFmSharedSecret
import de.mickkc.vibin.settings.server.SpotifyClientId
import de.mickkc.vibin.settings.server.SpotifyClientSecret
import de.mickkc.vibin.settings.user.LastFmUserApiKey
import de.mickkc.vibin.settings.user.LastFmUserSharedSecret
import de.mickkc.vibin.settings.user.SpotifyUserClientId
import de.mickkc.vibin.settings.user.SpotifyUserClientSecret
import utils.UserTestUtils
import utils.testApp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiCredentialsTest {

    @Test
    fun testLoadUserApiCredentials_HasPermission_IsSetForUser() = testApp(false) {
        val (user, _) = UserTestUtils.createUserAndSessionWithPermissions(
            "MyUser",
            "MyPassword",
            PermissionType.USE_SERVER_API_CREDENTIALS to true
        )
        SettingsRepo.updateUserSetting(SpotifyUserClientSecret, user.id.value, "MyOwnKey")
        SettingsRepo.updateServerSetting(SpotifyClientSecret, "TheServerKey")

        val (source, value) = Settings.getUserOrServer(
            SpotifyUserClientSecret,
            SpotifyClientSecret,
            user.id.value,
            PermissionType.USE_SERVER_API_CREDENTIALS,
            String::isNotBlank
        )
        assertEquals(Settings.SettingSource.User, source)
        assertEquals("MyOwnKey", value)
    }

    @Test
    fun testLoadUserApiCredentials_HasPermission_IsNotSetForUser() = testApp(false) {
        val (user, _) = UserTestUtils.createUserAndSessionWithPermissions(
            "MyUser",
            "MyPassword",
            PermissionType.USE_SERVER_API_CREDENTIALS to true
        )
        SettingsRepo.updateServerSetting(SpotifyClientId, "TheServerKey")

        val (source, value) = Settings.getUserOrServer(
            SpotifyUserClientId,
            SpotifyClientId,
            user.id.value,
            PermissionType.USE_SERVER_API_CREDENTIALS,
            String::isNotBlank
        )
        assertEquals(Settings.SettingSource.Server, source)
        assertEquals("TheServerKey", value)
    }

    @Test
    fun testLoadUserApiCredentials_NoPermission_IsSetForUser() = testApp(false) {
        val (user, _) = UserTestUtils.createUserAndSessionWithPermissions(
            "MyUser",
            "MyPassword",
            PermissionType.USE_SERVER_API_CREDENTIALS to false
        )
        SettingsRepo.updateUserSetting(LastFmUserApiKey, user.id.value, "MyOwnKey")
        SettingsRepo.updateServerSetting(LastFmApiKey, "TheServerKey")

        val (source, value) = Settings.getUserOrServer(
            LastFmUserApiKey,
            LastFmApiKey,
            user.id.value,
            PermissionType.USE_SERVER_API_CREDENTIALS,
            String::isNotBlank
        )
        assertEquals(Settings.SettingSource.User, source)
        assertEquals("MyOwnKey", value)
    }

    @Test
    fun testLoadUserApiCredentials_NoPermission_IsNotSetForUser() = testApp(false) {
        val (user, _) = UserTestUtils.createUserAndSessionWithPermissions(
            "MyUser",
            "MyPassword",
            PermissionType.USE_SERVER_API_CREDENTIALS to false
        )
        SettingsRepo.updateServerSetting(LastFmSharedSecret, "TheServerKey")

        val (source, value) = Settings.getUserOrServer(
            LastFmUserSharedSecret,
            LastFmSharedSecret,
            user.id.value,
            PermissionType.USE_SERVER_API_CREDENTIALS,
            String::isNotBlank
        )
        assertEquals(Settings.SettingSource.None, source)
        assertNull(value)
    }

    @Test
    fun testLoadUserApiCredentials_HasPermission_NoneSet() = testApp(false) {
        val (user, _) = UserTestUtils.createUserAndSessionWithPermissions(
            "MyUser",
            "MyPassword",
            PermissionType.USE_SERVER_API_CREDENTIALS to true
        )

        val (source, value) = Settings.getUserOrServer(
            SpotifyUserClientSecret,
            SpotifyClientSecret,
            user.id.value,
            PermissionType.USE_SERVER_API_CREDENTIALS,
            String::isNotBlank
        )
        assertEquals(Settings.SettingSource.None, source)
        assertNull(value)
    }

    @Test
    fun testLoadUserApiCredentials_NoPermission_NoneSet() = testApp(false) {
        val (user, _) = UserTestUtils.createUserAndSessionWithPermissions(
            "MyUser",
            "MyPassword",
            PermissionType.USE_SERVER_API_CREDENTIALS to false
        )

        val (source, value) = Settings.getUserOrServer(
            SpotifyUserClientId,
            SpotifyClientId,
            user.id.value,
            PermissionType.USE_SERVER_API_CREDENTIALS,
            String::isNotBlank
        )
        assertEquals(Settings.SettingSource.None, source)
        assertNull(value)
    }

    @Test
    fun testSpotifyCredentials_SetForUser() = testApp(false) {
        val (user, _) = UserTestUtils.createUserAndSessionWithPermissions(
            "MyUser",
            "MyPassword",
            PermissionType.USE_SERVER_API_CREDENTIALS to false
        )

        SettingsRepo.updateUserSetting(SpotifyUserClientId, user.id.value, "UserClientId")
        SettingsRepo.updateUserSetting(SpotifyUserClientSecret, user.id.value, "UserClientSecret")

        assertTrue(Parser.trackSearchProviders["Spotify"]!!.isAvailable(user))
    }

    @Test
    fun testSpotifyCredentials_WithPermission_SetForServer() = testApp(false) {
        val (user, _) = UserTestUtils.createUserAndSessionWithPermissions(
            "MyUser",
            "MyPassword",
            PermissionType.USE_SERVER_API_CREDENTIALS to true
        )

        SettingsRepo.updateServerSetting(SpotifyClientId, "ServerClientId")
        SettingsRepo.updateServerSetting(SpotifyClientSecret, "ServerClientSecret")

        assertTrue(Parser.trackSearchProviders["Spotify"]!!.isAvailable(user))
    }

    @Test
    fun testSpotifyCredentials_NoPermission_SetForServer() = testApp(false) {
        val (user, _) = UserTestUtils.createUserAndSessionWithPermissions(
            "MyUser",
            "MyPassword",
            PermissionType.USE_SERVER_API_CREDENTIALS to false
        )

        SettingsRepo.updateServerSetting(SpotifyClientId, "ServerClientId")
        SettingsRepo.updateServerSetting(SpotifyClientSecret, "ServerClientSecret")

        assertFalse(Parser.trackSearchProviders["Spotify"]!!.isAvailable(user))
    }

    @Test
    fun testSpotifyCredentials_WithPermission_SourceMismatch() = testApp(false) {
        val (user, _) = UserTestUtils.createUserAndSessionWithPermissions(
            "MyUser",
            "MyPassword",
            PermissionType.USE_SERVER_API_CREDENTIALS to false
        )

        SettingsRepo.updateServerSetting(SpotifyClientId, "ServerClientId")
        SettingsRepo.updateUserSetting(SpotifyUserClientSecret, user.id.value, "UserClientSecret")

        assertFalse(Parser.trackSearchProviders["Spotify"]!!.isAvailable(user))
    }


    @Test
    fun testSpotifyCredentials_WithPermission_PartiallySet() = testApp(false) {
        val (user, _) = UserTestUtils.createUserAndSessionWithPermissions(
            "MyUser",
            "MyPassword",
            PermissionType.USE_SERVER_API_CREDENTIALS to false
        )

        SettingsRepo.updateUserSetting(SpotifyUserClientSecret, user.id.value, "UserClientSecret")

        assertFalse(Parser.trackSearchProviders["Spotify"]!!.isAvailable(user))
    }

}