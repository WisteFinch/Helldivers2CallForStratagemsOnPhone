package indie.wistefinch.callforstratagems.fragments.stratagemslist

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import indie.wistefinch.callforstratagems.CFSApplication
import indie.wistefinch.callforstratagems.Constants
import indie.wistefinch.callforstratagems.data.viewmodel.AsrKeywordViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.AsrKeywordViewModelFactory
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModelFactory
import indie.wistefinch.callforstratagems.databinding.FragmentStratagemsListBinding
import indie.wistefinch.callforstratagems.fragments.viewgroup.StratagemViewAdapter

class StratagemsListFragment : Fragment() {

    /**
     * The stratagem view model.
     */
    private val stratagemViewModel: StratagemViewModel by activityViewModels {
        StratagemViewModelFactory(
            (activity?.application as CFSApplication).stratagemDb.stratagemDao()
        )
    }

    /**
     * The Asr keyword view model.
     */
    private val asrKeywordViewModel: AsrKeywordViewModel by activityViewModels {
        AsrKeywordViewModelFactory(
            (activity?.application as CFSApplication).asrKeywordDb.asrKeywordDao()
        )
    }

    /**
     * The stratagem recycler view's adapter.
     */
    private val adapter: StratagemViewAdapter by lazy { StratagemViewAdapter() }

    // View binding.
    private var _binding: FragmentStratagemsListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentStratagemsListBinding.inflate(inflater, container, false)
        val view = binding.root

        // Init menu
        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }

        // Get database name
        val preference = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val dbName =
            preference.getString("db_name", Constants.ID_DB_HD2)!!
        var lang = preference.getString("ctrl_lang", "auto")!!
        if (lang == "auto") {
            lang = requireContext().resources.configuration.locales.get(0).toLanguageTag()
        }

        val displayName = when (lang) {
            "zh-CN" -> preference.getString("db_name_zh", "")!!
            else -> preference.getString("db_name_en", "")!!
        }
        binding.stratagemsListTitle.text = displayName.ifEmpty {
            dbName
        }

        val recyclerView = binding.stratagemsListRecyclerView
        recyclerView.adapter = adapter
        recyclerView.autoFitColumns(90)
        adapter.setData(
            stratagemViewModel.getAllItems(),
            dbName,
            lang,
            asrKeywordViewModel,
            requireActivity()
        )

        return view
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