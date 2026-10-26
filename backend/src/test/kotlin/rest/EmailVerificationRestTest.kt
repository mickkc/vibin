package rest

import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.db.security.EmailVerificationTokenEntity
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.SecurityRepo
import de.mickkc.vibin.security.MailVerificationManager
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.VerificationMailExpirationDuration
import de.mickkc.vibin.utils.DateTimeUtils
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.MailTestUtils
import utils.UserTestUtils
import utils.UserTestUtils.getAdminUser
import utils.UserTestUtils.setUserEmail
import utils.UserTestUtils.setUserEmailVerified
import utils.testApp
import kotlin.test.*

class EmailVerificationRestTest {

    private fun expireVerificationToken(token: String) = transaction {
        EmailVerificationTokenEntity.findById(CryptoUtil.hashToken(token))?.let {
            it.createdAt = DateTimeUtils.now() - (Settings.get(VerificationMailExpirationDuration) + 60)
        }
    }

    private fun verificationTokenCount(): Long = transaction {
        EmailVerificationTokenEntity.count()
    }

    private fun verificationTokenExists(token: String): Boolean = transaction {
        EmailVerificationTokenEntity.findById(CryptoUtil.hashToken(token)) != null
    }

    private fun userEmailVerified(userId: Long): Boolean = transaction {
        UserEntity.findById(userId)?.emailVerified ?: false
    }

    @BeforeTest
    fun clearRateLimiter() {
        MailVerificationManager.rateLimiter.resetAll()
    }

    // region SecurityRepo

    @Test
    fun testSentVerificationEmail_CreatesToken() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")

        SecurityRepo.sentVerificationEmail(user, "test@example.com", "test-token")

