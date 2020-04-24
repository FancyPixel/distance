package it.fancypixel.distance.services.firebase

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.fancypixel.distance.R
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.db.BumpRepository
import it.fancypixel.distance.global.Constants
import it.fancypixel.distance.ui.activities.MainActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        if (remoteMessage.data.isNotEmpty()) {
            val uuid = remoteMessage.data["uuid"]
            val bumps = BumpRepository().getBumpWithPositiveUuid(uuid ?: "")

            Log.d("ciao", "list: ${BumpRepository().getTodayBumps()}")
            if (bumps != null && bumps.size > 0) {
                notifyWarningMessage()
            }
        }
    }

    private fun notifyWarningMessage() {
        createNotificationChannel(this)

        val builder = NotificationCompat.Builder(this, getString(R.string.warning_notification_channel_id))
            .setSmallIcon(R.drawable.ic_stat_person)
            .setContentTitle(getString(R.string.positive_warning_notification_title))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(getString(R.string.positive_warning_notification_subtitle))
            )
            .setColor(ContextCompat.getColor(this, R.color.errorColorText))
            .setAutoCancel(true)
            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Main intent that open the activity
        builder.setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT))

        with(NotificationManagerCompat.from(this)) {
            val id = Constants.WARNING_NOTIFICATION_ID
            notify(id, builder.build())
            cancel(id)
        }
    }

    private fun createNotificationChannel(mContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            with(NotificationManagerCompat.from(mContext)) {
                createNotificationChannel(
                    NotificationChannel(
                        mContext.getString(R.string.warning_notification_channel_id),
                        mContext.getString(R.string.warning_notification_channel_name),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = mContext.getString(R.string.warning_notification_channel_description)
                    })
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FIREBASE", token)
        Preferences.firebaseToken = token

        // TODO: send token to the server
    }
}