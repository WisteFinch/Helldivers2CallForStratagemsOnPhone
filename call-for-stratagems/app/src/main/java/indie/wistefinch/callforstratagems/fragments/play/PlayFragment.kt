package indie.wistefinch.callforstratagems.fragments.play

import android.app.Service
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.AudioAttributes.USAGE_GAME
import android.media.SoundPool
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibrationEffect.DEFAULT_AMPLITUDE
import android.os.Vibrator
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
import indie.wistefinch.callforstratagems.CFSApplication
import indie.wistefinch.callforstratagems.Constants
import indie.wistefinch.callforstratagems.MainActivity
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.data.models.GroupData
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModelFactory
import indie.wistefinch.callforstratagems.databinding.FragmentPlayBinding
import indie.wistefinch.callforstratagems.network.AppClient
import indie.wistefinch.callforstratagems.network.AppClientEvent
import indie.wistefinch.callforstratagems.network.StratagemInputData
import indie.wistefinch.callforstratagems.network.StratagemMacroData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    /**
     * Enable sound effects.
     */
    private var enableSfx: Boolean = false

    /**
     * Enable vibrator.
     */
    private var enableVibrator: Boolean = false

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
     * Language of stratagem name.
     */
    private var lang: String = "auto"

    // Sound effects and vibrator.
    /**
     * Step input sfx id.
     */
    private var sfxStep: Int = 0

    /**
     * Input failed sfx id.
     */
    private var sfxFail: Int = 1

    /**
     * Stratagem activation sfx id.
     */
    private var sfxActivation: Int = 2

    /**
     * Vibrator.
     */
    private lateinit var vibrator: Vibrator

    /***
     * Sound effects pool.
     */
    private lateinit var sfxPool: SoundPool

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Get preference.
        val preferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }!!
        distanceThreshold = preferences.getInt("ctrl_sdt", 100).toDouble()
        velocityThreshold = preferences.getInt("ctrl_svt", 50).toDouble()
        enableSimplifiedMode = preferences.getBoolean("ctrl_simplified", false)
        enableSfx = preferences.getBoolean("ctrl_sfx", false)
        enableVibrator = preferences.getBoolean("ctrl_vibrator", false)
        address = preferences.getString("conn_addr", "127.0.0.1")!!
        port = preferences.getInt("conn_port", 23333)
        retryLimit = preferences.getInt("conn_retry", 5)
        sid = preferences.getString("sid", "NULL")!!
        lang = preferences.getString("ctrl_lang", "auto")!!
        if (lang == "auto") {
            lang = context?.resources?.configuration?.locales?.get(0)?.toLanguageTag()!!
        }

        // Set screen.
        // For compatibility with lower SDKs, ignore the deprecated warning.
        @Suppress("DEPRECATION")
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        @Suppress("DEPRECATION")
        oriSystemUiVisibility = activity?.window?.decorView?.systemUiVisibility!!
        @Suppress("DEPRECATION")
        activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        activity?.requestedOrientation = if (enableSimplifiedMode) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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
            binding.playBlank.visibility = View.GONE
            binding.playBanner.visibility = View.INVISIBLE
            binding.playMode.visibility = View.GONE
            binding.playExit.visibility = View.GONE
            binding.playFreeInputTitle.visibility = View.INVISIBLE
            binding.playFreeInputImage.visibility = View.INVISIBLE
            binding.playStratagemScrollView.visibility = View.GONE
            binding.playGesture.visibility = View.GONE
            binding.playBgCross.visibility = View.GONE
            binding.playBgMask.visibility = View.GONE
            binding.playSimplifiedScrollView.visibility = View.VISIBLE
            binding.playRoot.setBackgroundColor(requireContext().getColor(R.color.playBackgroundPrimary))
            binding.playModeFAB.visibility = View.VISIBLE
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

        // Setup sfx & vibrator.
        val spb = SoundPool.Builder()
        spb.setMaxStreams(16)
        spb.setAudioAttributes(AudioAttributes.Builder().setFlags(USAGE_GAME).build())
        sfxPool = spb.build()
        sfxStep = sfxPool.load(requireContext(), R.raw.step, 1)
        sfxFail = sfxPool.load(requireContext(), R.raw.fail, 1)
        sfxActivation = sfxPool.load(requireContext(), R.raw.activation, 1)
        // For compatibility with lower SDKs, ignore the deprecated warning.
        @Suppress("DEPRECATION")
        vibrator = requireContext().getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
        if (!vibrator.hasVibrator()) {
            enableVibrator = false
        }

        // Setup view.
        setupRecyclerView()
        setupEventListener()

        // Setup client using coroutine.
        lifecycleScope.launch {
            setupClient()
        }

        return view
    }

    /**
     * Setup client.
     */
    private fun setupClient() {
        AppClient.setEventListener { ev, _ ->
            lifecycleScope.launch(Dispatchers.Main) {
                when (ev) {
                    AppClientEvent.CONNECTED -> {
                        binding.playConnectTitle.text = String.format(
                            getString(R.string.network_addr_suffix),
                            String.format(getString(R.string.network_connected)),
                            address,
                            port
                        )
                        binding.playConnectStatus1.visibility = View.GONE
                        binding.playConnectStatus2.visibility = View.VISIBLE
                        binding.playConnectStatus2.setImageResource(R.drawable.ic_circle)
                        binding.playConnectStatus2.drawable.setTintList(
                            context?.resources?.getColorStateList(
                                R.color.green,
                                context?.theme
                            )
                        )
                    }

                    AppClientEvent.CONNECTING -> {
                        if (AppClient.retriedTimes() > 0) {
                            binding.playConnectTitle.text = String.format(
                                getString(R.string.network_addr_suffix),
                                String.format(
                                    getString(R.string.network_retry),
                                    AppClient.retriedTimes(),
                                    retryLimit
                                ),
                                address,
                                port
                            )
                        } else {
                            binding.playConnectTitle.text = String.format(
                                getString(R.string.network_addr_suffix),
                                String.format(getString(R.string.network_connecting)),
                                address,
                                port
                            )
                            binding.playConnectStatus1.visibility = View.VISIBLE
                            binding.playConnectStatus2.visibility = View.GONE
                        }
                    }

                    AppClientEvent.AUTHING -> {
                        binding.playConnectTitle.text = String.format(
                            getString(R.string.network_addr_suffix),
                            String.format(getString(R.string.network_auth), sid),
                            address,
                            port
                        )
                    }

                    AppClientEvent.RETRYING -> {
                        binding.playConnectTitle.text = String.format(
                            getString(R.string.network_addr_suffix),
                            String.format(
                                getString(R.string.network_waiting),
                                AppClient.retriedTimes(),
                                retryLimit
                            ),
                            address,
                            port
                        )
                    }

                    AppClientEvent.FAILED -> {
                        if (AppClient.retriedTimes() == retryLimit) {
                            binding.playConnectTitle.text = String.format(
                                getString(R.string.network_addr_suffix),
                                String.format(getString(R.string.network_failed)),
                                address,
                                port
                            )
                            binding.playConnectStatus1.visibility = View.GONE
                            binding.playConnectStatus2.visibility = View.VISIBLE
                            binding.playConnectStatus2.setImageResource(R.drawable.ic_alert)
                        }
                    }

                    else -> {}
                }
            }
        }
        AppClient.initClient(address, port, sid, retryLimit)
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
            stratagemView.autoFitColumns(preference.getInt("ctrl_stratagem_size", 100))
            // Retrieve stratagem entry from database and check the validation.
            val list: Vector<StratagemData> = Vector()
            for (i in groupData.list) {
                if (stratagemViewModel.isIdValid(i)) {
                    list.add(stratagemViewModel.retrieveItem(i))
                }
            }
            stratagemSimplifiedAdapter.setData(
                list.toList(),
                preference.getString(
                    "db_name",
                    Constants.ID_DB_HD2
                )!!,
                preference.getInt("ctrl_stratagem_size", 100)
            )
            // Setup click listener.
            stratagemSimplifiedAdapter.onItemClick = { data ->
                // Activate stratagem
                lifecycleScope.launch {
                    activateStratagem(data)
                }
            }
        } else {
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
            stratagemAdapter.setData(
                list.toList(),
                preference.getString(
                    "db_name",
                    Constants.ID_DB_HD2
                )!!
            )
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
        binding.playModeFAB.setOnClickListener {
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
                    if (abs(diffX) > abs(diffY)) {
                        // left or right swipe.
                        if (abs(diffX) > distanceThreshold && abs(velocityX) > velocityThreshold) {
                            if (diffX >= 0) {
                                // right.
                                onInputting(4)
                            } else {
                                // left.
                                onInputting(3)
                            }
                            return true
                        } else {
                            return super.onFling(e1, e2, velocityX, velocityY)
                        }
                    } else {
                        // top or bottom swipe.
                        if (abs(diffY) > distanceThreshold && abs(velocityY) > velocityThreshold) {
                            if (diffY >= 0) {
                                // bottom.
                                onInputting(2)
                            } else {
                                // top.
                                onInputting(1)
                            }
                            return true
                        } else {
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
            } else {
                itemSelected = true
                currentItem = data
                stepAdapter.clear()
                stepAdapter.setData(data.steps)
                stepsList = data.steps.toMutableList()
                binding.playBlank.visibility = View.INVISIBLE
                binding.playStratagemTitle.visibility = View.VISIBLE
                binding.playStepsScrollView.visibility = View.VISIBLE

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
            if (enableSimplifiedMode) {
                binding.playSimplifiedScrollView.visibility = View.INVISIBLE
                binding.playGesture.visibility = View.VISIBLE
                binding.playModeFAB.drawable
                    .setTintList(
                        requireContext().resources.getColorStateList(
                            R.color.highlight,
                            requireContext().theme
                        )
                    )
            } else {
                binding.playStratagemScrollView.visibility = View.INVISIBLE
                binding.playBlank.visibility = View.INVISIBLE
                binding.playStratagemTitle.visibility = View.INVISIBLE
                binding.playStepsScrollView.visibility = View.INVISIBLE
                binding.playMode.drawable
                    .setTintList(
                        requireContext().resources.getColorStateList(
                            R.color.highlight,
                            requireContext().theme
                        )
                    )
            }
            binding.playFreeInputTitle.visibility = View.VISIBLE
            binding.playFreeInputImage.visibility = View.VISIBLE
            if (!isFreeInput) {
                lifecycleScope.launch {
                    activateStep(0, 3)
                }
            }
        } else {
            if (enableSimplifiedMode) {
                binding.playSimplifiedScrollView.visibility = View.VISIBLE
                binding.playGesture.visibility = View.GONE
                binding.playModeFAB.drawable
                    .setTintList(
                        requireContext().resources.getColorStateList(
                            R.color.white,
                            requireContext().theme
                        )
                    )
            } else {
                binding.playStratagemScrollView.visibility = View.VISIBLE
                binding.playBlank.visibility = View.VISIBLE
                binding.playStratagemTitle.visibility = View.INVISIBLE
                binding.playStepsScrollView.visibility = View.INVISIBLE
                binding.playMode.drawable
                    .setTintList(
                        requireContext().resources.getColorStateList(
                            R.color.white,
                            requireContext().theme
                        )
                    )
            }
            binding.playFreeInputTitle.visibility = View.INVISIBLE
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
                when (dir) {
                    1 -> binding.playFreeInputUp.drawable
                        .setTintList(
                            requireContext().resources.getColorStateList(
                                R.color.highlight,
                                requireContext().theme
                            )
                        )

                    2 -> binding.playFreeInputDown.drawable
                        .setTintList(
                            requireContext().resources.getColorStateList(
                                R.color.highlight,
                                requireContext().theme
                            )
                        )

                    3 -> binding.playFreeInputLeft.drawable
                        .setTintList(
                            requireContext().resources.getColorStateList(
                                R.color.highlight,
                                requireContext().theme
                            )
                        )

                    4 -> binding.playFreeInputRight.drawable
                        .setTintList(
                            requireContext().resources.getColorStateList(
                                R.color.highlight,
                                requireContext().theme
                            )
                        )
                }
                delay(200)
                when (dir) {
                    1 -> binding.playFreeInputUp.drawable
                        .setTintList(
                            requireContext().resources.getColorStateList(
                                R.color.white,
                                requireContext().theme
                            )
                        )

                    2 -> binding.playFreeInputDown.drawable
                        .setTintList(
                            requireContext().resources.getColorStateList(
                                R.color.white,
                                requireContext().theme
                            )
                        )

                    3 -> binding.playFreeInputLeft.drawable
                        .setTintList(
                            requireContext().resources.getColorStateList(
                                R.color.white,
                                requireContext().theme
                            )
                        )

                    4 -> binding.playFreeInputRight.drawable
                        .setTintList(
                            requireContext().resources.getColorStateList(
                                R.color.white,
                                requireContext().theme
                            )
                        )
                }
            }
        } else if (itemSelected) { // In normal mode, record input.
            if (dir == stepsList[currentStepPos]) {
                stepsList[currentStepPos] += 4
                currentStepPos++
                stepAdapter.setData(stepsList)
                // Play sfx
                if (enableSfx) {
                    sfxPool.play(sfxStep, 1f, 1f, 0, 0, 1f)
                }
                if (enableVibrator) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            100, DEFAULT_AMPLITUDE
                        )
                    )
                }
            } else {
                // Play sfx
                if (enableSfx) {
                    sfxPool.play(sfxFail, 1f, 1f, 0, 0, 1f)
                }
                if (enableVibrator) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, DEFAULT_AMPLITUDE))
                }
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
        val callback =
            object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
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
     * Activate stratagem, send stratagem data to the server.
     */
    private suspend fun activateStratagem(stratagemData: StratagemData) {
        // Play sfx
        if (enableSfx) {
            sfxPool.play(sfxActivation, 1f, 1f, 0, 0, 1f)
        }
        if (enableVibrator) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, DEFAULT_AMPLITUDE))
        }
        // Send stratagem data
        withContext(Dispatchers.IO) {
            AppClient.optMacro(
                StratagemMacroData(
                    when (lang) {
                        "zh-CN" -> stratagemData.nameZh
                        else -> stratagemData.name
                    },
                    stratagemData.steps
                )
            )
        }
    }

    /**
     * Activate step, send step data to the server.
     */
    private suspend fun activateStep(step: Int, type: Int) {
        // Play sfx
        if (enableSfx) {
            sfxPool.play(sfxStep, 1f, 1f, 0, 0, 1f)
        }
        if (enableVibrator) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, DEFAULT_AMPLITUDE))
        }
        // Send step data
        withContext(Dispatchers.IO) {
            AppClient.optInput(
                StratagemInputData(
                    step,
                    type
                )
            )
        }
    }

    override fun onDestroy() {
        // Reset screen
        // For compatibility with lower SDKs, ignore the deprecated warning.
        @Suppress("DEPRECATION")
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        @Suppress("DEPRECATION")
        activity?.window?.decorView?.systemUiVisibility = oriSystemUiVisibility
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        view?.keepScreenOn = false

        // Close client
        AppClient.closeClient()

        super.onDestroy()
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