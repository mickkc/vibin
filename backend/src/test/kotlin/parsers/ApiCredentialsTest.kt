package parsers

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
import kotlin.test.assertNull

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

        val value = Settings.getUserOrServer(
            SpotifyUserClientSecret,
            SpotifyClientSecret,
            user.id.value,
            PermissionType.USE_SERVER_API_CREDENTIALS,
            String::isNotBlank
        )
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

        val value = Settings.getUserOrServer(
            SpotifyUserClientId,
            SpotifyClientId,
            user.id.value,
            PermissionType.USE_SERVER_API_CREDENTIALS,
            String::isNotBlank
        )
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

        val value = Settings.getUserOrServer(
            LastFmUserApiKey,
            LastFmApiKey,
            user.id.value,
            PermissionType.USE_SERVER_API_CREDENTIALS,
            String::isNotBlank
        )
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

        val value = Settings.getUserOrServer(
            LastFmUserSharedSecret,
            LastFmSharedSecret,
            user.id.value,
            PermissionType.USE_SERVER_API_CREDENTIALS,
            String::isNotBlank
        )
        assertNull(value)
    }

    @Test
    fun testLoadUserApiCredentials_HasPermission_NoneSet() = testApp(false) {
        val (user, _) = UserTestUtils.createUserAndSessionWithPermissions(
            "MyUser",
            "MyPassword",
            PermissionType.USE_SERVER_API_CREDENTIALS to true
        )

        val value = Settings.getUserOrServer(
            SpotifyUserClientSecret,
            SpotifyClientSecret,
            user.id.value,
            PermissionType.USE_SERVER_API_CREDENTIALS,
            String::isNotBlank
        )
        assertNull(value)
    }

    @Test
    fun testLoadUserApiCredentials_NoPermission_NoneSet() = testApp(false) {
        val (user, _) = UserTestUtils.createUserAndSessionWithPermissions(
            "MyUser",
            "MyPassword",
            PermissionType.USE_SERVER_API_CREDENTIALS to false
        )

        val value = Settings.getUserOrServer(
            SpotifyUserClientId,
            SpotifyClientId,
            user.id.value,
            PermissionType.USE_SERVER_API_CREDENTIALS,
            String::isNotBlank
        )
        assertNull(value)
    }

}