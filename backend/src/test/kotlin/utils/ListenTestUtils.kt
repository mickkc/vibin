package utils

import de.mickkc.vibin.db.TrackListenDurationEntity
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.utils.DateTimeUtils
import org.jetbrains.exposed.sql.transactions.transaction

object ListenTestUtils {
    fun listenedToTrack(user: UserEntity, track: TrackEntity?, duration: Long, at: Long = DateTimeUtils.now()): TrackListenDurationEntity = transaction {
        TrackListenDurationEntity.new {
            this.listenedDurationSeconds = duration
            this.createdAt = at
            this.user = user
            this.track = track
        }
    }
}