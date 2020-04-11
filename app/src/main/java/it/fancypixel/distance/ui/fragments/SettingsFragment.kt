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
import com.skydoves.powermenu.MenuAnimation
import com.skydoves.powermenu.PowerMenu
import com.skydoves.powermenu.PowerMenuItem
import it.fancypixel.distance.R
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.databinding.SettingsFragmentBinding
import it.fancypixel.distance.ui.activities.MainActivity
import it.fancypixel.distance.ui.viewmodels.MainViewModel
import it.fancypixel.distance.utils.openURI
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

        subscribeUi(binding, viewModel.settings, viewModel.darkThemeMode)
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

    private fun showDarkThemeMenu() {
        val mode = viewModel.darkThemeMode.value
        val powerMenuBuilder =
            PowerMenu.Builder(context)
                .addItem(PowerMenuItem(getString(R.string.settings_subtitle_dark_theme_light), mode == AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_NO))
                .addItem(PowerMenuItem(getString(R.string.settings_subtitle_dark_theme_dark), mode == AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_YES))

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            powerMenuBuilder.addItem(PowerMenuItem(getString(R.string.settings_subtitle_dark_theme_follow_system), mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
        } else {
            powerMenuBuilder.addItem(PowerMenuItem(getString(R.string.settings_subtitle_dark_theme_by_battery_saver), mode == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY, AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY))
        }

        powerMenuBuilder.setAnimation(MenuAnimation.SHOW_UP_CENTER)
            .setAutoDismiss(true)
            .setMenuRadius(5f)
            .setMenuShadow(10f)
            .setWidth(600)
            .setLifecycleOwner(viewLifecycleOwner)
            .setMenuColor(ContextCompat.getColor(activity!!, R.color.colorPrimary))
            .setTextColor(ContextCompat.getColor(activity!!, R.color.colorPrimaryText))
            .setSelectedMenuColor(ContextCompat.getColor(activity!!, R.color.colorPrimary))
            .setSelectedTextColor(ContextCompat.getColor(activity!!, R.color.colorAccent))
            .setOnMenuItemClickListener { _: Int, item: PowerMenuItem ->
                Preferences.darkThemePreference = item.tag as Int
            }

        powerMenuBuilder.build().showAsAnchorCenter(main)
    }

    private fun subscribeUi(
        binding: SettingsFragmentBinding,
        settings: MutableLiveData<ArrayList<Any>>,
        darkThemeMode: LiveData<Int>
    ) {
        settings.observe(viewLifecycleOwner, Observer {
            adapter.updateData(it)
        })

        darkThemeMode.observe(viewLifecycleOwner, Observer {
            AppCompatDelegate.setDefaultNightMode(it)
            updateSettingsMenu()
        })
    }

    private fun updateSettingsMenu() {
        val settings = ArrayList<Any>()
        settings.add(getString(R.string.settings_header_main))

        settings.add(getString(R.string.settings_header_style))
        when (viewModel.darkThemeMode.value) {
            AppCompatDelegate.MODE_NIGHT_NO -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_choose_theme),
                        getString(R.string.settings_subtitle_dark_theme_light),
                        R.drawable.ic_day_night,
                        View.OnClickListener { showDarkThemeMenu() })
                )
            }
            AppCompatDelegate.MODE_NIGHT_YES -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_choose_theme),
                        getString(R.string.settings_subtitle_dark_theme_dark),
                        R.drawable.ic_day_night,
                        View.OnClickListener { showDarkThemeMenu() })
                )
            }
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_choose_theme),
                        getString(R.string.settings_subtitle_dark_theme_by_battery_saver),
                        R.drawable.ic_day_night,
                        View.OnClickListener { showDarkThemeMenu() })
                )
            }
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> {
                settings.add(
                    SettingsItem(
                        getString(R.string.settings_title_choose_theme),
                        getString(R.string.settings_subtitle_dark_theme_follow_system),
                        R.drawable.ic_day_night,
                        View.OnClickListener { showDarkThemeMenu() })
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
//                    activity?.sendEmailTo("rgb@fancypixel.it")
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
