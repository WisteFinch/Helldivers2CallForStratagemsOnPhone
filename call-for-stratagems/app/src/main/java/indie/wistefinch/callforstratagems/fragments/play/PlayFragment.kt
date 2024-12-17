package indie.wistefinch.callforstratagems.fragments.play

import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import indie.wistefinch.callforstratagems.CFSApplication
import indie.wistefinch.callforstratagems.MainActivity
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.data.models.GroupData
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModelFactory
import indie.wistefinch.callforstratagems.databinding.FragmentPlayBinding
import indie.wistefinch.callforstratagems.socket.Client
import indie.wistefinch.callforstratagems.socket.ReceiveAuthData
import indie.wistefinch.callforstratagems.socket.ReceiveStatusData
import indie.wistefinch.callforstratagems.socket.RequestAuthPacket
import indie.wistefinch.callforstratagems.socket.RequestStatusPacket
import indie.wistefinch.callforstratagems.socket.StratagemInputData
import indie.wistefinch.callforstratagems.socket.StratagemInputPacket
import indie.wistefinch.callforstratagems.socket.StratagemMacroData
import indie.wistefinch.callforstratagems.socket.StratagemMacroPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Vector
import kotlin.math.abs

class PlayFragment : Fragment() {

    /**
     * The stratagem view model,.
     */
    private val stratagemViewModel: StratagemViewModel by activityViewModels {
        StratagemViewModelFactory(
            (activity?.application as CFSApplication).stratagemDb.stratagemDao()
        )
    }

    // View binding.
    private var _binding: FragmentPlayBinding? = null
    private val binding get() = _binding!!

    // Runtime variables.
    /**
     * The group data used this time.
     */
    private lateinit var groupData: GroupData

    /**
     * The selected stratagem entry.
     */
    private lateinit var currentItem: StratagemData

    /**
     * Is it in free input mode, if yes, display the free input Ui.
     */
    private var isFreeInput: Boolean = false

    /**
     * Is there a stratagem entry selected, if yes, display the steps Ui.
     */
    private var itemSelected: Boolean = false

    /**
     * The steps corresponding to the current stratagem entry, the number will +4 to indicate that user input the step.
     */
    private lateinit var stepsList: MutableList<Int>

    /**
     * The current step position of the stepsList.
     */
    private var currentStepPos: Int = 0

    /**
     * Store the original screen settings, and restore it when closing the fragment.
     */
    private var oriSystemUiVisibility: Int = 0

    /**
     * Version of the app.
     */
    private lateinit var version: String

    // Recycler view adapters.
    /**
     * The stratagem recycler view's adapter.
     */
    private val stratagemAdapter: StratagemPlayAdapter by lazy { StratagemPlayAdapter() }

    /**
     * The simplified stratagem recycler view's adapter.
     */
    private val stratagemSimplifiedAdapter: StratagemSimplifiedAdapter by lazy { StratagemSimplifiedAdapter() }

    /**
     * The step recycler view's adapter.
     */
    private val stepAdapter: StepPlayAdapter by lazy { StepPlayAdapter() }

    // The configuration of the gesture detector.
    /**
     * The minimum distance for gesture detector to respond.
     */
    private var distanceThreshold = 100.0

    /**
     * The minimum velocity for gesture detector to respond.
     */
    private var velocityThreshold = 50.0

    // Other preferences
    /**
     * Keep only marcos.
     */
    private var enableSimplifiedMode = false

    // Runtime variables for socket.
    /**
     * The socket client.
     */
    private val client = Client()

    /**
     * Whether the socket is connect.
     */
    private var isConnected: Boolean = false

    /**
     * Reflect whether network communication is currently in progress.
     *
     * Lock during any network communication.
     */
    private var networkLock = Mutex()

    /**
     * Reflect whether it is currently attempting to connect to the server.
     *
     * Lock during the connect process.
     */
    private var connectingLock = Mutex()

    /**
     * Server address. Get from the preference.
     */
    private var address: String = "127.0.0.1"

    /**
     * Server port. Get from the preference.
     */
    private var port: Int = 23333

    /**
     * Connect retry times limit. Get from the preference.
     */
    private var retryLimit: Int = 5

    /**
     * Device secure id.
     */
    private lateinit var sid: String

