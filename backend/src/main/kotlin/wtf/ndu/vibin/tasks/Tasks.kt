package wtf.ndu.vibin.tasks

import wtf.ndu.vibin.tasks.impl.DeleteUnusedImagesTask
import wtf.ndu.vibin.tasks.impl.RemoveDeletedTracksTask

object Tasks {

    fun configure() {
        TaskManager.registerTask(DeleteUnusedImagesTask())
        TaskManager.registerTask(RemoveDeletedTracksTask())
    }

}