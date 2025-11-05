package rest

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import utils.TagTestUtils
import utils.UserTestUtils
import utils.testApp
import wtf.ndu.vibin.dto.tags.TagDto
import wtf.ndu.vibin.dto.tags.TagEditDto
import wtf.ndu.vibin.permissions.PermissionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TagRestTest {

    // region Get

    @Test
    fun testGetTags() = testApp { client ->
        TagTestUtils.createTag("Rock", "Rock music genre", "#FF0000")
        TagTestUtils.createTag("Jazz", "Jazz music genre", "#00FF00")
        TagTestUtils.createTag("Classical", "Classical music genre", "#0000FF")

        val response = client.get("/api/tags")

        assertTrue(response.status.isSuccess())

        val tags = response.body<List<TagDto>>()

        assertEquals(3, tags.size)

        val firstTag = tags[0]
        assertEquals("Classical", firstTag.name)
        assertEquals("Classical music genre", firstTag.description)
        assertEquals("#0000FF", firstTag.color)

        val secondTag = tags[1]
        assertEquals("Jazz", secondTag.name)
        assertEquals("Jazz music genre", secondTag.description)
        assertEquals("#00FF00", secondTag.color)

        val thirdTag = tags[2]
        assertEquals("Rock", thirdTag.name)
        assertEquals("Rock music genre", thirdTag.description)
        assertEquals("#FF0000", thirdTag.color)
    }

    @Test
    fun testGetTags_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_TAGS to false
        )

        val response = client.get("/api/tags") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testGetTags_Search() = testApp { client ->
        TagTestUtils.createTag("Rock Music", "Rock music genre")
        TagTestUtils.createTag("Jazz", "Jazz music genre")
        TagTestUtils.createTag("Hard Rock", "Hard rock music genre")

        val response = client.get("/api/tags") {
            parameter("query", "rock")
        }

        assertTrue(response.status.isSuccess())

        val tags = response.body<List<TagDto>>()

        assertEquals(2, tags.size)
        assertEquals("Rock Music", tags[0].name)
        assertEquals("Hard Rock", tags[1].name)
    }

    @Test
    fun testGetTags_WithLimit() = testApp { client ->
        TagTestUtils.createTag("Rock", "Rock music genre")
        TagTestUtils.createTag("Jazz", "Jazz music genre")
        TagTestUtils.createTag("Classical", "Classical music genre")

        val response = client.get("/api/tags") {
            parameter("limit", 2)
        }

        assertTrue(response.status.isSuccess())

        val tags = response.body<List<TagDto>>()

        assertEquals(2, tags.size)
    }

    @Test
    fun testGetTagsByIds() = testApp { client ->
        val tag1 = TagTestUtils.createTag("Rock", "Rock music genre")
        val tag2 = TagTestUtils.createTag("Jazz", "Jazz music genre")
        TagTestUtils.createTag("Classical", "Classical music genre")

        val response = client.get("/api/tags/ids") {
            parameter("ids", "${tag1.id.value},${tag2.id.value}")
        }

        assertTrue(response.status.isSuccess())

        val tags = response.body<List<TagDto>>()

        assertEquals(2, tags.size)
        assertEquals("Jazz", tags[0].name)
        assertEquals("Rock", tags[1].name)
    }

    @Test
    fun testGetTagsByIds_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_TAGS to false
        )

        val response = client.get("/api/tags/ids") {
            parameter("ids", "1,2")
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testGetTagByName() = testApp { client ->
        TagTestUtils.createTag("Rock", "Rock music genre", "#FF0000")

        val response = client.get("/api/tags/named/Rock")

        assertTrue(response.status.isSuccess())

        val tag = response.body<TagDto>()

        assertEquals("Rock", tag.name)
        assertEquals("Rock music genre", tag.description)
        assertEquals("#FF0000", tag.color)
    }

    @Test
    fun testGetTagByName_NotFound() = testApp { client ->
        val response = client.get("/api/tags/named/NonExistentTag")

        assertEquals(404, response.status.value)
    }

    @Test
    fun testGetTagByName_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_TAGS to false
        )

        val response = client.get("/api/tags/named/Rock") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testCheckTagExists() = testApp { client ->
        TagTestUtils.createTag("Rock", "Rock music genre")

        val responseExists = client.get("/api/tags/check/Rock")
        assertTrue(responseExists.status.isSuccess())

        val responseNotExists = client.get("/api/tags/check/NonExistentTag")
        assertTrue(responseNotExists.status.isSuccess())
    }

    @Test
    fun testCheckTagExists_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_TAGS to false
        )

        val response = client.get("/api/tags/check/Rock") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    // endregion

    // region Create

    @Test
    fun testCreateTag() = testApp { client ->
        val createDto = TagEditDto(
            name = "Rock",
            description = "Rock music genre",
            color = "#FF0000"
        )

        val response = client.post("/api/tags") {
            setBody(createDto)
        }

        assertTrue(response.status.isSuccess())

        val createdTag = response.body<TagDto>()
        assertEquals("Rock", createdTag.name)
        assertEquals("Rock music genre", createdTag.description)
        assertEquals("#FF0000", createdTag.color)
    }

    @Test
    fun testCreateTag_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.CREATE_TAGS to false
        )

        val createDto = TagEditDto(
            name = "Rock",
            description = "Rock music genre",
            color = "#FF0000"
        )

        val response = client.post("/api/tags") {
            bearerAuth(token)
            setBody(createDto)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testCreateTag_DuplicateName() = testApp { client ->
        TagTestUtils.createTag("Rock", "Rock music genre")

        val createDto = TagEditDto(
            name = "Rock",
            description = "Another rock genre",
            color = "#FF0000"
        )

        val response = client.post("/api/tags") {
            setBody(createDto)
        }

        assertEquals(400, response.status.value)
    }

    @Test
    fun testCreateTag_DuplicateNameCaseInsensitive() = testApp { client ->
        TagTestUtils.createTag("Rock", "Rock music genre")

        val createDto = TagEditDto(
            name = "rock",
            description = "Another rock genre",
            color = "#FF0000"
        )

        val response = client.post("/api/tags") {
            setBody(createDto)
        }

        assertEquals(400, response.status.value)
    }

    @Test
    fun testCreateTag_MinimalFields() = testApp { client ->
        val createDto = TagEditDto(
            name = "Jazz",
            description = null,
            color = null
        )

        val response = client.post("/api/tags") {
            setBody(createDto)
        }

        assertTrue(response.status.isSuccess())

        val createdTag = response.body<TagDto>()
        assertEquals("Jazz", createdTag.name)
        assertEquals("", createdTag.description)
        assertEquals(null, createdTag.color)
    }

    // endregion

    // region Update

    @Test
    fun testUpdateTag() = testApp { client ->
        val tag = TagTestUtils.createTag("Old Name", "Old Description", "#000000")

        val updateDto = TagEditDto(
            name = "New Name",
            description = "New Description",
            color = "#FFFFFF"
        )

        val response = client.put("/api/tags/${tag.id.value}") {
            setBody(updateDto)
        }

        assertTrue(response.status.isSuccess())

        val updatedTag = response.body<TagDto>()
        assertEquals("New Name", updatedTag.name)
        assertEquals("New Description", updatedTag.description)
        assertEquals("#FFFFFF", updatedTag.color)
    }

    @Test
    fun testUpdateTag_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.MANAGE_TAGS to false
        )

        val tag = TagTestUtils.createTag("Old Name", "Old Description")

        val updateDto = TagEditDto(
            name = "New Name",
            description = "New Description",
            color = null
        )

        val response = client.put("/api/tags/${tag.id.value}") {
            bearerAuth(token)
            setBody(updateDto)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testUpdateTag_NotFound() = testApp { client ->
        val updateDto = TagEditDto(
            name = "New Name",
            description = "New Description",
            color = null
        )

        val response = client.put("/api/tags/9999") {
            setBody(updateDto)
        }

        assertEquals(404, response.status.value)
    }

    @Test
    fun testUpdateTag_DuplicateName() = testApp { client ->
        TagTestUtils.createTag("Rock", "Rock music genre")
        val tag2 = TagTestUtils.createTag("Jazz", "Jazz music genre")

        val updateDto = TagEditDto(
            name = "Rock",
            description = "Trying to use existing name",
            color = null
        )

        val response = client.put("/api/tags/${tag2.id.value}") {
            setBody(updateDto)
        }

        assertEquals(400, response.status.value)
    }

    @Test
    fun testUpdateTag_DuplicateNameCaseInsensitive() = testApp { client ->
        TagTestUtils.createTag("Rock", "Rock music genre")
        val tag2 = TagTestUtils.createTag("Jazz", "Jazz music genre")

        val updateDto = TagEditDto(
            name = "rock",
            description = "Trying to use existing name",
            color = null
        )

        val response = client.put("/api/tags/${tag2.id.value}") {
            setBody(updateDto)
        }

        assertEquals(400, response.status.value)
    }

    @Test
    fun testUpdateTag_SameNameAllowed() = testApp { client ->
        val tag = TagTestUtils.createTag("Rock", "Rock music genre", "#FF0000")

        val updateDto = TagEditDto(
            name = "Rock",
            description = "Updated description",
            color = "#00FF00"
        )

        val response = client.put("/api/tags/${tag.id.value}") {
            setBody(updateDto)
        }

        assertTrue(response.status.isSuccess())

        val updatedTag = response.body<TagDto>()
        assertEquals("Rock", updatedTag.name)
        assertEquals("Updated description", updatedTag.description)
        assertEquals("#00FF00", updatedTag.color)
    }

    // endregion

    // region Delete

    @Test
    fun testDeleteTag() = testApp { client ->
        val tag = TagTestUtils.createTag("Rock", "Rock music genre")

        val response = client.delete("/api/tags/${tag.id.value}")

        assertTrue(response.status.isSuccess())

        val getResponse = client.get("/api/tags/named/Rock")
        assertEquals(404, getResponse.status.value)
    }

    @Test
    fun testDeleteTag_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.DELETE_TAGS to false
        )

        val tag = TagTestUtils.createTag("Rock", "Rock music genre")

        val response = client.delete("/api/tags/${tag.id.value}") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testDeleteTag_NotFound() = testApp { client ->
        val response = client.delete("/api/tags/9999")

        assertEquals(404, response.status.value)
    }

    // endregion

    // region Autocomplete

    @Test
    fun testAutocompleteTag() = testApp { client ->
        TagTestUtils.createTag("Rock Music", "Rock music genre")
        TagTestUtils.createTag("Jazz", "Jazz music genre")
        TagTestUtils.createTag("Hard Rock", "Hard rock music genre")

        val response = client.get("/api/tags/autocomplete") {
            parameter("query", "rock")
            parameter("limit", 5)
        }

        assertTrue(response.status.isSuccess())

        val suggestions = response.body<List<String>>()

        assertEquals(2, suggestions.size)
        assertEquals("Rock Music", suggestions[0])
        assertEquals("Hard Rock", suggestions[1])
    }

    @Test
    fun testAutocompleteTag_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_TAGS to false
        )

        val response = client.get("/api/tags/autocomplete") {
            parameter("query", "rock")
            parameter("limit", 5)
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    // endregion
}
