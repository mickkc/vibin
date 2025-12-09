package rest

import com.helger.css.reader.CSSReader
import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.dto.widgets.CreateWidgetDto
import de.mickkc.vibin.dto.widgets.WidgetDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.repos.WidgetRepo
import de.mickkc.vibin.utils.ChecksumUtil
import de.mickkc.vibin.widgets.ImageCryptoUtil
import de.mickkc.vibin.widgets.WidgetType
import de.mickkc.vibin.widgets.WidgetUtils
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.EntityTagVersion
import org.jsoup.Jsoup
import utils.UserTestUtils
import utils.testApp
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.test.*

class WidgetRestTest {

    // region Get Styles

    @Test
    fun testGetWidgetStyles() = testApp { client ->
        val response = client.get("/api/widgets/styles")

        assertTrue(response.status.isSuccess())
        assertEquals("text/css; charset=UTF-8", response.contentType()?.toString())

        val css = response.body<String>()
        assertTrue(css.isNotEmpty())

        val parsed = CSSReader.readFromString(css)
        assertNotNull(parsed)
        assertTrue(parsed.hasRules())
    }

    // endregion

    // region Get Widget Images

    @Test
    fun testGetWidgetImage_DefaultImage() = testApp { client ->
        val checksum = "default-track"

        val imageDir = createTempDirectory("widget-images")
        imageDir.toFile().apply {
            mkdirs()
            deleteOnExit()
        }
        EnvUtil.addOverride(EnvUtil.THUMBNAIL_DIR, imageDir.absolutePathString())

        val signedUrl = ImageCryptoUtil.generateSignedImageUrl(checksum, 192)

        val response = client.get(signedUrl)

        assertTrue(response.status.isSuccess())

        val bytes = response.body<ByteArray>()

        val responseChecksum = ChecksumUtil.getChecksum(bytes)

        // Precomputed checksum of the default image in 192x192 size
        assertEquals("9d893b2a161d255c154aa628d10c471f", responseChecksum)
    }

