package it.fancypixel.distance.components

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate.*
import com.chibatching.kotpref.KotprefModel
import it.fancypixel.distance.global.Constants

object Preferences : KotprefModel() {
    var isServiceEnabled by booleanPref(default = false)
    var darkThemePreference by intPref(default = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) MODE_NIGHT_FOLLOW_SYSTEM else MODE_NIGHT_AUTO_BATTERY)
    var deviceMajor by intPref(default = -1)
    var deviceMinor by intPref(default = -1)
    var notificationType by intPref(default = Constants.PREFERENCE_NOTIFICATION_TYPE_ONLY_VIBRATE)
    var tolerance by intPref(default = Constants.PREFERENCE_TOLERANCE_DEFAULT)
    var deviceLocation by intPref(default = Constants.PREFERENCE_DEVICE_LOCATION_POCKET)
}
