package it.fancypixel.distance.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.global.Constants

class ToggleServiceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constants.ACTION_STOP_FOREGROUND_SERVICE) {
            BeaconService.stopBeaconService(context)
        }

        if (intent.action == Constants.ACTION_START_FOREGROUND_SERVICE) {
            BeaconService.startBeaconService(context)
        }

        if (intent.action == Constants.ACTION_CHANGE_DEVICE_LOCATION_TO_POCKET) {
            Preferences.deviceLocation = Constants.PREFERENCE_DEVICE_LOCATION_POCKET
            BeaconService.updateNotification(context)
        }

        if (intent.action == Constants.ACTION_CHANGE_DEVICE_LOCATION_TO_DESK) {
            Preferences.deviceLocation = Constants.PREFERENCE_DEVICE_LOCATION_DESK
            BeaconService.updateNotification(context)
        }

        if (intent.action == Constants.ACTION_CHANGE_DEVICE_LOCATION_TO_BACKPACK) {
            Preferences.deviceLocation = Constants.PREFERENCE_DEVICE_LOCATION_BACKPACK
            BeaconService.updateNotification(context)
        }
    }
}
