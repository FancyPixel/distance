package it.fancypixel.distance.ui.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import com.google.android.material.transition.MaterialContainerTransformSharedElementCallback
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import it.fancypixel.distance.R
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.ui.viewmodels.MainViewModel
import it.fancypixel.distance.utils.toast
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {

  private lateinit var viewModel: MainViewModel
  private val mainNavController: NavController? by lazy {
    Navigation.findNavController(
      this,
      R.id.content_fragment
    )
  }

  private val bluetoothBroadcastReceiver by lazy {
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
          when(val status = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
            BluetoothAdapter.STATE_OFF,
            BluetoothAdapter.STATE_TURNING_OFF,
            BluetoothAdapter.STATE_ON,
            BluetoothAdapter.STATE_TURNING_ON -> {
              viewModel.updateBluetoothStatus(status)
            }
          }
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    // Keep system bars (status bar, navigation bar) persistent throughout the transition.
    window.sharedElementsUseOverlay = false

    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_main)

    viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

    // Restart the Service
    viewModel.restartService()
  }

  override fun onResume() {
    super.onResume()

    if (Preferences.showIntro && mainNavController?.currentDestination?.id != R.id.onboardingFragment) {
      mainNavController?.navigate(R.id.onboardingFragment)
    }

    // Update the bluetooth status
    with(BluetoothAdapter.getDefaultAdapter()) {
      viewModel.updateBluetoothStatus(if (isEnabled) BluetoothAdapter.STATE_ON else BluetoothAdapter.STATE_OFF)
    }

    // Check location permission
    viewModel.isPermissionGranted.value = checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val intentFiler = IntentFilter().apply {
      addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
    }
    registerReceiver(bluetoothBroadcastReceiver, intentFiler)
  }

  override fun onStop() {
    unregisterReceiver(bluetoothBroadcastReceiver)
    super.onStop()
  }
}
