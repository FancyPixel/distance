package it.fancypixel.distance.ui.fragments

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import it.fancypixel.distance.R
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.components.events.DeviceLocationUpdateEvent
import it.fancypixel.distance.components.events.NearbyBeaconEvent
import it.fancypixel.distance.databinding.MainFragmentBinding
import it.fancypixel.distance.global.Constants
import it.fancypixel.distance.services.ToggleServiceReceiver
import it.fancypixel.distance.ui.activities.MainActivity
import it.fancypixel.distance.ui.viewmodels.MainViewModel
import it.fancypixel.distance.utils.toPixel
import it.fancypixel.distance.utils.toast
import kotlinx.android.synthetic.main.main_fragment.*
import net.idik.lib.slimadapter.SlimAdapter
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconTransmitter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.Exception


class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: SlimAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis.create(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis.create(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        val binding = DataBindingUtil.inflate<MainFragmentBinding>(inflater, R.layout.main_fragment, container, false)

        subscribeUi(binding, viewModel.darkThemeMode, viewModel.isServiceEnabled, viewModel.nearbyBeacons, viewModel.bluetoothStatus, viewModel.isPermissionGranted, viewModel.debug)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        action_settings.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_mainFragment_to_settingsFragment)
        }

        action_ble_error.setOnClickListener {
            MaterialAlertDialogBuilder(requireActivity(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setCancelable(true)
                .setTitle(getString(R.string.advertising_not_available_title))
                .setMessage(resources.getString(R.string.advertising_not_possible_message))
                .setPositiveButton(resources.getString(android.R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        action_request_permission.setOnClickListener {
            Dexter.withContext(requireActivity())
                .withPermissions(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ).withListener(object: MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            if (report.areAllPermissionsGranted()){
                                viewModel.isPermissionGranted.value = true
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
                    viewModel.isPermissionGranted.value = false
                }
                .check()
        }

        beacons_list.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            requireActivity(),
            androidx.recyclerview.widget.LinearLayoutManager.VERTICAL,
            false
        )
        beacons_list.isNestedScrollingEnabled = true
        beacons_list.isVerticalScrollBarEnabled = false

        adapter = SlimAdapter.create()
        adapter
            .register<Beacon>(R.layout.nearby_beacons_item_layout) { item, injector ->
                val position = when ((item.id3.toInt() - item.id3.toInt() % 1000) / 1000) {
                    Constants.PREFERENCE_DEVICE_LOCATION_DESK -> getString(R.string.settings_subtitle_device_location_desk)
                    Constants.PREFERENCE_DEVICE_LOCATION_POCKET -> getString(R.string.settings_subtitle_device_location_pocket)
                    Constants.PREFERENCE_DEVICE_LOCATION_BACKPACK -> getString(R.string.settings_subtitle_device_location_backpack)
                    else -> getString(R.string.device_location_unkown)
                }
                val batteryLevel = item.id3.toInt() % 1000
                injector
                    .text(R.id.title, "${item.id1}")
                    .text(R.id.subtitle, "${getString(R.string.battery)}: $batteryLevel%")
                    .text(R.id.subtitle2, position)
                    .text(R.id.distance, "~%.2fM".format(item.distance))
            }
            .register<String>(R.layout.settings_header_layout) { header, injector ->
                injector.text(R.id.header, header)
            }
            .attachTo(beacons_list)

        BottomSheetBehavior.from(bottom_sheet).addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottom_sheet.shapeAppearanceModel =
                        bottom_sheet.shapeAppearanceModel.withCornerSize(0f)
                } else {
                    bottom_sheet.shapeAppearanceModel = ShapeAppearanceModel.builder(requireContext(), R.style.ShapeAppearanceOverlay_MaterialCardView_Cut, 0).build()
                }
            }

        })
    }


    private fun subscribeUi(
        binding: MainFragmentBinding,
        darkThemeMode: LiveData<Int>,
        isServiceEnabled: LiveData<Boolean>,
        nearbyBeacons: LiveData<ArrayList<Any>>,
        bluetoothStatus: MutableLiveData<Boolean>,
        isPermissionGranted: MutableLiveData<Boolean>,
        debug: LiveData<Boolean>
    ) {
        darkThemeMode.observe(viewLifecycleOwner, Observer {
            AppCompatDelegate.setDefaultNightMode(it)
        })

        binding.isAdvertisingPossible = !viewModel.showAdvertisingError

        nearbyBeacons.observe(viewLifecycleOwner, Observer {
            if (Preferences.isServiceEnabled) {
                header.text = getString(
                    R.string.header_nearby_beacons,
                    it.size
                )
            }
            adapter.updateData(it)
        })

        isServiceEnabled.observe(viewLifecycleOwner, Observer {
            if (!it) {
                viewModel.clearNearbyBeacons()
                header.text = getString(R.string.enabled_the_service_message)
            } else if (viewModel.nearbyBeacons.value?.size == 0) {
                header.text = getString(R.string.empty_nearby_beacons_list_message)
            } else {
                header.text = getString(
                    R.string.header_nearby_beacons,
                    viewModel.nearbyBeacons.value?.size ?: 0
                )
            }

            if (it) {
                ripple.startRippleAnimation()
            } else {
                ripple.stopRippleAnimation()
            }
        })

        isPermissionGranted.observe(viewLifecycleOwner, Observer {
            binding.isPermissionsGranted = it
        })

        debug.observe(viewLifecycleOwner, Observer {
            beacons_list.visibility = if (it) View.VISIBLE else View.GONE
            binding.isDebugModeEnabled = it
        })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNearbyBeaconEvent(event: NearbyBeaconEvent) {
        viewModel.updateBeaconList(event.nearbyBeacon)
    }

    override fun onStart() {
        EventBus.getDefault().register(this)
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }
}
