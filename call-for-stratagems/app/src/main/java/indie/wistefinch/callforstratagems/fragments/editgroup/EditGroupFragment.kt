package indie.wistefinch.callforstratagems.fragments.editgroup

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.CFSApplication
import indie.wistefinch.callforstratagems.Constants
import indie.wistefinch.callforstratagems.data.models.GroupData
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.data.viewmodel.GroupViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.GroupViewModelFactory
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModelFactory
import indie.wistefinch.callforstratagems.databinding.FragmentEditGroupBinding
import indie.wistefinch.callforstratagems.utils.ItemTouchHelperCallback

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

    private lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment.
        _binding = FragmentEditGroupBinding.inflate(inflater, container, false)
        val view = binding.root

        // Init menu
        binding.editGroupMenuSave.setOnClickListener {
            // Save data to database
            if (isEdit) {
                updateDataToDb()
            } else {
                insertDataToDb()
            }
            findNavController().popBackStack(R.id.rootFragment, false)
        }
        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }

        preferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }!!

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
                listOf(1, 2, 3), // The default stratagems: Reinforce, SOS and Resupply.,
                preferences.getString("db_name", Constants.ID_DB_HD2)!!
            )
        }

        setupRecyclerView()

        return view
    }

    /**
     * In edit mode: update the existing entry in the database.
     */
    private fun updateDataToDb() {
        groupViewModel.updateItem(
            currentItem.id,
            when (binding.editGroupTitle.text.toString()) {
                "" -> getString(R.string.editGroup_title)
                else -> binding.editGroupTitle.text.toString()
            },
            adapter.getEnabledStratagems(),
            preferences.getString("db_name", Constants.ID_DB_HD2)!!,
            currentItem.idx
        )
    }

    /**
     * Not in edit mode: Add a new entry to the database.
     */
    private fun insertDataToDb() {
        groupViewModel.addItem(
            when (binding.editGroupTitle.text.toString()) {
                "" -> { // User did not name the item, use the default name.
                    binding.editGroupTitle.autofillHints!![0]
                }

                else -> binding.editGroupTitle.text.toString()
            },
            adapter.getEnabledStratagems(),
            preferences.getString("db_name", Constants.ID_DB_HD2)!!
        )
    }

    /**
     * Setup the stratagem recycler view
     */
    private fun setupRecyclerView() {
        // Get views.
        val recyclerView = binding.editGroupRecyclerView
        recyclerView.adapter = adapter
        recyclerView.recycledViewPool.setMaxRecycledViews(0, 0)
        recyclerView.autoFitColumns(90)
        // Init data.
        val list = stratagemViewModel.getAllItems()
        val preference = PreferenceManager.getDefaultSharedPreferences(requireContext())
        var lang: String = preference.getString("ctrl_lang", "auto")!!
        if (lang == "auto") {
            lang = context?.resources?.configuration?.locales?.get(0)?.toLanguageTag()!!
        }
        val orderedList: MutableList<StratagemData> = emptyList<StratagemData>().toMutableList()
        for (i in currentItem.list) {
            if (stratagemViewModel.isIdValid(i)) {
                orderedList.add(stratagemViewModel.retrieveItem(i))
            } else {
                orderedList.add(
                    StratagemData(
                        i,
                        "Unknown [$i]",
                        "未知 [$i]",
                        String(),
                        emptyList()
                    )
                )
            }
        }
        for (i in list) {
            if (!orderedList.contains(i)) {
                orderedList.add(i)
            }
        }
        adapter.setData(
            orderedList,
            currentItem.list.toMutableSet(),
            preference.getString("db_name", Constants.ID_DB_HD2)!!,
            lang
        )
        val callback: ItemTouchHelper.Callback = ItemTouchHelperCallback(adapter)
        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(recyclerView)
    }

    companion object {
        /**
         * Automatically adjust the number of columns
         */
        fun RecyclerView.autoFitColumns(columnWidth: Int) {
            val displayMetrics = this.context.resources.displayMetrics
            val noOfColumns =
                ((displayMetrics.widthPixels / displayMetrics.density) / columnWidth).toInt()
            this.layoutManager = GridLayoutManager(this.context, noOfColumns)
        }
    }
}