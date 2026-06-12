package de.mickkc.vibin.streaming

import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.dto.Streamable
import de.mickkc.vibin.utils.AccessCryptoUtil
import org.jetbrains.exposed.sql.transactions.transaction

object StreamBuilder {

    const val STREAM_VALIDITY_SECONDS = 24L * 60L * 60L // 24 hours

    fun build(track: TrackEntity): Streamable = transaction {

        val cover = track.cover ?: track.album.cover
        var coverSignature: String? = null
        var coverExpiration: Long? = null

        if (cover != null) {
            val coverCryptoResult = AccessCryptoUtil.generateSignatureAndTimestamp(cover.sourceChecksum, STREAM_VALIDITY_SECONDS)
            coverSignature = coverCryptoResult.first
            coverExpiration = coverCryptoResult.second
        }

        val (trackSignature, trackExpiration) = AccessCryptoUtil.generateSignatureAndTimestamp(track.checksum, STREAM_VALIDITY_SECONDS)

        return@transaction Streamable(
            trackId = track.id.value,
            title = track.title,
            artist = track.artists.joinToString { it.name },
            album = track.album.title,
            duration = track.duration,
            volumeOffset = track.volumeOffset,
            coverChecksum = cover?.sourceChecksum,
            coverSignature = coverSignature,
            coverExpiration = coverExpiration,
            streamSignature = trackSignature,
            streamExpiration = trackExpiration
        )
    }

}