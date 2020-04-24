package it.fancypixel.distance.ui.fragments

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.transition.MaterialSharedAxis
import io.realm.RealmResults
import it.fancypixel.distance.R
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.components.RealmLiveData
import it.fancypixel.distance.components.events.NearbyBeaconEvent
import it.fancypixel.distance.databinding.MainFragmentBinding
import it.fancypixel.distance.db.models.Bump
import it.fancypixel.distance.global.Constants
import it.fancypixel.distance.ui.activities.MainActivity
import it.fancypixel.distance.ui.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.battery_saver_warning_layout.*
import kotlinx.android.synthetic.main.device_count_layout.*
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.android.synthetic.main.main_fragment.header
import net.idik.lib.slimadapter.SlimAdapter
import org.altbeacon.beacon.Beacon
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


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

        subscribeUi(binding, viewModel.darkThemeMode, viewModel.isServiceEnabled, viewModel.nearbyBeacons, viewModel.isBluetoothDisabled, viewModel.isBatterySaverEnabled, viewModel.todayBumps, viewModel.debug)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Toolbar
        action_settings.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_mainFragment_to_settingsFragment)
        }

        // Beacon List
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

        // Action buttons
        action_enable.setOnClickListener {
            disabled_service_ui.animate().translationY(disabled_service_ui.height.toFloat()).withEndAction {
                viewModel.startService()
            }.start()
        }

        action_toggle.setOnClickListener {
            disabled_service_ui.animate().translationY(0f).withEndAction {
                viewModel.stopService()
            }.start()
        }

        action_toggle_container.setOnClickListener {
            disabled_service_ui.animate().translationY(0f).withEndAction {
                viewModel.stopService()
            }.start()
        }

        if (Preferences.showAdvertisingError && viewModel.showAdvertisingError) {
            MaterialAlertDialogBuilder(requireActivity(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setCancelable(true)
                .setTitle(getString(R.string.advertising_not_available_title))
                .setMessage(resources.getString(R.string.advertising_not_possible_message))
                .setPositiveButton(resources.getString(android.R.string.ok)) { dialog, _ ->
                    Preferences.showAdvertisingError = false
                    dialog.dismiss()
                }
                .show()
        }
    }


    private fun subscribeUi(
        binding: MainFragmentBinding,
        darkThemeMode: LiveData<Int>,
        isServiceEnabled: LiveData<Boolean>,
        nearbyBeacons: LiveData<ArrayList<Any>>,
        isBluetoothDisabled: MutableLiveData<Boolean>,
        isBatterySaverEnabled: MutableLiveData<Boolean>,
        todayBumps: RealmLiveData<Bump>?,
        debug: LiveData<Boolean>
    ) {
        darkThemeMode.observe(viewLifecycleOwner, Observer {
            AppCompatDelegate.setDefaultNightMode(it)
        })

        nearbyBeacons.observe(viewLifecycleOwner, Observer {
            if (Preferences.isServiceEnabled) {
                header.text = getString(
                    R.string.header_nearby_beacons,
                    it.size
                )
            }
            adapter.updateData(it)

            nearby_device_count.text = it.size.toString()
            nearby_device_text.text = if (it.size == 1) getString(R.string.near_device) else getString(R.string.near_devices)
            nearby_device_count.setTextColor(ContextCompat.getColor(requireContext(), if (it.size == 0) R.color.colorAccent else R.color.errorColorText))
        })

        todayBumps?.observe(viewLifecycleOwner, Observer {
            val groupedBumps = extractTodayBumps(it)
            bump_count.text = groupedBumps.keys.size.toString()
            bump_count_text.text = if (groupedBumps.keys.size == 1) getString(R.string.today_bump) else getString(R.string.today_bumps)
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

            if (disabled_service_ui.height == 0) {
                disabled_service_ui.translationY = if (it) Resources.getSystem().displayMetrics.heightPixels.toFloat() else 0f
            } else {
                disabled_service_ui.translationY = if (it) disabled_service_ui.height.toFloat() else 0f
            }
        })

        debug.observe(viewLifecycleOwner, Observer {
            beacons_list.visibility = if (it) View.VISIBLE else View.GONE
            binding.isDebugModeEnabled = it
        })

        isBluetoothDisabled.observe(viewLifecycleOwner, Observer {
            ble_off_message.isVisible = it
            service_status_bg.setBackgroundColor(ContextCompat.getColor(requireContext(), if (it) R.color.errorColorText else android.R.color.transparent))
            service_status.text = if (it) getString(R.string.service_paused) else getString(R.string.service_enabled)
        })

        isBatterySaverEnabled.observe(viewLifecycleOwner, Observer {
            battery_saver_warning.isVisible = it
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

    private fun extractTodayBumps(bumps: RealmResults<Bump>?): Map<String, Int> {
        return bumps?.groupingBy { it.beaconUuid }?.eachCount() ?: HashMap()
    }

    /**
     *  Data Binding adapters for UI margins
     */
    @BindingAdapter("android:layout_marginTop")
    fun ViewGroup.setMarginTopValue(marginValue: Float) =
        (layoutParams as ViewGroup.MarginLayoutParams).apply { topMargin = marginValue.toInt() }

    @BindingAdapter("android:layout_marginBottom")
    fun ViewGroup.setMarginBottomValue(marginValue: Float) =
        (layoutParams as ViewGroup.MarginLayoutParams).apply { bottomMargin = marginValue.toInt() }

    @BindingAdapter("android:layout_marginStart")
    fun ViewGroup.setMarginStartValue(marginValue: Float) =
        (layoutParams as ViewGroup.MarginLayoutParams).apply { leftMargin = marginValue.toInt() }

    @BindingAdapter("android:layout_marginEnd")
    fun ViewGroup.setMarginEndValue(marginValue: Float) =
        (layoutParams as ViewGroup.MarginLayoutParams).apply { rightMargin = marginValue.toInt() }

}
