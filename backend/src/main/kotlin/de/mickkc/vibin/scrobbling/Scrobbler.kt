package de.mickkc.vibin.scrobbling

import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.settings.Setting
import de.mickkc.vibin.settings.Settings
import io.ktor.client.*

abstract class Scrobbler(val client: HttpClient) {

    abstract val id: String

    /**
     * Human-readable name of the service this implementation scrobbles to.
     */
    abstract val name: String

    /**
     * A list of settings that apply to this scrobbler.
     */
    open val settings: List<Setting<*>> = emptyList()

    /**
     * Called when the currently playing track changes, or a track is resumed after being paused.
     *
     * @param userId The id of the user that's listening to a track
     * @param track The track the user ist listening to
     */
    open suspend fun setNowPlaying(userId: Long, track: TrackEntity) {}

    /**
     * Called after a user listened to a track and playback stopped or switched to another track.
     *
     * @param userId The id of the user that listened to a track
     * @param track The track the user listened to
     * @param duration How long (in seconds) the user listened to that track
     * @param timestamp The UNIX timestamp (Seconds, UTC) the listening STARTED at
     */
    abstract suspend fun scrobble(userId: Long, track: TrackEntity, duration: Long, timestamp: Long)

    /**
     * Called to check if this scrobbler can be used for a specific user.
     * Useful for checking if all [settings] are configured correctly for that user.
     *
     * By default, it checks of all [settings] are valid using their [de.mickkc.vibin.settings.Setting.validationRegex].
     *
     * @return true if the scrobbler can be used for that user, false otherwise.
     */
    open fun isAvailable(userId: Long): Boolean = Settings.allValid(settings, userId)
}