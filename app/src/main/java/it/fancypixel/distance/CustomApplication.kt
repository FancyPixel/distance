package it.fancypixel.distance

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.chibatching.kotpref.Kotpref
import com.google.firebase.iid.FirebaseInstanceId
import io.realm.Realm
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.db.BumpRepository
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.powersave.BackgroundPowerSaver


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
        BumpRepository().clearOldBumps()


        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
            Preferences.firebaseToken = it.token
            Log.d("FIREBASE",
                Preferences.firebaseToken
            )

            // TODO: send token to the server
        }
    }
}