package it.fancypixel.distance.ui.fragments

import android.Manifest
import android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.browser.customtabs.CustomTabsClient.getPackageName
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.transition.MaterialFadeThrough
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import it.fancypixel.distance.R
import it.fancypixel.distance.components.Preferences
import it.fancypixel.distance.ui.activities.MainActivity
import it.fancypixel.distance.ui.viewmodels.MainViewModel
import it.fancypixel.distance.utils.setCurrentItem
import kotlinx.android.synthetic.main.intro_view.view.*
import kotlinx.android.synthetic.main.onboarding_fragment.*
import net.idik.lib.slimadapter.SlimAdapter
import kotlin.math.abs

class OnboardingFragment : Fragment() {

    companion object {
        fun newInstance() = OnboardingFragment()

        enum class IntroSection(val page: Int) {
            WELCOME(0),
            BLUETOOTH(1),
            NOTIFICATION(2),
            ALL_SET(3)
        }

        const val SCROLL_ANIMATION_DURATION = 350L
    }

    private lateinit var viewModel: MainViewModel
    private val adapter by lazy { SlimAdapter.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialFadeThrough.create()
        reenterTransition = MaterialFadeThrough.create()

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            moveBack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        return inflater.inflate(R.layout.onboarding_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter
            .register<IntroItem>(R.layout.intro_view) { item, injector ->
                injector.text(R.id.title, item.title)
                    .text(R.id.subtitle, item.message)
                    .image(R.id.icon, item.icon)
                    .clicked(R.id.action_button, item.buttonCallBack)
                    .visibility(R.id.action_button, if (item.buttonCallBack == null ||  viewModel.isPermissionGranted.value == true) View.GONE else View.VISIBLE)
            }

        pager.adapter = adapter
        pager.setPageTransformer { page, position ->
            if (position >= -1  && position <= 1) {
                page.icon.translationY = 200f * abs(position)
                page.icon_container.scaleX = 1f - (0.2f * abs(position))
                page.icon_container.scaleY = 1f - (0.2f * abs(position))
            }
        }
//        pager.isUserInputEnabled = false
        dots_indicator.setViewPager2(pager)

        adapter.updateData(listOf(
            IntroItem(IntroSection.WELCOME, getString(R.string.intro_welcome_title), getString(R.string.intro_welcome_subtitle), ContextCompat.getDrawable(requireContext(), R.drawable.ic_intro_circles)),
            IntroItem(IntroSection.BLUETOOTH, getString(R.string.intro_location_title), getString(R.string.intro_location_subtitle), ContextCompat.getDrawable(requireContext(), R.drawable.ic_intro_ble),
               View.OnClickListener { requirePermission() }
            ),
            IntroItem(IntroSection.NOTIFICATION, getString(R.string.intro_notifications_title), getString(
                            R.string.intro_notifications_subtitle), ContextCompat.getDrawable(requireContext(), R.drawable.ic_intro_notify)),
            IntroItem(IntroSection.ALL_SET, getString(R.string.intro_all_set_title), getString(R.string.intro_all_set_subtitle), ContextCompat.getDrawable(requireContext(), R.drawable.ic_intro_end))
        ))

        action_next.setOnClickListener {
            val item = adapter.getItem(pager.currentItem)
            if (item is IntroItem) {
                when (item.id) {
                    IntroSection.WELCOME -> {
                        moveNext()
                    }
                    IntroSection.BLUETOOTH -> {
//                        if (requireActivity().checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            moveNext()
//                        }
                    }
                    IntroSection.NOTIFICATION -> {
                        moveNext()
                    }
                    IntroSection.ALL_SET -> {}
                }
            }
        }

        action_finish.setOnClickListener {
            Preferences.showIntro = false
            viewModel.startService()
            it.findNavController().navigate(R.id.action_onboardingFragment_to_mainFragment)
        }

        arguments?.let { bundle ->
            bundle["section"]?.let {
                pager.setCurrentItem((it as IntroSection).page, false)
                pager.isUserInputEnabled = false
                bottom_bar.visibility = View.INVISIBLE
            }
        }

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                pager.isUserInputEnabled = position != IntroSection.BLUETOOTH.page || viewModel.isPermissionGranted.value == true
                action_next.isVisible = (position < adapter.data.size - 1) && (position != IntroSection.BLUETOOTH.page || viewModel.isPermissionGranted.value == true)
                action_finish.isVisible = position == adapter.data.size - 1
            }
        })

        viewModel.isPermissionGranted.observe(viewLifecycleOwner, Observer {
            adapter.notifyItemChanged(IntroSection.BLUETOOTH.page)
        })
    }

    private fun moveNext() {
        if (pager.currentItem < adapter.data.size - 1) {
            pager.setCurrentItem(pager.currentItem + 1, SCROLL_ANIMATION_DURATION)
        }
    }

    private fun moveBack() {
        when {
            !Preferences.showIntro -> {
                requireActivity().finish()
            }
            pager.currentItem > 0 -> {
                pager.setCurrentItem(pager.currentItem - 1, SCROLL_ANIMATION_DURATION)
            }
            Preferences.showIntro -> {
                requireActivity().finish()
            }
        }
    }

    private fun requirePermission() {
        Dexter.withContext(requireActivity())
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION
            ).withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if (report.areAllPermissionsGranted()){
                            viewModel.isPermissionGranted.value = true
                            if (Preferences.showIntro) {
                                action_next.isVisible = true
                                pager.isUserInputEnabled = true
                                adapter.notifyItemChanged(IntroSection.BLUETOOTH.page)
                            } else {
                                bottom_bar.findNavController()
                                    .navigate(R.id.action_onboardingFragment_to_mainFragment)
                            }
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

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + requireActivity().packageName)
                )
            )
        }
    }

    private class IntroItem(val id: IntroSection, val title: String, val message: String, val icon: Drawable?, val buttonCallBack: View.OnClickListener? = null)

}