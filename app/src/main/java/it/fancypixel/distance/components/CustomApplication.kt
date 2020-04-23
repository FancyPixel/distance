package it.fancypixel.distance.components

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.chibatching.kotpref.Kotpref
import io.realm.Realm
import it.fancypixel.distance.BuildConfig
import it.fancypixel.distance.components.events.NearbyBeaconEvent
import it.fancypixel.distance.models.Bump
import it.fancypixel.distance.services.BeaconService
import kotlinx.coroutines.*
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.Identifier
import org.altbeacon.beacon.Region
import org.altbeacon.beacon.powersave.BackgroundPowerSaver
import org.altbeacon.beacon.service.RunningAverageRssiFilter
import org.altbeacon.beacon.startup.BootstrapNotifier
import org.altbeacon.beacon.startup.RegionBootstrap
import org.greenrobot.eventbus.EventBus
import java.util.*


class CustomApplication: Application() {

    lateinit var backgroundPowerSaver: BackgroundPowerSaver

    override fun onCreate() {
        super.onCreate()

        // Realm
        Realm.init(this)

        // Preferences
        Kotpref.init(this)

        // Dark theme
        AppCompatDelegate.setDefaultNightMode(Preferences.darkThemePreference)

        BeaconManager.setDebug(org.altbeacon.beacon.BuildConfig.DEBUG)
        backgroundPowerSaver = BackgroundPowerSaver(this)

        // Clean old bumps
        Realm.getDefaultInstance().executeTransactionAsync { realm ->
            val lastUseFullTime = Calendar.getInstance()
            lastUseFullTime.set(Calendar.HOUR_OF_DAY, 0)
            lastUseFullTime.set(Calendar.MINUTE, 0)
            lastUseFullTime.set(Calendar.SECOND, 0)
            lastUseFullTime.set(Calendar.MILLISECOND, 0)
            lastUseFullTime.add(Calendar.DAY_OF_YEAR, -21)

            val list = realm.where(Bump::class.java).lessThan("date", lastUseFullTime.timeInMillis).findAll()
            list.deleteAllFromRealm()
        }
    }
}