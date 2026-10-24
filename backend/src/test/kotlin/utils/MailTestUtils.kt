package utils

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.mail.MailSender
import jakarta.mail.Session
import java.util.Properties

object MailTestUtils {

    fun withFakeMailConfig(block: () -> Any) {
        try {
            MailSender.fromAddress = "test@example.com"
            MailSender.session = Session.getInstance(Properties())
            EnvUtil.addOverride(EnvUtil.EXTERNAL_URL, "http://localhost:8080")
            EnvUtil.addOverride(EnvUtil.SMTP_MOCK, "true")
            block()
        }
        finally {
            MailSender.fromAddress = null
            MailSender.session = null
            EnvUtil.overrides.remove(EnvUtil.EXTERNAL_URL.name)
            EnvUtil.overrides.remove(EnvUtil.SMTP_MOCK.name)
        }
    }

}