        transaction {
            val entity = EmailVerificationTokenEntity.findById(CryptoUtil.hashToken("test-token"))
            assertNotNull(entity)
            assertEquals("test@example.com", entity.emailAddress)
            assertEquals(user.id.value, entity.user.id.value)
            assertEquals(CryptoUtil.hashToken("test-token"), entity.id.value)
        }
    }

    @Test
    fun testGetMailAddressFromVerificationToken_ValidToken() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")
        SecurityRepo.sentVerificationEmail(user, "test@example.com", "test-token")

        val email = SecurityRepo.getMailAddressFromVerificationToken("test-token")
        assertEquals("test@example.com", email)

        // Reading the email should not consume the token
        assertTrue(verificationTokenExists("test-token"))
    }

    @Test
    fun testGetMailAddressFromVerificationToken_InvalidToken() = testApp(false) { client ->
        val email = SecurityRepo.getMailAddressFromVerificationToken("does-not-exist")
        assertNull(email)
    }

    @Test
    fun testGetMailAddressFromVerificationToken_ExpiredToken() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")
        SecurityRepo.sentVerificationEmail(user, "test@example.com", "test-token")
        expireVerificationToken("test-token")

        val email = SecurityRepo.getMailAddressFromVerificationToken("test-token")
        assertNull(email)
        assertFalse(verificationTokenExists("test-token"))
    }

    @Test
    fun testVerifyEmailAddress_ValidToken() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")
        assertFalse(userEmailVerified(user.id.value))

        SecurityRepo.sentVerificationEmail(user, "test@example.com", "test-token")

        assertTrue(SecurityRepo.verifyEmailAddress("test-token"))
        assertTrue(userEmailVerified(user.id.value))
        assertFalse(verificationTokenExists("test-token"))
    }

    @Test
    fun testVerifyEmailAddress_InvalidToken() = testApp(false) { client ->
        assertFalse(SecurityRepo.verifyEmailAddress("does-not-exist"))
    }

    @Test
    fun testVerifyEmailAddress_EmailMismatch() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")
        // Token was created for a different email address than the user's current one
        SecurityRepo.sentVerificationEmail(user, "old@example.com", "test-token")

        assertFalse(SecurityRepo.verifyEmailAddress("test-token"))
        assertFalse(userEmailVerified(user.id.value))
        assertFalse(verificationTokenExists("test-token"))
    }

    @Test
    fun testVerifyEmailAddress_ExpiredToken() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")
        SecurityRepo.sentVerificationEmail(user, "test@example.com", "test-token")
        expireVerificationToken("test-token")

        assertFalse(SecurityRepo.verifyEmailAddress("test-token"))
        assertFalse(userEmailVerified(user.id.value))
        assertFalse(verificationTokenExists("test-token"))
    }

    @Test
    fun testVerifyEmailAddress_TokenNotReusable() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")
        SecurityRepo.sentVerificationEmail(user, "test@example.com", "test-token")

        assertTrue(SecurityRepo.verifyEmailAddress("test-token"))
        // Second attempt with the same token must fail
        assertFalse(SecurityRepo.verifyEmailAddress("test-token"))
    }

    @Test
    fun testDeleteVerificationEmailsForUser() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")
        SecurityRepo.sentVerificationEmail(user, "test@example.com", "test-token-a")
        SecurityRepo.sentVerificationEmail(user, "test@example.com", "test-token-b")

        assertEquals(2, verificationTokenCount())

        val deleted = SecurityRepo.deleteVerificationEmailsForUser(user.id.value)
        assertEquals(2, deleted)
        assertEquals(0, verificationTokenCount())
    }

    // endregion

    // region MailVerificationManager

    @Test
    fun testSendVerificationMail_AlreadyVerified() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")
        setUserEmailVerified(user, true)

        val result = MailVerificationManager.sendVerificationMail(user)
        assertEquals(MailVerificationManager.MailVerificationResult.AlreadyVerified, result)
        assertEquals(0, verificationTokenCount())
    }

    @Test
    fun testSendVerificationMail_InvalidEmail() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, null)

        val result = MailVerificationManager.sendVerificationMail(user)
        assertEquals(MailVerificationManager.MailVerificationResult.InvalidEmail, result)
        assertEquals(0, verificationTokenCount())
    }

    @Test
    fun testSendVerificationMail_NotConfigured() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")

        val result = MailVerificationManager.sendVerificationMail(user)
        assertEquals(MailVerificationManager.MailVerificationResult.NotConfigured, result)
        assertEquals(0, verificationTokenCount())
    }

    @Test
    fun testSendVerificationMail_RateLimited() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")

        repeat(2) {
            val result = MailVerificationManager.sendVerificationMail(user)
            assertEquals(MailVerificationManager.MailVerificationResult.NotConfigured, result)
        }

        val result = MailVerificationManager.sendVerificationMail(user)
        assertEquals(MailVerificationManager.MailVerificationResult.RateLimited, result)
    }

    @Test
    fun testSendVerificationMail_Sent() {
        MailTestUtils.withFakeMailConfig {
            testApp(false) { client ->
                val user = getAdminUser()
                setUserEmail(user, "test@example.com")

                val result = MailVerificationManager.sendVerificationMail(user)
                assertEquals(MailVerificationManager.MailVerificationResult.Sent, result)
                assertEquals(1, verificationTokenCount())

                transaction {
                    val entity = EmailVerificationTokenEntity.all().first()
                    assertEquals("test@example.com", entity.emailAddress)
                    assertTrue(entity.id.value.matches(Regex("^[0-9a-f]{64}$")))
                }
            }
        }
    }

    // endregion

    // region GET /api/mail/verify

    @Test
    fun testVerifyMail_Get_ValidToken() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")
        SecurityRepo.sentVerificationEmail(user, "test@example.com", "test-token")

        val response = client.get("/api/mail/verify") {
            parameter("tk", "test-token")
        }
        assertTrue(response.status.isSuccess())
        val body = response.bodyAsText()
        assertContains(body, "Verify your email address")
        assertContains(body, "test@example.com")

        // GET must not consume the token
        assertTrue(verificationTokenExists("test-token"))
    }

    @Test
    fun testVerifyMail_Get_InvalidToken() = testApp(false) { client ->
        val response = client.get("/api/mail/verify") {
            parameter("tk", "does-not-exist")
        }
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "Invalid link")
    }

    @Test
    fun testVerifyMail_Get_MissingToken() = testApp(false) { client ->
        val response = client.get("/api/mail/verify")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(response.bodyAsText(), "\"parameterName\":\"tk\"")
    }

    @Test
    fun testVerifyMail_Get_ExpiredToken() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")
        SecurityRepo.sentVerificationEmail(user, "test@example.com", "test-token")
        expireVerificationToken("test-token")

        val response = client.get("/api/mail/verify") {
            parameter("tk", "test-token")
        }
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "Invalid link")
        assertFalse(verificationTokenExists("test-token"))
    }

    // endregion

    // region POST /api/mail/verify

    @Test
    fun testVerifyMail_Post_ValidToken() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")
        SecurityRepo.sentVerificationEmail(user, "test@example.com", "test-token")

        val response = client.submitForm(
            url = "/api/mail/verify",
            formParameters = Parameters.build { append("token", "test-token") }
        )
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "All done!")

        assertTrue(userEmailVerified(user.id.value))
        assertFalse(verificationTokenExists("test-token"))
    }

    @Test
    fun testVerifyMail_Post_InvalidToken() = testApp(false) { client ->
        val response = client.submitForm(
            url = "/api/mail/verify",
            formParameters = Parameters.build { append("token", "does-not-exist") }
        )
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "Invalid link")
    }

    @Test
    fun testVerifyMail_Post_MissingToken() = testApp(false) { client ->
        val response = client.submitForm(
            url = "/api/mail/verify",
            formParameters = Parameters.build {}
        )
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "Error!")
        assertContains(response.bodyAsText(), "A token is required")
    }

    @Test
    fun testVerifyMail_Post_TokenNotReusable() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")
        SecurityRepo.sentVerificationEmail(user, "test@example.com", "test-token")

        val first = client.submitForm(
            url = "/api/mail/verify",
            formParameters = Parameters.build { append("token", "test-token") }
        )
        assertTrue(first.status.isSuccess())
        assertContains(first.bodyAsText(), "All done!")

        val second = client.submitForm(
            url = "/api/mail/verify",
            formParameters = Parameters.build { append("token", "test-token") }
        )
        assertTrue(second.status.isSuccess())
        assertContains(second.bodyAsText(), "Invalid link")
    }

    @Test
    fun testVerifyMail_Post_ExpiredToken() = testApp(false) { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")
        SecurityRepo.sentVerificationEmail(user, "test@example.com", "test-token")
        expireVerificationToken("test-token")

        val response = client.submitForm(
            url = "/api/mail/verify",
            formParameters = Parameters.build { append("token", "test-token") }
        )
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "Invalid link")

        assertFalse(userEmailVerified(user.id.value))
    }

    // endregion

    // region POST /api/auth/mail/verify

    @Test
    fun testSendMail_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions("noperms", "password",
            PermissionType.MANAGE_OWN_USER to false)

        val response = client.post("/api/auth/mail/verify") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun testSendMail_AlreadyVerified() = testApp { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")
        setUserEmailVerified(user, true)

        val response = client.post("/api/auth/mail/verify")
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun testSendMail_InvalidEmail() = testApp { client ->
        val user = getAdminUser()
        setUserEmail(user, null)

        val response = client.post("/api/auth/mail/verify")
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun testSendMail_NotConfigured() = testApp { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")

        val response = client.post("/api/auth/mail/verify")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }

    @Test
    fun testSendMail_RateLimited() = testApp { client ->
        val user = getAdminUser()
        setUserEmail(user, "test@example.com")

        repeat(2) {
            val response = client.post("/api/auth/mail/verify")
            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        }

        val response = client.post("/api/auth/mail/verify")
        assertEquals(HttpStatusCode.TooManyRequests, response.status)
    }

    @Test
    fun testSendMail_Success() {
        MailTestUtils.withFakeMailConfig {
            testApp { client ->
                val user = getAdminUser()
                setUserEmail(user, "test@example.com")

                val response = client.post("/api/auth/mail/verify")
                assertTrue(response.status.isSuccess())

                assertEquals(1, verificationTokenCount())
                transaction {
                    val entity = EmailVerificationTokenEntity.all().first()
                    assertEquals("test@example.com", entity.emailAddress)
                    assertTrue(entity.id.value.matches(Regex("^[0-9a-f]{64}$")))
                }
            }
        }
    }

    // endregion
}
