/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.orientation.view.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import net.mm2d.android.orientationfaker.R
import net.mm2d.orientation.control.OrientationIdManager
import net.mm2d.orientation.settings.Settings
import net.mm2d.orientation.view.widget.RemoteViewsCreator

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object NotificationHelper {
    private const val OLD_CHANNEL_ID = "CHANNEL_ID"
    private const val CHANNEL_ID = "CONTROL"
    private const val NOTIFICATION_ID = 10

    @RequiresApi(VERSION_CODES.O)
    private fun createChannel(context: Context) {
        val name = context.getString(R.string.notification_channel_name)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).also {
            it.enableLights(false)
            it.enableVibration(false)
        }
        getNotificationManager(context)?.also {
            if (it.getNotificationChannel(OLD_CHANNEL_ID) != null) {
                it.deleteNotificationChannel(OLD_CHANNEL_ID)
            }
            it.createNotificationChannel(channel)
        }
    }

    private fun getNotificationManager(context: Context): NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    fun startForeground(service: Service) {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            createChannel(service)
        }
        service.startForeground(NOTIFICATION_ID, makeNotification(service))
    }

    fun stopForeground(service: Service) {
        service.stopForeground(true)
    }

    private fun makeNotification(context: Context): Notification {
        val settings = Settings.get()
        val orientation = settings.orientation
        val visibility =
            if (settings.notifySecret) NotificationCompat.VISIBILITY_SECRET
            else NotificationCompat.VISIBILITY_PUBLIC
        val icon =
            if (settings.shouldUseBlankIconForNotification) R.drawable.ic_blank
            else OrientationIdManager.getIconIdFromOrientation(orientation)
        val views = RemoteViewsCreator.create(context, orientation, true)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setDefaults(0)
            .setContentTitle(context.getText(R.string.app_name))
            .setVisibility(visibility)
            .setCustomContentView(views)
            .setSmallIcon(icon)
            .setOngoing(true)
            .build()
    }
}
