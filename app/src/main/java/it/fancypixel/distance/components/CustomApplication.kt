package it.fancypixel.distance.components

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.chibatching.kotpref.Kotpref
import it.fancypixel.distance.services.BeaconService
import org.altbeacon.beacon.powersave.BackgroundPowerSaver




class CustomApplication: Application() {

    lateinit var backgroundPowerSaver: BackgroundPowerSaver

    override fun onCreate() {
        super.onCreate()

        // Preferences
        Kotpref.init(this)

        // Dark theme
        AppCompatDelegate.setDefaultNightMode(Preferences.darkThemePreference)

        backgroundPowerSaver = BackgroundPowerSaver(this)
    }
}