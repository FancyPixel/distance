package it.fancypixel.distance.services

import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
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
    private val beaconManager by lazy { BeaconManager.getInstanceForApplication(this).apply { isRegionStatePersistenceEnabled = false } }
    private val beaconParser by lazy { BeaconParser().setBeaconLayout(BEACON_LAYOUT) }
    private val beaconTransmitter by lazy { BeaconTransmitter(this, beaconParser) }

    private val bluetoothBroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    updateServiceNotification()
                }
            }
        }
    }

    private var hasBeenNotified = false

    // Get instance of Vibrator from current Context
    private val v: Vibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    private val ringtone by lazy { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 500) }

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
                beaconManager.enableForegroundServiceScanning(getServiceNotificationBuilder().build(), NOTIFICATION_ID)
                beaconManager.setEnableScheduledScanJobs(false)

                with(NotificationManagerCompat.from(this)) {
                    notify(NOTIFICATION_ID, getServiceNotificationBuilder().build())
                }
                beaconManager.backgroundMode = true

                // Update th parser to handle the iBeacon format
                beaconManager.beaconParsers.clear()
                beaconManager.beaconParsers.add(beaconParser)

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
        } else if (intent.action == Constants.ACTION_UPDATE_THE_SERVICE_NOTIFICATION) {
            with(NotificationManagerCompat.from(this)) {
                notify(NOTIFICATION_ID, getServiceNotificationBuilder().build())
            }
        }
        return START_NOT_STICKY
    }

    private fun startAdvertising() {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager?
        val batLevel = bm!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val beacon = Beacon.Builder()
            .setId1(BEACON_ID)
            .setId2(Preferences.deviceMajor.toString())
            .setId3("${Preferences.deviceLocation}${"%03d".format(batLevel)}")
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

    override fun onBeaconServiceConnect() {
        beaconManager.removeAllMonitorNotifiers()

        beaconManager.addMonitorNotifier(object : MonitorNotifier {
            override fun didEnterRegion(region: Region?) {
                Log.i(TAG, "I just saw a beacon for the first time!")
            }

            override fun didExitRegion(region: Region?) {
                Log.i(TAG, "I no longer see a beacon.")
            }

            override fun didDetermineStateForRegion(state: Int, region: Region?) {
                Log.i(TAG, "I have just switched from seeing/not seeing beacons: $state")

                beaconManager.backgroundBetweenScanPeriod = if (state > 0) 1900L else 10000L
                beaconManager.backgroundScanPeriod = if (state > 0) 1100L else 1100L
                beaconManager.startRangingBeaconsInRegion(Region(REGION_TAG, Identifier.parse(BEACON_ID), null, null))

                beaconManager.removeAllRangeNotifiers()
                beaconManager.addRangeNotifier { beacons, _ ->
                    var isSomeoneNear = false
                    for (beacon in beacons) {
                        Log.d(TAG, "I see a beacon at ${beacon.distance} meters away with a ${beacon.id3.toInt() % 1000}% battery.")
                        EventBus.getDefault().post(NearbyBeaconEvent(beacon))

                        // Device location error
                        val deviceLocationError = when (Preferences.deviceLocation) {
                            Constants.PREFERENCE_DEVICE_LOCATION_DESK -> 0.0
                            Constants.PREFERENCE_DEVICE_LOCATION_POCKET -> 2.0
                            Constants.PREFERENCE_DEVICE_LOCATION_BACKPACK -> 3.0
                            else -> 0.0
                        }

                        val transmittingDeviceLocationError = when (beacon.id3.toInt() - beacon.id3.toInt() % 1000) {
                            Constants.PREFERENCE_DEVICE_LOCATION_DESK -> 0.0
                            Constants.PREFERENCE_DEVICE_LOCATION_POCKET -> 2.0
                            Constants.PREFERENCE_DEVICE_LOCATION_BACKPACK -> 3.0
                            else -> 0.0
                        }

                        // Tolerance error
                        val toleranceError = when (Preferences.tolerance) {
                            Constants.PREFERENCE_TOLERANCE_LOW -> -1.0
                            Constants.PREFERENCE_TOLERANCE_MIN -> -0.5
                            Constants.PREFERENCE_TOLERANCE_DEFAULT -> 0.0
                            Constants.PREFERENCE_TOLERANCE_HIGH -> 0.5
                            Constants.PREFERENCE_TOLERANCE_MAX -> 1.0
                            else -> 0.0
                        }

                        // Battery level error
                        val level = beacon.id3.toInt() % 1000
                        val batteryLevelError = (100 - level) / 100 * 10.0

                        val calculatedMinDistance = 2.0 + deviceLocationError + transmittingDeviceLocationError + toleranceError + batteryLevelError
//                        toast("Distance: $deviceLocationError + $toleranceError + $batteryLevelError = $calculatedMinDistance")

                        isSomeoneNear = isSomeoneNear || beacon.distance < calculatedMinDistance
                    }

                    notifyIf(isSomeoneNear)
                }
            }
        })

        try {
            beaconManager.startMonitoringBeaconsInRegion(Region(REGION_TAG, Identifier.parse(BEACON_ID), null, null))
        } catch (e: RemoteException) {
            toast(getString(R.string.generic_error))
            stopBeaconService(this)
        }
    }

    private fun notifyIf(isSomeoneNear: Boolean) {
        if (isSomeoneNear) {
            if (!hasBeenNotified) {
                if (!isInteractive()) {
                    wakeUpThePhone()
                }
                hasBeenNotified = true

                if (Preferences.notificationType != Constants.PREFERENCE_NOTIFICATION_TYPE_ONLY_VIBRATE) {
                    try {
                        ringtone.stopTone()
                        ringtone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (Preferences.notificationType != Constants.PREFERENCE_NOTIFICATION_TYPE_ONLY_SOUND) {
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
                }

                GlobalScope.launch(Dispatchers.IO) {
                    delay(3000)
                    withContext(Dispatchers.Main) {
                        hasBeenNotified = false
                    }
                }

            }
        } else {
            v.cancel()
            ringtone.stopTone()
        }


    }

    private fun isInteractive(): Boolean {
        val powerManager =
            getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isInteractive
    }

    private fun wakeUpThePhone() {
        val builder = NotificationCompat.Builder(this@BeaconService, getString(R.string.wakeup_notification_channel_id))
            .setSmallIcon(R.drawable.ic_stat_person)
            .setContentTitle("Someone is near")
            .setContentText("Fly out, away you fool!")
            .setSound(null)
            .setColor(ContextCompat.getColor(this@BeaconService, R.color.colorAccent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID + 1, builder.build())
            cancel(NOTIFICATION_ID + 1)
        }
    }

    private fun getServiceNotificationBuilder(): NotificationCompat.Builder {
        // Handle the notification channel
        createNotificationChannel(this@BeaconService)

        var title = getString(R.string.notification_title)
        var subtitle = getString(R.string.notification_message)

        with(BluetoothAdapter.getDefaultAdapter()) {
            if (!isEnabled) {
                title = this@BeaconService.getString(R.string.notification_title_with_ble_off)
                subtitle = this@BeaconService.getString(R.string.notification_message_with_ble_off)
            }
        }

        val builder = NotificationCompat.Builder(this@BeaconService, getString(R.string.ongoing_notification_channel_id))
            .setSmallIcon(R.drawable.ic_stat_person)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setColor(ContextCompat.getColor(this@BeaconService, R.color.colorAccent))
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Action to change the position of the device
        val insidePocket = Preferences.deviceLocation == Constants.PREFERENCE_DEVICE_LOCATION_POCKET
        val changeDevicePositionIntent: PendingIntent = PendingIntent.getBroadcast(this@BeaconService, 2, Intent(this@BeaconService, ToggleServiceReceiver::class.java).apply {
            action = if (insidePocket) Constants.ACTION_CHANGE_DEVICE_LOCATION_TO_DESK else Constants.ACTION_CHANGE_DEVICE_LOCATION_TO_POCKET
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(EXTRA_NOTIFICATION_ID, 0)
            }
        }, 0)

        builder.addAction(R.drawable.ic_stat_check, if (insidePocket) getString(R.string.action_move_to_desk) else getString(R.string.action_move_to_pocket), changeDevicePositionIntent)

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

    private fun updateServiceNotification() {
        if (Preferences.isServiceEnabled) {
            with(NotificationManagerCompat.from(this)) {
                notify(NOTIFICATION_ID, getServiceNotificationBuilder().build())
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val intentFiler = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(bluetoothBroadcastReceiver, intentFiler)
    }

    override fun onDestroy() {
        Preferences.isServiceEnabled = false
        with(NotificationManagerCompat.from(this)) {
            cancel(NOTIFICATION_ID)
        }
        unregisterReceiver(bluetoothBroadcastReceiver)
        ringtone.release()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 364
        const val REGION_TAG = "REGION_NEAR"
        const val BEACON_ID = "A2B2265F-77F6-4C6A-82ED-297B366FC684"
        const val BEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"

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

        fun updateNotification(mContext: Context) {
            val stopIntent = Intent(mContext, BeaconService::class.java)
            stopIntent.action = Constants.ACTION_UPDATE_THE_SERVICE_NOTIFICATION
            mContext.startService(stopIntent)
        }

        private fun createNotificationChannel(mContext: Context) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                with(NotificationManagerCompat.from(mContext)) {
                    createNotificationChannel(
                        NotificationChannel(
                            mContext.getString(R.string.ongoing_notification_channel_id),
                            mContext.getString(R.string.ongoing_notification_channel_name),
                            NotificationManager.IMPORTANCE_LOW
                        ).apply {
                            description = mContext.getString(R.string.ongoing_notification_channel_description)
                        })

                    createNotificationChannel(
                        NotificationChannel(
                            mContext.getString(R.string.wakeup_notification_channel_id),
                            mContext.getString(R.string.wakeup_notification_channel_name),
                            NotificationManager.IMPORTANCE_DEFAULT
                        ).apply {
                            description = mContext.getString(R.string.wakeup_notification_channel_description)
                        })
                }
            }
        }
    }
}
