package it.fancypixel.distance.ui.fragments

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
import com.google.android.material.transition.MaterialSharedAxis
import it.fancypixel.distance.BuildConfig
import it.fancypixel.distance.R
import it.fancypixel.distance.components.BottomSheetMenu
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.databinding.SettingsFragmentBinding
import it.fancypixel.distance.global.Constants
import it.fancypixel.distance.ui.activities.MainActivity
import it.fancypixel.distance.ui.viewmodels.MainViewModel
import it.fancypixel.distance.utils.openURI
import it.fancypixel.distance.utils.shareAppLink
import kotlinx.android.synthetic.main.settings_fragment.*
import net.idik.lib.slimadapter.SlimAdapter

class SettingsFragment : Fragment() {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis.create(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis.create(MaterialSharedAxis.X, false)
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: SlimAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        val binding = DataBindingUtil.inflate<SettingsFragmentBinding>(inflater, R.layout.settings_fragment, container, false)

        subscribeUi(viewModel.settings, viewModel.darkThemeMode, viewModel.tolerance, viewModel.pocketTolerance, viewModel.notificationType, viewModel.batteryLevel, viewModel.debug)
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (!BuildConfig.DEBUG) {
            Preferences.debug = false
        }

        action_back.setOnClickListener {
            Navigation.findNavController(it).popBackStack()
        }

        settings_list.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            requireActivity(),
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
                    .longClicked(R.id.item, item.longClickListener)

                item.iconRes?.let {
                    injector.image(R.id.icon, ContextCompat.getDrawable(requireActivity(), it))
                }
            }
            .register<String>(R.layout.settings_header_layout) { header, injector ->
                injector.text(R.id.header, header)
            }
            .attachTo(settings_list)

