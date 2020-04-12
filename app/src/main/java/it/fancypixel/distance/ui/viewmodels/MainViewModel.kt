package it.fancypixel.distance.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chibatching.kotpref.livedata.asLiveData
import it.fancypixel.distance.R
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.services.BeaconService
import org.altbeacon.beacon.Beacon


class MainViewModel(application: Application) : AndroidViewModel(application) {

    var darkThemeMode: LiveData<Int> = Preferences.asLiveData(Preferences::darkThemePreference)
    var isServiceEnabled: LiveData<Boolean> = Preferences.asLiveData(Preferences::isServiceEnabled)

    val _nearbyBeacons: MutableLiveData<ArrayList<Any>> = MutableLiveData(arrayListOf())
    val nearbyBeacons: LiveData<ArrayList<Any>> = _nearbyBeacons
    val settings: MutableLiveData<ArrayList<Any>> = MutableLiveData(ArrayList())

    fun updateBeaconList(updatedBeacon: Beacon) {
        _nearbyBeacons.value?.let {
            var found = false
            val list = it
            val iterator: MutableListIterator<Any> = list.listIterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (next is Beacon && next.id2 == updatedBeacon.id2 && next.id3 == updatedBeacon.id3) {
                    iterator.set(updatedBeacon)
                    found = true
                }
            }
            if (!found) {
                iterator.add(updatedBeacon)
            }

            _nearbyBeacons.value = list
        }
    }

    fun startService() {
        BeaconService.startService(getApplication())
    }

    fun stopService() {
        BeaconService.stopService(getApplication())
    }

    fun toggleService() {
        if (Preferences.isServiceEnabled) {
            BeaconService.stopService(getApplication())
        } else {
            BeaconService.startService(getApplication())
        }
    }

    fun clearNearbyBeacons() {
        _nearbyBeacons.value = arrayListOf()
    }
}
