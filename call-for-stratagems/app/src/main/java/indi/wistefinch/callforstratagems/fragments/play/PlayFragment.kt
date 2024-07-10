package indi.wistefinch.callforstratagems.fragments.play

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import indi.wistefinch.callforstratagems.CFSApplication
import indi.wistefinch.callforstratagems.MainActivity
import indi.wistefinch.callforstratagems.data.models.GroupData
import indi.wistefinch.callforstratagems.data.models.StratagemData
import indi.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import indi.wistefinch.callforstratagems.data.viewmodel.StratagemViewModelFactory
import indi.wistefinch.callforstratagems.databinding.FragmentPlayBinding
import java.util.Vector

class PlayFragment : Fragment() {

    private val stratagemViewModel: StratagemViewModel by activityViewModels {
        StratagemViewModelFactory(
            (activity?.application as CFSApplication).stratagemDb.stratagemDao()
        )
    }

    // binding
    private var _binding: FragmentPlayBinding? = null
    private val binding get() = _binding!!

    // runtime
    private lateinit var groupData: GroupData
    private lateinit var currentItem: StratagemData
    private var isFreeInput: Boolean = false
    private var isInputting: Boolean = false

    // original screen settings
    private var oriSystemUiVisibility: Int = 0

    // recycler view adapter
    private val stratagemAdapter: StratagemPlayAdapter by lazy { StratagemPlayAdapter() }
    private val stepAdapter: StepPlayAdapter by lazy { StepPlayAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Set full screen
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        oriSystemUiVisibility = activity?.window?.decorView?.systemUiVisibility!!
        activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        // Hide toolbar
        (activity as MainActivity).supportActionBar?.hide()

        // Inflate the layout for this fragment
        _binding = FragmentPlayBinding.inflate(inflater, container, false)
        val view = binding.root

        // Init runtime
        groupData = arguments?.getParcelable("currentItem")!!
        isFreeInput = false

        setupRecyclerView()
        setupEventListener()

        return view
    }

    private fun setupRecyclerView() {
        // Setup stratagem recycler view
        val stratagemView = binding.playStratagemRecyclerView
        stratagemView.adapter = stratagemAdapter
        stratagemView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        val list: Vector<StratagemData> = Vector()
        for (i in groupData.list) {
            if (stratagemViewModel.isIdValid(i)) {
                list.add(stratagemViewModel.retrieveItem(i))
            }
        }
        stratagemAdapter.onItemClick = { data ->
            onStratagemClicked(data)
        }
        stratagemAdapter.setData(list.toList())

        // Setup step recycler view
        val stepView = binding.playStepsRecyclerView
        stepView.adapter = stepAdapter
        stepView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    }

    private fun setupEventListener() {
        // exit button
        binding.playExit.setOnClickListener {
            findNavController().popBackStack()
        }
        // free input button
        binding.playMode.setOnClickListener {
            setFreeInputMode(!isFreeInput)
        }
    }

    /**
     * Called when clicking a stratagem in the recycler view, change the steps recycler view
     */
    private fun onStratagemClicked(data: StratagemData) {
        if(!isFreeInput) {
            isInputting = true
            currentItem = data
            stepAdapter.setData(data.steps)
            binding.playStratagemTitle.text = data.name
            binding.playBlank.visibility = View.INVISIBLE
            binding.playStratagemTitle.visibility = View.VISIBLE
            binding.playStepsScrollView.visibility = View.VISIBLE
        }
        else {
            isInputting = true
            binding.playStratagemTitle.visibility = View.INVISIBLE
            binding.playStepsScrollView.visibility = View.INVISIBLE
        }
    }

    /**
     * Setup the free input mode
     */
    private fun setFreeInputMode(flag: Boolean) {
        isFreeInput = flag
        if(flag) {
            binding.playStratagemScrollView.visibility = View.INVISIBLE
            binding.playBlank.visibility = View.INVISIBLE
            binding.playStratagemTitle.visibility = View.INVISIBLE
            binding.playStepsScrollView.visibility = View.INVISIBLE
            binding.playFreeInput.visibility = View.VISIBLE
            binding.playFreeInputImage.visibility = View.VISIBLE
        }
        else {
            binding.playStratagemScrollView.visibility = View.VISIBLE
            binding.playBlank.visibility = View.VISIBLE
            binding.playStratagemTitle.visibility = View.INVISIBLE
            binding.playStepsScrollView.visibility = View.INVISIBLE
            binding.playFreeInput.visibility = View.INVISIBLE
            binding.playFreeInputImage.visibility = View.INVISIBLE
        }

    }

    override fun onDestroy() {
        // Cancel full screen
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.window?.decorView?.systemUiVisibility = oriSystemUiVisibility

        // Show toolbar
        (activity as MainActivity).supportActionBar?.show()

        super.onDestroy()
    }
}