package it.fancypixel.distance.ui.viewmodels

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.chibatching.kotpref.livedata.asLiveData
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.services.BeaconService
import kotlinx.coroutines.*
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconTransmitter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Settings
    var darkThemeMode: LiveData<Int> = Preferences.asLiveData(Preferences::darkThemePreference)
    var tolerance: LiveData<Int> = Preferences.asLiveData(Preferences::tolerance)
    var debug: LiveData<Boolean> = Preferences.asLiveData(Preferences::debug)
    var notificationType: LiveData<Int> = Preferences.asLiveData(Preferences::notificationType)
    var batteryLevel: LiveData<Boolean> = Preferences.asLiveData(Preferences::useBatteryLevel)
    val settings: MutableLiveData<ArrayList<Any>> = MutableLiveData(ArrayList())

    var isServiceEnabled: LiveData<Boolean> = Preferences.asLiveData(Preferences::isServiceEnabled)

    // Nearby beacons list
    private val _nearbyBeacons: MutableLiveData<ArrayList<Any>> = MutableLiveData(arrayListOf())
    val nearbyBeacons: LiveData<ArrayList<Any>> = _nearbyBeacons
    private val beaconsRegisteredTime: HashMap<String, Job> = HashMap()

    // Warnings
    val showAdvertisingError: Boolean by lazy {
        BluetoothAdapter.getDefaultAdapter().isEnabled && BeaconTransmitter.checkTransmissionSupported(application) != BeaconTransmitter.SUPPORTED
    }
    val isPermissionGranted: MutableLiveData<Boolean> = MutableLiveData(false)
    val isBluetoothDisabled: MutableLiveData<Boolean> = MutableLiveData(false)

    fun updateBeaconList(updatedBeacon: Beacon) {
        _nearbyBeacons.value?.let {
            var found = false
            val list = it
            val iterator: MutableListIterator<Any> = list.listIterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (next is Beacon && next.id2 == updatedBeacon.id2) {
                    iterator.set(updatedBeacon)
                    found = true
                }
            }
            if (!found) {
                iterator.add(updatedBeacon)
            }


            // Start timer to check when a beacon leave the region
            val key = "${updatedBeacon.id2}"
            if (beaconsRegisteredTime.containsKey(key)) {
                beaconsRegisteredTime[key]!!.cancel()
            }
            beaconsRegisteredTime[key] = getTimeoutJob(key)

            // Update the beacons list
            _nearbyBeacons.value = list
        }
    }

    private fun getTimeoutJob(key: String) = viewModelScope.launch(Dispatchers.IO) {
        delay(10000)
        withContext(Dispatchers.Main) {
            _nearbyBeacons.value?.let { list ->
                val iterator: MutableListIterator<Any> = list.listIterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    if (next is Beacon && "${next.id2}" == key) {
                        iterator.remove()
                        break
                    }
                }
                _nearbyBeacons.value = list
            }
        }
    }

    fun startService() {
        if (Preferences.deviceUUID == "") {
            Preferences.deviceUUID = UUID.randomUUID().toString()
            // TODO: send to backend to check uniqueness
        }
        BeaconService.startBeaconService(getApplication())
    }

    fun stopService() = BeaconService.stopBeaconService(getApplication())

    fun toggleService(view: View?) {
        view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        if (Preferences.isServiceEnabled) stopService() else startService()
    }
    fun restartService() = if (Preferences.isServiceEnabled) startService() else null

    fun updateDeviceLocation() = BeaconService.updateDeviceLocation(getApplication())

    fun clearNearbyBeacons() {
        _nearbyBeacons.value = arrayListOf()
        beaconsRegisteredTime.clear()
    }

    fun updateBluetoothStatus(status: Int) = isBluetoothDisabled.postValue(status == BluetoothAdapter.STATE_OFF)
}
