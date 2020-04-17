package it.fancypixel.distance.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.components.events.DeviceLocationUpdateEvent
import it.fancypixel.distance.global.Constants
import org.greenrobot.eventbus.EventBus

class ToggleServiceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constants.ACTION_STOP_FOREGROUND_SERVICE) {
            BeaconService.stopBeaconService(context)
        }

        if (intent.action == Constants.ACTION_START_FOREGROUND_SERVICE) {
            BeaconService.startBeaconService(context)
        }
    }
}
