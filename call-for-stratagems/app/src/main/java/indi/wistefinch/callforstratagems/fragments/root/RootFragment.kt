package indi.wistefinch.callforstratagems.fragments.root

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import indi.wistefinch.callforstratagems.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import indi.wistefinch.callforstratagems.CFSApplication
import indi.wistefinch.callforstratagems.data.viewmodel.GroupViewModel
import indi.wistefinch.callforstratagems.data.viewmodel.GroupViewModelFactory
import indi.wistefinch.callforstratagems.databinding.FragmentRootBinding

class RootFragment : Fragment() {

    // Init the group view model
    private val viewModel: GroupViewModel by activityViewModels {
        GroupViewModelFactory(
            (activity?.application as CFSApplication).database.groupDao()
        )
    }

    // Init the group recycler view adapter
    private val adapter: GroupListAdapter by lazy { GroupListAdapter() }

    private var _binding: FragmentRootBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRootBinding.inflate(inflater, container, false)
        val view = binding.root

        // Add FAB
        binding.rootNewGroupFAB.setOnClickListener {
            val bundle = bundleOf(Pair("currentId", -1))
            findNavController().navigate(R.id.action_rootFragment_to_editGroupFragment, bundle)
        }

        // Setup group recycler view
        val recyclerView = binding.rootRecyclerView
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)
        viewModel.allItems.observe(viewLifecycleOwner, Observer { data->
            adapter.setData(data)
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.root_fragment_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.root_menu_preferences -> {
                        findNavController().navigate(R.id.action_rootFragment_to_settingsFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}