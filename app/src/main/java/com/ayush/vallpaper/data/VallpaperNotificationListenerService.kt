package com.ayush.vallpaper.data

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ayush.vallpaper.VallpaperApplication

class VallpaperNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "VallpaperMedia"
    }

    private val vallpaperApplication: VallpaperApplication
        get() = application as VallpaperApplication

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
        vallpaperApplication.trackRepository.onListenerConnected()
    }

    override fun onListenerDisconnected() {
        Log.d(TAG, "Notification listener disconnected")
        vallpaperApplication.trackRepository.onListenerDisconnected()

        requestRebind(
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
