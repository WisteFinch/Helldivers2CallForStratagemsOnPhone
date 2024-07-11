package indi.wistefinch.callforstratagems.fragments.play

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
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
import kotlin.math.abs

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
    private var itemSelected: Boolean = false
    private lateinit var stepsList: MutableList<Int>
    private var currentStepPos: Int = 0

    // original screen settings
    private var oriSystemUiVisibility: Int = 0

    // recycler view adapter
    private val stratagemAdapter: StratagemPlayAdapter by lazy { StratagemPlayAdapter() }
    private val stepAdapter: StepPlayAdapter by lazy { StepPlayAdapter() }

    // gesture
    private var distanceThreshold = 100.0
    private var velocityThreshold = 50.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Set full screen
        // For compatibility with lower SDKs, ignore the warning
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

        // Get preference
        val preferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }!!
        distanceThreshold = preferences.getString("scroll_distance_threshold", "100")?.toDouble()!!
        velocityThreshold = preferences.getString("scroll_velocity_threshold", "50")?.toDouble()!!

        setupRecyclerView()
        setupEventListener()

        return view
    }

    private fun setupRecyclerView() {
        // Setup stratagem recycler view
        val stratagemView = binding.playStratagemRecyclerView
        stratagemView.adapter = stratagemAdapter
        stratagemView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
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

        // gesture detector
        val gestureDetector = GestureDetector(activity,
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    val diffX = -(e1?.x?.minus(e2.x) ?: 0.0).toDouble()
                    val diffY = -(e1?.y?.minus(e2.y) ?: 0.0).toDouble()
                    // Calculate direction
                    if (abs(diffX) > abs(diffY))
                    {
                        // left or right swipe
                        if (abs(diffX) > distanceThreshold && abs(velocityX) > velocityThreshold) {
                            if (diffX >= 0) {
                                // right
                                onScrolling(4)
                            }
                            else {
                                // left
                                onScrolling(3)
                            }
                            return true
                        }
                        else {
                            return super.onFling(e1, e2, velocityX, velocityY)
                        }
                    }
                    else {
                        // top or bottom swipe
                        if (abs(diffY) > distanceThreshold && abs(velocityY) > velocityThreshold) {
                            if (diffY >= 0) {
                                // bottom
                                onScrolling(2)
                            }
                            else {
                                // top
                                onScrolling(1)
                            }
                            return true
                        }
                        else {
                            return super.onFling(e1, e2, velocityX, velocityY)
                        }
                    }
                }
            })
        binding.playGesture.setOnTouchListener { view, event ->
            if(event.action == MotionEvent.ACTION_UP) {
                view.performClick()
            }
            gestureDetector.onTouchEvent(event)
        }
    }

    /**
     * Called when clicking a stratagem in the recycler view, change the steps recycler view
     */
    private fun onStratagemClicked(data: StratagemData) {
        // Reset some flags
        currentStepPos = 0

        // Set Uis and flags
        if (!isFreeInput) {
            itemSelected = true
            currentItem = data
            stepAdapter.setData(data.steps)
            stepsList = data.steps.toMutableList()
            binding.playStratagemTitle.text = data.name
            binding.playBlank.visibility = View.INVISIBLE
            binding.playStratagemTitle.visibility = View.VISIBLE
            binding.playStepsScrollView.visibility = View.VISIBLE
        }
        else {
            itemSelected = false
            binding.playStratagemTitle.visibility = View.INVISIBLE
            binding.playStepsScrollView.visibility = View.INVISIBLE
        }
    }

    /**
     * Setup the free input mode
     */
    private fun setFreeInputMode(flag: Boolean) {
        isFreeInput = flag
        if (flag) {
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

    /**
     * Analyse scroll input
     */
    fun onScrolling(dir: Int) {
        if (isFreeInput) {
            // TODO: Free input
        }
        else if (itemSelected){
            if (dir == stepsList[currentStepPos]) {
                stepsList[currentStepPos] += 4
                currentStepPos++
                stepAdapter.setData(stepsList)
            }
            if (currentStepPos >= stepsList.size) {
                finishInput()
            }
        }
    }

    /**
     * Called when stratagem inputs is complete
     */
    private fun finishInput() {
        // TODO: tcp
        // Reset Uis and flags
        itemSelected = false
        currentStepPos = 0
        stepAdapter.setData(listOf())
        binding.playBlank.visibility = View.VISIBLE
        binding.playStratagemTitle.visibility = View.INVISIBLE
        binding.playStepsScrollView.visibility = View.INVISIBLE
    }

    override fun onDestroy() {
        // Reset screen
        // For compatibility with lower SDKs, ignore the warning
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.window?.decorView?.systemUiVisibility = oriSystemUiVisibility

        // Show toolbar
        (activity as MainActivity).supportActionBar?.show()

        super.onDestroy()
    }
}