package it.fancypixel.distance.ui.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.skydoves.powermenu.MenuAnimation
import com.skydoves.powermenu.PowerMenu
import com.skydoves.powermenu.PowerMenuItem
import it.fancypixel.distance.R
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.databinding.SettingsFragmentBinding
import it.fancypixel.distance.global.Constants
import it.fancypixel.distance.ui.activities.MainActivity
import it.fancypixel.distance.ui.viewmodels.MainViewModel
import it.fancypixel.distance.utils.openURI
import it.fancypixel.distance.utils.sendEmailTo
import it.fancypixel.distance.utils.toPixel
import kotlinx.android.synthetic.main.settings_fragment.*
import net.idik.lib.slimadapter.SlimAdapter

class SettingsFragment : Fragment() {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: SlimAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        val binding = DataBindingUtil.inflate<SettingsFragmentBinding>(inflater, R.layout.settings_fragment, container, false)

        subscribeUi(binding, viewModel.settings, viewModel.darkThemeMode, viewModel.tolerance, viewModel.deviceLocation, viewModel.notificationType)
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        action_back.setOnClickListener {
            Navigation.findNavController(it).popBackStack()
        }

        settings_list.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            activity!!,
            androidx.recyclerview.widget.LinearLayoutManager.VERTICAL,
            false
        )

        adapter = SlimAdapter.create()
        adapter
            .register<SettingsItem>(R.layout.settings_item_layout) { item, injector ->
                injector
                    .text(R.id.title, item.title)
                    .text(R.id.subtitle, item.subtitle)
                    .clicked(R.id.item, item.clickListener)
                    .visibility(R.id.error_indicator, if (item.hasError) View.VISIBLE else View.GONE)

                item.iconRes?.let {
                    injector.image(R.id.icon, ContextCompat.getDrawable(activity!!, it))
                }
            }
            .register<String>(R.layout.settings_header_layout) { header, injector ->
                injector.text(R.id.header, header)
            }
            .attachTo(settings_list)

        updateSettingsMenu()
    }

    private fun showToleranceMenu(view: View) {
        val tolerance = viewModel.tolerance.value
        getStyledPowerMenuBuilder(context)
            .addItem(PowerMenuItem(getString(R.string.settings_subtitle_tolerance_low), tolerance == Constants.PREFERENCE_TOLERANCE_LOW, Constants.PREFERENCE_TOLERANCE_LOW))
            .addItem(PowerMenuItem(getString(R.string.settings_subtitle_tolerance_min), tolerance == Constants.PREFERENCE_TOLERANCE_MIN, Constants.PREFERENCE_TOLERANCE_MIN))
            .addItem(PowerMenuItem(getString(R.string.settings_subtitle_tolerance_default), tolerance == Constants.PREFERENCE_TOLERANCE_DEFAULT, Constants.PREFERENCE_TOLERANCE_DEFAULT))
            .addItem(PowerMenuItem(getString(R.string.settings_subtitle_tolerance_high), tolerance == Constants.PREFERENCE_TOLERANCE_HIGH, Constants.PREFERENCE_TOLERANCE_HIGH))
            .addItem(PowerMenuItem(getString(R.string.settings_subtitle_tolerance_max), tolerance == Constants.PREFERENCE_TOLERANCE_MAX, Constants.PREFERENCE_TOLERANCE_MAX))
            .setOnMenuItemClickListener { _: Int, item: PowerMenuItem ->
                Preferences.tolerance = item.tag as Int
            }
            .build().showAsDropDown(view, 78.toPixel(activity!!), (-8).toPixel(activity!!))
    }

    private fun showDeviceLocationMenu(view: View) {
        val deviceLocation = viewModel.deviceLocation.value
        getStyledPowerMenuBuilder(context)
            .addItem(PowerMenuItem(getString(R.string.settings_subtitle_device_location_pocket), deviceLocation == Constants.PREFERENCE_DEVICE_LOCATION_POCKET, Constants.PREFERENCE_DEVICE_LOCATION_POCKET))
            .addItem(PowerMenuItem(getString(R.string.settings_subtitle_device_location_desk), deviceLocation == Constants.PREFERENCE_DEVICE_LOCATION_DESK, Constants.PREFERENCE_DEVICE_LOCATION_DESK))
            .setOnMenuItemClickListener { _: Int, item: PowerMenuItem ->
                Preferences.deviceLocation = item.tag as Int
            }
            .build().showAsDropDown(view, 78.toPixel(activity!!), (-8).toPixel(activity!!))
    }

    private fun showNotificationTypeMenu(view: View) {
        val notificationType = viewModel.notificationType.value
        getStyledPowerMenuBuilder(context)
            .addItem(PowerMenuItem(getString(R.string.settings_subtitle_notification_type_only_vibrate), notificationType == Constants.PREFERENCE_NOTIFICATION_TYPE_ONLY_VIBRATE, Constants.PREFERENCE_NOTIFICATION_TYPE_ONLY_VIBRATE))
            .addItem(PowerMenuItem(getString(R.string.settings_subtitle_notification_type_only_sound), notificationType == Constants.PREFERENCE_NOTIFICATION_TYPE_ONLY_SOUND, Constants.PREFERENCE_NOTIFICATION_TYPE_ONLY_SOUND))
            .addItem(PowerMenuItem(getString(R.string.settings_subtitle_notification_type_both), notificationType == Constants.PREFERENCE_NOTIFICATION_TYPE_BOTH, Constants.PREFERENCE_NOTIFICATION_TYPE_BOTH))
            .setOnMenuItemClickListener { _: Int, item: PowerMenuItem ->
                Preferences.notificationType = item.tag as Int
            }
            .build().showAsDropDown(view, 78.toPixel(activity!!), (-8).toPixel(activity!!))
    }

    private fun showDarkThemeMenu(view: View) {
        val mode = viewModel.darkThemeMode.value
        val powerMenuBuilder = getStyledPowerMenuBuilder(context)
            .addItem(PowerMenuItem(getString(R.string.settings_subtitle_dark_theme_light), mode == AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_NO))
            .addItem(PowerMenuItem(getString(R.string.settings_subtitle_dark_theme_dark), mode == AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_YES))

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            powerMenuBuilder.addItem(PowerMenuItem(getString(R.string.settings_subtitle_dark_theme_follow_system), mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
        } else {
            powerMenuBuilder.addItem(PowerMenuItem(getString(R.string.settings_subtitle_dark_theme_by_battery_saver), mode == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY, AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY))
        }

        powerMenuBuilder.setOnMenuItemClickListener { _: Int, item: PowerMenuItem ->
            Preferences.darkThemePreference = item.tag as Int
        }

        powerMenuBuilder.build().showAsDropDown(view, 78.toPixel(activity!!), (-8).toPixel(activity!!))
    }

    private fun getStyledPowerMenuBuilder(context: Context?) = PowerMenu.Builder(context)
        .setAutoDismiss(true)
        .setMenuRadius(24f)
        .setMenuShadow(10f)
        .setWidth(600)
        .setAnimation(MenuAnimation.SHOWUP_TOP_LEFT)
        .setLifecycleOwner(viewLifecycleOwner)
        .setBackgroundAlpha(0f)
        .setMenuColor(ContextCompat.getColor(activity!!, R.color.colorPrimary))
        .setTextColor(ContextCompat.getColor(activity!!, R.color.colorPrimaryText))
        .setSelectedMenuColor(ContextCompat.getColor(activity!!, R.color.colorPrimary))
        .setSelectedTextColor(ContextCompat.getColor(activity!!, R.color.colorAccent))

    private fun subscribeUi(
        binding: SettingsFragmentBinding,
        settings: MutableLiveData<ArrayList<Any>>,
        darkThemeMode: LiveData<Int>,
        tolerance: LiveData<Int>,
        deviceLocation: LiveData<Int>,
        notificationType: LiveData<Int>
    ) {
        settings.observe(viewLifecycleOwner, Observer {
            adapter.updateData(it)
        })

        darkThemeMode.observe(viewLifecycleOwner, Observer {
            AppCompatDelegate.setDefaultNightMode(it)
            updateSettingsMenu()
        })

        tolerance.observe(viewLifecycleOwner, Observer {
            updateSettingsMenu()
        })

        deviceLocation.observe(viewLifecycleOwner, Observer {
            updateSettingsMenu()
        })

        notificationType.observe(viewLifecycleOwner, Observer {
            updateSettingsMenu()
        })


    }

    private fun updateSettingsMenu() {
        val settings = ArrayList<Any>()
        settings.add(getString(R.string.settings_header_main))
        when (viewModel.deviceLocation.value) {
            Constants.PREFERENCE_DEVICE_LOCATION_POCKET -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_device_location),
                        getString(R.string.settings_subtitle_device_location_pocket),
                        R.drawable.round_dock,
                        View.OnClickListener { showDeviceLocationMenu(it) })
                )
            }
            Constants.PREFERENCE_DEVICE_LOCATION_DESK -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_device_location),
                        getString(R.string.settings_subtitle_device_location_desk),
                        R.drawable.round_dock,
                        View.OnClickListener { showDeviceLocationMenu(it) })
                )
            }
        }
        when (viewModel.tolerance.value) {
            Constants.PREFERENCE_TOLERANCE_LOW -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_tolerance),
                        getString(R.string.settings_subtitle_tolerance_low),
                        R.drawable.round_tune_24,
                        View.OnClickListener { showToleranceMenu(it) })
                )
            }
            Constants.PREFERENCE_TOLERANCE_MIN -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_tolerance),
                        getString(R.string.settings_subtitle_tolerance_min),
                        R.drawable.round_tune_24,
                        View.OnClickListener { showToleranceMenu(it) })
                )
            }
            Constants.PREFERENCE_TOLERANCE_DEFAULT -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_tolerance),
                        getString(R.string.settings_subtitle_tolerance_default),
                        R.drawable.round_tune_24,
                        View.OnClickListener { showToleranceMenu(it) })
                )
            }
            Constants.PREFERENCE_TOLERANCE_HIGH -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_tolerance),
                        getString(R.string.settings_subtitle_tolerance_high),
                        R.drawable.round_tune_24,
                        View.OnClickListener { showToleranceMenu(it) })
                )
            }
            Constants.PREFERENCE_TOLERANCE_MAX -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_tolerance),
                        getString(R.string.settings_subtitle_tolerance_max),
                        R.drawable.round_tune_24,
                        View.OnClickListener { showToleranceMenu(it) })
                )
            }
        }
        when (viewModel.notificationType.value) {
            Constants.PREFERENCE_NOTIFICATION_TYPE_ONLY_VIBRATE -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_notification_type),
                        getString(R.string.settings_subtitle_notification_type_only_vibrate),
                        R.drawable.round_notifications,
                        View.OnClickListener { showNotificationTypeMenu(it) })
                )
            }
            Constants.PREFERENCE_NOTIFICATION_TYPE_ONLY_SOUND -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_notification_type),
                        getString(R.string.settings_subtitle_notification_type_only_sound),
                        R.drawable.round_notifications,
                        View.OnClickListener { showNotificationTypeMenu(it) })
                )
            }
            Constants.PREFERENCE_NOTIFICATION_TYPE_BOTH -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_notification_type),
                        getString(R.string.settings_subtitle_notification_type_both),
                        R.drawable.round_notifications,
                        View.OnClickListener { showNotificationTypeMenu(it) })
                )
            }
        }

        settings.add(getString(R.string.settings_header_style))
        when (viewModel.darkThemeMode.value) {
            AppCompatDelegate.MODE_NIGHT_NO -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_choose_theme),
                        getString(R.string.settings_subtitle_dark_theme_light),
                        R.drawable.ic_day_night,
                        View.OnClickListener { showDarkThemeMenu(it) })
                )
            }
            AppCompatDelegate.MODE_NIGHT_YES -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_choose_theme),
                        getString(R.string.settings_subtitle_dark_theme_dark),
                        R.drawable.ic_day_night,
                        View.OnClickListener { showDarkThemeMenu(it) })
                )
            }
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_choose_theme),
                        getString(R.string.settings_subtitle_dark_theme_by_battery_saver),
                        R.drawable.ic_day_night,
                        View.OnClickListener { showDarkThemeMenu(it) })
                )
            }
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_choose_theme),
                        getString(R.string.settings_subtitle_dark_theme_follow_system),
                        R.drawable.ic_day_night,
                        View.OnClickListener { showDarkThemeMenu(it) })
                )
            }
        }

        settings.add(getString(R.string.settings_header_development))
        settings.add(
            SettingsItem(
                getString(R.string.settings_title_company),
                getString(R.string.settings_subtitle_company),
                R.drawable.round_business_24,
                View.OnClickListener { activity?.openURI("https://www.fancypixel.it/") })
        )
//        settings.add(
//            SettingsItem(
//                getString(R.string.settings_title_feedback),
//                getString(R.string.settings_subtitle_feedback),
//                R.drawable.round_send_24,
//                View.OnClickListener {
//                    activity?.sendEmailTo("support@fancypixel.it")
//                }
//            )
//        )

        viewModel.settings.value = settings
    }

    class SettingsItem(
        var title: String = "",
        var subtitle: String = "",
        var iconRes: Int? = null,
        var clickListener: View.OnClickListener? = null,
        var hasError: Boolean = false
    )

}
