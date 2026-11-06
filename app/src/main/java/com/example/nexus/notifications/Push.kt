package com.example.nexus.notifications

import android.content.Context
import android.util.Log
import com.example.nexus.NexusApp
import com.example.nexus.R
import com.example.nexus.auth.AuthSession
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Real-time push through Firebase Cloud Messaging. The device's FCM token is sent to the API
 * (POST /users/me/devices). When the account changes on another device, the API pushes a
 * notification here and the app syncs immediately.
 *
 * Firebase is active only when the build includes app/google-services.json; otherwise
 * [isAvailable] is false and push is skipped while local reminders keep working.
 * Reference: https://firebase.google.com/docs/cloud-messaging/android/client (accessed 2026-11-05).
 */
object PushRegistration {
    private const val TAG = "PushRegistration"
    private const val PREFS = "nexus_push"
    private const val KEY_TOKEN = "fcm_token"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Sent as X-Nexus-Device-Token so the API does not notify the device that made a change. */
    @Volatile var currentToken: String? = null
        private set

    fun isAvailable(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    fun init(context: Context) {
        currentToken = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)
    }

    /** Fetches the FCM token and registers it for the signed-in account. Safe to call repeatedly. */
    fun register(context: Context) {
        if (!isAvailable(context)) {
            Log.i(TAG, "Firebase not configured (no google-services.json); push disabled")
            return
        }
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> onToken(context, token) }
            .addOnFailureListener { error -> Log.w(TAG, "Could not get FCM token", error) }
    }

    fun onToken(context: Context, token: String) {
        currentToken = token
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TOKEN, token).apply()
        if (AuthSession.cacheOwnerId() == null) return
        scope.launch {
            runCatching { NexusApp.userRepository.registerDevice(token) }
                .onSuccess { Log.i(TAG, "Device registered for push") }
                .onFailure { Log.w(TAG, "Device registration failed; will retry on next sign-in or start", it) }
        }
    }
}

class NexusMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushRegistration.onToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Ignore pushes that arrive after sign-out on this device.
        if (AuthSession.cacheOwnerId() == null) return
        val text = localized(applicationContext)
        val title = message.notification?.title ?: message.data["title"] ?: text.getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: text.getString(R.string.push_default_body)
        NexusNotifications.show(applicationContext, NexusNotifications.ID_PUSH, NexusNotifications.CHANNEL_UPDATES, title, body)
        if (message.data["type"] == "sync") {
            // Upload anything pending, then re-download projects and tasks into the offline cache so
            // the change is visible as soon as the user opens the app.
            CoroutineScope(Dispatchers.IO).launch { runCatching { NexusApp.dashboardRepository.getDashboard() } }
        }
        Log.i("NexusMessagingService", "Push received: ${message.data["type"]}")
    }
}
