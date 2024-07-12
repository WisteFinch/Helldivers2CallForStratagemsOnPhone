package indi.wistefinch.callforstratagems.fragments.play

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import indi.wistefinch.callforstratagems.CFSApplication
import indi.wistefinch.callforstratagems.MainActivity
import indi.wistefinch.callforstratagems.R
import indi.wistefinch.callforstratagems.data.models.GroupData
import indi.wistefinch.callforstratagems.data.models.StratagemData
import indi.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import indi.wistefinch.callforstratagems.data.viewmodel.StratagemViewModelFactory
import indi.wistefinch.callforstratagems.databinding.FragmentPlayBinding
import indi.wistefinch.callforstratagems.socket.Client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
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

    // Socket
    private val client = Client()
    private var isConnected: Boolean = false
    private var networkLock = Mutex()
    private var connectingLock = Mutex()
    private var address: String = "127.0.0.1"
    private var port: Int = 23333
    private var retryLimit: Int = 5

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Set screen
        // For compatibility with lower SDKs, ignore the warning
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        oriSystemUiVisibility = activity?.window?.decorView?.systemUiVisibility!!
        activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        view?.keepScreenOn = true

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
        distanceThreshold = preferences.getString("swipe_distance_threshold", "100")?.toDouble()!!
        velocityThreshold = preferences.getString("swipe_velocity_threshold", "50")?.toDouble()!!
        address = preferences.getString("tcp_add", "127.0.0.1")!!
        port = preferences.getString("tcp_port", "23333")?.toInt()!!
        retryLimit = preferences.getString("tcp_retry", "5")?.toInt()!!

        setupRecyclerView()
        setupEventListener()

        // Setup client
        lifecycleScope.launch {
            setupClient()
            this.launch {
                clientKeepAlive()
            }
        }

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
        swipeToActivate(stratagemView)


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
                                onSwiping(4)
                            }
                            else {
                                // left
                                onSwiping(3)
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
                                onSwiping(2)
                            }
                            else {
                                // top
                                onSwiping(1)
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
            lifecycleScope.launch {
                activateStep(0, 1)
            }
        }
        else {
            binding.playStratagemScrollView.visibility = View.VISIBLE
            binding.playBlank.visibility = View.VISIBLE
            binding.playStratagemTitle.visibility = View.INVISIBLE
            binding.playStepsScrollView.visibility = View.INVISIBLE
            binding.playFreeInput.visibility = View.INVISIBLE
            binding.playFreeInputImage.visibility = View.INVISIBLE
            lifecycleScope.launch {
                activateStep(0, 2)
            }
        }

    }

    /**
     * Analyse swipe input
     */
    fun onSwiping(dir: Int) {
        if (isFreeInput) {
            lifecycleScope.launch {
                activateStep(dir, 0)
            }
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
        // Activate stratagem
        lifecycleScope.launch {
            activateStratagem(currentItem)
        }
        // Reset Uis and flags
        itemSelected = false
        currentStepPos = 0
        stepAdapter.setData(listOf())
        binding.playBlank.visibility = View.VISIBLE
        binding.playStratagemTitle.visibility = View.INVISIBLE
        binding.playStepsScrollView.visibility = View.INVISIBLE
    }

    /**
     * Called when swiping a stratagem item, activate it immediately (like macro)
     */
    private fun swipeToActivate(recyclerView: RecyclerView) {
        val callback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.LEFT, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Restore recycle view
                recyclerView.adapter?.notifyItemChanged(viewHolder.adapterPosition)
                // Activate stratagem
                val item = stratagemAdapter.dataList[viewHolder.adapterPosition]
                lifecycleScope.launch {
                    activateStratagem(item)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    /**
     * Setup the tcp client
     */
    private suspend fun setupClient() {
        connectingLock.lock()
        var tryTimes = 0
        withContext(Dispatchers.IO) {
            // Initial connect
            withContext(Dispatchers.Main) {
                binding.playConnectTitle.text = String.format(
                    getString(R.string.network_connecting),
                    address,
                    port
                )
                binding.playConnectStatus.drawable.setTintList(context?.resources?.getColorStateList(R.color.orange, context?.theme))
            }
            networkLock.lock()
            isConnected = client.connect(address, port)
            networkLock.unlock()
            isConnected = checkClient()
            // Retry
            while (!isConnected && tryTimes < 5) {
                tryTimes++
                delay(2000)
                withContext(Dispatchers.Main) {
                    binding.playConnectTitle.text = String.format(
                        getString(R.string.network_retry),
                        address,
                        port,
                        tryTimes,
                        retryLimit
                    )
                }
                networkLock.lock()
                isConnected = client.connect(address, port)
                networkLock.unlock()
                isConnected = checkClient()
            }
            // Set status
            withContext(Dispatchers.Main) {
                if (isConnected) {
                    binding.playConnectTitle.text = String.format(
                        getString(R.string.network_connected),
                        address,
                        port
                    )
                    binding.playConnectStatus.drawable.setTintList(context?.resources?.getColorStateList(R.color.green, context?.theme))
                }
                else {
                    binding.playConnectTitle.text = String.format(
                        getString(R.string.network_failed),
                        address,
                        port
                    )
                    binding.playConnectStatus.drawable.setTintList(context?.resources?.getColorStateList(R.color.red, context?.theme))
                }
            }
        }
        connectingLock.unlock()
    }

    /**
     * Check if the client is valid by sending heartbeat package
     */
    private suspend fun checkClient(): Boolean {
        if(!isConnected) {
            return false
        }
        lateinit var res: String
        withContext(Dispatchers.IO) {
            networkLock.lock()
            try {
                client.send("{\"operation\":0}")
                res = client.receive()

            }
            catch (_: Exception) {
                res = String()
            }
            networkLock.unlock()
        }
        return res == "ready"
    }

    /**
     * Check if the client is valid every 10s, if not, restart it
     */
    private suspend fun clientKeepAlive() {
        while (true) {
            delay(10000)
            if (isConnected && !connectingLock.isLocked) {
                val tmp = checkClient()
                if (!tmp && !connectingLock.isLocked) {
                    setupClient()
                }
            }
        }
    }

    /**
     * Activate stratagem, send stratagem data to the server
     */
    private suspend fun activateStratagem(stratagemData: StratagemData) {
        // Check and restart the client
        if (!isConnected) {
            if (connectingLock.isLocked) {
                return
            }
            setupClient()
            if (!isConnected) {
                return
            }
        }
        // send stratagem data
        withContext(Dispatchers.IO) {
            networkLock.lock()
            try {
                client.send(String.format("{\"operation\":1,\"macro\":{\"name\":\"%s\",\"steps\":%s}}",
                    stratagemData.name,
                    Gson().toJson(stratagemData.steps).toString()
                ))
            }
            catch (e: Exception) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
            }
            networkLock.unlock()
        }
    }

    /**
     * Activate step, send step data to the server
     */
    private suspend fun activateStep(step: Int, type: Int) {
        // Check and restart the client
        if (!isConnected) {
            if (connectingLock.isLocked) {
                return
            }
            setupClient()
            if (!isConnected) {
                return
            }
        }
        // send step data
        withContext(Dispatchers.IO) {
            networkLock.lock()
            try {
                client.send(String.format("{\"operation\":2,\"input\":{\"step\":%s,\"type\":%s}}",
                    step.toString(),
                    type.toString()
                ))
            }
            catch (e: Exception) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
            }
            networkLock.unlock()
        }
    }

    override fun onDestroy() {
        // Reset screen
        // For compatibility with lower SDKs, ignore the warning
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.window?.decorView?.systemUiVisibility = oriSystemUiVisibility
        view?.keepScreenOn = false

        // Show toolbar
        (activity as MainActivity).supportActionBar?.show()

        // Close client
        client.disconnect()

        super.onDestroy()
    }
}