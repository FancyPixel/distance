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
import it.fancypixel.distance.BuildConfig
import it.fancypixel.distance.R
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.components.events.NearbyBeaconEvent
import it.fancypixel.distance.global.Constants
import it.fancypixel.distance.ui.activities.MainActivity
import it.fancypixel.distance.utils.toast
import kotlinx.coroutines.*
import org.altbeacon.beacon.*
import org.altbeacon.beacon.service.RunningAverageRssiFilter
import org.greenrobot.eventbus.EventBus


class BeaconService : Service(), BeaconConsumer {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private val TAG = "MonitoringActivity"
    private val beaconManager by lazy { BeaconManager.getInstanceForApplication(this).apply { isRegionStatePersistenceEnabled = !BuildConfig.DEBUG } }
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

            if (!beaconManager.isBound(this@BeaconService)) {
                // Optimize the distance calculator
                BeaconManager.setRssiFilterImplClass(RunningAverageRssiFilter::class.java)
                RunningAverageRssiFilter.setSampleExpirationMilliseconds(5000L)

                // Start the service in foreground
                startForeground(NOTIFICATION_ID, getServiceNotificationBuilder().build())
                beaconManager.setEnableScheduledScanJobs(false)

                // Update th parser to handle the iBeacon format
                beaconManager.beaconParsers.clear()
                beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BEACON_LAYOUT))

                beaconManager.bind(this@BeaconService)
                Preferences.isServiceEnabled = true
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

    // ADVERTISING
    private fun startAdvertising() {
        val beacon = Beacon.Builder()
            .setId1(BEACON_ID)
            .setId2(Preferences.deviceMajor.toString())
            .setId3(Preferences.deviceMinor.toString())
            .setManufacturer(0x004c)
            .setTxPower(-59)
            .build()

        // Change the layout below for other beacon types
        beaconTransmitter.stopAdvertising()
        beaconTransmitter.startAdvertising(beacon, object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                toast(getString(R.string.generic_error))
                Preferences.isServiceEnabled = false
            }
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            }
        })
    }

    // BEACON HANDLER
    override fun onBeaconServiceConnect() {
        beaconManager.removeAllMonitorNotifiers()
        beaconManager.removeAllRangeNotifiers()

        beaconManager.addMonitorNotifier(object : MonitorNotifier {
            override fun didEnterRegion(region: Region?) {
                Log.i(TAG, "I just saw a beacon for the first time!")
            }

            override fun didExitRegion(region: Region?) {
                Log.i(TAG, "I no longer see a beacon")
            }

            override fun didDetermineStateForRegion(state: Int, region: Region?) {
                Log.i(TAG, "I have just switched from seeing/not seeing beacons: $state")
            }
        })

        beaconManager.addRangeNotifier { beacons, _ ->
            var isSomeoneNear = false
            for (beacon in beacons) {
                Log.d(TAG, "I see a beacon at ${beacon.distance} meters away.")
                EventBus.getDefault().post(NearbyBeaconEvent(beacon))
                isSomeoneNear = isSomeoneNear || beacon.distance < 2L
            }

            vibrateIf(isSomeoneNear)
        }

        startMonitoring()
    }

    private fun startMonitoring() {
        try {
            beaconManager.startMonitoringBeaconsInRegion(Region(REGION_TAG, null, null, null))
        } catch (e: RemoteException) {
            toast(getString(R.string.generic_error))
            stopBeaconService(this)
        }
    }

    private fun vibrateIf(isSomeoneNear: Boolean) {
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
                    delay(3000)
                    withContext(Dispatchers.Main) {
                        hasVibrated = false
                    }
                }
            }
        } else {
            v.cancel()
        }
    }

    private fun getServiceNotificationBuilder(): NotificationCompat.Builder {
        // Handle the notification channel
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
        val actionIntent: PendingIntent = PendingIntent.getBroadcast(this@BeaconService, 1, Intent(this@BeaconService, ToggleServiceReceiver::class.java).apply {
            action = Constants.ACTION_STOP_FOREGROUND_SERVICE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(EXTRA_NOTIFICATION_ID, 0)
            }
        }, 0)

        builder.addAction(R.drawable.ic_stat_check, getString(R.string.action_disable), actionIntent)

        // Main intent that open the activity
        builder.setContentIntent(PendingIntent.getActivity(this@BeaconService, 0, Intent(this@BeaconService, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT))

        return builder
    }

    override fun onDestroy() {
        Preferences.isServiceEnabled = false
        with(NotificationManagerCompat.from(this)) {
            cancel(NOTIFICATION_ID)
        }
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 364
        private const val REGION_TAG = "REGION_NEAR"
        private const val BEACON_ID = "A2B2265F-77F6-4C6A-82ED-297B366FC684"
        private const val BEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"

        fun startBeaconService(mContext: Context) {

            val startIntent = Intent(mContext, BeaconService::class.java)
            startIntent.action = Constants.ACTION_START_FOREGROUND_SERVICE
            mContext.startService(startIntent)
        }

        fun stopBeaconService(mContext: Context) {
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
