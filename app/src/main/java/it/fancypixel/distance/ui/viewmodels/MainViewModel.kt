package it.fancypixel.distance.ui.viewmodels

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chibatching.kotpref.livedata.asLiveData
import it.fancypixel.distance.components.Preferences


class MainViewModel(application: Application) : AndroidViewModel(application) {

    var darkThemeMode: LiveData<Int> = Preferences.asLiveData(Preferences::darkThemePreference)
    var isServiceEnabled: LiveData<Boolean> = Preferences.asLiveData(Preferences::isServiceEnabled)

    val settings: MutableLiveData<ArrayList<Any>> = MutableLiveData(ArrayList())

    fun startService() {
        Preferences.isServiceEnabled = true
    }

}
