package it.fancypixel.distance.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import androidx.navigation.Navigation

import it.fancypixel.distance.R
import it.fancypixel.distance.databinding.MainFragmentBinding
import it.fancypixel.distance.ui.activities.MainActivity
import it.fancypixel.distance.ui.viewmodels.MainViewModel
import it.fancypixel.distance.utils.reveal
import kotlinx.android.synthetic.main.main_fragment.*
import net.idik.lib.slimadapter.SlimAdapter
import org.altbeacon.beacon.BeaconTransmitter

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

        subscribeUi(binding, viewModel.darkThemeMode, viewModel.isServiceEnabled)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        action_settings.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_mainFragment_to_settingsFragment)
        }
    }


    private fun subscribeUi(
        binding: MainFragmentBinding,
        darkThemeMode: LiveData<Int>,
        isServiceEnabled: LiveData<Boolean>
    ) {
        darkThemeMode.observe(viewLifecycleOwner, Observer {
            AppCompatDelegate.setDefaultNightMode(it)
        })

        binding.isAdvertisingPossible = BeaconTransmitter.checkTransmissionSupported(activity!!) == BeaconTransmitter.SUPPORTED
        isServiceEnabled.observe(viewLifecycleOwner, Observer {

        })
    }
}