    @Test
    fun testGetWidgetImage_InvalidSignature() = testApp { client ->
        val checksum = "default-track"
        val response = client.get("/api/widgets/images/$checksum") {
            parameter("quality", 192)
            parameter("exp", System.currentTimeMillis() / 1000 + 3600)
            parameter("sig", "invalid_signature")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun testGetWidgetImage_ExpiredSignature() = testApp { client ->
        val checksum = "default-track"
        val expiredTimestamp = System.currentTimeMillis() / 1000 - 3600 // 1 hour ago

        val response = client.get("/api/widgets/images/$checksum") {
            parameter("quality", 192)
            parameter("exp", expiredTimestamp)
            parameter("sig", "doesnt_matter")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun testGetWidgetImage_NonExistentChecksum() = testApp { client ->
        val checksum = "nonexistent"
        val signedUrl = ImageCryptoUtil.generateSignedImageUrl(checksum, 192)

        val response = client.get(signedUrl)

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // endregion

    // region Get Widget HTML

    @Test
    fun testGetWidget() = testApp { client ->
        val user = UserTestUtils.createTestUser("testuser", "password")
        val widget = WidgetRepo.shareWidget(user, listOf(WidgetType.SERVER_STATS), null, null, null)

        val response = client.get("/api/widgets/${widget.id.value}") {
            parameter("lang", "en")
        }

        assertTrue(response.status.isSuccess())
        assertEquals("text/html; charset=UTF-8", response.contentType()?.toString())

        val html = response.body<String>()
        assertTrue(html.contains("<html"))
        assertTrue(html.contains("Server Statistics"))
        assertTrue(html.contains("</html>"))
    }

    @Test
    fun testGetWidget_WithCustomColors() = testApp { client ->
        val user = UserTestUtils.createTestUser("testuser", "password")
        val widget = WidgetRepo.shareWidget(
            user,
            listOf(WidgetType.SERVER_STATS),
            bgColor = 0x123456,
            fgColor = 0xABCDEF,
            accentColor = 0xFF0000
        )

        val response = client.get("/api/widgets/${widget.id.value}")

        assertTrue(response.status.isSuccess())
        assertEquals("text/html; charset=UTF-8", response.contentType()?.toString())

        val html = response.body<String>()
        val parsed = Jsoup.parse(html)

        assertEquals("#123456", parsed.body().attr("style").substringAfter("background-color: ").substringBefore(";"))
        assertEquals("#ABCDEF", parsed.body().attr("style").substringAfter(" color: ").substringBefore(";"))
        assertEquals("#FF0000", parsed.body().attr("style").substringAfterLast("accent-color: ").substringBefore(";"))
    }

    @Test
    fun testGetWidget_WithQueryColors() = testApp { client ->
        val user = UserTestUtils.createTestUser("testuser", "password")
        val widget = WidgetRepo.shareWidget(user, listOf(WidgetType.SERVER_STATS), null, null, null)

        val response = client.get("/api/widgets/${widget.id.value}") {
            parameter("bgColor", "123456")
            parameter("fgColor", "ABCDEF")
            parameter("accentColor", "FF0000")
        }

        assertTrue(response.status.isSuccess())
        assertEquals("text/html; charset=UTF-8", response.contentType()?.toString())

        val html = response.body<String>()
        val parsed = Jsoup.parse(html)

        assertEquals("#123456", parsed.body().attr("style").substringAfter("background-color: ").substringBefore(";"))
        assertEquals("#ABCDEF", parsed.body().attr("style").substringAfter(" color: ").substringBefore(";"))
        assertEquals("#FF0000", parsed.body().attr("style").substringAfterLast("accent-color: ").substringBefore(";"))
    }

    @Test
    fun testGetWidget_WithLanguage() = testApp { client ->
        val user = UserTestUtils.createTestUser("testuser", "password")
        val widget = WidgetRepo.shareWidget(user, listOf(WidgetType.SERVER_STATS), null, null, null)

        val response = client.get("/api/widgets/${widget.id.value}") {
            parameter("lang", "de")
        }

        assertTrue(response.status.isSuccess())

        val html = response.body<String>()
        assertTrue(html.contains("<html lang=\"de\""))
    }

    @Test
    fun testGetWidget_WithAcceptLanguageHeader_1() = testApp { client ->
        val user = UserTestUtils.createTestUser("testuser", "password")
        val widget = WidgetRepo.shareWidget(user, listOf(WidgetType.SERVER_STATS), null, null, null)

        val response = client.get("/api/widgets/${widget.id.value}") {
            header("Accept-Language", "de,en-US;q=0.9")
        }

        assertTrue(response.status.isSuccess())

        val html = response.body<String>()
        assertTrue(html.contains("<html lang=\"de\""))
    }

    @Test
    fun testGetWidget_WithAcceptLanguageHeader_2() = testApp { client ->
        val user = UserTestUtils.createTestUser("testuser", "password")
        val widget = WidgetRepo.shareWidget(user, listOf(WidgetType.SERVER_STATS), null, null, null)

        val response = client.get("/api/widgets/${widget.id.value}") {
            header("Accept-Language", "de-DE,en-US;q=0.9")
        }

        assertTrue(response.status.isSuccess())

        val html = response.body<String>()
        assertTrue(html.contains("<html lang=\"de\""))
    }

    @Test
    fun testGetWidget_UnsupportedLanguageFallsBackToEnglish() = testApp { client ->
        val user = UserTestUtils.createTestUser("testuser", "password")
        val widget = WidgetRepo.shareWidget(user, listOf(WidgetType.SERVER_STATS), null, null, null)

        val response = client.get("/api/widgets/${widget.id.value}") {
            parameter("lang", "unsupported")
        }

        assertTrue(response.status.isSuccess())

        val html = response.body<String>()
        assertTrue(html.contains("<html lang=\"en\""))
    }

    @Test
    fun testGetWidget_MultipleTypes() = testApp { client ->
        val user = UserTestUtils.createTestUser("testuser", "password")
        val widget = WidgetRepo.shareWidget(
            user,
            listOf(WidgetType.SERVER_STATS, WidgetType.USER),
            null,
            null,
            null
        )

        val response = client.get("/api/widgets/${widget.id.value}")

        assertTrue(response.status.isSuccess())
        assertEquals("text/html; charset=UTF-8", response.contentType()?.toString())

        val html = response.body<String>()
        val parsed = Jsoup.parse(html)

        assertEquals(2, parsed.body().select(".widget-body").size)
    }

    @Test
    fun testGetWidget_NotFound() = testApp { client ->
        val response = client.get("/api/widgets/nonexistent-id")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // endregion

    // region Get All Widgets

    @Test
    fun testGetAllWidgets() = testApp { client ->
        val user = UserRepo.getById(1L)!!
        WidgetRepo.shareWidget(user, listOf(WidgetType.SERVER_STATS), null, null, null)
        WidgetRepo.shareWidget(user, listOf(WidgetType.USER), null, null, null)

        val response = client.get("/api/widgets")

        assertTrue(response.status.isSuccess())

        val widgets = response.body<List<WidgetDto>>()
        assertEquals(2, widgets.size)
    }

    @Test
    fun testGetAllWidgets_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "password",
            PermissionType.MANAGE_WIDGETS to false
        )

        val response = client.get("/api/widgets") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun testGetAllWidgets_Unauthorized() = testApp(false) { client ->
        val response = client.get("/api/widgets")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testGetAllWidgets_Empty() = testApp { client ->
        val response = client.get("/api/widgets")

        assertTrue(response.status.isSuccess())

        val widgets = response.body<List<WidgetDto>>()
        assertEquals(0, widgets.size)
    }

    // endregion

    // region Create Widget

    @Test
    fun testCreateWidget() = testApp { client ->
        val createDto = CreateWidgetDto(
            types = listOf("SERVER_STATS", "USER"),
            bgColor = null,
            fgColor = null,
            accentColor = null
        )

        val response = client.post("/api/widgets") {
            contentType(ContentType.Application.Json)
            setBody(createDto)
        }

        assertTrue(response.status.isSuccess())

        val widgets = WidgetRepo.getAllForUser(UserRepo.getById(1L)!!)
        assertEquals(1, widgets.size)

        val widget = widgets.single()

        assertEquals(null, widget.bgColor)
        assertEquals(null, widget.fgColor)
        assertEquals(null, widget.accentColor)

        val types = WidgetRepo.getTypes(widget)
        assertEquals(2, types.size)
        assertEquals(WidgetType.SERVER_STATS, types[0])
        assertEquals(WidgetType.USER, types[1])
    }

    @Test
    fun testCreateWidget_WithColors() = testApp { client ->
        val createDto = CreateWidgetDto(
            types = listOf("SERVER_STATS"),
            bgColor = "123456",
            fgColor = "ABCDEF",
            accentColor = "FF0000"
        )

        val response = client.post("/api/widgets") {
            contentType(ContentType.Application.Json)
            setBody(createDto)
        }

        assertTrue(response.status.isSuccess())

        val widgets = WidgetRepo.getAllForUser(UserRepo.getById(1L)!!)
        assertEquals(1, widgets.size)

        val widget = widgets.single()

        assertEquals(WidgetUtils.colorFromHex("123456"), widget.bgColor)
        assertEquals(WidgetUtils.colorFromHex("ABCDEF"), widget.fgColor)
        assertEquals(WidgetUtils.colorFromHex("FF0000"), widget.accentColor)

        val types = WidgetRepo.getTypes(widget)
        assertEquals(1, types.size)
        assertEquals(WidgetType.SERVER_STATS, types[0])
    }

    @Test
    fun testCreateWidget_InvalidType() = testApp { client ->
        val createDto = CreateWidgetDto(
            types = listOf("INVALID_TYPE"),
            bgColor = null,
            fgColor = null,
            accentColor = null
        )

        val response = client.post("/api/widgets") {
            contentType(ContentType.Application.Json)
            setBody(createDto)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testCreateWidget_EmptyTypes() = testApp { client ->
        val createDto = CreateWidgetDto(
            types = emptyList(),
            bgColor = null,
            fgColor = null,
            accentColor = null
        )

        val response = client.post("/api/widgets") {
            contentType(ContentType.Application.Json)
            setBody(createDto)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testCreateWidget_InvalidColorFormat() = testApp { client ->
        val createDto = CreateWidgetDto(
            types = listOf("SERVER_STATS"),
            bgColor = "GGGGGG", // Invalid hex
            fgColor = "ABCDEF",
            accentColor = "FF0000"
        )

        val response = client.post("/api/widgets") {
            contentType(ContentType.Application.Json)
            setBody(createDto)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testCreateWidget_PartialColors() = testApp { client ->
        val createDto = CreateWidgetDto(
            types = listOf("SERVER_STATS"),
            bgColor = "123456",
            fgColor = null,  // Missing color
            accentColor = "FF0000"
        )

        val response = client.post("/api/widgets") {
            contentType(ContentType.Application.Json)
            setBody(createDto)
        }

        // Should fail because either all or none of the colors must be provided
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testCreateWidget_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "password",
            PermissionType.MANAGE_WIDGETS to false
        )

        val createDto = CreateWidgetDto(
            types = listOf("SERVER_STATS"),
            bgColor = null,
            fgColor = null,
            accentColor = null
        )

        val response = client.post("/api/widgets") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(createDto)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun testCreateWidget_Unauthorized() = testApp(false) { client ->
        val createDto = CreateWidgetDto(
            types = listOf("SERVER_STATS"),
            bgColor = null,
            fgColor = null,
            accentColor = null
        )

        val response = client.post("/api/widgets") {
            contentType(ContentType.Application.Json)
            setBody(createDto)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // endregion

    // region Delete Widget

    @Test
    fun testDeleteWidget() = testApp { client ->
        val user = UserRepo.getById(1L)!!
        val widget = WidgetRepo.shareWidget(user, listOf(WidgetType.SERVER_STATS), null, null, null)

        val response = client.delete("/api/widgets/${widget.id.value}")
        assertTrue(response.status.isSuccess())

        val getResponse = client.get("/api/widgets/${widget.id.value}")
        assertEquals(HttpStatusCode.NotFound, getResponse.status)
    }

    @Test
    fun testDeleteWidget_NotFound() = testApp { client ->
        val response = client.delete("/api/widgets/nonexistent-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testDeleteWidget_NotOwnedByUser() = testApp(false) { client ->
        val user1 = UserTestUtils.createTestUser("user1", "password1")
        val widget = WidgetRepo.shareWidget(user1, listOf(WidgetType.SERVER_STATS), null, null, null)

        val (_, token2) = UserTestUtils.createUserWithSession("user2", "password2")

        val response = client.delete("/api/widgets/${widget.id.value}") {
            bearerAuth(token2)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun testDeleteWidget_NoPermission() = testApp(false) { client ->
        val (user, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "password",
            PermissionType.MANAGE_WIDGETS to false
        )

        val widget = WidgetRepo.shareWidget(user, listOf(WidgetType.SERVER_STATS), null, null, null)

        val response = client.delete("/api/widgets/${widget.id.value}") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun testDeleteWidget_Unauthorized() = testApp(false) { client ->

        val widget = WidgetRepo.shareWidget(
            UserRepo.getById(1L)!!,
            listOf(WidgetType.SERVER_STATS),
            null,
            null,
            null
        )

        val response = client.delete("/api/widgets/${widget.id.value}")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // endregion
}