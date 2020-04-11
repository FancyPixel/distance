package it.fancypixel.distance.components

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

class PreferencesLiveData(private val sharedPreferences: SharedPreferences) : LiveData<Map<String, *>?>() {

    private val mPreferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs: SharedPreferences?, key: String? ->
            value = prefs?.all
        }

    override fun onActive() {
        super.onActive()
        value = sharedPreferences.all
        sharedPreferences.registerOnSharedPreferenceChangeListener(mPreferenceListener)
    }

    override fun onInactive() {
        super.onInactive()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(mPreferenceListener)
    }
}