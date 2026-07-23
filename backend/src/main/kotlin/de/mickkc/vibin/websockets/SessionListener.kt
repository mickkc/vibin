package de.mickkc.vibin.websockets

import de.mickkc.vibin.db.ListenType
import de.mickkc.vibin.dto.ControlSessionQueueItemInDto
import de.mickkc.vibin.repos.SessionRepeatState
import de.mickkc.vibin.routes.WsFrame

abstract class SessionListener(var session: Session) {

    /**
     * The app has pinged the server.
     */
    open suspend fun onPing(timestamp: Long) {}

    /**
     * A track has finished.
     * @param trackId The id of the finished track. May be null in the future for tracks not from this server.
     */
    open suspend fun onTrackFinished(trackId: Long?, timestamp: Long) {}

    /**
     * Playback was started / resumed.
     */
    open suspend fun onPlay(timestamp: Long) {}

    /**
     * playback was paused.
     */
    open suspend fun onPause(timestamp: Long) {}

    /**
     * Playback was stopped.
     */
    open suspend fun onStop(timestamp: Long) {}

    /**
     * The player is buffering (loading).
     */
    open suspend fun onBuffering(timestamp: Long) {}

    /**
     * A track was skipped and the next track is played.
     * @param skippedTrackId The id of the track that was skipped.
     */
    open suspend fun onTrackSkippedNext(skippedTrackId: Long, timestamp: Long) {}

    /**
     * A track was skipped and the previous track is played.
     * @param skippedTrackId The id of the track that was skipped.
     */
    open suspend fun onTrackSkippedPrev(skippedTrackId: Long, timestamp: Long) {}

    /**
     * A track was started (may be buffering while this event is received, bvut will play shortly).
     * @param trackId The track id that is / will be playing.
     */
    open suspend fun onTrackStarted(trackId: Long, timestamp: Long) {}

    /**
     * The user listened to an album, playlist or artist.
     * @param type The type; album, playlist or artist.
     * @param id The id of the album, playlist or artist.
     */
    open suspend fun onListen(type: ListenType, id: Long, timestamp: Long) {}

    /**
     * The repeat state of the player has changed.
     * @param state The repeat state; all, current track or off
     */
    open suspend fun onRepeatChanged(state: SessionRepeatState, timestamp: Long) {}

    /**
     * The shuffle state of the player has changed.
     * @param state The shuffle state; true for shuffling, false for linear.
     */
    open suspend fun onShuffleChanged(state: Boolean, timestamp: Long) {}

    /**
     * The volume of the player has changed.
     * @param volume The new volume from 0.0 (muted) to 1.0 (full).
     */
    open suspend fun onVolumeChanged(volume: Double, timestamp: Long) {}

    /**
     * The speed of the player has changed.
     * @param speed The new speed from 0.1x to 2x.
     */
    open suspend fun onSpeedChanged(speed: Double, timestamp: Long) {}

    /**
     * The queue has been modified.
     * @param queue The new queue.
     */
    open suspend fun onQueueChanged(queue: List<ControlSessionQueueItemInDto>, timestamp: Long) {}

    /**
     * A frame has been received via the websocket.
     * @param frame The raw frame.
     */
    open suspend fun onFrame(frame: WsFrame) {}

    /**
     * The connection to the client was closed.
     */
    open suspend fun onClose() {}

    /**
     * The listener was unregistered.
     * Also called before it gets closed.
     */
    open suspend fun onUnregister() {}

    suspend fun unregister() = session.unregisterListener(this)
}