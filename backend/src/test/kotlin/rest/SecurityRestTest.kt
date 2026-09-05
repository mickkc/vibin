package rest

import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.db.SessionEntity
import de.mickkc.vibin.db.SessionState
import de.mickkc.vibin.db.SessionTable
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.db.security.LoginNotificationEntity
import de.mickkc.vibin.db.security.LoginNotificationTable
import de.mickkc.vibin.db.security.TrustedDeviceIdTable
import de.mickkc.vibin.dto.LoginResultDto
import de.mickkc.vibin.repos.SecurityRepo
import de.mickkc.vibin.repos.SessionRepo
import de.mickkc.vibin.repos.SettingsRepo
import de.mickkc.vibin.security.LoginNotificationManager
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.LoginNotificationTokenValidity
import de.mickkc.vibin.settings.user.SendMailOnNewLogin
import de.mickkc.vibin.utils.DateTimeUtils
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import utils.testApp
import kotlin.test.*

class SecurityRestTest {

    private fun adminUser(): UserEntity = transaction { UserEntity.findById(1)!! }

    /**
     * Creates a session for user and a matching login notification for it, as if a new login email had just been sent
     * for that session.
     */
    private fun createNotification(
        user: UserEntity,
        sessionToken: String,
        deviceId: String,
        deviceName: String,
        notificationToken: String
    ): LoginNotificationEntity {
        val session = SessionRepo.addSession(user, sessionToken, deviceId, deviceName, SessionState.Valid)
        val hashedDeviceId = CryptoUtil.hashPassword(deviceId, user.salt)
        return SecurityRepo.sentLoginNotificationEmail(hashedDeviceId, deviceName, notificationToken, session, user)
    }

    private fun trustDevice(user: UserEntity, deviceId: String, deviceName: String) {
        val hashed = CryptoUtil.hashPassword(deviceId, user.salt)
        SecurityRepo.trustDeviceId(hashed, deviceName, user)
    }

    private fun sessionExists(token: String): Boolean = transaction {
        SessionEntity.find { SessionTable.token eq token }.any()
    }

    private fun notificationCount(): Long = transaction {
        LoginNotificationTable.selectAll().count()
    }

    private fun trustedDeviceCount(user: UserEntity): Long = transaction {
        TrustedDeviceIdTable.selectAll().where { TrustedDeviceIdTable.userId eq user.id.value }.count()
    }

    private fun expireNotification(notification: LoginNotificationEntity) = transaction {
        notification.createdAt = DateTimeUtils.now() - (Settings.get(LoginNotificationTokenValidity) + 60)
    }

    // region Trust device

    @Test
    fun testTrustDevice_Get_ValidToken() = testApp(false) { client ->
        val user = adminUser()
        val notification = createNotification(user, "sus-token-1", "attackerDevice", "Attacker's Phone", "trust-tk-1")

        val response = client.get("/api/mail/trust-device") {
            parameter("tk", "trust-tk-1")
        }
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "Trust device")

