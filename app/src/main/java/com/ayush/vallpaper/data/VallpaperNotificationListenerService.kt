package com.ayush.vallpaper.data

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ayush.vallpaper.VallpaperApplication

class VallpaperNotificationListenerService :
    NotificationListenerService() {

    private val vallpaperApplication: VallpaperApplication
        get() = application as VallpaperApplication

    override fun onListenerConnected() {
        super.onListenerConnected()
        vallpaperApplication.trackRepository.start()
        vallpaperApplication.trackRepository.refresh()
    }

    override fun onListenerDisconnected() {
        // Android documents requestRebind() as the supported recovery path
        // when a NotificationListenerService becomes disconnected.
        NotificationListenerService.requestRebind(
            ComponentName(
                this,
                VallpaperNotificationListenerService::class.java
            )
        )
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        vallpaperApplication.trackRepository.refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        vallpaperApplication.trackRepository.refresh()
    }
}
