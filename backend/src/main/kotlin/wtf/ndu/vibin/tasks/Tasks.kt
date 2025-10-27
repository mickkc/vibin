package wtf.ndu.vibin.tasks

import wtf.ndu.vibin.tasks.impl.DeleteUnusedImagesTask

object Tasks {

    fun configure() {
        TaskManager.registerTask(DeleteUnusedImagesTask())
    }

}