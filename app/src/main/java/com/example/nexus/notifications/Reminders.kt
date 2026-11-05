package com.example.nexus.notifications

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.nexus.NexusApp
import com.example.nexus.R
import com.example.nexus.auth.AuthSession
import com.example.nexus.domain.ReminderPlanner
import com.example.nexus.settings.SettingsSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Due-date reminders. WorkManager runs [ReminderWorker] about every 12 hours, even when the app is
 * closed, and it reads the offline Room cache, so reminders also work without a network.
 * Reference: https://developer.android.com/develop/background-work/background-tasks/persistent (accessed 2026-11-05).
 */
object Reminders {
    private const val PERIODIC = "nexus-due-reminders"
    private const val NOW = "nexus-due-reminders-now"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Runs a check immediately, e.g. right after the user turns reminders on. */
    fun runNow(context: Context) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<ReminderWorker>().build())
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!SettingsSession.notificationsEnabled.value || AuthSession.cacheOwnerId() == null) return Result.success()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val summary = ReminderPlanner.plan(NexusApp.taskRepository.getCachedTasks(), today) ?: return Result.success()
        val text = localized(applicationContext)
        val body = when {
            summary.overdue > 0 && summary.dueToday > 0 ->
                text.getString(R.string.reminder_body_both, summary.overdue, summary.dueToday)
            summary.overdue > 0 -> text.resources.getQuantityString(R.plurals.reminder_overdue, summary.overdue, summary.overdue)
            else -> text.resources.getQuantityString(R.plurals.reminder_due_today, summary.dueToday, summary.dueToday)
        }
        NexusNotifications.show(
            applicationContext, NexusNotifications.ID_REMINDER, NexusNotifications.CHANNEL_REMINDERS,
            text.getString(R.string.reminder_title), body, body + "\n• " + summary.highlights.joinToString("\n• "),
        )
        Log.i("ReminderWorker", "Reminder posted: ${summary.overdue} overdue, ${summary.dueToday} due today")
        return Result.success()
    }
}

/** A context whose resources use the in-app language (en/zu/tn), not just the device locale. */
fun localized(context: Context): Context {
    val config = Configuration(context.resources.configuration)
    config.setLocale(Locale.forLanguageTag(SettingsSession.language.value))
    return context.createConfigurationContext(config)
}