    /**
     * Socket authentication token.
     */
    private var token: String = "NULL"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Get preference.
        val preferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }!!
        distanceThreshold = preferences.getString("swipe_distance_threshold", "100")?.toDouble()!!
        velocityThreshold = preferences.getString("swipe_velocity_threshold", "50")?.toDouble()!!
        enableSimplifiedMode = preferences.getBoolean("enable_simplified_mode", false)
        address = preferences.getString("tcp_add", "127.0.0.1")!!
        port = preferences.getString("tcp_port", "23333")?.toInt()!!
        retryLimit = preferences.getString("tcp_retry", "5")?.toInt()!!
        sid = preferences.getString("sid", "0")!!

        // Set screen.
        if (!enableSimplifiedMode) {
            // For compatibility with lower SDKs, ignore the deprecated warning.
            @Suppress("DEPRECATION")
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            @Suppress("DEPRECATION")
            oriSystemUiVisibility = activity?.window?.decorView?.systemUiVisibility!!
            @Suppress("DEPRECATION")
            activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        view?.keepScreenOn = true

        // Hide toolbar.
        (activity as MainActivity).supportActionBar?.hide()

        // Inflate the layout for this fragment.
        _binding = FragmentPlayBinding.inflate(inflater, container, false)
        val view = binding.root

        // Disable back gesture on some devices.
        if (!enableSimplifiedMode) {
            binding.root.doOnNextLayout {
                val exclusions = listOf(Rect(0, 0, it.width, it.height))
                ViewCompat.setSystemGestureExclusionRects(binding.root, exclusions)
            }
        }

        // Check simplified mode
        if (enableSimplifiedMode) {
            binding.playBlank.visibility = View.INVISIBLE
            binding.playBanner.visibility = View.INVISIBLE
            binding.playMode.visibility = View.INVISIBLE
            binding.playExit.visibility = View.INVISIBLE
            binding.playStratagemScrollView.visibility = View.INVISIBLE
            binding.playGesture.visibility = View.INVISIBLE
            binding.playSimplifiedScrollView.visibility = View.VISIBLE
        }

        // Init runtime.
        // For compatibility with lower SDKs, ignore the deprecated warning.
        @Suppress("DEPRECATION")
        groupData = arguments?.getParcelable("currentItem")!!
        isFreeInput = false

        // Get version.
        val pkgName = context?.packageName!!
        val pkgInfo = context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
        version = pkgInfo.versionName

        setupRecyclerView()
        setupEventListener()

        // Setup client using continue.
        lifecycleScope.launch {
            setupClient()
            this.launch {
                withContext(Dispatchers.IO) {
                    clientKeepAlive()
                }
            }
        }

        return view
    }

    /**
     * Setup stratagem recycler view in this fragment.
     */
    private fun setupRecyclerView() {
        val preference = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (enableSimplifiedMode) {
            // Setup simplified stratagem recycler view.
            val stratagemView = binding.playSimplifiedRecyclerView
            stratagemView.adapter = stratagemSimplifiedAdapter
            stratagemView.autoFitColumns(100)
            // Retrieve stratagem entry from database and check the validation.
            val list: Vector<StratagemData> = Vector()
            for (i in groupData.list) {
                if (stratagemViewModel.isIdValid(i)) {
                    list.add(stratagemViewModel.retrieveItem(i))
                }
            }
            stratagemSimplifiedAdapter.setData(list.toList(),
                preference.getString("db_name", context?.resources?.getString(R.string.db_hd2_name))!!)
            // Setup click listener.
            stratagemSimplifiedAdapter.onItemClick = { data ->
                // Activate stratagem
                lifecycleScope.launch {
                    activateStratagem(data)
                }
            }
        }
        else {
            // Setup stratagem recycler view.
            val stratagemView = binding.playStratagemRecyclerView
            stratagemView.adapter = stratagemAdapter
            stratagemView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            // Retrieve stratagem entry from database and check the validation.
            val list: Vector<StratagemData> = Vector()
            for (i in groupData.list) {
                if (stratagemViewModel.isIdValid(i)) {
                    list.add(stratagemViewModel.retrieveItem(i))
                }
            }
            stratagemAdapter.setData(list.toList(),
                preference.getString("db_name", context?.resources?.getString(R.string.db_hd2_name))!!)
            // Setup click listener.
            stratagemAdapter.onItemClick = { data ->
                onStratagemClicked(data)
            }
            // Enable "swipe and activate stratagem"(macro).
            swipeToActivate(stratagemView)


            // Setup step recycler view, the data will be specified when selecting a stratagem entry.
            val stepView = binding.playStepsRecyclerView
            stepView.adapter = stepAdapter
            stepView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        }
    }

    private fun setupEventListener() {
        // Setup exit button.
        binding.playExit.setOnClickListener {
            findNavController().popBackStack()
        }

        // Setup free input button.
        binding.playMode.setOnClickListener {
            setFreeInputMode(!isFreeInput)
        }

        // Setup gesture detector.
        // Override the detector.
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
                    // Calculate direction.
                    if (abs(diffX) > abs(diffY))
                    {
                        // left or right swipe.
                        if (abs(diffX) > distanceThreshold && abs(velocityX) > velocityThreshold) {
                            if (diffX >= 0) {
                                // right.
                                onInputting(4)
                            }
                            else {
                                // left.
                                onInputting(3)
                            }
                            return true
                        }
                        else {
                            return super.onFling(e1, e2, velocityX, velocityY)
                        }
                    }
                    else {
                        // top or bottom swipe.
                        if (abs(diffY) > distanceThreshold && abs(velocityY) > velocityThreshold) {
                            if (diffY >= 0) {
                                // bottom.
                                onInputting(2)
                            }
                            else {
                                // top.
                                onInputting(1)
                            }
                            return true
                        }
                        else {
                            return super.onFling(e1, e2, velocityX, velocityY)
                        }
                    }
                }
            })
        // Setup the touch listener.
        binding.playGesture.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                view.performClick()
            }
            gestureDetector.onTouchEvent(event)
        }
    }

    /**
     * Called when clicking a stratagem in the recycler view, change the steps recycler view.
     */
    private fun onStratagemClicked(data: StratagemData) {
        // Reset.
        currentStepPos = 0

        // Set Uis and flags.
        if (!isFreeInput) {
            if (itemSelected && currentItem == data) {
                resetUi()
            }
            else {
                itemSelected = true
                currentItem = data
                stepAdapter.clear()
                stepAdapter.setData(data.steps)
                stepsList = data.steps.toMutableList()
                binding.playBlank.visibility = View.INVISIBLE
                binding.playStratagemTitle.visibility = View.VISIBLE
                binding.playStepsScrollView.visibility = View.VISIBLE

                val lang: String = context?.resources?.configuration?.locales?.get(0)?.toLanguageTag()!!
                binding.playStratagemTitle.text = when (lang) {
                    "zh-CN" -> data.nameZh
                    else -> data.name
                }
            }
        }
    }

    /**
     * Setup the free input mode.
     */
    private fun setFreeInputMode(flag: Boolean) {
        if (flag) {
            binding.playStratagemScrollView.visibility = View.INVISIBLE
            binding.playBlank.visibility = View.INVISIBLE
            binding.playStratagemTitle.visibility = View.INVISIBLE
            binding.playStepsScrollView.visibility = View.INVISIBLE
            binding.playFreeInput.visibility = View.VISIBLE
            binding.playFreeInputImage.visibility = View.VISIBLE
            if (!isFreeInput) {
                lifecycleScope.launch {
                    activateStep(0, 3)
                }
            }
        }
        else {
            binding.playStratagemScrollView.visibility = View.VISIBLE
            binding.playBlank.visibility = View.VISIBLE
            binding.playStratagemTitle.visibility = View.INVISIBLE
            binding.playStepsScrollView.visibility = View.INVISIBLE
            binding.playFreeInput.visibility = View.INVISIBLE
            binding.playFreeInputImage.visibility = View.INVISIBLE
            if (isFreeInput) {
                lifecycleScope.launch {
                    activateStep(0, 4)
                }
            }

        }
        isFreeInput = flag
    }

    /**
     * Analyse input.
     */
    fun onInputting(dir: Int) {
        if (isFreeInput) { // In free input mode, activate step independently.
            lifecycleScope.launch {
                activateStep(dir, 0)
            }
        }
        else if (itemSelected){ // In normal mode, record input.
            if (dir == stepsList[currentStepPos]) {
                stepsList[currentStepPos] += 4
                currentStepPos++
                stepAdapter.setData(stepsList)
            }
            // If the input is complete , activate the stratagem.
            if (currentStepPos >= stepsList.size) {
                finishInput()
            }
        }
    }

    /**
     * Called when stratagem inputs is complete.
     */
    private fun finishInput() {
        // Activate stratagem
        lifecycleScope.launch {
            activateStratagem(currentItem)
        }
        resetUi()
    }

    /**
     * Reset Ui
     */
    private fun resetUi() {
        // Reset Uis and flags
        itemSelected = false
        currentStepPos = 0
        stepAdapter.clear()
        binding.playBlank.visibility = View.VISIBLE
        binding.playStratagemTitle.visibility = View.INVISIBLE
        binding.playStepsScrollView.visibility = View.INVISIBLE
    }

    /**
     * Called when swiping a stratagem item, activate it immediately (like macro).
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
                // Restore recycle view.
                recyclerView.adapter?.notifyItemChanged(viewHolder.adapterPosition)
                // Activate stratagem.
                val item = stratagemAdapter.dataList[viewHolder.adapterPosition]
                lifecycleScope.launch {
                    activateStratagem(item)
                }
                resetUi()
            }
        }
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    /**
     * Setup the tcp client.
     */
    private suspend fun setupClient() {
        connectingLock.lock()
        var tryTimes = 0
        withContext(Dispatchers.IO) {
            // Initial connection.
            withContext(Dispatchers.Main) {
                binding.playConnectTitle.text = String.format(
                    getString(R.string.network_add_suffix),
                    String.format(getString(R.string.network_connecting)),
                    address,
                    port
                )
                binding.playConnectStatus.drawable.setTintList(context?.resources?.getColorStateList(R.color.orange, context?.theme))
            }
            networkLock.withLock {
                isConnected = client.connect(address, port)
            }
            isConnected = checkClient()

            // If the connection is not successful, retry.
            while (!isConnected && tryTimes < 5) {
                withContext(Dispatchers.Main) {
                    binding.playConnectTitle.text = String.format(
                        getString(R.string.network_add_suffix),
                        String.format(
                            getString(R.string.network_waiting),
                            tryTimes,
                            retryLimit
                        ),
                        address,
                        port
                    )
                }
                tryTimes++
                delay(2000)
                withContext(Dispatchers.Main) {
                    binding.playConnectTitle.text = String.format(
                        getString(R.string.network_add_suffix),
                        String.format(
                            getString(R.string.network_retry),
                            tryTimes,
                            retryLimit
                        ),
                        address,
                        port
                    )
                }
                networkLock.withLock {
                    isConnected = client.connect(address, port)
                }
                isConnected = checkClient()
            }

            // Set connection status.
            withContext(Dispatchers.Main) {
                if (isConnected) {
                    binding.playConnectTitle.text = String.format(
                        getString(R.string.network_add_suffix),
                        String.format(getString(R.string.network_connected)),
                        address,
                        port
                    )
                    binding.playConnectStatus.drawable.setTintList(context?.resources?.getColorStateList(R.color.green, context?.theme))
                }
                else {
                    binding.playConnectTitle.text = String.format(
                        getString(R.string.network_add_suffix),
                        String.format(getString(R.string.network_failed)),
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
     * Check if the client is valid by sending heartbeat package.
     */
    private suspend fun checkClient(): Boolean {
        if (!isConnected) {
            return false
        }
        var flag: Boolean
        networkLock.withLock {
            try {
                // Send status request.
                client.send(Gson().toJson(RequestStatusPacket(version)).toString())
                // Receive status.
                val res: ReceiveStatusData = Gson().fromJson(client.receive(), ReceiveStatusData::class.java)
                // Check the server status.
                when (res.status) {
                    0 -> flag = true
                    1 -> flag = false
                    2 -> {
                        // Request authentication.
                        withContext(Dispatchers.Main) {
                            binding.playConnectTitle.text = String.format(
                                getString(R.string.network_add_suffix),
                                String.format(getString(R.string.network_auth), sid),
                                address,
                                port
                            )
                        }
                        client.send(Gson().toJson(RequestAuthPacket(sid)).toString())
                        client.toggleTimeout(false)
                        val auth = Gson().fromJson(client.receive(), ReceiveAuthData::class.java)
                        client.toggleTimeout(true)
                        if (auth.auth) {
                            token = auth.token
                            flag = true
                        }
                        else {
                            flag = false
                        }
                    }
                    else -> flag = false
                }
            } catch (_: Exception) { // Json convert error & socket timeout.
                flag = false
            }
        }
        return flag
    }

    /**
     * Check if the client is valid every 10s, if not, reconnect.
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
     * Activate stratagem, send stratagem data to the server.
     */
    private suspend fun activateStratagem(stratagemData: StratagemData) {
        val lang: String = context?.resources?.configuration?.locales?.get(0)?.toLanguageTag()!!
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
            val packet = StratagemMacroPacket(
                StratagemMacroData(
                    when (lang) {
                        "zh-CN" -> stratagemData.nameZh
                        else -> stratagemData.name
                    },
                    stratagemData.steps
                ),
                token
            )

            if(networkLock.tryLock()) {
                try {
                    client.send(Gson().toJson(packet).toString())
                }
                catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
                networkLock.unlock()
            }
        }
    }

    /**
     * Activate step, send step data to the server.
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
            val packet = StratagemInputPacket(
                StratagemInputData(
                    step,
                    type
                ),
                token
            )
            if (networkLock.tryLock()) {
                try {
                    client.send(Gson().toJson(packet).toString())
                }
                catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
                networkLock.unlock()
            }
        }
    }

    override fun onDestroy() {
        // Reset screen
        if (!enableSimplifiedMode) {
            // For compatibility with lower SDKs, ignore the deprecated warning.
            @Suppress("DEPRECATION")
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            @Suppress("DEPRECATION")
            activity?.window?.decorView?.systemUiVisibility = oriSystemUiVisibility
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        view?.keepScreenOn = false

        // Show toolbar
        (activity as MainActivity).supportActionBar?.show()

        // Close client
        if (connectingLock.isLocked) {
            client.forceClose()
        }
        else {
            client.disconnect()
        }

        super.onDestroy()
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