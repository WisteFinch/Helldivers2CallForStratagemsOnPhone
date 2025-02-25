package indie.wistefinch.callforstratagems.fragments.root

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import indie.wistefinch.callforstratagems.utils.AppButton
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
            // Setup dialog.
            val dialog = AlertDialog.Builder(requireContext()).create()
            val view: View = View.inflate(requireContext(), R.layout.dialog_info, null)
            dialog.setView(view)
            dialog.show()

            view.findViewById<TextView>(R.id.dialog_info_title).setText(R.string.hint_db_incomplete)
            view.findViewById<TextView>(R.id.dialog_info_msg)
                .setText(R.string.hint_db_incomplete_desc)
            view.findViewById<AppButton>(R.id.dialog_info_button1).setOnClickListener {
                dialog.hide()
            }
            val button2 = view.findViewById<AppButton>(R.id.dialog_info_button2)
            button2.setTitle(resources.getString(R.string.dialog_settings))
            button2.setOnClickListener {
                val bundle = bundleOf(Pair("jump_to_entry", R.id.set_info_db))
                findNavController().navigate(R.id.settingsFragment, bundle)
                dialog.hide()
            }
            val button3 = view.findViewById<AppButton>(R.id.dialog_info_button3)
            button3.visibility = VISIBLE
            button3.setTitle(resources.getString(R.string.dialog_ignore))
            button3.setAlert(true)
            button3.setOnClickListener {
                preferences.edit().putBoolean("hint_db_incomplete", true).apply()
                dialog.hide()
            }
        }

        // Check and show welcome.
        val pkgName = context?.packageName!!
        val pkgInfo = context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
        val ver = pkgInfo.versionName
        val ignoreWelcome = preferences.getBoolean("hint_welcome_$ver", false)
        if (!ignoreWelcome) {
            // Setup dialog.
            val dialog = AlertDialog.Builder(requireContext()).create()
            val view: View = View.inflate(requireContext(), R.layout.dialog_info, null)
            dialog.setView(view)
            dialog.setOnDismissListener {
                preferences.edit().putBoolean("hint_welcome_$ver", true).apply()
            }
            dialog.show()

            view.findViewById<TextView>(R.id.dialog_info_title).text =
                String.format(resources.getString(R.string.hint_welcome), ver)
            view.findViewById<TextView>(R.id.dialog_info_msg).setText(R.string.hint_welcome_desc)
            view.findViewById<AppButton>(R.id.dialog_info_button1).setOnClickListener {
                preferences.edit().putBoolean("hint_welcome_$ver", true).apply()
                dialog.hide()
            }
            @SuppressLint("CutPasteId")
            view.findViewById<AppButton>(R.id.dialog_info_button2).visibility = GONE
            val button3 = view.findViewById<AppButton>(R.id.dialog_info_button3)
            button3.visibility = VISIBLE
            button3.setTitle(resources.getString(R.string.hint_welcome_usage))
            button3.setOnClickListener {
                preferences.edit().putBoolean("hint_welcome_$ver", true).apply()
                val uri = Uri.parse(resources.getString(R.string.usage_url))
                val internet = Intent(Intent.ACTION_VIEW, uri)
                internet.addCategory(Intent.CATEGORY_BROWSABLE)
                startActivity(internet)
                dialog.hide()
            }
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

        // Init menu.
        binding.rootMenuList.setOnClickListener {
            findNavController().navigate(R.id.action_rootFragment_to_stratagemsListFragment)
        }
        binding.rootMenuSettings.setOnClickListener {
            findNavController().navigate(R.id.action_rootFragment_to_settingsFragment)
        }

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

    /**
     * Show the group database empty image.
     */
    private fun showEmptyDbViews(empty: Boolean) {
        if (empty) {
            binding.noGroupTextView.visibility = VISIBLE
            binding.noGroupImageView.visibility = VISIBLE
        } else {
            binding.noGroupTextView.visibility = INVISIBLE
            binding.noGroupImageView.visibility = INVISIBLE
        }
    }

    /**
     * Setup the group recycler view.
     */
    private fun setupRecyclerView() {
        val preferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }!!

        val recyclerView = binding.rootRecyclerView
        recyclerView.adapter = adapter
        recyclerView.recycledViewPool.setMaxRecycledViews(0, 0)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        adapter.setStratagemViewModel(stratagemViewModel)
        groupViewModel.allItems.observe(viewLifecycleOwner) { data ->
            sharedViewModel.checkIfDbIsEmpty(data)
            adapter.setData(data, preferences.getBoolean("enable_fastboot_mode", false))
        }
    }
}