package rest

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import utils.UserTestUtils
import utils.testApp
import wtf.ndu.vibin.dto.KeyValueDto
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.settings.Settings
import wtf.ndu.vibin.settings.server.MetadataLanguage
import wtf.ndu.vibin.settings.server.WelcomeTexts
import wtf.ndu.vibin.settings.server.serverSettings
import wtf.ndu.vibin.settings.user.BlockedArtists
import wtf.ndu.vibin.settings.user.BlockedTags
import wtf.ndu.vibin.settings.user.ShowActivitiesToOthers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingRestTest {

    // region Get Server Settings

    @Test
    fun testGetServerSettings() = testApp { client ->

        val response = client.get("/api/settings/server")

        assertTrue(response.status.isSuccess())

        val settings = response.body<Map<String, @Serializable @Contextual Any>>()

        serverSettings.forEach {
            assertTrue(settings.containsKey(it.key))
            assertEquals(Settings.get(it), settings[it.key])
            assertEquals(it.defaultValue, settings[it.key])
        }
    }

    @Test
    fun testGetServerSettings_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "regularuser",
            password = "password",
            PermissionType.CHANGE_SERVER_SETTINGS to false
        )

        val response = client.get("/api/settings/server") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    // endregion

    // region Get User Settings

    @Test
    fun testGetUserSettings() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "password",
            PermissionType.CHANGE_OWN_USER_SETTINGS to true
        )

        val response = client.get("/api/settings/user") {
            bearerAuth(token)
        }

        assertTrue(response.status.isSuccess())

        val settings = response.body<Map<String, @Serializable @Contextual Any>>()

        // Verify default user settings exist
        assertTrue(settings.containsKey("show_activities_to_others"))
        assertTrue(settings.containsKey("blocked_artists"))
        assertTrue(settings.containsKey("blocked_tags"))
    }

    @Test
    fun testGetUserSettings_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "password",
            PermissionType.CHANGE_OWN_USER_SETTINGS to false
        )

        val response = client.get("/api/settings/user") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    // endregion

    // region Update Server Settings

    @Test
    fun testUpdateServerSetting_String() = testApp { client ->

        val response = client.put("/api/settings/metadata_language") {
            setBody("fr")
        }

        assertTrue(response.status.isSuccess())

        val keyValue = response.body<KeyValueDto>()
        assertEquals("metadata_language", keyValue.key)
        assertEquals("fr", keyValue.value)

        // Verify the setting was updated
        val updatedValue = Settings.get(MetadataLanguage)
        assertEquals("fr", updatedValue)
    }

    @Test
    fun testUpdateServerSetting_List() = testApp { client ->

        val newWelcomeTexts = listOf("Hello!", "Welcome!", "Hi there!")
        val jsonValue = Json.encodeToString(newWelcomeTexts)

        val response = client.put("/api/settings/welcome_texts") {
            setBody(jsonValue)
        }

        assertTrue(response.status.isSuccess())

        val keyValue = response.body<KeyValueDto>()
        assertEquals("welcome_texts", keyValue.key)

        // Verify the setting was updated
        val updatedValue = Settings.get(WelcomeTexts)
        assertEquals(3, updatedValue.size)
        assertTrue(updatedValue.contains("Hello!"))
        assertTrue(updatedValue.contains("Welcome!"))
        assertTrue(updatedValue.contains("Hi there!"))
    }

    @Test
    fun testUpdateServerSetting_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "regularuser",
            password = "password",
            PermissionType.CHANGE_SERVER_SETTINGS to false
        )

        val response = client.put("/api/settings/metadata_language") {
            bearerAuth(token)
            setBody("fr")
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testUpdateServerSetting_NotFound() = testApp { client ->

        val response = client.put("/api/settings/nonexistent_setting") {
            setBody("value")
        }

        assertEquals(404, response.status.value)
    }

    // endregion

    // region Update User Settings

    @Test
    fun testUpdateUserSetting_Boolean() = testApp { client ->

        val response = client.put("/api/settings/show_activities_to_others") {
            setBody("false")
        }

        assertTrue(response.status.isSuccess())

        val keyValue = response.body<KeyValueDto>()
        assertEquals("show_activities_to_others", keyValue.key)
        assertEquals(false, keyValue.value)

        // Verify the setting was updated
        val updatedValue = Settings.get(ShowActivitiesToOthers, 1)
        assertFalse(updatedValue)
    }

    @Test
    fun testUpdateUserSetting_ListOfIds() = testApp { client ->

        val blockedArtistIds = listOf(1L, 2L, 3L)
        val jsonValue = Json.encodeToString(blockedArtistIds)

        val response = client.put("/api/settings/blocked_artists") {
            setBody(jsonValue)
        }

        assertTrue(response.status.isSuccess())

        val keyValue = response.body<KeyValueDto>()
        assertEquals("blocked_artists", keyValue.key)

        val updatedValue = Settings.get(BlockedArtists, 1)
        assertEquals(3, updatedValue.size)
        assertTrue(updatedValue.containsAll(blockedArtistIds))
    }

    @Test
    fun testUpdateUserSetting_EmptyList() = testApp { client ->

        val emptyList = emptyList<Long>()
        val jsonValue = Json.encodeToString(emptyList)

        val response = client.put("/api/settings/blocked_tags") {
            setBody(jsonValue)
        }

        assertTrue(response.status.isSuccess())

        val keyValue = response.body<KeyValueDto>()
        assertEquals("blocked_tags", keyValue.key)

        val updatedValue = Settings.get(BlockedTags, 1)
        assertTrue(updatedValue.isEmpty())
    }

    @Test
    fun testUpdateUserSetting_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "password",
            PermissionType.CHANGE_OWN_USER_SETTINGS to false
        )

        val response = client.put("/api/settings/show_activities_to_others") {
            bearerAuth(token)
            setBody("false")
        }

        assertEquals(403, response.status.value)
    }

    // endregion

    // region User Settings Isolation

    @Test
    fun testUserSettings_IsolatedBetweenUsers() = testApp(false) { client ->
        val (user1, token1) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "user1",
            password = "password",
            PermissionType.CHANGE_OWN_USER_SETTINGS to true
        )

        val (user2, token2) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "user2",
            password = "password",
            PermissionType.CHANGE_OWN_USER_SETTINGS to true
        )

        // User 1 sets their setting to false
        val response1 = client.put("/api/settings/show_activities_to_others") {
            bearerAuth(token1)
            setBody("false")
        }
        assertTrue(response1.status.isSuccess())

        // User 2 sets their setting to true
        val response2 = client.put("/api/settings/show_activities_to_others") {
            bearerAuth(token2)
            setBody("true")
        }
        assertTrue(response2.status.isSuccess())

        // Verify user 1's setting is still false
        val user1Value = Settings.get(ShowActivitiesToOthers, user1.id.value)
        assertFalse(user1Value)

        // Verify user 2's setting is true
        val user2Value = Settings.get(ShowActivitiesToOthers, user2.id.value)
        assertTrue(user2Value)
    }

    // endregion

    // region Settings Persistence

    @Test
    fun testServerSettings_Persistence() = testApp { client ->

        // Update a setting
        val updateResponse = client.put("/api/settings/metadata_language") {
            setBody("de")
        }
        assertTrue(updateResponse.status.isSuccess())

        // Retrieve all settings and verify the update persisted
        val getResponse = client.get("/api/settings/server")
        assertTrue(getResponse.status.isSuccess())

        val settings = getResponse.body<Map<String, @Serializable @Contextual Any>>()
        assertEquals("de", settings["metadata_language"])
    }

    @Test
    fun testUserSettings_Persistence() = testApp { client ->

        // Update a setting
        val updateResponse = client.put("/api/settings/show_activities_to_others") {
            setBody("false")
        }
        assertTrue(updateResponse.status.isSuccess())

        // Retrieve all settings and verify the update persisted
        val getResponse = client.get("/api/settings/user")
        assertTrue(getResponse.status.isSuccess())

        val settings = getResponse.body<Map<String, @Serializable @Contextual Any>>()
        assertEquals(false, settings["show_activities_to_others"])
    }

    // endregion
}
