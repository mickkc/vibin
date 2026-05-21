package rest

import de.mickkc.vibin.db.TrackListenDurationEntity
import de.mickkc.vibin.dto.tracks.batch.BatchEditAction
import de.mickkc.vibin.dto.tracks.batch.BatchEditActionType
import de.mickkc.vibin.dto.tracks.batch.BatchEditDto
import de.mickkc.vibin.dto.tracks.batch.BatchEditResultDto
import de.mickkc.vibin.dto.tracks.batch.BatchEditResultDtoChangeType
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.ListenTrackingRepo
import de.mickkc.vibin.repos.PlaylistTrackRepo
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.utils.DateTimeUtils
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.*
import kotlin.test.*

class BatchEditRestTest {

    private fun makeDto(
        trackIds: List<Long>? = null,
        query: String? = null,
        actions: List<BatchEditAction>,
        dryRun: Boolean = false
    ) = BatchEditDto(trackIds = trackIds, query = query, actions = actions, dryRun = dryRun)

    private fun titleAction(from: String, to: String) = BatchEditAction(
        actionType = BatchEditActionType.TITLE,
        data = mapOf("from" to from, "to" to to)
    )

    private fun commentAction(from: String, to: String) = BatchEditAction(
        actionType = BatchEditActionType.CHANGE_COMMENT,
        data = mapOf("from" to from, "to" to to)
    )

    private fun addTagsAction(ids: List<Long>) = BatchEditAction(
        actionType = BatchEditActionType.ADD_TAGS,
        data = mapOf("ids" to ids)
    )

    private fun removeTagsAction(ids: List<Long>) = BatchEditAction(
        actionType = BatchEditActionType.REMOVE_TAGS,
        data = mapOf("ids" to ids)
    )

    private fun addToPlaylistAction(id: Long) = BatchEditAction(
        actionType = BatchEditActionType.ADD_TO_PLAYLIST,
        data = mapOf("id" to id)
    )

    private fun removeFromPlaylistAction(id: Long) = BatchEditAction(
        actionType = BatchEditActionType.REMOVE_FROM_PLAYLIST,
        data = mapOf("id" to id)
    )

    private fun removeListenDurationAction() = BatchEditAction(
        actionType = BatchEditActionType.REMOVE_LISTEN_DURATION,
        data = emptyMap()
    )

    private fun regenerateFileMetadataAction() = BatchEditAction(
        actionType = BatchEditActionType.REGENERATE_FILE_METADATA,
        data = emptyMap()
    )

    private fun changeAlbumAction(id: Long) = BatchEditAction(
        actionType = BatchEditActionType.CHANGE_ALBUM,
        data = mapOf("id" to id)
    )

    // region Permission

