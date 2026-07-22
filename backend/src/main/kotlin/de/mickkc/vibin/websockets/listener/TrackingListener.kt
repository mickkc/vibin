package de.mickkc.vibin.websockets.listener

import de.mickkc.vibin.db.ListenType
import de.mickkc.vibin.repos.ListenRepo
import de.mickkc.vibin.repos.ListenTrackingRepo
import de.mickkc.vibin.websockets.Session
import de.mickkc.vibin.websockets.SessionListener

class TrackingListener(session: Session) : SessionListener(session) {

    override suspend fun onTrackFinished(trackId: Long?, timestamp: Long) {
        if (trackId != null) {
            ListenTrackingRepo.trackFinished(session.sessionId, trackId, timestamp)
        }
    }

    override suspend fun onPlay(timestamp: Long) {
        ListenTrackingRepo.trackResumed(session.sessionId, timestamp)
    }

    override suspend fun onPause(timestamp: Long) {
        ListenTrackingRepo.trackPaused(session.sessionId, timestamp)
    }

    override suspend fun onStop(timestamp: Long) {
        ListenTrackingRepo.stopped(session.sessionId, timestamp)
    }

    override suspend fun onTrackSkippedNext(skippedTrackId: Long, timestamp: Long) {
        ListenTrackingRepo.trackSkipped(session.sessionId, timestamp)
    }

    override suspend fun onTrackSkippedPrev(skippedTrackId: Long, timestamp: Long) {
        ListenTrackingRepo.trackSkipped(session.sessionId, timestamp)
    }

    override suspend fun onTrackStarted(trackId: Long, timestamp: Long) {
        ListenTrackingRepo.trackStarted(session.sessionId, session.userId, trackId, timestamp)
    }

    override suspend fun onListen(type: ListenType, id: Long, timestamp: Long) {
        ListenRepo.listenedTo(
            userId = session.userId,
            entityId = id,
            type = type
        )
    }

    override suspend fun onClose() {
        ListenTrackingRepo.stopped(session.sessionId, ListenTrackingRepo.getNowWithOffset(session.sessionId))
        ListenTrackingRepo.deleteSessionData(session.sessionId)
    }
}