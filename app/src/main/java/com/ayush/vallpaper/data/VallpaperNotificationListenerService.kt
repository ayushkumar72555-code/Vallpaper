package com.ayush.vallpaper.data

import android.service.notification.NotificationListenerService

class VallpaperNotificationListenerService :
    NotificationListenerService() {

    private val application: VallpaperApplication
        get() = getApplication() as VallpaperApplication

    override fun onListenerConnected() {
        super.onListenerConnected()
        application.trackRepository.start()
        application.trackRepository.refresh()
    }

    override fun onListenerDisconnected() {
        application.trackRepository.refresh()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(
        sbn: android.service.notification.StatusBarNotification?
    ) {
        application.trackRepository.refresh()
    }

    override fun onNotificationRemoved(
        sbn: android.service.notification.StatusBarNotification?
    ) {
        application.trackRepository.refresh()
    }
}