    @Test
    fun testBatchEdit_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.MANAGE_TRACKS to false
        )

        val track = TrackTestUtils.createTrack("Track", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            bearerAuth(token)
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(titleAction("Track", "New"))
            ))
        }

        assertEquals(403, response.status.value)
    }

    // endregion

    // region Track selection

    @Test
    fun testBatchEdit_NoTrackIdsOrQuery() = testApp { client ->
        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = null,
                query = null,
                actions = listOf(titleAction("Old", "New"))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNotNull(result.error)
        assertEquals("No trackIds or query provided.", result.error)
        assertEquals(0, result.affectedTracks)
        assertFalse(result.didWriteToDatabase)
    }

    @Test
    fun testBatchEdit_ByTrackIds_OnlyAffectsSpecifiedTracks() = testApp { client ->
        val track1 = TrackTestUtils.createTrack("Rock Song", "Album", "Artist")
        val track2 = TrackTestUtils.createTrack("Jazz Song", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track1.id.value),
                actions = listOf(titleAction("Rock", "Blues"))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(1, result.affectedTracks)
        assertTrue(result.didWriteToDatabase)

        transaction {
            assertEquals("Blues Song", TrackRepo.getById(track1.id.value)!!.title)
            assertEquals("Jazz Song", TrackRepo.getById(track2.id.value)!!.title)
        }
    }

    @Test
    fun testBatchEdit_ByQuery_OnlyAffectsMatchingTracks() = testApp { client ->
        val track1 = TrackTestUtils.createTrack("Rock Song", "Album", "Artist")
        val track2 = TrackTestUtils.createTrack("Jazz Song", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                query = "Rock",
                actions = listOf(titleAction("Rock", "Metal"))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(1, result.affectedTracks)
        assertTrue(result.didWriteToDatabase)

        transaction {
            assertEquals("Metal Song", TrackRepo.getById(track1.id.value)!!.title)
            assertEquals("Jazz Song", TrackRepo.getById(track2.id.value)!!.title)
        }
    }

    @Test
    fun testBatchEdit_EmptyTrackIds() = testApp { client ->
        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = emptyList(),
                actions = listOf(titleAction("Old", "New"))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(0, result.affectedTracks)
        assertEquals(0, result.affecteddFields)
        assertTrue(result.changes.isEmpty())
    }

    @Test
    fun testBatchEdit_NonExistentTrackIds() = testApp { client ->
        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(99999L, 88888L),
                actions = listOf(titleAction("Old", "New"))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(0, result.affectedTracks)
    }

    @Test
    fun testBatchEdit_MultipleTrackIds() = testApp { client ->
        val track1 = TrackTestUtils.createTrack("Old Song 1", "Album", "Artist")
        val track2 = TrackTestUtils.createTrack("Old Song 2", "Album", "Artist")
        val track3 = TrackTestUtils.createTrack("Untouched Song", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track1.id.value, track2.id.value),
                actions = listOf(titleAction("Old", "New"))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(2, result.affectedTracks)
        assertEquals(2, result.affecteddFields)

        transaction {
            assertEquals("New Song 1", TrackRepo.getById(track1.id.value)!!.title)
            assertEquals("New Song 2", TrackRepo.getById(track2.id.value)!!.title)
            assertEquals("Untouched Song", TrackRepo.getById(track3.id.value)!!.title)
        }
    }

    // endregion

    // region Dry run

    @Test
    fun testBatchEdit_DryRun_DoesNotWriteToDatabase() = testApp { client ->
        val track = TrackTestUtils.createTrack("Old Title", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(titleAction("Old", "New")),
                dryRun = true
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertFalse(result.didWriteToDatabase)
        assertEquals(1, result.affectedTracks)
        assertEquals(1, result.affecteddFields)

        transaction {
            assertEquals("Old Title", TrackRepo.getById(track.id.value)!!.title)
        }
    }

    @Test
    fun testBatchEdit_DryRun_ReturnsPreviewOfChanges() = testApp { client ->
        val track = TrackTestUtils.createTrack("Old Title", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(titleAction("Old Title", "New Title")),
                dryRun = true
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertFalse(result.didWriteToDatabase)
        assertEquals(1, result.changes.size)
        assertEquals("Old Title", result.changes[0].before)
        assertEquals("New Title", result.changes[0].after)
        assertEquals(track.id.value, result.changes[0].track.id)
    }

    // endregion

    // region TITLE action

    @Test
    fun testBatchEdit_Title_RegexReplacement() = testApp { client ->
        val track = TrackTestUtils.createTrack("Old Title", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(titleAction("Old", "New"))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(1, result.affectedTracks)
        assertEquals(1, result.affecteddFields)
        assertTrue(result.didWriteToDatabase)
        assertEquals("Old Title", result.changes[0].before)
        assertEquals("New Title", result.changes[0].after)

        transaction {
            assertEquals("New Title", TrackRepo.getById(track.id.value)!!.title)
        }
    }

    @Test
    fun testBatchEdit_Title_NoRegexMatch_NoChange() = testApp { client ->
        val track = TrackTestUtils.createTrack("Jazz Song", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(titleAction("Rock", "Metal"))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(0, result.affectedTracks)
        assertEquals(0, result.affecteddFields)
        assertTrue(result.changes.isEmpty())

        transaction {
            assertEquals("Jazz Song", TrackRepo.getById(track.id.value)!!.title)
        }
    }

    @Test
    fun testBatchEdit_Title_InvalidFromType_ReturnsError() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(BatchEditAction(
                    actionType = BatchEditActionType.TITLE,
                    data = mapOf("from" to 123, "to" to "New")
                ))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNotNull(result.error)
        assertFalse(result.didWriteToDatabase)
    }

    @Test
    fun testBatchEdit_Title_SequentialActions_EachSeesUpdatedTitle() = testApp { client ->
        val track = TrackTestUtils.createTrack("Old Title", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(
                    titleAction("Old Title", "Middle Title"),
                    titleAction("Middle Title", "Final Title")
                ),
                dryRun = true
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(2, result.changes.size)
        assertEquals("Old Title", result.changes[0].before)
        assertEquals("Middle Title", result.changes[0].after)
        assertEquals("Middle Title", result.changes[1].before)
        assertEquals("Final Title", result.changes[1].after)
    }

    // endregion

    // region CHANGE_COMMENT action

    @Test
    fun testBatchEdit_ChangeComment_RegexReplacement() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist", comment = "original comment")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(commentAction("original", "updated"))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(1, result.affectedTracks)
        assertEquals("original comment", result.changes[0].before)
        assertEquals("updated comment", result.changes[0].after)

        transaction {
            assertEquals("updated comment", TrackRepo.getById(track.id.value)!!.comment)
        }
    }

    @Test
    fun testBatchEdit_ChangeComment_NoRegexMatch_NoChange() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist", comment = "some comment")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(commentAction("nomatch", "replacement"))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(0, result.affectedTracks)
        assertTrue(result.changes.isEmpty())
    }

    @Test
    fun testBatchEdit_ChangeComment_InvalidToType_ReturnsError() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist", comment = "some comment")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(BatchEditAction(
                    actionType = BatchEditActionType.CHANGE_COMMENT,
                    data = mapOf("from" to "some", "to" to 456)
                ))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNotNull(result.error)
        assertFalse(result.didWriteToDatabase)
    }

    // endregion

    // region REMOVE_LISTEN_DURATION action

    @Test
    fun testBatchEdit_RemoveListenDuration_ClearsData() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist", duration = 180000)

        transaction {
            TrackListenDurationEntity.new {
                this.track = track
                this.user = UserRepo.getById(1)!!
                this.listenedDurationSeconds = 120
                this.createdAt = DateTimeUtils.now()
            }
        }

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(removeListenDurationAction())
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(1, result.affectedTracks)
        assertEquals(1, result.affecteddFields)
        assertTrue(result.didWriteToDatabase)
        assertEquals("120s", result.changes[0].before)
        assertEquals("0s", result.changes[0].after)

        assertEquals(0, ListenTrackingRepo.getGlobalListenDurationForTrack(track.id.value))
    }

    @Test
    fun testBatchEdit_RemoveListenDuration_NoExistingData_NoChange() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(removeListenDurationAction())
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(0, result.affectedTracks)
        assertTrue(result.changes.isEmpty())
    }

    @Test
    fun testBatchEdit_RemoveListenDuration_DryRun_DoesNotDelete() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist", duration = 180000)

        transaction {
            TrackListenDurationEntity.new {
                this.track = track
                this.user = UserRepo.getById(1)!!
                this.listenedDurationSeconds = 60
                this.createdAt = DateTimeUtils.now()
            }
        }

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(removeListenDurationAction()),
                dryRun = true
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertFalse(result.didWriteToDatabase)
        assertEquals(1, result.affectedTracks)

        assertEquals(60, ListenTrackingRepo.getGlobalListenDurationForTrack(track.id.value))
    }

    // endregion

    // region ADD_TAGS action

    @Test
    fun testBatchEdit_AddTags_AddsTagToTrack() = testApp { client ->
        val tag = TagTestUtils.createTag("Rock")
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(addTagsAction(listOf(tag.id.value)))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(1, result.affectedTracks)
        assertTrue(result.didWriteToDatabase)

        transaction {
            assertEquals(1, TrackRepo.getById(track.id.value)!!.tags.count())
        }
    }

    @Test
    fun testBatchEdit_AddTags_InvalidIdsType_ReturnsError() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(BatchEditAction(
                    actionType = BatchEditActionType.ADD_TAGS,
                    data = mapOf("ids" to "not-a-list")
                ))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNotNull(result.error)
        assertFalse(result.didWriteToDatabase)
    }

    // endregion

    // region REMOVE_TAGS action

    @Test
    fun testBatchEdit_RemoveTags_RemovesTagFromTrack() = testApp { client ->
        val tag = TagTestUtils.createTag("Pop")
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist", tags = listOf("Pop"))

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(removeTagsAction(listOf(tag.id.value)))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(1, result.affectedTracks)
        assertTrue(result.didWriteToDatabase)

        transaction {
            assertEquals(0, TrackRepo.getById(track.id.value)!!.tags.count())
        }
    }

    @Test
    fun testBatchEdit_RemoveTags_TagNotOnTrack_NoChange() = testApp { client ->
        val tag = TagTestUtils.createTag("Classical")
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(removeTagsAction(listOf(tag.id.value)))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(0, result.affectedTracks)
        assertTrue(result.changes.isEmpty())
    }

    // endregion

    // region ADD_TO_PLAYLIST action

    @Test
    fun testBatchEdit_AddToPlaylist_AddsTrackToPlaylist() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(addToPlaylistAction(playlist.id.value))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(1, result.affectedTracks)
        assertTrue(result.didWriteToDatabase)

        val playlistTracks = PlaylistTrackRepo.getTracksAsDtos(playlist)
        assertEquals(1, playlistTracks.size)
        assertEquals(track.id.value, playlistTracks[0].track.id)
    }

    @Test
    fun testBatchEdit_AddToPlaylist_AlreadyInPlaylist_NoChange() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)
        PlaylistTrackRepo.addTrackToPlaylist(playlist, track, UserRepo.getById(1)!!)

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(addToPlaylistAction(playlist.id.value))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(0, result.affectedTracks)
        assertTrue(result.changes.isEmpty())
    }

    @Test
    fun testBatchEdit_AddToPlaylist_PlaylistNotFound_ReturnsError() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(addToPlaylistAction(99999L))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNotNull(result.error)
        assertFalse(result.didWriteToDatabase)
    }

    // endregion

    // region REMOVE_FROM_PLAYLIST action

    @Test
    fun testBatchEdit_RemoveFromPlaylist_RemovesTrackFromPlaylist() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)
        PlaylistTrackRepo.addTrackToPlaylist(playlist, track, UserRepo.getById(1)!!)

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(removeFromPlaylistAction(playlist.id.value))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(1, result.affectedTracks)
        assertTrue(result.didWriteToDatabase)

        val playlistTracks = PlaylistTrackRepo.getTracksAsDtos(playlist)
        assertTrue(playlistTracks.isEmpty())
    }

    @Test
    fun testBatchEdit_RemoveFromPlaylist_TrackNotInPlaylist_NoChange() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(removeFromPlaylistAction(playlist.id.value))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(0, result.affectedTracks)
        assertTrue(result.changes.isEmpty())
    }

    // endregion

    // region CHANGE_ALBUM action

    @Test
    fun testBatchEdit_ChangeAlbum_MovesTrackToNewAlbum() = testApp { client ->
        AlbumTestUtils.createAlbum("Original Album")
        val newAlbum = AlbumTestUtils.createAlbum("New Album")
        val track = TrackTestUtils.createTrack("Test Track", "Original Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(changeAlbumAction(newAlbum.id.value))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(1, result.affectedTracks)
        assertTrue(result.didWriteToDatabase)
        assertEquals("Original Album", result.changes[0].before)
        assertEquals("New Album", result.changes[0].after)

        transaction {
            assertEquals("New Album", TrackRepo.getById(track.id.value)!!.album.title)
        }
    }

    @Test
    fun testBatchEdit_ChangeAlbum_SameAlbum_NoChange() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Some Album", "Artist")
        val albumId = transaction { track.album.id.value }

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(changeAlbumAction(albumId))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(0, result.affectedTracks)
        assertTrue(result.changes.isEmpty())
    }

    @Test
    fun testBatchEdit_ChangeAlbum_AlbumNotFound_ReturnsError() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(changeAlbumAction(99999L))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNotNull(result.error)
        assertFalse(result.didWriteToDatabase)
    }

    // endregion

    // region REGENERATE_FILE_METADATA action

    @Test
    fun testBatchEdit_RegenerateFileMetadata_FileNotFound_ReturnsError() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(regenerateFileMetadataAction())
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        // File does not exist in test environment, so an exception is expected
        assertNotNull(result.error)
        assertFalse(result.didWriteToDatabase)
    }

    // TODO: Add test to run REGENERATE_FILE_METADATA on an actual file

    // endregion

    // region Multiple actions and tracks

    @Test
    fun testBatchEdit_MultipleActions_AllApplied() = testApp { client ->
        val track = TrackTestUtils.createTrack("Old Title", "Album", "Artist", comment = "old comment")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(
                    titleAction("Old Title", "New Title"),
                    commentAction("old comment", "new comment")
                )
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(1, result.affectedTracks)
        assertEquals(2, result.affecteddFields)
        assertTrue(result.didWriteToDatabase)

        transaction {
            val updated = TrackRepo.getById(track.id.value)!!
            assertEquals("New Title", updated.title)
            assertEquals("new comment", updated.comment)
        }
    }

    @Test
    fun testBatchEdit_MultipleActions_PartialMatch_CountsAffected() = testApp { client ->
        val track = TrackTestUtils.createTrack("Old Title", "Album", "Artist", comment = "unchanged")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(
                    titleAction("Old Title", "New Title"),
                    commentAction("nomatch", "replacement")
                )
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(1, result.affectedTracks)
        assertEquals(1, result.affecteddFields)
    }

    @Test
    fun testBatchEdit_MultipleActions_DryRun_NoWrite() = testApp { client ->
        val tagToAdd = TagTestUtils.createTag("New Tag")
        val tagToRemove = TagTestUtils.createTag("Existing Tag")
        AlbumTestUtils.createAlbum("Original Album")
        val targetAlbum = AlbumTestUtils.createAlbum("Target Album")
        val playlistToAddTo = PlaylistTestUtils.createPlaylist("Playlist To Add", true, 1)
        val playlistToRemoveFrom = PlaylistTestUtils.createPlaylist("Playlist To Remove", true, 1)

        val track = TrackTestUtils.createTrack(
            title = "Old Title",
            album = "Original Album",
            artists = "Artist",
            comment = "old comment",
            tags = listOf("Existing Tag"),
            duration = 180000
        )

        PlaylistTrackRepo.addTrackToPlaylist(playlistToRemoveFrom, track, UserRepo.getById(1)!!)

        transaction {
            TrackListenDurationEntity.new {
                this.track = track
                this.user = UserRepo.getById(1)!!
                this.listenedDurationSeconds = 90
                this.createdAt = DateTimeUtils.now()
            }
        }

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(
                    titleAction("Old Title", "New Title"),
                    commentAction("old comment", "new comment"),
                    addTagsAction(listOf(tagToAdd.id.value)),
                    removeTagsAction(listOf(tagToRemove.id.value)),
                    addToPlaylistAction(playlistToAddTo.id.value),
                    removeFromPlaylistAction(playlistToRemoveFrom.id.value),
                    removeListenDurationAction(),
                    changeAlbumAction(targetAlbum.id.value)
                ),
                dryRun = true
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertFalse(result.didWriteToDatabase)
        assertEquals(8, result.changes.size)

        // Validate each individual change
        result.changes.forEach { assertEquals(track.id.value, it.track.id) }

        val titleChange = result.changes[0]
        assertEquals(BatchEditResultDtoChangeType.NAME, titleChange.type)
        assertEquals("Old Title", titleChange.before)
        assertEquals("New Title", titleChange.after)

        val commentChange = result.changes[1]
        assertEquals(BatchEditResultDtoChangeType.COMMENT, commentChange.type)
        assertEquals("old comment", commentChange.before)
        assertEquals("new comment", commentChange.after)

        val addTagsChange = result.changes[2]
        assertEquals(BatchEditResultDtoChangeType.TAGS, addTagsChange.type)
        assertEquals("Existing Tag", addTagsChange.before)
        assertEquals("Existing Tag, New Tag", addTagsChange.after)

        val removeTagsChange = result.changes[3]
        assertEquals(BatchEditResultDtoChangeType.TAGS, removeTagsChange.type)
        assertEquals("Existing Tag, New Tag", removeTagsChange.before) // state carries forward from ADD_TAGS
        assertEquals("New Tag", removeTagsChange.after)

        val addPlaylistChange = result.changes[4]
        assertEquals(BatchEditResultDtoChangeType.PLAYLIST, addPlaylistChange.type)
        assertEquals("Playlist To Remove", addPlaylistChange.before)
        assertEquals("Playlist To Remove, Playlist To Add", addPlaylistChange.after)

        val removePlaylistChange = result.changes[5]
        assertEquals(BatchEditResultDtoChangeType.PLAYLIST, removePlaylistChange.type)
        assertEquals("Playlist To Remove, Playlist To Add", removePlaylistChange.before) // state carries forward from ADD_TO_PLAYLIST
        assertEquals("Playlist To Add", removePlaylistChange.after)

        val listenDurationChange = result.changes[6]
        assertEquals(BatchEditResultDtoChangeType.LISTEN_DURATION, listenDurationChange.type)
        assertEquals("90s", listenDurationChange.before)
        assertEquals("0s", listenDurationChange.after)

        val albumChange = result.changes[7]
        assertEquals(BatchEditResultDtoChangeType.ALBUM, albumChange.type)
        assertEquals("Original Album", albumChange.before)
        assertEquals("Target Album", albumChange.after)

        // Validate nothing was written to the database
        transaction {
            val unchanged = TrackRepo.getById(track.id.value)!!
            assertEquals("Old Title", unchanged.title)
            assertEquals("old comment", unchanged.comment)
            assertEquals("Original Album", unchanged.album.title)
            assertEquals(1, unchanged.tags.count())
            assertEquals("Existing Tag", unchanged.tags.first().name)
        }

        assertTrue(PlaylistTrackRepo.getTracksAsDtos(playlistToAddTo).isEmpty())
        assertEquals(1, PlaylistTrackRepo.getTracksAsDtos(playlistToRemoveFrom).size)
        assertEquals(90, ListenTrackingRepo.getGlobalListenDurationForTrack(track.id.value))
    }

    // endregion

    // region Response structure

    @Test
    fun testBatchEdit_ResponseContainsTrackInfo() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Title", "Test Album", "Test Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = listOf(track.id.value),
                actions = listOf(titleAction("Test Title", "Updated Title"))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertEquals(1, result.changes.size)

        val change = result.changes[0]
        assertEquals(track.id.value, change.track.id)
        assertEquals("Test Title", change.before)
        assertEquals("Updated Title", change.after)
    }

    @Test
    fun testBatchEdit_SuccessResponse_ErrorIsNull() = testApp { client ->
        TrackTestUtils.createTrack("Test", "Album", "Artist")

        val response = client.post("/api/tracks/edit/batch") {
            setBody(makeDto(
                trackIds = emptyList(),
                actions = listOf(titleAction("Test", "Updated"))
            ))
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<BatchEditResultDto>()

        assertNull(result.error)
        assertFalse(result.didWriteToDatabase)
    }

    // endregion
}
