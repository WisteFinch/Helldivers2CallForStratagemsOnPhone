package indie.wistefinch.callforstratagems.fragments.root

import android.content.Intent
import android.net.Uri
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.CFSApplication
import indie.wistefinch.callforstratagems.data.viewmodel.GroupViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.GroupViewModelFactory
import indie.wistefinch.callforstratagems.data.viewmodel.SharedViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModelFactory
import indie.wistefinch.callforstratagems.databinding.FragmentRootBinding

class RootFragment : Fragment() {

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
     * The shared view model, used to reflect whether the group database is empty or not.
     */
    private val sharedViewModel: SharedViewModel by viewModels()

    /**
     * The group recycler view's adapter.
     */
    private val adapter: GroupListAdapter by lazy { GroupListAdapter() }

    // View binding.
    private var _binding: FragmentRootBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Check database integrity.
        val dbVer = preferences.getString("db_version", "0")!!
        val ignoreDbCheck = preferences.getBoolean("hint_db_incomplete", false)
        if ((dbVer == "0" || dbVer == "1") && !ignoreDbCheck) {
            val dialog: AlertDialog = AlertDialog.Builder(requireContext())
                .setTitle(R.string.hint_db_incomplete)
                .setMessage(R.string.hint_db_incomplete_desc)
                .setIcon(R.drawable.ic_info)
                .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                }.setNeutralButton(R.string.dialog_no_ask) { _, _ ->
                    preferences.edit().putBoolean("hint_db_incomplete", true).apply()
                }.create()
            dialog.show()
        }

        // Check and show welcome.
        val pkgName = context?.packageName!!
        val pkgInfo = context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
        val ver = pkgInfo.versionName
        val ignoreWelcome = preferences.getBoolean("hint_welcome_$ver", false)
        if (!ignoreWelcome) {
            val dialog: AlertDialog = AlertDialog.Builder(requireContext())
                .setTitle(String.format(resources.getString(R.string.hint_welcome), ver))
                .setMessage(R.string.hint_welcome_desc)
                .setIcon(R.mipmap.ic_launcher)
                .setOnDismissListener {
                    preferences.edit().putBoolean("hint_welcome_$ver", true).apply()
                }.setPositiveButton(R.string.dialog_confirm) { _, _ ->
                    preferences.edit().putBoolean("hint_welcome_$ver", true).apply()
                }.setNeutralButton(R.string.hint_welcome_usage) { _, _ ->
                    preferences.edit().putBoolean("hint_welcome_$ver", true).apply()
                    val uri = Uri.parse(resources.getString(R.string.usage_url))
                    val internet = Intent(Intent.ACTION_VIEW, uri)
                    internet.addCategory(Intent.CATEGORY_BROWSABLE)
                    startActivity(internet)
                }.create()
            dialog.show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRootBinding.inflate(inflater, container, false)
        val view = binding.root

        // Add FAB.
        binding.rootNewGroupFAB.setOnClickListener {
            val bundle = bundleOf(Pair("isEdit", false))
            findNavController().navigate(R.id.action_rootFragment_to_editGroupFragment, bundle)
        }

        setupRecyclerView()

        // Check whether to show the group database empty image.
        sharedViewModel.emptyDatabase.observe(viewLifecycleOwner) {
            showEmptyDbViews(it)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Setup menu.
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
                    R.id.root_menu_stratagemsList -> {
                        findNavController().navigate(R.id.action_rootFragment_to_stratagemsListFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    /**
     * Show the group database empty image.
     */
    private fun showEmptyDbViews(empty: Boolean) {
        if (empty) {
            binding.noGroupTextView.visibility = View.VISIBLE
            binding.noGroupImageView.visibility = View.VISIBLE
        }
        else {
            binding.noGroupTextView.visibility = View.INVISIBLE
            binding.noGroupImageView.visibility = View.INVISIBLE
        }
    }

    /**
     * Setup the group recycler view.
     */
    private fun setupRecyclerView() {
        val recyclerView = binding.rootRecyclerView
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)
        adapter.setStratagemViewModel(stratagemViewModel)
        groupViewModel.allItems.observe(viewLifecycleOwner) { data ->
            sharedViewModel.checkIfDbIsEmpty(data)
            adapter.setData(data)
        }
    }
}