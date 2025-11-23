package utils

import org.junit.jupiter.api.assertNotNull
import de.mickkc.vibin.db.playlists.PlaylistEntity
import de.mickkc.vibin.dto.playlists.PlaylistEditDto
import de.mickkc.vibin.repos.PlaylistRepo
import de.mickkc.vibin.repos.UserRepo

object PlaylistTestUtils {

    suspend fun createPlaylist(name: String, isPublic: Boolean, ownerId: Long, vararg collaboratorIds: Long): PlaylistEntity {
        val user = UserRepo.getById(ownerId)
        assertNotNull(user)
        val playlist = PlaylistRepo.createOrUpdatePlaylist(user, PlaylistEditDto(
            name = name,
            description = "Test Description",
            isPublic = isPublic,
            collaboratorIds = collaboratorIds.toList()
        ), null)
        assertNotNull(playlist)
        return playlist
    }

}