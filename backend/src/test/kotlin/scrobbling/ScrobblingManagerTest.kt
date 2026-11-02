package scrobbling

import de.mickkc.vibin.scrobbling.ScrobblingManager
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScrobblingManagerTest {

    @BeforeTest
    fun clearRequests() {
        ScrobblingManager.clearSetCurrentTrackRequests()
    }

    @Test
    fun testSetCurrentRequest_firstRequest() {
        assertTrue(ScrobblingManager.checkAndRecordSetCurrentRequest(userId = 1, trackId = 1, now = 1000))
    }

    @Test
    fun testSetCurrentRequest_sameTrackWithinWindow() {
        assertTrue(ScrobblingManager.checkAndRecordSetCurrentRequest(userId = 1, trackId = 1, now = 1000))
        assertFalse(ScrobblingManager.checkAndRecordSetCurrentRequest(userId = 1, trackId = 1, now = 1000 + ScrobblingManager.SET_CURRENT_DEBOUNCE_SECONDS - 1))
    }

    @Test
    fun testSetCurrentRequest_differentTrackWithinWindow() {
        assertTrue(ScrobblingManager.checkAndRecordSetCurrentRequest(userId = 1, trackId = 1, now = 1000))
        assertTrue(ScrobblingManager.checkAndRecordSetCurrentRequest(userId = 1, trackId = 2, now = 1001))
    }

    @Test
    fun testSetCurrentRequest_sameTrackAfterWindow() {
        assertTrue(ScrobblingManager.checkAndRecordSetCurrentRequest(userId = 1, trackId = 1, now = 1000))
        assertTrue(ScrobblingManager.checkAndRecordSetCurrentRequest(userId = 1, trackId = 1, now = 1000 + ScrobblingManager.SET_CURRENT_DEBOUNCE_SECONDS))
    }

    @Test
    fun testSetCurrentRequest_differentUser() {
        assertTrue(ScrobblingManager.checkAndRecordSetCurrentRequest(userId = 1, trackId = 1, now = 1000))
        assertTrue(ScrobblingManager.checkAndRecordSetCurrentRequest(userId = 2, trackId = 1, now = 1001))
    }
}
