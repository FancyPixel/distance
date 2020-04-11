package it.fancypixel.distance.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import it.fancypixel.distance.global.Constants

class DisableServiceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constants.ACTION_STOP_FOREGROUND_SERVICE) {
            BeaconService.stopService(context)
        }
    }
}
