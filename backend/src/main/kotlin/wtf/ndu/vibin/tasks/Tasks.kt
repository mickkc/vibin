package wtf.ndu.vibin.tasks

import wtf.ndu.vibin.tasks.impl.DeleteUnusedAlbumsTask
import wtf.ndu.vibin.tasks.impl.DeleteUnusedArtistsTask
import wtf.ndu.vibin.tasks.impl.DeleteUnusedImagesTask
import wtf.ndu.vibin.tasks.impl.RemoveDeletedTracksTask

object Tasks {

    fun configure() {
        TaskManager.registerTask(DeleteUnusedImagesTask())
        TaskManager.registerTask(DeleteUnusedAlbumsTask())
        TaskManager.registerTask(DeleteUnusedArtistsTask())
        TaskManager.registerTask(RemoveDeletedTracksTask())
    }

}