        updateSettingsMenu()
    }

    private fun showToleranceMenu() {
        BottomSheetMenu<Int>(requireContext(), getString(R.string.tolerance_menu_title)).selectResource(Preferences.tolerance)
            .addItem(getString(R.string.settings_subtitle_tolerance_max), Constants.PREFERENCE_TOLERANCE_MAX)
            .addItem(getString(R.string.settings_subtitle_tolerance_high), Constants.PREFERENCE_TOLERANCE_HIGH)
            .addItem(getString(R.string.settings_subtitle_tolerance_default), Constants.PREFERENCE_TOLERANCE_DEFAULT)
            .addItem(getString(R.string.settings_subtitle_tolerance_min), Constants.PREFERENCE_TOLERANCE_MIN)
            .addItem(getString(R.string.settings_subtitle_tolerance_low), Constants.PREFERENCE_TOLERANCE_LOW)
            .addOnSelectItemListener { value ->
                Preferences.tolerance = value
            }.show()
    }

    private fun showPocketToleranceMenu() {
        BottomSheetMenu<Int>(requireContext(), getString(R.string.tolerance_menu_title)).selectResource(Preferences.pocketTolerance)
            .addItem(getString(R.string.settings_subtitle_tolerance_max), Constants.PREFERENCE_TOLERANCE_MAX)
            .addItem(getString(R.string.settings_subtitle_tolerance_high), Constants.PREFERENCE_TOLERANCE_HIGH)
            .addItem(getString(R.string.settings_subtitle_tolerance_default), Constants.PREFERENCE_TOLERANCE_DEFAULT)
            .addItem(getString(R.string.settings_subtitle_tolerance_min), Constants.PREFERENCE_TOLERANCE_MIN)
            .addItem(getString(R.string.settings_subtitle_tolerance_low), Constants.PREFERENCE_TOLERANCE_LOW)
            .addOnSelectItemListener { value ->
                Preferences.pocketTolerance = value
            }.show()
    }

    private fun showNotificationTypeMenu() {
        BottomSheetMenu<Int>(requireContext()).selectResource(Preferences.notificationType)
            .addItem(getString(R.string.settings_subtitle_notification_type_only_vibrate), Constants.PREFERENCE_NOTIFICATION_TYPE_ONLY_VIBRATE)
            .addItem(getString(R.string.settings_subtitle_notification_type_only_sound), Constants.PREFERENCE_NOTIFICATION_TYPE_ONLY_SOUND)
            .addItem(getString(R.string.settings_subtitle_notification_type_both), Constants.PREFERENCE_NOTIFICATION_TYPE_BOTH)
            .addOnSelectItemListener { value ->
                Preferences.notificationType = value
            }.show()
    }

    private fun showDarkThemeMenu() {
        BottomSheetMenu<Int>(requireContext())
            .selectResource(Preferences.darkThemePreference)
            .addItem(
                getString(R.string.settings_subtitle_dark_theme_light),
                AppCompatDelegate.MODE_NIGHT_NO
            )
            .addItem(
                getString(R.string.settings_subtitle_dark_theme_dark),
                AppCompatDelegate.MODE_NIGHT_YES
            )
            .addItem(
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) getString(R.string.settings_subtitle_dark_theme_follow_system) else getString(R.string.settings_subtitle_dark_theme_by_battery_saver),
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
            )
            .addOnSelectItemListener { value ->
                Preferences.darkThemePreference = value
            }.show()
    }

    private fun subscribeUi(
        settings: MutableLiveData<ArrayList<Any>>,
        darkThemeMode: LiveData<Int>,
        tolerance: LiveData<Int>,
        pocketTolerance: LiveData<Int>,
        notificationType: LiveData<Int>,
        batteryLevel: LiveData<Boolean>,
        debug: LiveData<Boolean>
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

        pocketTolerance.observe(viewLifecycleOwner, Observer {
            updateSettingsMenu()
        })

        notificationType.observe(viewLifecycleOwner, Observer {
            updateSettingsMenu()
        })

        batteryLevel.observe(viewLifecycleOwner, Observer {
            updateSettingsMenu()
        })

        debug.observe(viewLifecycleOwner, Observer {
            updateSettingsMenu()
        })
    }

    private fun updateSettingsMenu() {
        val settings = ArrayList<Any>()

        // General
        settings.add(getString(R.string.settings_header_main))
        settings.add(
            SettingsItem(
                getString(R.string.settings_title_id),
                Preferences.deviceUUID.padStart(5, '0')
            )
        )
        settings.add(
            SettingsItem(
                getString(R.string.settings_title_choose_theme),
                when (viewModel.darkThemeMode.value) {
                    AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.settings_subtitle_dark_theme_light)
                    AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.settings_subtitle_dark_theme_dark)
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY -> getString(R.string.settings_subtitle_dark_theme_by_battery_saver)
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> getString(R.string.settings_subtitle_dark_theme_follow_system)
                    else -> ""
                },
                clickListener = View.OnClickListener { showDarkThemeMenu() }
            )
        )


        // Alert
        settings.add(getString(R.string.settings_header_alarm))
        settings.add(
            SettingsItem(
                getString(R.string.settings_title_notification_type),
                when (viewModel.notificationType.value) {
                    Constants.PREFERENCE_NOTIFICATION_TYPE_ONLY_VIBRATE -> getString(R.string.settings_subtitle_notification_type_only_vibrate)
                    Constants.PREFERENCE_NOTIFICATION_TYPE_ONLY_SOUND -> getString(R.string.settings_subtitle_notification_type_only_sound)
                    Constants.PREFERENCE_NOTIFICATION_TYPE_BOTH -> getString(R.string.settings_subtitle_notification_type_both)
                    else -> ""
                },
                clickListener = View.OnClickListener { showNotificationTypeMenu() }
            )
        )
        settings.add(
            SettingsItem(
                getString(R.string.settings_title_tolerance),
                when (viewModel.tolerance.value) {
                    Constants.PREFERENCE_TOLERANCE_LOW -> getString(R.string.settings_subtitle_tolerance_low)
                    Constants.PREFERENCE_TOLERANCE_MIN -> getString(R.string.settings_subtitle_tolerance_min)
                    Constants.PREFERENCE_TOLERANCE_DEFAULT -> getString(R.string.settings_subtitle_tolerance_default)
                    Constants.PREFERENCE_TOLERANCE_HIGH -> getString(R.string.settings_subtitle_tolerance_high)
                    Constants.PREFERENCE_TOLERANCE_MAX -> getString(R.string.settings_subtitle_tolerance_max)
                    else -> ""
                },
                clickListener = View.OnClickListener { showToleranceMenu() }
            )
        )
        settings.add(
            SettingsItem(
                getString(R.string.settings_title_tolerance_pocket),
                when (viewModel.pocketTolerance.value) {
                    Constants.PREFERENCE_TOLERANCE_LOW -> getString(R.string.settings_subtitle_tolerance_low)
                    Constants.PREFERENCE_TOLERANCE_MIN -> getString(R.string.settings_subtitle_tolerance_min)
                    Constants.PREFERENCE_TOLERANCE_DEFAULT -> getString(R.string.settings_subtitle_tolerance_default)
                    Constants.PREFERENCE_TOLERANCE_HIGH -> getString(R.string.settings_subtitle_tolerance_high)
                    Constants.PREFERENCE_TOLERANCE_MAX -> getString(R.string.settings_subtitle_tolerance_max)
                    else -> ""
                },
                clickListener = View.OnClickListener { showPocketToleranceMenu() }
            )
        )


        if (Preferences.debug) {
            settings.add(
                SettingsItem(
                    getString(R.string.settings_title_battery_error),
                    when (viewModel.batteryLevel.value) {
                        true -> getString(R.string.settings_subtitle_battery_error_on)
                        false -> getString(R.string.settings_subtitle_battery_error_off)
                        else -> ""
                    },
                    clickListener = View.OnClickListener { Preferences.useBatteryLevel = false }
                )
            )
        }

        settings.add(getString(R.string.settings_header_development))

        if (viewModel.showAdvertisingError) {
            settings.add(
                SettingsItem(
                    getString(R.string.advertising_not_available_title),
                    getString(R.string.advertising_not_possible_message)
                )
            )
        }

        settings.add(
            SettingsItem(
                getString(R.string.action_share),
                getString(R.string.action_share_message),
                clickListener = View.OnClickListener { activity?.shareAppLink() }
            )
        )

        settings.add(
            SettingsItem(
                getString(R.string.settings_title_company),
                getString(R.string.settings_subtitle_company),
                clickListener = View.OnClickListener { activity?.openURI("https://www.fancypixel.it/") }
            )
        )

        settings.add(
            SettingsItem(
                getString(R.string.settings_title_app_version),
                BuildConfig.VERSION_NAME,
                longClickListener = View.OnLongClickListener { if (BuildConfig.DEBUG) { Preferences.debug = !Preferences.debug }; true }
            )
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
        var hasError: Boolean = false,
        var longClickListener: View.OnLongClickListener? = null
    )

}
