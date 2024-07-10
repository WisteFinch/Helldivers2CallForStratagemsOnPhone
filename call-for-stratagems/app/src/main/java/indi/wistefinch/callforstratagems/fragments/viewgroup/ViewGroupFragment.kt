package indi.wistefinch.callforstratagems.fragments.viewgroup

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
import androidx.navigation.fragment.findNavController
import indi.wistefinch.callforstratagems.CFSApplication
import indi.wistefinch.callforstratagems.R
import indi.wistefinch.callforstratagems.data.models.GroupData
import indi.wistefinch.callforstratagems.data.models.StratagemData
import indi.wistefinch.callforstratagems.data.viewmodel.GroupViewModel
import indi.wistefinch.callforstratagems.data.viewmodel.GroupViewModelFactory
import indi.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import indi.wistefinch.callforstratagems.data.viewmodel.StratagemViewModelFactory
import indi.wistefinch.callforstratagems.databinding.FragmentViewGroupBinding
import indi.wistefinch.callforstratagems.layout.AppGridLayoutManager
import java.util.Vector

class ViewGroupFragment : Fragment() {

    private val groupViewModel: GroupViewModel by activityViewModels {
        GroupViewModelFactory(
            (activity?.application as CFSApplication).groupDb.groupDao()
        )
    }

    private val stratagemViewModel: StratagemViewModel by activityViewModels {
        StratagemViewModelFactory(
            (activity?.application as CFSApplication).stratagemDb.stratagemDao()
        )
    }

    // Init the stratagem recycler view adapter
    private val adapter: StratagemViewAdapter by lazy { StratagemViewAdapter() }

    private var _binding: FragmentViewGroupBinding? = null
    private val binding get() = _binding!!

    private lateinit var currentItem: GroupData

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentViewGroupBinding.inflate(inflater, container, false)
        val view = binding.root

        currentItem = arguments?.getParcelable("currentItem")!!
        binding.viewGroupTitle.text = currentItem.title

        // Add FAB
        binding.viewGroupPlayFAB.setOnClickListener {
            val bundle = bundleOf(Pair("currentItem", currentItem))
            findNavController().navigate(R.id.action_viewGroupFragment_to_playFragment, bundle)
        }

        setupRecyclerView()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.view_group_fragment_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.viewGroup_menu_edit-> {
                        val bundle = bundleOf(Pair("currentItem", currentItem), Pair("isEdit", true))
                        findNavController().navigate(R.id.action_viewGroupFragment_to_editGroupFragment, bundle)
                        true
                    }
                    R.id.viewGroup_menu_delete-> {
                        groupViewModel.deleteItem(currentItem)
                        findNavController().popBackStack(R.id.rootFragment, false)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    /**
     * Setup the stratagem recycler view
     */
    private fun setupRecyclerView() {
        val recyclerView = binding.viewGroupRecyclerView
        recyclerView.adapter = adapter
        recyclerView.layoutManager = AppGridLayoutManager(context, 3)
        val list: Vector<StratagemData> = Vector()
        for (i in currentItem.list) {
            if (stratagemViewModel.isIdValid(i)) {
                list.add(stratagemViewModel.retrieveItem(i))
            }
        }
        adapter.setData(list.toList())
        binding.viewGroupRecyclerView.suppressLayout(true)
    }
}