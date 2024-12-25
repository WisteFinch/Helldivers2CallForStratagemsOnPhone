package indie.wistefinch.callforstratagems.fragments.viewgroup

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import indie.wistefinch.callforstratagems.CFSApplication
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.data.models.GroupData
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.data.viewmodel.GroupViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.GroupViewModelFactory
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModelFactory
import indie.wistefinch.callforstratagems.databinding.FragmentViewGroupBinding
import java.util.Vector

class ViewGroupFragment : Fragment() {

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
    private val adapter: StratagemViewAdapter by lazy { StratagemViewAdapter() }

    // View binding.
    private var _binding: FragmentViewGroupBinding? = null
    private val binding get() = _binding!!

    /**
     * Currently viewed data.
     */
    private lateinit var currentItem: GroupData

    private lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentViewGroupBinding.inflate(inflater, container, false)
        val view = binding.root

        // For compatibility with lower SDKs, ignore the deprecated warning.
        @Suppress("DEPRECATION")
        currentItem = arguments?.getParcelable("currentItem")!!
        binding.viewGroupTitle.text = currentItem.title

        // Add FAB.
        binding.viewGroupPlayFAB.setOnClickListener {
            val bundle = bundleOf(Pair("currentItem", currentItem))
            findNavController().navigate(R.id.action_viewGroupFragment_to_playFragment, bundle)
        }

        // Check database name.
        preferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }!!
        val dbName = preferences.getString("db_name", getString(R.string.db_hd2_name))
        if (currentItem.dbName != "0" && currentItem.dbName != dbName) {
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle(R.string.hint_group_db_not_match)
                .setMessage(String.format(
                    getString(R.string.hint_group_db_not_match_desc),
                    currentItem.dbName,
                    dbName
                ))
                .setIcon(R.drawable.ic_info)
                .setPositiveButton(R.string.dialog_confirm) { _, _ ->

                }.create()
            dialog.show()
        }

        setupRecyclerView()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Setup menu.
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
        recyclerView.autoFitColumns(100)
        val list: Vector<StratagemData> = Vector()
        for (i in currentItem.list) {
            if (stratagemViewModel.isIdValid(i)) {
                list.add(stratagemViewModel.retrieveItem(i))
            }
        }
        val preference = PreferenceManager.getDefaultSharedPreferences(requireContext())
        var lang: String = preference.getString("lang_stratagem", "auto")!!
        if (lang == "auto") {
            lang = context?.resources?.configuration?.locales?.get(0)?.toLanguageTag()!!
        }
        adapter.setData(list.toList(), preference.getString("db_name",
            context?.resources?.getString(R.string.db_hd2_name))!!,
            lang)
    }

    companion object {
        /**
         * Automatically adjust the number of columns
         */
        fun RecyclerView.autoFitColumns(columnWidth: Int) {
            val displayMetrics = this.context.resources.displayMetrics
            val noOfColumns = ((displayMetrics.widthPixels / displayMetrics.density) / columnWidth).toInt()
            this.layoutManager = GridLayoutManager(this.context, noOfColumns)
        }
    }
}