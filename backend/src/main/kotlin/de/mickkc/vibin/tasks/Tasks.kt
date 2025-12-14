package de.mickkc.vibin.tasks

import de.mickkc.vibin.tasks.impl.CalculateMissingVolumeLevelsTask
import de.mickkc.vibin.tasks.impl.DeleteExpiredWidgetImagesTask
import de.mickkc.vibin.tasks.impl.DeleteUnusedAlbumsTask
import de.mickkc.vibin.tasks.impl.DeleteUnusedArtistsTask
import de.mickkc.vibin.tasks.impl.DeleteUnusedImagesTask
import de.mickkc.vibin.tasks.impl.RemoveDeletedTracksTask
import de.mickkc.vibin.tasks.impl.RemoveExpiredRateLimitsTask
import de.mickkc.vibin.tasks.impl.ScanForNewTracksTask

object Tasks {

    fun configure() {
        TaskManager.registerTask(DeleteUnusedImagesTask())
        TaskManager.registerTask(DeleteUnusedAlbumsTask())
        TaskManager.registerTask(DeleteUnusedArtistsTask())
        TaskManager.registerTask(RemoveDeletedTracksTask())
        TaskManager.registerTask(DeleteExpiredWidgetImagesTask())
        TaskManager.registerTask(RemoveExpiredRateLimitsTask())
        TaskManager.registerTask(ScanForNewTracksTask())
        TaskManager.registerTask(CalculateMissingVolumeLevelsTask())
    }

}