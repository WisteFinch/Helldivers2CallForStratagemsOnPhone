package indi.wistefinch.callforstratagems.fragments.editgroup

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import indi.wistefinch.callforstratagems.R
import indi.wistefinch.callforstratagems.CFSApplication
import indi.wistefinch.callforstratagems.data.models.GroupData
import indi.wistefinch.callforstratagems.data.viewmodel.GroupViewModel
import indi.wistefinch.callforstratagems.data.viewmodel.GroupViewModelFactory
import indi.wistefinch.callforstratagems.databinding.FragmentEditGroupBinding


class EditGroupFragment : Fragment() {

    private val viewModel: GroupViewModel by activityViewModels {
        GroupViewModelFactory(
            (activity?.application as CFSApplication).database.groupDao()
        )
    }

    private var _binding: FragmentEditGroupBinding? = null
    private val binding get() = _binding!!

    private lateinit var currentItem: GroupData
    private var isEdit: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentEditGroupBinding.inflate(inflater, container, false)
        val view = binding.root

        // Setup view with arguments
        isEdit = arguments?.getBoolean("isEdit")!!
        if (isEdit) {
            currentItem = arguments?.getParcelable("currentItem")!!
            binding.editGroupTitle.setText(currentItem.title)
        } else {
            binding.editGroupTitle.text = null
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.edit_group_fragment_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.editGroup_menu_save -> {
                        // Save data to database
                        if (isEdit) {
                            updateDataToDb()
                        }
                        else {
                            insertDataToDb()
                        }
                        findNavController().navigate(R.id.action_editGroupFragment_to_rootFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    fun updateDataToDb() {
        viewModel.updateItem(
            currentItem.id,
            when (binding.editGroupTitle.text.toString()) {
                "" -> {
                    binding.editGroupTitle.autofillHints!![0]
                }
                else -> binding.editGroupTitle.text.toString()
            },
            listOf(0)
        )
    }

    fun insertDataToDb() {
        viewModel.addItem(
            0,
            when (binding.editGroupTitle.text.toString()) {
                "" -> {
                    binding.editGroupTitle.autofillHints!![0]
                }
                else -> binding.editGroupTitle.text.toString()
            },
            listOf(0)
        )
    }

}