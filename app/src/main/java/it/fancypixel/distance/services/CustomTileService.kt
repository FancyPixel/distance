package it.fancypixel.distance.services

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import it.fancypixel.distance.R
import it.fancypixel.distance.components.Preferences

@RequiresApi(Build.VERSION_CODES.N)
class CustomTileService: TileService(){

    override fun onClick() {
        super.onClick()

        val tile = qsTile
        if (Preferences.isServiceEnabled) {
            BeaconService.stopService(this)
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.service_disabled)
            }
        } else {
            BeaconService.startService(this)
            tile.state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.service_enabled)
            }
        }
        tile.updateTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()

        updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()

        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile
        tile.state = if (Preferences.isServiceEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(if (Preferences.isServiceEnabled) R.string.service_enabled else R.string.service_disabled)
        }
        tile.updateTile()
    }
}