        // GET must not consume the token
        assertNotNull(SecurityRepo.getLoginNotificationFromToken("trust-tk-1"))
    }

    @Test
    fun testTrustDevice_Get_InvalidToken() = testApp(false) { client ->
        val response = client.get("/api/mail/trust-device") {
            parameter("tk", "does-not-exist")
        }
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "Invalid link")
    }

    @Test
    fun testTrustDevice_Get_MissingToken() = testApp(false) { client ->
        val response = client.get("/api/mail/trust-device")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(response.bodyAsText(), "\"parameterName\":\"tk\"")
    }

    @Test
    fun testTrustDevice_Post_ValidToken_MarksDeviceTrusted() = testApp(false) { client ->
        val user = adminUser()
        createNotification(user, "sus-token-2", "attackerDevice", "Attacker's Phone", "trust-tk-2")

        assertFalse(SecurityRepo.isDeviceIdTrusted("attackerDevice", "Attacker's Phone", user))

        val response = client.submitForm(
            url = "/api/mail/trust-device",
            formParameters = Parameters.build { append("token", "trust-tk-2") }
        )
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "All done!")

        assertTrue(SecurityRepo.isDeviceIdTrusted("attackerDevice", "Attacker's Phone", user))
        assertEquals(1, trustedDeviceCount(user))
    }

    @Test
    fun testTrustDevice_Post_TokenNotReusable() = testApp(false) { client ->
        val user = adminUser()
        createNotification(user, "sus-token-3", "attackerDevice", "Attacker's Phone", "trust-tk-3")

        client.submitForm(
            url = "/api/mail/trust-device",
            formParameters = Parameters.build { append("token", "trust-tk-3") }
        )

        val second = client.submitForm(
            url = "/api/mail/trust-device",
            formParameters = Parameters.build { append("token", "trust-tk-3") }
        )
        assertTrue(second.status.isSuccess())
        assertContains(second.bodyAsText(), "Invalid link")
    }

    @Test
    fun testTrustDevice_Post_OnlyResolvesNotificationsWithMatchingDeviceName() = testApp(false) { client ->
        val user = adminUser()
        // Same deviceId, but two different reported device names -> two independent notifications.
        createNotification(user, "sus-token-4a", "sharedDevice", "Name A", "trust-tk-4a")
        createNotification(user, "sus-token-4b", "sharedDevice", "Name B", "trust-tk-4b")

        client.submitForm(
            url = "/api/mail/trust-device",
            formParameters = Parameters.build { append("token", "trust-tk-4a") }
        )

        // Only the "Name A" notification/session pair should be considered resolved.
        assertTrue(SecurityRepo.isDeviceIdTrusted("sharedDevice", "Name A", user))
        assertFalse(SecurityRepo.isDeviceIdTrusted("sharedDevice", "Name B", user))

        // The other notification (different device name) must still be usable.
        assertNotNull(SecurityRepo.getLoginNotificationFromToken("trust-tk-4b"))
    }

    @Test
    fun testTrustDevice_Post_MissingToken() = testApp(false) { client ->
        val response = client.submitForm(
            url = "/api/mail/trust-device",
            formParameters = Parameters.build {}
        )
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "Error!")
        assertContains(response.bodyAsText(), "A token is required")
    }

    @Test
    fun testTrustDevice_Post_ExpiredToken() = testApp(false) { client ->
        val user = adminUser()
        val notification = createNotification(user, "sus-token-5", "attackerDevice", "Attacker's Phone", "trust-tk-5")
        expireNotification(notification)

        val response = client.submitForm(
            url = "/api/mail/trust-device",
            formParameters = Parameters.build { append("token", "trust-tk-5") }
        )
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "Invalid link")

        assertFalse(SecurityRepo.isDeviceIdTrusted("attackerDevice", "Attacker's Phone", user))
        assertNull(SecurityRepo.getLoginNotificationFromToken("trust-tk-5"))
    }

    // endregion

    // region Reset password / invalidate sessions

    @Test
    fun testResetPassword_Get_ValidToken() = testApp(false) { client ->
        val user = adminUser()
        createNotification(user, "sus-token-6", "attackerDevice", "Attacker's Phone", "reset-tk-6")

        val response = client.get("/api/mail/reset-password") {
            parameter("tk", "reset-tk-6")
        }
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "Reset password")
    }

    @Test
    fun testResetPassword_Get_InvalidToken() = testApp(false) { client ->
        val response = client.get("/api/mail/reset-password") {
            parameter("tk", "does-not-exist")
        }
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "Invalid link")
    }

    @Test
    fun testResetPassword_Post_ValidToken_ChangesPassword() = testApp(false) { client ->
        val user = adminUser()
        createNotification(user, "sus-token-7", "attackerDevice", "Attacker's Phone", "reset-tk-7")

        val response = client.submitForm(
            url = "/api/mail/reset-password",
            formParameters = Parameters.build {
                append("token", "reset-tk-7")
                append("newPassword", "newSecurePassword123")
                append("confirmNewPassword", "newSecurePassword123")
            }
        )
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "All done!")

        // Old password no longer works, new one does.
        val oldLogin = client.post("/api/auth/login") {
            parameter("username", "Admin")
            parameter("password", "admin")
            parameter("deviceId", "someOtherDevice")
            parameter("deviceName", "Some Other Device")
        }
        assertEquals(HttpStatusCode.Unauthorized, oldLogin.status)

        val newLogin = client.post("/api/auth/login") {
            parameter("username", "Admin")
            parameter("password", "newSecurePassword123")
            parameter("deviceId", "someOtherDevice")
            parameter("deviceName", "Some Other Device")
        }
        assertTrue(newLogin.status.isSuccess())
        assertTrue(newLogin.body<LoginResultDto>().success)
    }

    @Test
    fun testResetPassword_Post_InvalidatesSession() = testApp(false) { client ->
        val user = adminUser()
        createNotification(user, "sus-token-8", "attackerDevice", "Attacker's Phone", "reset-tk-8")
        assertTrue(sessionExists("sus-token-8"))

        client.submitForm(
            url = "/api/mail/reset-password",
            formParameters = Parameters.build {
                append("token", "reset-tk-8")
                append("newPassword", "newSecurePassword123")
                append("confirmNewPassword", "newSecurePassword123")
            }
        )

        assertFalse(sessionExists("sus-token-8"))
    }

    @Test
    fun testResetPassword_Post_PreservesTrustedSession() = testApp(false) { client ->
        val user = adminUser()

        // A previously trusted device/session, must survive the password reset.
        trustDevice(user, "trustedDevice", "My Laptop")
        SessionRepo.addSession(user, "trusted-session-token", "trustedDevice", "My Laptop", SessionState.Valid)

        createNotification(user, "sus-token-9", "attackerDevice", "Attacker's Phone", "reset-tk-9")

        client.submitForm(
            url = "/api/mail/reset-password",
            formParameters = Parameters.build {
                append("token", "reset-tk-9")
                append("newPassword", "newSecurePassword123")
                append("confirmNewPassword", "newSecurePassword123")
            }
        )

        assertTrue(sessionExists("trusted-session-token"))
        assertFalse(sessionExists("sus-token-9"))
    }

    @Test
    fun testResetPassword_Post_InvalidatesSameDeviceIdUnderDifferentName() = testApp(false) { client ->
        val user = adminUser()

        trustDevice(user, "sharedDevice", "My Laptop")

        createNotification(user, "sus-token-10", "sharedDevice", "Unknown Browser", "reset-tk-10")
        assertFalse(SecurityRepo.isDeviceIdTrusted("sharedDevice", "Unknown Browser", user))

        client.submitForm(
            url = "/api/mail/reset-password",
            formParameters = Parameters.build {
                append("token", "reset-tk-10")
                append("newPassword", "newSecurePassword123")
                append("confirmNewPassword", "newSecurePassword123")
            }
        )

        // Session must be gone, even though its deviceId is trusted under a different device name.
        assertFalse(sessionExists("sus-token-10"))
    }

    @Test
    fun testResetPassword_Post_InvalidatesAllUntrustedSessionsRegardlessOfDevice() = testApp(false) { client ->
        val user = adminUser()

        // Two independent untrusted sessions unrelated to the notification being resolved.
        SessionRepo.addSession(user, "other-untrusted-1", "otherDevice1", "Other Device 1", SessionState.Valid)
        SessionRepo.addSession(user, "other-untrusted-2", "otherDevice2", "Other Device 2", SessionState.Valid)

        createNotification(user, "sus-token-11", "attackerDevice", "Attacker's Phone", "reset-tk-11")

        client.submitForm(
            url = "/api/mail/reset-password",
            formParameters = Parameters.build {
                append("token", "reset-tk-11")
                append("newPassword", "newSecurePassword123")
                append("confirmNewPassword", "newSecurePassword123")
            }
        )

        assertFalse(sessionExists("other-untrusted-1"))
        assertFalse(sessionExists("other-untrusted-2"))
        assertFalse(sessionExists("sus-token-11"))
    }

    @Test
    fun testResetPassword_Post_TokenNotReusable() = testApp(false) { client ->
        val user = adminUser()
        createNotification(user, "sus-token-12", "attackerDevice", "Attacker's Phone", "reset-tk-12")

        client.submitForm(
            url = "/api/mail/reset-password",
            formParameters = Parameters.build {
                append("token", "reset-tk-12")
                append("newPassword", "newSecurePassword123")
                append("confirmNewPassword", "newSecurePassword123")
            }
        )

        val second = client.submitForm(
            url = "/api/mail/reset-password",
            formParameters = Parameters.build {
                append("token", "reset-tk-12")
                append("newPassword", "anotherPassword123")
                append("confirmNewPassword", "anotherPassword123")
            }
        )
        assertTrue(second.status.isSuccess())
        assertContains(second.bodyAsText(), "Invalid link")
    }

    @Test
    fun testResetPassword_Post_OnlyDeletesNotificationsWithMatchingDeviceName() = testApp(false) { client ->
        val user = adminUser()

        // "Name B" is trusted, so its session (and notification) survives the invalidation triggered by resolving the
        // "Name A" notification below.
        trustDevice(user, "sharedDevice", "Name B")

        createNotification(user, "sus-token-13a", "sharedDevice", "Name A", "reset-tk-13a")
        createNotification(user, "sus-token-13b", "sharedDevice", "Name B", "reset-tk-13b")

        client.submitForm(
            url = "/api/mail/reset-password",
            formParameters = Parameters.build {
                append("token", "reset-tk-13a")
                append("newPassword", "newSecurePassword123")
                append("confirmNewPassword", "newSecurePassword123")
            }
        )

        assertTrue(sessionExists("sus-token-13b"))
        assertNotNull(SecurityRepo.getLoginNotificationFromToken("reset-tk-13b"))
    }

    @Test
    fun testResetPassword_Post_PasswordMismatch() = testApp(false) { client ->
        val user = adminUser()
        createNotification(user, "sus-token-14", "attackerDevice", "Attacker's Phone", "reset-tk-14")

        val response = client.submitForm(
            url = "/api/mail/reset-password",
            formParameters = Parameters.build {
                append("token", "reset-tk-14")
                append("newPassword", "newSecurePassword123")
                append("confirmNewPassword", "differentPassword123")
            }
        )
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "does not match")

        // Token must remain usable
        assertNotNull(SecurityRepo.getLoginNotificationFromToken("reset-tk-14"))
    }

    @Test
    fun testResetPassword_Post_PasswordTooShort() = testApp(false) { client ->
        val user = adminUser()
        createNotification(user, "sus-token-15", "attackerDevice", "Attacker's Phone", "reset-tk-15")

        val response = client.submitForm(
            url = "/api/mail/reset-password",
            formParameters = Parameters.build {
                append("token", "reset-tk-15")
                append("newPassword", "short")
                append("confirmNewPassword", "short")
            }
        )
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "too short")

        // Token must remain usable
        assertNotNull(SecurityRepo.getLoginNotificationFromToken("reset-tk-15"))
    }

    @Test
    fun testResetPassword_Post_MissingToken() = testApp(false) { client ->
        val response = client.submitForm(
            url = "/api/mail/reset-password",
            formParameters = Parameters.build {
                append("newPassword", "newSecurePassword123")
                append("confirmNewPassword", "newSecurePassword123")
            }
        )
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "A token is required")
    }

    @Test
    fun testResetPassword_Post_MissingPassword() = testApp(false) { client ->
        val user = adminUser()
        createNotification(user, "sus-token-16", "attackerDevice", "Attacker's Phone", "reset-tk-16")

        val response = client.submitForm(
            url = "/api/mail/reset-password",
            formParameters = Parameters.build {
                append("token", "reset-tk-16")
                append("confirmNewPassword", "newSecurePassword123")
            }
        )
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "No new password was provided")
    }

    @Test
    fun testResetPassword_Post_ExpiredToken() = testApp(false) { client ->
        val user = adminUser()
        val notification = createNotification(user, "sus-token-17", "attackerDevice", "Attacker's Phone", "reset-tk-17")
        expireNotification(notification)

        val response = client.submitForm(
            url = "/api/mail/reset-password",
            formParameters = Parameters.build {
                append("token", "reset-tk-17")
                append("newPassword", "newSecurePassword123")
                append("confirmNewPassword", "newSecurePassword123")
            }
        )
        assertTrue(response.status.isSuccess())
        assertContains(response.bodyAsText(), "Invalid link")

        assertTrue(sessionExists("sus-token-17"))
    }

    // endregion

    // region isDeviceIdTrusted semantics

    @Test
    fun testIsDeviceIdTrusted_RequiresExactDeviceNameMatch() = testApp(false) { client ->
        val user = adminUser()
        trustDevice(user, "someDevice", "Name A")

        assertTrue(SecurityRepo.isDeviceIdTrusted("someDevice", "Name A", user))
        assertFalse(SecurityRepo.isDeviceIdTrusted("someDevice", "Name B", user))
        assertFalse(SecurityRepo.isDeviceIdTrusted("otherDevice", "Name A", user))
    }

    // endregion

    // region sendIfNotTrusted gating

    @Test
    fun testSendIfNotTrusted_SkipsWhenSettingDisabled() = testApp(false) { client ->
        val user = adminUser()
        val session = SessionRepo.addSession(user, "gating-session-1", "newDevice", "New Device", SessionState.Valid)

        assertEquals(
            LoginNotificationManager.LoginNotificationResult.Disabled,
            LoginNotificationManager.sendIfNotTrusted(CryptoUtil.hashPassword("newDevice", user.salt), "New Device", "127.0.0.1", session, user)
        )

        assertEquals(0, notificationCount())
    }

    @Test
    fun testSendIfNotTrusted_SkipsWhenUserHasNoEmail() = testApp(false) { client ->
        val user = adminUser()

        transaction { user.email = null }
        SettingsRepo.updateUserSetting(SendMailOnNewLogin, user.id.value, "true")

        val session = SessionRepo.addSession(user, "gating-session-2", "newDevice", "New Device", SessionState.Valid)

        assertEquals(
            LoginNotificationManager.LoginNotificationResult.InvalidEmail,
            LoginNotificationManager.sendIfNotTrusted(CryptoUtil.hashPassword("newDevice", user.salt), "New Device", "127.0.0.1", session, user)
        )

        assertEquals(0, notificationCount())
    }

    @Test
    fun testSendIfNotTrusted_SkipsWhenDeviceAlreadyTrusted() = testApp(false) { client ->
        val user = adminUser()

        transaction { user.email = "admin@example.com" }
        SettingsRepo.updateUserSetting(SendMailOnNewLogin, user.id.value, "true")

        trustDevice(user, "trustedDevice", "My Laptop")
        val session = SessionRepo.addSession(user, "gating-session-3", "trustedDevice", "My Laptop", SessionState.Valid)

        assertEquals(
            LoginNotificationManager.LoginNotificationResult.AlreadyTrusted,
            LoginNotificationManager.sendIfNotTrusted(CryptoUtil.hashPassword("trustedDevice", user.salt), "My Laptop", "127.0.0.1", session, user)
        )

        assertEquals(0, notificationCount())
    }

    @Test
    fun testSendIfNotTrusted_SkipsWhenMailSenderNotConfigured() = testApp(false) { client ->
        val user = adminUser()

        transaction { user.email = "admin@example.com" }
        SettingsRepo.updateUserSetting(SendMailOnNewLogin, user.id.value, "true")

        val session = SessionRepo.addSession(user, "gating-session-4", "newDevice", "New Device", SessionState.Valid)

        // MailSender is not configured with SMTP credentials in the test environment.
        assertEquals(
            LoginNotificationManager.LoginNotificationResult.NotConfigured,
            LoginNotificationManager.sendIfNotTrusted(CryptoUtil.hashPassword("newDevice", user.salt), "New Device", "127.0.0.1", session, user)
        )

        assertEquals(0, notificationCount())
    }

    // endregion
}
