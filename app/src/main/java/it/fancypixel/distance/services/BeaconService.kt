package it.fancypixel.distance.services

import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import it.fancypixel.distance.R
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.global.Constants
import it.fancypixel.distance.ui.activities.MainActivity
import it.fancypixel.distance.utils.toast
import kotlinx.coroutines.*
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.BeaconTransmitter
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.Region
import java.util.*


class BeaconService : Service(), BeaconConsumer {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private val TAG = "MonitoringActivity"
    private val beaconManager by lazy { BeaconManager.getInstanceForApplication(this) }
    private val beaconParser by lazy { BeaconParser().setBeaconLayout(BEACON_LAYOUT) }
    private val beaconTransmitter by lazy { BeaconTransmitter(this, beaconParser) }

    private var hasVibrated = false


    // Get instance of Vibrator from current Context
    private val v: Vibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val isAdvertisingPossible = BeaconTransmitter.checkTransmissionSupported(this) == BeaconTransmitter.SUPPORTED

        if (intent.action == Constants.ACTION_START_FOREGROUND_SERVICE) {
            if (isAdvertisingPossible) {
                startAdvertising()
            }

            with(NotificationManagerCompat.from(this)) {
                createNotificationChannel(this@BeaconService)

                val builder = NotificationCompat.Builder(this@BeaconService, getString(R.string.channel_id))
                    .setSmallIcon(R.drawable.ic_stat_person)
                    .setContentTitle(getString(R.string.notification_title))
                    .setContentText(getString(R.string.notification_message))
                    .setColor(ContextCompat.getColor(this@BeaconService, R.color.colorAccent))
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)

                // Action to disable the app
                val actionIntent: PendingIntent = PendingIntent.getBroadcast(this@BeaconService, 1, Intent(this@BeaconService, DisableServiceReceiver::class.java).apply {
                    action = Constants.ACTION_STOP_FOREGROUND_SERVICE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        putExtra(EXTRA_NOTIFICATION_ID, 0)
                    }
                }, 0)

                builder.addAction(R.drawable.ic_stat_check, getString(R.string.action_disable), actionIntent)

                // Main intent that open the activity
                builder.setContentIntent(PendingIntent.getActivity(this@BeaconService, 0, Intent(this@BeaconService, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT))

                if (!beaconManager.isBound(this@BeaconService)) {
                    startForeground(NOTIFICATION_ID, builder.build())
                    beaconManager.setEnableScheduledScanJobs(false)
                    beaconManager.bind(this@BeaconService)

                    Preferences.isServiceEnabled = true
                }
            }

        } else if (intent.action == Constants.ACTION_STOP_FOREGROUND_SERVICE) {
            // Unbind the BLE receiver and stop the advertising
            if (isAdvertisingPossible) {
                beaconTransmitter.stopAdvertising()
            }
            beaconManager.unbind(this)

            stopForeground(true)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startAdvertising() {
        val beacon = Beacon.Builder()
            .setId1(BEACON_ID)
            .setId2(UUID.randomUUID().toString())
            .setId3(UUID.randomUUID().toString())
            .setManufacturer(0x0118) // Radius Networks.  Change this for other beacon layouts
            .setTxPower(-59)
            .setDataFields(listOf(0L)) // Remove this for beacon layouts without d: fields
            .build()

        // Change the layout below for other beacon types
        beaconTransmitter.stopAdvertising()
        beaconTransmitter.startAdvertising(beacon, object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                toast("Error: $errorCode")
                Preferences.isServiceEnabled = false
            }
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            }
        })
    }

    override fun onDestroy() {
        Preferences.isServiceEnabled = false
        with(NotificationManagerCompat.from(this)) {
            cancel(NOTIFICATION_ID)
        }
        super.onDestroy()
    }

    override fun onBeaconServiceConnect() {
        beaconManager.removeAllMonitorNotifiers()
        beaconManager.addMonitorNotifier(object : MonitorNotifier {
            override fun didEnterRegion(region: Region?) {
                Log.i(TAG, "I just saw an beacon for the first time!")
                beaconManager.startRangingBeaconsInRegion(Region(BEACON_ID, null, null, null))

                beaconManager.addRangeNotifier { beacons, _ ->
                    var isSomeoneNear = false
                    for (beacon in beacons) {
                        Log.d(TAG, "I see a beacon at ${beacon.distance} meters away.")
                        isSomeoneNear = isSomeoneNear || beacon.distance < 2L
                    }

                    if (isSomeoneNear) {
                        if (!hasVibrated) {
                            hasVibrated = true
                            val pattern = longArrayOf(0, 100, 100, 100, 100, 100, 3000)
                            if (Build.VERSION.SDK_INT >= 26) {
                                v.vibrate(
                                    VibrationEffect.createWaveform(
                                        pattern,
                                        VibrationEffect.DEFAULT_AMPLITUDE
                                    )
                                )
                            } else {
                                v.vibrate(pattern, -1)
                            }


                            GlobalScope.launch(Dispatchers.IO) {
                                delay(2000)
                                withContext(Dispatchers.Main) {
                                    hasVibrated = false
                                }
                            }
                        }
                    } else {
                        v.cancel()
                    }
                }
            }

            override fun didExitRegion(region: Region?) {
                Log.i(TAG, "I no longer see an beacon")
            }

            override fun didDetermineStateForRegion(state: Int, region: Region?) {
                Log.i(TAG, "I have just switched from seeing/not seeing beacons: $state")
            }
        })

        try {
            beaconManager.startMonitoringBeaconsInRegion(Region(BEACON_ID, null, null, null))
        } catch (e: RemoteException) {

        }
    }

    companion object {
        private const val NOTIFICATION_ID = 364
        private const val BEACON_ID = "A2B2265F-77F6-4C6A-82ED-297B366FC684"
        private const val BEACON_LAYOUT = "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"

        fun startService(mContext: Context) {
            val startIntent = Intent(mContext, BeaconService::class.java)
            startIntent.action = Constants.ACTION_START_FOREGROUND_SERVICE
            mContext.startService(startIntent)
        }

        fun stopService(mContext: Context) {
            val stopIntent = Intent(mContext, BeaconService::class.java)
            stopIntent.action = Constants.ACTION_STOP_FOREGROUND_SERVICE
            mContext.startService(stopIntent)
        }

        private fun createNotificationChannel(mContext: Context) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = mContext.getString(R.string.channel_name)
                val descriptionText = mContext.getString(R.string.channel_description)
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel(mContext.getString(R.string.channel_id), name, importance).apply {
                    description = descriptionText
                }
                // Register the channel with the system
                val notificationManager = NotificationManagerCompat.from(mContext)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
