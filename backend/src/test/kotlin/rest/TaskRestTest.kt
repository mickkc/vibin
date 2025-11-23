package rest

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.*
import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.db.images.ImageEntity
import de.mickkc.vibin.dto.TaskDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.AlbumRepo
import de.mickkc.vibin.repos.ArtistRepo
import de.mickkc.vibin.repos.ImageRepo
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.tasks.TaskManager
import de.mickkc.vibin.tasks.TaskResult
import de.mickkc.vibin.utils.DateTimeUtils
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteExisting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TaskRestTest {

    // region Get

    @Test
    fun testGetTasks() = testApp { client ->

        val response = client.get("/api/tasks")
        assertTrue(response.status.isSuccess())

        val tasks = response.body<List<TaskDto>>()
        assertEquals(TaskManager.getTasks().size, tasks.size)
    }

    @Test
    fun testGetTasks_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.MANAGE_TASKS to false
        )

        val response = client.get("/api/tasks") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testGetTasks_VerifyTaskIds() = testApp { client ->
        val response = client.get("/api/tasks")

        assertTrue(response.status.isSuccess())

        val tasks = response.body<List<TaskDto>>()

        val taskIds = tasks.map { it.id }
        TaskManager.getTasks().forEach { task ->
            assertTrue(taskIds.contains(task.id))
        }
    }
    // endregion

    // region Enable/Disable

    @Test
    fun testEnableTask() = testApp { client ->

        val taskId = TaskManager.getTasks().first().id
        val response = client.put("/api/tasks/$taskId/enable") {
            parameter("enable", "true")
        }

        assertTrue(response.status.isSuccess())

        val updatedTasksResponse = client.get("/api/tasks")
        val updatedTasks = updatedTasksResponse.body<List<TaskDto>>()
        val updatedTask = updatedTasks.find { it.id == taskId }

        assertNotNull(updatedTask)
        assertTrue(updatedTask.enabled)
    }

    @Test
    fun testDisableTask() = testApp { client ->

        val taskId = TaskManager.getTasks().first().id
        val response = client.put("/api/tasks/$taskId/enable") {
            parameter("enable", "false")
        }

        assertTrue(response.status.isSuccess())

        val updatedTasksResponse = client.get("/api/tasks")
        val updatedTasks = updatedTasksResponse.body<List<TaskDto>>()
        val updatedTask = updatedTasks.find { it.id == taskId }

        assertNotNull(updatedTask)
        assertFalse(updatedTask.enabled)
    }

    @Test
    fun testEnableTask_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.MANAGE_TASKS to false
        )

        val taskId = TaskManager.getTasks().first().id

        val response = client.put("/api/tasks/$taskId/enable") {
            bearerAuth(token)
            parameter("enable", "true")
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testEnableTask_MissingEnableParameter() = testApp { client ->

        val taskId = TaskManager.getTasks().first().id
        val response = client.put("/api/tasks/$taskId/enable")

        assertEquals(400, response.status.value)
    }

    @Test
    fun testEnableTask_InvalidEnableParameter() = testApp { client ->

        val taskId = TaskManager.getTasks().first().id
        val response = client.put("/api/tasks/$taskId/enable") {
            parameter("enable", "not-a-boolean")
        }

        assertEquals(400, response.status.value)
    }

    @Test
    fun testEnableTask_NonExistentTask() = testApp { client ->
        val response = client.put("/api/tasks/non-existent-task-id/enable") {
            parameter("enable", "true")
        }

        assertEquals(404, response.status.value)
    }
    // endregion

    // region Run

    @Test
    fun testRunTask() = testApp { client ->

        val before = DateTimeUtils.now()

        val taskId = TaskManager.getTasks().first().id
        val response = client.post("/api/tasks/$taskId/run")

        val after = DateTimeUtils.now()

        assertTrue(response.status.isSuccess())

        val result = response.body<TaskResult>()
        assertNotNull(result)

        val task = TaskManager.getById(taskId)
        assertNotNull(task)
        assertNotNull(task.lastRun)
        assertTrue(task.lastRun in before..after)
    }

    @Test
    fun testRunTask_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.MANAGE_TASKS to false
        )

        val taskId = TaskManager.getTasks().first().id
        val response = client.post("/api/tasks/$taskId/run") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testRunTask_NotFound() = testApp { client ->

        val response = client.post("/api/tasks/non-existent-task-id/run")

        assertEquals(404, response.status.value)
    }
    // endregion

    // region DeleteUnusedImagesTask

    @Test
    fun testDeleteUnusedImagesTask_NoUnusedImages() = testApp { client ->

        val track = TrackTestUtils.createTrack("Test Track", album = "Test Album", artists = "Test Artist")

        transaction {
            val image = ImageEntity.new {
                this.sourcePath = "/images/test_small.jpg"
                this.sourceChecksum = "test_image_checksum"
            }
            track.cover = image
        }

        assertEquals(1, transaction { ImageEntity.count() })

        val response = client.post("/api/tasks/delete_unused_images/run")

        assertTrue(response.status.isSuccess())

        val result = response.body<TaskResult>()
        assertTrue(result.success)

        assertEquals(1, transaction { ImageEntity.count() })
    }

    @Test
    fun testDeleteUnusedImagesTask_WithUnusedImages() = testApp { client ->

        transaction {
            ImageRepo.createImage(
                sourcePath = "/images/unused.jpg",
                checksum = "unused_image_checksum"
            )
        }

        val imageCountBefore = transaction { ImageEntity.count() }
        assertTrue(imageCountBefore > 0)

        val response = client.post("/api/tasks/delete_unused_images/run")
        assertTrue(response.status.isSuccess())

        val result = response.body<TaskResult>()
        assertTrue(result.success)

        val imageCountAfter = transaction { ImageEntity.count() }
        assertTrue(imageCountAfter < imageCountBefore)
    }
    // endregion

    // region DeleteUnusedAlbumsTask

    @Test
    fun testDeleteUnusedAlbumsTask_NoUnusedAlbums() = testApp { client ->

        val album = AlbumTestUtils.createAlbum("Test Album", "Description", 2020)
        TrackTestUtils.createTrack("Test Track", album = "Test Album", artists = "Test Artist")

        val response = client.post("/api/tasks/delete_unused_albums/run")
        assertTrue(response.status.isSuccess())

        val result = response.body<TaskResult>()
        assertTrue(result.success)

        val albumStillExists = AlbumRepo.getById(album.id.value)
        assertNotNull(albumStillExists)

        assertEquals(1, AlbumRepo.count())
    }

    @Test
    fun testDeleteUnusedAlbumsTask_WithUnusedAlbums() = testApp { client ->

        val unusedAlbum = AlbumTestUtils.createAlbum("Unused Album", "No tracks here", 2021)

        val albumCountBefore = AlbumRepo.count()
        assertEquals(1, albumCountBefore)

        val response = client.post("/api/tasks/delete_unused_albums/run")
        assertTrue(response.status.isSuccess())

        val result = response.body<TaskResult>()
        assertTrue(result.success)

        val albumCountAfter = AlbumRepo.count()
        assertTrue(albumCountAfter < albumCountBefore)

        val deletedAlbum = transaction { AlbumRepo.getById(unusedAlbum.id.value) }
        assertEquals(null, deletedAlbum)
    }
    // endregion

    // region DeleteUnusedArtistsTask

    @Test
    fun testDeleteUnusedArtistsTask_NoUnusedArtists() = testApp { client ->

        ArtistTestUtils.createArtist("Test Artist", "Description")
        TrackTestUtils.createTrack("Test Track", album = "Test Album", artists = "Test Artist")

        val artistCountBefore = ArtistRepo.count()
        assertEquals(1, artistCountBefore)

        val response = client.post("/api/tasks/delete_unused_artists/run")
        assertTrue(response.status.isSuccess())

        val result = response.body<TaskResult>()
        assertTrue(result.success)

        val artistCountAfter = ArtistRepo.count()
        assertEquals(artistCountBefore, artistCountAfter)
    }

    @Test
    fun testDeleteUnusedArtistsTask_WithUnusedArtists() = testApp { client ->

        val unusedArtist = ArtistTestUtils.createArtist("Unused Artist", "No tracks for this artist")

        val artistCountBefore = ArtistRepo.count()
        assertEquals(1, artistCountBefore)

        val response = client.post("/api/tasks/delete_unused_artists/run")
        assertTrue(response.status.isSuccess())

        val result = response.body<TaskResult>()
        assertTrue(result.success)

        val artistCountAfter = ArtistRepo.count()
        assertTrue(artistCountAfter < artistCountBefore)

        val deletedArtist = transaction { ArtistRepo.getById(unusedArtist.id.value) }
        assertEquals(null, deletedArtist)
    }
    // endregion

    // region RemoveDeletedTracksTask

    @Test
    fun testRemoveDeletedTracksTask_NoDeletedTracks() = testApp { client ->

        val tempDir = createTempDirectory("music_dir")
        EnvUtil.addOverride(EnvUtil.MUSIC_DIR, tempDir.absolutePathString())

        val randomFile = tempDir.resolve("test_track.mp3")
        randomFile.createFile()

        TrackTestUtils.createTrack(
            title = "Test Track",
            album = "Test Album",
            artists = "Test Artist",
            path = "test_track.mp3"
        )

        val trackCountBefore = TrackRepo.count()
        assertEquals(1, trackCountBefore)

        val response = client.post("/api/tasks/remove_deleted_tracks/run")
        assertTrue(response.status.isSuccess())

        val result = response.body<TaskResult>()
        assertTrue(result.success)

        val trackCountAfter = TrackRepo.count()
        assertEquals(trackCountBefore, trackCountAfter)

        // Clean up
        randomFile.deleteExisting()
        tempDir.deleteExisting()
    }

    @Test
    fun testRemoveDeletedTracksTask_WithDeletedTracks() = testApp { client ->

        val trackWithMissingFile = TrackTestUtils.createTrack(
            title = "Missing Track",
            album = "Test Album",
            artists = "Test Artist",
            path = "/nonexistent/path/missing_track.mp3"
        )

        val trackCountBefore = TrackRepo.count()
        assertEquals(1, trackCountBefore)

        val response = client.post("/api/tasks/remove_deleted_tracks/run")
        assertTrue(response.status.isSuccess())

        val result = response.body<TaskResult>()
        assertTrue(result.success)
        assertNotNull(result.message)

        val trackCountAfter = TrackRepo.count()
        assertTrue(trackCountAfter < trackCountBefore)

        val deletedTrack = transaction { TrackRepo.getById(trackWithMissingFile.id.value) }
        assertEquals(null, deletedTrack)
    }
    // endregion

    // region Task Results

    @Test
    fun testTaskResults_PersistAfterRun() = testApp { client ->

        val runResponse = client.post("/api/tasks/delete_unused_images/run")
        assertTrue(runResponse.status.isSuccess())

        val tasksResponse = client.get("/api/tasks")
        val tasks = tasksResponse.body<List<TaskDto>>()

        val task = tasks.find { it.id == "delete_unused_images" }
        assertNotNull(task)
        assertNotNull(task.lastResult)
        assertTrue(task.lastResult.success)
    }

    @Test
    fun testTaskResults_LastRunUpdated() = testApp { client ->

        val initialTasksResponse = client.get("/api/tasks")
        val initialTasks = initialTasksResponse.body<List<TaskDto>>()
        val initialTask = initialTasks.find { it.id == "delete_unused_albums" }
        assertNotNull(initialTask)
        val initialLastRun = initialTask.lastRun

        val runResponse = client.post("/api/tasks/delete_unused_albums/run")
        assertTrue(runResponse.status.isSuccess())

        val updatedTasksResponse = client.get("/api/tasks")
        val updatedTasks = updatedTasksResponse.body<List<TaskDto>>()
        val updatedTask = updatedTasks.find { it.id == "delete_unused_albums" }
        assertNotNull(updatedTask)
        assertNotNull(updatedTask.lastRun)

        if (initialLastRun != null) {
            assertTrue(updatedTask.lastRun >= initialLastRun)
        }
    }
    // endregion
}
