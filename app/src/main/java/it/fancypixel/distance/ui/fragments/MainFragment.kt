package it.fancypixel.distance.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.navigation.Navigation
import it.fancypixel.distance.components.events.NearbyBeaconEvent
import it.fancypixel.distance.databinding.MainFragmentBinding
import it.fancypixel.distance.ui.activities.MainActivity
import it.fancypixel.distance.ui.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.main_fragment.*
import net.idik.lib.slimadapter.SlimAdapter
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconTransmitter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import it.fancypixel.distance.R

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

        subscribeUi(binding, viewModel.darkThemeMode, viewModel.isServiceEnabled, viewModel.nearbyBeacons)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        action_settings.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_mainFragment_to_settingsFragment)
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
        nearbyBeacons: LiveData<ArrayList<Any>>
    ) {
        darkThemeMode.observe(viewLifecycleOwner, Observer {
            AppCompatDelegate.setDefaultNightMode(it)
        })

        binding.isAdvertisingPossible = BeaconTransmitter.checkTransmissionSupported(activity!!) == BeaconTransmitter.SUPPORTED

        nearbyBeacons.observe(viewLifecycleOwner, Observer {
            if (it.size > 0) {
                adapter.updateData(ArrayList(listOf(getString(R.string.header_nearby_beacons, it.size)) + it))
            } else {
                adapter.updateData(arrayListOf(getString(R.string.empty_nearby_beacons_list_message)))
            }
        })

        isServiceEnabled.observe(viewLifecycleOwner, Observer {
            if (!it) {
                viewModel.clearNearbyBeacons()
            }
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
