package com.example.nexus.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.nexus.MainActivity
import com.example.nexus.R

/**
 * Notification channels and posting. Two channels let users silence one kind without the other:
 * due-date reminders (scheduled locally) and real-time updates (pushed through FCM).
 */
object NexusNotifications {
    const val CHANNEL_REMINDERS = "nexus_reminders"
    const val CHANNEL_UPDATES = "nexus_updates" // must match the channelId the API sends with FCM
    private const val TAG = "NexusNotifications"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_reminders_description)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_UPDATES, context.getString(R.string.channel_updates), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_updates_description)
            },
        )
    }

    /** Android 13+ requires the runtime POST_NOTIFICATIONS permission; earlier versions grant it. */
    fun canPost(context: Context): Boolean = (
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    @android.annotation.SuppressLint("MissingPermission") // checked by canPost()
    fun show(context: Context, id: Int, channel: String, title: String, body: String, expanded: String? = null) {
        if (!canPost(context)) {
            Log.i(TAG, "Notification $id not shown: permission not granted")
            return
        }
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expanded ?: body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(if (channel == CHANNEL_UPDATES) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
        Log.i(TAG, "Posted notification $id on $channel")
    }

    const val ID_REMINDER = 1001
    const val ID_TEST = 1002
    const val ID_PUSH = 1003
}
