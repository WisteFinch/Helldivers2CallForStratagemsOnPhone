package indi.wistefinch.callforstratagems.fragments.viewgroup

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import indi.wistefinch.callforstratagems.CFSApplication
import indi.wistefinch.callforstratagems.R
import indi.wistefinch.callforstratagems.data.models.GroupData
import indi.wistefinch.callforstratagems.data.viewmodel.GroupViewModel
import indi.wistefinch.callforstratagems.data.viewmodel.GroupViewModelFactory
import indi.wistefinch.callforstratagems.databinding.FragmentEditGroupBinding
import indi.wistefinch.callforstratagems.databinding.FragmentViewGroupBinding

class ViewGroupFragment : Fragment() {

    private val viewModel: GroupViewModel by activityViewModels {
        GroupViewModelFactory(
            (activity?.application as CFSApplication).database.groupDao()
        )
    }

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
                        viewModel.deleteItem(currentItem)
                        findNavController().navigate(R.id.action_viewGroupFragment_to_rootFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}