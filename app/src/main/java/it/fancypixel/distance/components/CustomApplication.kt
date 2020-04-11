package it.fancypixel.distance.components

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.chibatching.kotpref.Kotpref

class CustomApplication: Application() {
    override fun onCreate() {

        // Preferences
        Kotpref.init(this)

        AppCompatDelegate.setDefaultNightMode(Preferences.darkThemePreference)
        super.onCreate()
    }
}