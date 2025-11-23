package de.mickkc.vibin.tasks

import de.mickkc.vibin.tasks.impl.DeleteUnusedAlbumsTask
import de.mickkc.vibin.tasks.impl.DeleteUnusedArtistsTask
import de.mickkc.vibin.tasks.impl.DeleteUnusedImagesTask
import de.mickkc.vibin.tasks.impl.RemoveDeletedTracksTask

object Tasks {

    fun configure() {
        TaskManager.registerTask(DeleteUnusedImagesTask())
        TaskManager.registerTask(DeleteUnusedAlbumsTask())
        TaskManager.registerTask(DeleteUnusedArtistsTask())
        TaskManager.registerTask(RemoveDeletedTracksTask())
    }

}