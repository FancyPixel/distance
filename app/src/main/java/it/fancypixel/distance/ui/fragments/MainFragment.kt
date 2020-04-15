package it.fancypixel.distance.ui.fragments

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import it.fancypixel.distance.R
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.components.events.NearbyBeaconEvent
import it.fancypixel.distance.databinding.MainFragmentBinding
import it.fancypixel.distance.ui.activities.MainActivity
import it.fancypixel.distance.ui.viewmodels.MainViewModel
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        val binding = DataBindingUtil.inflate<MainFragmentBinding>(inflater, R.layout.main_fragment, container, false)

        subscribeUi(binding, viewModel.darkThemeMode, viewModel.isServiceEnabled, viewModel.nearbyBeacons, viewModel.bluetoothStatus, viewModel.isPermissionGranted)
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
            MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setCancelable(true)
                .setTitle(getString(R.string.advertising_not_available_title))
                .setMessage(resources.getString(R.string.advertising_not_possible_message))
                .setPositiveButton(resources.getString(android.R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        action_turn_on_bluetooth.setOnClickListener {
            try {
                BluetoothAdapter.getDefaultAdapter().enable()
            } catch (ex: Exception) {
                activity?.toast(getString(R.string.generic_error))
            }
        }

        action_request_permission.setOnClickListener {
            Dexter.withContext(activity!!)
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
            activity!!,
            androidx.recyclerview.widget.LinearLayoutManager.VERTICAL,
            false
        )

        adapter = SlimAdapter.create()
        adapter
            .register<Beacon>(R.layout.nearby_beacons_item_layout) { item, injector ->
                injector
                    .text(R.id.title, item.id1.toString())
                    .text(R.id.subtitle, "ID2: ${item.id2}")
                    .text(R.id.subtitle2, "ID3: ${item.id3}")
                    .text(R.id.distance, "%.2fM".format(item.distance))
            }
            .register<String>(R.layout.settings_header_layout) { header, injector ->
                injector.text(R.id.header, header)
            }
            .attachTo(beacons_list)
    }


    private fun subscribeUi(
        binding: MainFragmentBinding,
        darkThemeMode: LiveData<Int>,
        isServiceEnabled: LiveData<Boolean>,
        nearbyBeacons: LiveData<ArrayList<Any>>,
        bluetoothStatus: MutableLiveData<Boolean>,
        isPermissionGranted: MutableLiveData<Boolean>
    ) {
        darkThemeMode.observe(viewLifecycleOwner, Observer {
            AppCompatDelegate.setDefaultNightMode(it)
        })

        binding.isAdvertisingPossible = !viewModel.showAdvertisingError

        nearbyBeacons.observe(viewLifecycleOwner, Observer {
            if (Preferences.isServiceEnabled) {
                if (it.size > 0) {
                    adapter.updateData(
                        ArrayList(
                            listOf(
                                getString(
                                    R.string.header_nearby_beacons,
                                    it.size
                                )
                            ) + it
                        )
                    )
                } else {
                    adapter.updateData(arrayListOf(getString(R.string.empty_nearby_beacons_list_message)))
                }
            } else {
                adapter.updateData(arrayListOf(getString(R.string.enabled_the_service_message)))
            }
        })

        isServiceEnabled.observe(viewLifecycleOwner, Observer {
            if (!it) {
                viewModel.clearNearbyBeacons()
            } else if (viewModel.nearbyBeacons.value?.size == 0) {
                adapter.updateData(arrayListOf(getString(R.string.empty_nearby_beacons_list_message)))
            }
        })

        bluetoothStatus.observe(viewLifecycleOwner, Observer {
            binding.isBluetoothDisabled = it
        })

        isPermissionGranted.observe(viewLifecycleOwner, Observer {
            binding.isPermissionsGranted = it
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
