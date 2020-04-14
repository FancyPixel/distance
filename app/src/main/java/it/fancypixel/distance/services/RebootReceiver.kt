package it.fancypixel.distance.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import it.fancypixel.distance.components.Preferences

class RebootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val prefs = Preferences
                if (Preferences.isServiceEnabled) {
                    BeaconService.startBeaconService(context)
                }
            }
        }
    }
}
