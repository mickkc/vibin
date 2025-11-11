package utils

import org.junit.jupiter.api.assertNotNull
import wtf.ndu.vibin.db.playlists.PlaylistEntity
import wtf.ndu.vibin.dto.playlists.PlaylistEditDto
import wtf.ndu.vibin.repos.PlaylistRepo
import wtf.ndu.vibin.repos.UserRepo

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