package indie.wistefinch.callforstratagems.fragments.editgroup

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.CFSApplication
import indie.wistefinch.callforstratagems.data.models.GroupData
import indie.wistefinch.callforstratagems.data.viewmodel.GroupViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.GroupViewModelFactory
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModelFactory
import indie.wistefinch.callforstratagems.databinding.FragmentEditGroupBinding

class EditGroupFragment : Fragment() {

    /**
     * The group view model.
     */
    private val groupViewModel: GroupViewModel by activityViewModels {
        GroupViewModelFactory(
            (activity?.application as CFSApplication).groupDb.groupDao()
        )
    }

    /**
     * The stratagem view model.
     */
    private val stratagemViewModel: StratagemViewModel by activityViewModels {
        StratagemViewModelFactory(
            (activity?.application as CFSApplication).stratagemDb.stratagemDao()
        )
    }

    /**
     * The stratagem recycler view's adapter.
     */
    private val adapter: StratagemEditAdapter by lazy { StratagemEditAdapter() }

    // View binding
    private var _binding: FragmentEditGroupBinding? = null
    private val binding get() = _binding!!

    /**
     * Currently edited data.
     */
    private lateinit var currentItem: GroupData

    /**
     * Is it currently in editing mode.
     * true - Modify existing entry in the database when saving.
     * false - Add new entry to the database when saving.
     */
    private var isEdit: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment.
        _binding = FragmentEditGroupBinding.inflate(inflater, container, false)
        val view = binding.root

        // Setup view with arguments.
        isEdit = arguments?.getBoolean("isEdit")!!
        if (isEdit) { // Edit mode, get existing entry from the fragment argument.
            // For compatibility with lower SDKs, ignore the deprecated warning.
            @Suppress("DEPRECATION")
            currentItem = arguments?.getParcelable("currentItem")!!
            binding.editGroupTitle.setText(currentItem.title)
        } else { // Otherwise, create a new entry.
            binding.editGroupTitle.text = null
            currentItem = GroupData(
                0,
                "",
                listOf(1, 2, 3) // The default stratagems: Reinforce, SOS and Resupply.
            )
        }

        setupRecyclerView()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Setup menu.
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.edit_group_fragment_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection.
                return when (menuItem.itemId) {
                    R.id.editGroup_menu_save -> {
                        // Save data to database
                        if (isEdit) {
                            updateDataToDb()
                        }
                        else {
                            insertDataToDb()
                        }
                        findNavController().popBackStack(R.id.rootFragment, false)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    /**
     * In edit mode: update the existing entry in the database.
     */
    fun updateDataToDb() {
        groupViewModel.updateItem(
            currentItem.id,
            when (binding.editGroupTitle.text.toString()) {
                "" -> {
                    binding.editGroupTitle.autofillHints!![0]
                }
                else -> binding.editGroupTitle.text.toString()
            },
            adapter.enabledStratagem.sorted().toList()
        )
    }

    /**
     * Not in edit mode: Add a new entry to the database.
     */
    fun insertDataToDb() {
        groupViewModel.addItem(
            when (binding.editGroupTitle.text.toString()) {
                "" -> { // User did not name the item, use the default name.
                    binding.editGroupTitle.autofillHints!![0]
                }
                else -> binding.editGroupTitle.text.toString()
            },
            adapter.enabledStratagem.sorted().toList()
        )
    }

    /**
     * Setup the stratagem recycler view
     */
    private fun setupRecyclerView() {
        val recyclerView = binding.editGroupRecyclerView
        recyclerView.adapter = adapter
        recyclerView.autoFitColumns(100)
        val list = stratagemViewModel.allItems
        adapter.setData(list, currentItem.list.toMutableSet())
    }

    companion object {
        fun RecyclerView.autoFitColumns(columnWidth: Int) {
            val displayMetrics = this.context.resources.displayMetrics
            val noOfColumns = ((displayMetrics.widthPixels / displayMetrics.density) / columnWidth).toInt()
            this.layoutManager = GridLayoutManager(this.context, noOfColumns)
        }
    }
}