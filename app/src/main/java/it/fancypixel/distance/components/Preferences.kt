package it.fancypixel.distance.components

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate.*
import com.chibatching.kotpref.KotprefModel

object Preferences : KotprefModel() {
    var isServiceEnabled by booleanPref(default = false)
    var darkThemePreference by intPref(default = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) MODE_NIGHT_FOLLOW_SYSTEM else MODE_NIGHT_AUTO_BATTERY)
}
