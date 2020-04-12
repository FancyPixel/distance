package it.fancypixel.distance.components

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.chibatching.kotpref.Kotpref
import it.fancypixel.distance.services.BeaconService

class CustomApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        // Preferences
        Kotpref.init(this)

        // Dark theme
        AppCompatDelegate.setDefaultNightMode(Preferences.darkThemePreference)

        // Service
        if (Preferences.isServiceEnabled) {
            BeaconService.startService(this)
        } else {
            BeaconService.stopService(this)
        }
    }
}