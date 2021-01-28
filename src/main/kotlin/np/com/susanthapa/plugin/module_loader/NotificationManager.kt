package np.com.susanthapa.plugin.module_loader

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object NotificationManager {
    private const val NOTIFICATION_GROUP_NAME = "Module Loader Group"

    fun notifyError(project: Project?, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_NAME)
            .createNotification(content, NotificationType.ERROR)
            .notify(project)
    }

    fun notifyWarn(project: Project?, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_NAME)
            .createNotification(content, NotificationType.WARNING)
            .notify(project)
    }

    fun notifyInformation(project: Project?, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_NAME)
            .createNotification(content, NotificationType.INFORMATION)
            .notify(project)
    }

}