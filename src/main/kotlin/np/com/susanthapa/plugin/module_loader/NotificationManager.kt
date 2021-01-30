package np.com.susanthapa.plugin.module_loader

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object NotificationManager {
    private val NOTIFICATION_GROUP = NotificationGroup("Module Loader Group", NotificationDisplayType.BALLOON, true)

    fun notifyError(project: Project?, content: String) {
        println("Error: $content")
        NOTIFICATION_GROUP
            .createNotification(content, NotificationType.ERROR)
            .notify(project)
    }

    fun notifyWarn(project: Project?, content: String) {
        println("Warn: $content")
        NOTIFICATION_GROUP
            .createNotification(content, NotificationType.WARNING)
            .notify(project)
    }

    fun notifyInformation(project: Project?, content: String) {
        println("Info: $content")
        NOTIFICATION_GROUP
            .createNotification(content, NotificationType.INFORMATION)
            .notify(project)
    }

}