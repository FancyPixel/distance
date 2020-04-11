package it.fancypixel.distance.ui.activities

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import it.fancypixel.distance.R
import it.fancypixel.distance.services.BeaconService
import it.fancypixel.distance.ui.viewmodels.MainViewModel
import it.fancypixel.distance.utils.toast


class MainActivity : AppCompatActivity() {

  private lateinit var viewModel: MainViewModel
  private val mainNavController: NavController? by lazy {
    Navigation.findNavController(
      this,
      R.id.content_fragment
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

    viewModel.isServiceEnabled.observe(this, Observer {
      if (it) {
        BeaconService.startService(this)
      } else {
        BeaconService.stopService(this)
      }
    })
  }

  override fun onResume() {
    super.onResume()
    Dexter.withContext(this@MainActivity)
      .withPermissions(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
      ).withListener(object: MultiplePermissionsListener {
        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
          report?.let {
            if (report.areAllPermissionsGranted()){
//              toast("OK")
            }
          }
        }
        override fun onPermissionRationaleShouldBeShown(
          permissions: MutableList<PermissionRequest>?,
          token: PermissionToken?
        ) {
          // Remember to invoke this method when the custom rationale is closed
          // or just by default if you don't want to use any custom rationale.
          token?.continuePermissionRequest()
        }
      })
      .withErrorListener {
        toast(it.name)
      }
      .check()
  }
}
