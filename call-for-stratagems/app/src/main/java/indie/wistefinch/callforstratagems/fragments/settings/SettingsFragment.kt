package indie.wistefinch.callforstratagems.fragments.settings

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityOptionsCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.king.camera.scan.CameraScan
import indie.wistefinch.callforstratagems.utils.AppButton
import indie.wistefinch.callforstratagems.CFSApplication
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.utils.Util
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModelFactory
import indie.wistefinch.callforstratagems.databinding.FragmentSettingsBinding
import indie.wistefinch.callforstratagems.scanner.QRCodeScanActivity
import indie.wistefinch.callforstratagems.socket.AddressData
import indie.wistefinch.callforstratagems.socket.Client
import indie.wistefinch.callforstratagems.socket.ReceiveAuthData
import indie.wistefinch.callforstratagems.socket.ReceiveStatusData
import indie.wistefinch.callforstratagems.socket.RequestAuthPacket
import indie.wistefinch.callforstratagems.socket.RequestStatusPacket
import indie.wistefinch.callforstratagems.socket.ServerConfigData
import indie.wistefinch.callforstratagems.socket.SyncConfigPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SettingsFragment : Fragment() {
    // View binding.
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // Preference.
    private lateinit var preferences: SharedPreferences

    // Environment
    private lateinit var sid: String
    private lateinit var dbVer: String
    private lateinit var dbName: String

    // Arrays
    private lateinit var inputValues: Array<String>
    private lateinit var inputTypeValues: Array<String>
    private lateinit var langValues: Array<String>

    // Dialogs
    private lateinit var dbDialog: AlertDialog
    private lateinit var appDialog: AlertDialog
    private lateinit var aboutDialog: AlertDialog
    private lateinit var connDialog: AlertDialog
    private lateinit var dbView: View
    private lateinit var appView: View
    private lateinit var aboutView: View
    private lateinit var connView: View

    /**
     * QR code scanner activity launcher
     */
    private val requestQRScanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    val data = result.data?.getStringExtra(CameraScan.SCAN_RESULT)
                    try {
                        val add = Gson().fromJson(data, AddressData::class.java)
                        binding.setConnAddr.setText(add.add)
                        binding.setConnPort.setText(add.port.toString())
                        with(preferences.edit()) {
                            putString("tcp_add", add.add)
                            putString("tcp_port", add.port.toString())
                            apply()
                        }
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.tcp_scan_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (_: Exception) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.tcp_scan_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                RESULT_CANCELED -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.tcp_scan_canceled),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.tcp_scan_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    /**
     * Socket client
     */
    private val client = Client()

    /**
     * Stratagem view model
     */
    private val stratagemViewModel: StratagemViewModel by activityViewModels {
        StratagemViewModelFactory(
            (activity?.application as CFSApplication).stratagemDb.stratagemDao()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root

        // Init menu
        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }

        preferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }!!
        sid = preferences.getString("sid", "0")!!
        dbVer = preferences.getString("db_version", "0")!!
        dbName = preferences.getString("db_name", resources.getString(R.string.db_hd2_name))!!

        inputValues = resources.getStringArray(R.array.input_values)
        inputTypeValues = resources.getStringArray(R.array.input_type_values)
        langValues = resources.getStringArray(R.array.lang_values)

        // Setup dialogs
        dbDialog = AlertDialog.Builder(requireContext()).create()
        dbView = View.inflate(requireContext(), R.layout.dialog_db_update_channel, null)
        dbDialog.setView(dbView)
        appDialog = AlertDialog.Builder(requireContext()).create()
        appView = View.inflate(requireContext(), R.layout.dialog_info, null)
        appDialog.setView(appView)
        aboutDialog = AlertDialog.Builder(requireContext()).create()
        aboutView = View.inflate(requireContext(), R.layout.dialog_info, null)
        aboutDialog.setView(aboutView)
        connDialog = AlertDialog.Builder(requireContext()).create()
        connView = View.inflate(requireContext(), R.layout.dialog_connect, null)
        connDialog.setView(connView)


        setupContent()
        setupEventListener()

        // Jump to specific entries
        if (arguments != null && arguments?.containsKey("jump_to_entry")!!) {
            val id: Int = arguments?.getInt("jump_to_entry")!!
            val entry: View = view.findViewById(id)
            binding.setScrollView.post {
                val location = intArrayOf(0, 0)
                entry.getLocationInWindow(location)
                binding.setScrollView.smoothScrollTo(0, location[1])
            }
            binding.setScrollView.postDelayed({
                view.findViewById<View>(id).performClick()
            }, 300)
        }

        return view
    }

    /**
     * Setup the content of the views
     */
    private fun setupContent() {
        // Connection
        binding.setConnAddr.setText(preferences.getString("tcp_add", "127.0.0.1"))
        binding.setConnPort.setText(preferences.getString("tcp_port", "23333"))
        binding.setConnRetry.setText(preferences.getString("tcp_retry", "5"))
        // Sync
        binding.setSyncPort.setText(preferences.getString("server_port", "23333"))
        binding.setSyncDelay.setText(preferences.getString("input_delay", "25"))
        binding.setSyncInputOpen.setSelection(
            inputValues.indexOf(
                preferences.getString(
                    "input_open",
                    "ctrl_left"
                )
            )
        )
        binding.setSyncTypeOpen.setSelection(
            inputTypeValues.indexOf(
                preferences.getString(
                    "input_type_open",
                    "hold"
                )
            )
        )
        binding.setSyncInputUp.setSelection(
            inputValues.indexOf(
                preferences.getString(
                    "input_up",
                    "w"
                )
            )
        )
        binding.setSyncInputDown.setSelection(
            inputValues.indexOf(
                preferences.getString(
                    "input_down",
                    "s"
                )
            )
        )
        binding.setSyncInputLeft.setSelection(
            inputValues.indexOf(
                preferences.getString(
                    "input_left",
                    "a"
                )
            )
        )
        binding.setSyncInputRight.setSelection(
            inputValues.indexOf(
                preferences.getString(
                    "input_right",
                    "d"
                )
            )
        )
        binding.setSyncAdvance.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_settingsSyncFragment)
        }
        // Control
        binding.setCtrlSimplifiedMode.isChecked =
            preferences.getBoolean("enable_simplified_mode", false)
        binding.setCtrlFastbootMode.isChecked =
            preferences.getBoolean("enable_fastboot_mode", false)
        binding.setCtrlSfx.isChecked =
            preferences.getBoolean("enable_sfx", false)
        binding.setCtrlVibrator.isChecked =
            preferences.getBoolean("enable_vibrator", false)
        binding.setCtrlGstSwpDistanceThreshold.setText(
            preferences.getString(
                "swipe_distance_threshold",
                "100"
            )
        )
        binding.setCtrlGstSwpVelocityThreshold.setText(
            preferences.getString(
                "swipe_velocity_threshold",
                "50"
            )
        )
        binding.setCtrlLangStratagem.setSelection(
            langValues.indexOf(
                preferences.getString(
                    "lang_stratagem",
                    "auto"
                )
            )
        )
        // Info
        // Set database version
        binding.setInfoDb.setHint(
            String.format(
                resources.getString(R.string.info_db_version_desc),
                dbName,
                when (dbVer) {
                    "0" -> resources.getString(R.string.info_db_version_empty)
                    "1" -> resources.getString(R.string.info_db_version_incomplete)
                    else -> dbVer
                }
            )
        )
        // Set app version.
        val pkgName = context?.packageName!!
        val pkgInfo = context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
        binding.setInfoApp.setHint(
            String.format(
                getString(R.string.info_app_version_desc),
                pkgInfo.versionName
            )
        )
    }


    /**
     * Setup event listener
     */
    private fun setupEventListener() {
        // Connection
        binding.setConnAddr.addTextChangedListener { text ->
            with(preferences.edit()) {
                putString("tcp_add", text.toString())
                apply()
            }
        }
        binding.setConnPort.addTextChangedListener { text ->
            with(preferences.edit()) {
                putString("tcp_port", text.toString())
                apply()
            }
        }
        binding.setConnRetry.addTextChangedListener { text ->
            with(preferences.edit()) {
                putString("tcp_retry", text.toString())
                apply()
            }
        }
        // Sync
        binding.setSyncPort.addTextChangedListener { text ->
            with(preferences.edit()) {
                putString("server_port", text.toString())
                apply()
            }
        }
        binding.setSyncDelay.addTextChangedListener { text ->
            with(preferences.edit()) {
                putString("input_delay", text.toString())
                apply()
            }
        }
        binding.setSyncInputOpen.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    with(preferences.edit()) {
                        putString("input_open", inputValues[pos])
                        apply()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        binding.setSyncTypeOpen.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    with(preferences.edit()) {
                        putString("input_type_open", inputTypeValues[pos])
                        apply()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        binding.setSyncInputUp.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    with(preferences.edit()) {
                        putString("input_up", inputValues[pos])
                        apply()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        binding.setSyncInputDown.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    with(preferences.edit()) {
                        putString("input_down", inputValues[pos])
                        apply()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        binding.setSyncInputLeft.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    with(preferences.edit()) {
                        putString("input_left", inputValues[pos])
                        apply()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        binding.setSyncInputRight.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    with(preferences.edit()) {
                        putString("input_right", inputValues[pos])
                        apply()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        // Control
        binding.setCtrlSimplifiedMode.setOnCheckedChangeListener { _, isChecked ->
            with(preferences.edit()) {
                putBoolean("enable_simplified_mode", isChecked)
                apply()
            }
        }
        binding.setCtrlFastbootMode.setOnCheckedChangeListener { _, isChecked ->
            with(preferences.edit()) {
                putBoolean("enable_fastboot_mode", isChecked)
                apply()
            }
        }
        binding.setCtrlSfx.setOnCheckedChangeListener { _, isChecked ->
            with(preferences.edit()) {
                putBoolean("enable_sfx", isChecked)
                apply()
            }
        }
        binding.setCtrlVibrator.setOnCheckedChangeListener { _, isChecked ->
            with(preferences.edit()) {
                putBoolean("enable_vibrator", isChecked)
                apply()
            }
        }
        binding.setCtrlGstSwpDistanceThreshold.addTextChangedListener { text ->
            with(preferences.edit()) {
                putString("swipe_distance_threshold", text.toString())
                apply()
            }
        }
        binding.setCtrlGstSwpVelocityThreshold.addTextChangedListener { text ->
            with(preferences.edit()) {
                putString("swipe_velocity_threshold", text.toString())
                apply()
            }
        }
        binding.setCtrlLangStratagem.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    with(preferences.edit()) {
                        putString("lang_stratagem", langValues[pos])
                        apply()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        // Scan QR Code
        binding.setConnScan.setOnClickListener {
            val optionsCompat = ActivityOptionsCompat.makeCustomAnimation(
                requireContext(),
                R.anim.from_right,
                R.anim.to_left
            )
            val intent = Intent(requireContext(), QRCodeScanActivity::class.java)
            requestQRScanLauncher.launch(intent, optionsCompat)
        }

        // Test Connection
        binding.setConnTest.setOnClickListener {
            if (connDialog.isShowing) {
                return@setOnClickListener
            }
            connDialog.show()
            connView.findViewById<TextView>(R.id.dialog_conn_title).text =
                resources.getString(R.string.set_conn_test_title)
            val progress = connView.findViewById<LinearLayout>(R.id.dialog_conn_progress)
            val msg = connView.findViewById<TextView>(R.id.dialog_conn_msg)
            progress.visibility = VISIBLE
            msg.visibility = GONE
            val button = connView.findViewById<AppButton>(R.id.dialog_conn_button)
            button.setTitle(resources.getString(R.string.dialog_cancel))
            val connFinish = { str: String ->
                progress.visibility = GONE
                msg.visibility = VISIBLE
                msg.text = str
                button.setTitle(resources.getString(R.string.dialog_confirm))
            }

            // Prepare socket and data.
            val add = binding.setConnAddr.text.toString()
            val port: Int = binding.setConnPort.text.toString().toInt()
            val pkgName = context?.packageName!!
            val pkgInfo = context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
            val version = pkgInfo.versionName
            // Launch coroutine.
            val job = lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    // Connect to the server.
                    val connected = client.connect(add, port)
                    if (connected) { // Connection successful, request server status.
                        client.send(Gson().toJson(RequestStatusPacket(version)).toString())
                        try {
                            val res: ReceiveStatusData =
                                Gson().fromJson(client.receive(), ReceiveStatusData::class.java)
                            // Check the server status.
                            when (res.status) {
                                0 -> {
                                    withContext(Dispatchers.Main) {
                                        connFinish(
                                            resources.getText(R.string.tcp_test_success).toString()
                                        )
                                    }
                                }

                                1 -> {
                                    withContext(Dispatchers.Main) {
                                        connFinish(
                                            String.format(
                                                getString(R.string.network_status_1),
                                                version.substring(0, version.lastIndexOf(".")),
                                                res.ver
                                            )
                                        )
                                    }
                                }

                                2 -> {
                                    withContext(Dispatchers.Main) {
                                        connFinish(
                                            String.format(getString(R.string.network_auth), sid)
                                        )
                                    }
                                    // Request authentication.
                                    client.send(Gson().toJson(RequestAuthPacket(sid)).toString())
                                    client.toggleTimeout(false)
                                    val auth = Gson().fromJson(
                                        client.receive(),
                                        ReceiveAuthData::class.java
                                    )
                                    client.toggleTimeout(true)
                                    withContext(Dispatchers.Main) {
                                        if (auth.auth) {
                                            connFinish(
                                                String.format(getString(R.string.tcp_test_success))
                                            )
                                        } else {
                                            connFinish(
                                                String.format(getString(R.string.network_auth_failed))
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    withContext(Dispatchers.Main) {
                                        connFinish(
                                            String.format(
                                                getString(R.string.network_status_error),
                                                res.status
                                            )
                                        )
                                    }
                                }
                            }
                        } catch (_: Exception) { // Json convert error & timeout.
                            withContext(Dispatchers.Main) {
                                connFinish(
                                    resources.getText(R.string.network_response_error).toString()
                                )
                            }
                        }
                    } else { // Connection failed.
                        withContext(Dispatchers.Main) {
                            connFinish(resources.getText(R.string.tcp_test_failed).toString())
                        }
                    }
                    // Disconnect.
                    client.disconnect()
                }
            }
            button.setOnClickListener {
                job.cancel()
                connDialog.hide()
            }
        }

        // Apply config
        binding.setSyncApply.setOnClickListener {
            if (connDialog.isShowing) {
                return@setOnClickListener
            }
            connDialog.show()
            connView.findViewById<TextView>(R.id.dialog_conn_title).text =
                resources.getString(R.string.set_sync_apply)
            val progress = connView.findViewById<LinearLayout>(R.id.dialog_conn_progress)
            val msg = connView.findViewById<TextView>(R.id.dialog_conn_msg)
            progress.visibility = VISIBLE
            msg.visibility = GONE
            val button = connView.findViewById<AppButton>(R.id.dialog_conn_button)
            button.setTitle(resources.getString(R.string.dialog_cancel))
            button.requestLayout()
            val connFinish = { str: String ->
                progress.visibility = GONE
                msg.visibility = VISIBLE
                msg.text = str
                button.setTitle(resources.getString(R.string.dialog_confirm))
                button.requestLayout()
            }

            // Prepare socket and data.
            val config = ServerConfigData(
                port = preferences.getString("server_port", "23333")!!.toInt(),
                delay = preferences.getString("input_delay", "25")!!.toInt(),
                open = preferences.getString("input_open", "ctrl_left")!!,
                openType = preferences.getString("input_type_open", "hold")!!,
                up = preferences.getString("input_up", "w")!!,
                down = preferences.getString("input_down", "s")!!,
                left = preferences.getString("input_left", "a")!!,
                right = preferences.getString("input_right", "d")!!,
                ip = "",
            )
            val add = binding.setConnAddr.text.toString()
            val port: Int = binding.setConnPort.text.toString().toInt()
            val pkgName = context?.packageName!!
            val pkgInfo = context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
            val version = pkgInfo.versionName
            // Launch coroutine.
            val job = lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    // Connect to the server.
                    val connected = client.connect(add, port)
                    if (connected) { // Connection successful, request server status.
                        client.send(Gson().toJson(RequestStatusPacket(version)).toString())
                        try {
                            val res: ReceiveStatusData =
                                Gson().fromJson(client.receive(), ReceiveStatusData::class.java)
                            // Check the server status.
                            when (res.status) {
                                0 -> {
                                    withContext(Dispatchers.Main) {
                                        connFinish(
                                            resources.getText(R.string.tcp_test_success).toString()
                                        )
                                    }
                                }

                                1 -> {
                                    withContext(Dispatchers.Main) {
                                        connFinish(
                                            String.format(
                                                getString(R.string.network_status_1),
                                                version.substring(0, version.lastIndexOf(".")),
                                                res.ver
                                            )
                                        )
                                    }
                                }

                                2 -> {
                                    withContext(Dispatchers.Main) {
                                        connFinish(
                                            String.format(getString(R.string.network_auth), sid)
                                        )
                                    }
                                    // Request authentication.
                                    client.send(Gson().toJson(RequestAuthPacket(sid)).toString())
                                    client.toggleTimeout(false)
                                    val auth = Gson().fromJson(
                                        client.receive(),
                                        ReceiveAuthData::class.java
                                    )
                                    client.toggleTimeout(true)
                                    if (auth.auth) {
                                        client.send(
                                            Gson().toJson(SyncConfigPacket(config, auth.token))
                                                .toString()
                                        )
                                        withContext(Dispatchers.Main) {
                                            connFinish(
                                                resources.getText(R.string.sync_config_finished)
                                                    .toString()
                                            )
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            connFinish(
                                                String.format(getString(R.string.network_auth_failed))
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    withContext(Dispatchers.Main) {
                                        connFinish(
                                            String.format(
                                                getString(R.string.network_status_error),
                                                res.status
                                            )
                                        )
                                    }
                                }
                            }
                        } catch (_: Exception) { // Json convert error & timeout.
                            withContext(Dispatchers.Main) {
                                connFinish(
                                    resources.getText(R.string.network_response_error).toString()
                                )
                            }
                        }
                    } else { // Connection failed.
                        withContext(Dispatchers.Main) {
                            connFinish(resources.getText(R.string.tcp_test_failed).toString())
                        }
                    }
                    // Disconnect.
                    client.disconnect()
                }
            }

            button.setOnClickListener {
                job.cancel()
                connDialog.hide()
            }
        }

        // Open about dialog
        binding.setInfoAbout.setOnClickListener {
            if (aboutDialog.isShowing) {
                return@setOnClickListener
            }
            aboutDialog.show()

            val pkgName = context?.packageName!!
            val pkgInfo = context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
            val curVer = pkgInfo.versionName

            aboutView.findViewById<TextView>(R.id.dialog_info_title).text = String.format(
                resources.getString(R.string.info_about_title),
                curVer
            )
            aboutView.findViewById<ImageView>(R.id.dialog_info_icon)
                .setImageResource(R.drawable.ic_launcher_foreground)
            aboutView.findViewById<TextView>(R.id.dialog_info_msg).setText(R.string.info_about_desc)
            val button1 = aboutView.findViewById<AppButton>(R.id.dialog_info_button1)
            button1.setTitle(resources.getString(R.string.info_about_usage))
            button1.setOnClickListener {
                val uri = Uri.parse(resources.getString(R.string.usage_url))
                val internet = Intent(Intent.ACTION_VIEW, uri)
                internet.addCategory(Intent.CATEGORY_BROWSABLE)
                startActivity(internet)
                aboutDialog.hide()
            }
            val button2 = aboutView.findViewById<AppButton>(R.id.dialog_info_button2)
            button2.setTitle(resources.getString(R.string.info_about_license))
            button2.setOnClickListener {
                val uri = Uri.parse(resources.getString(R.string.license_url))
                val internet = Intent(Intent.ACTION_VIEW, uri)
                internet.addCategory(Intent.CATEGORY_BROWSABLE)
                startActivity(internet)
                aboutDialog.hide()
            }
            val button3 = aboutView.findViewById<AppButton>(R.id.dialog_info_button3)
            button3.setTitle(resources.getString(R.string.info_about_repo))
            button3.visibility = VISIBLE
            button3.setOnClickListener {
                val uri = Uri.parse(resources.getString(R.string.repo_url))
                val internet = Intent(Intent.ACTION_VIEW, uri)
                internet.addCategory(Intent.CATEGORY_BROWSABLE)
                startActivity(internet)
                aboutDialog.hide()
            }
        }

        // Check database update.
        // Launch coroutine
        lifecycleScope.launch {
            checkDBUpdate()
        }

        // Check app update.
        // Launch coroutine
        lifecycleScope.launch {
            binding.setInfoApp.setTitle(resources.getString(R.string.info_app_version_check))
            try {
                val json =
                    JSONObject(Util.downloadToStr(resources.getString(R.string.release_api_url)))
                val newVer = json.getString("tag_name").substring(1)
                withContext(Dispatchers.Main) {
                    // Set version.
                    val pkgName = context?.packageName!!
                    val pkgInfo =
                        context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
                    val curVer = pkgInfo.versionName
                    val title: String
                    if (curVer != newVer) {
                        binding.setInfoApp.setTitle(resources.getString(R.string.info_app_version_updatable))
                        binding.setInfoApp.setHint(
                            String.format(
                                resources.getString(R.string.info_app_version_updatable_desc),
                                pkgInfo.versionName,
                                newVer
                            )
                        )
                        title = String.format(
                            resources.getString(R.string.info_app_version_log_updatable),
                            newVer
                        )
                    } else {
                        binding.setInfoApp.setTitle(resources.getString(R.string.info_app_version))
                        binding.setInfoApp.setHint(
                            String.format(
                                resources.getString(R.string.info_app_version_latest),
                                pkgInfo.versionName
                            )
                        )
                        title = String.format(
                            resources.getString(R.string.info_app_version_log_latest),
                            curVer
                        )
                    }
                    // Open version log.
                    binding.setInfoApp.setOnClickListener {
                        if (appDialog.isShowing) {
                            return@setOnClickListener
                        }
                        appDialog.show()

                        appView.findViewById<TextView>(R.id.dialog_info_title).text = title
                        appView.findViewById<ImageView>(R.id.dialog_info_icon)
                            .setImageResource(R.drawable.ic_launcher_foreground)
                        appView.findViewById<TextView>(R.id.dialog_info_msg).text =
                            json.getString("body")
                        val button1 = appView.findViewById<AppButton>(R.id.dialog_info_button1)
                        button1.setTitle(resources.getString(R.string.dialog_download))
                        button1.setOnClickListener {
                            val uri = Uri.parse(resources.getString(R.string.release_url))
                            val internet = Intent(Intent.ACTION_VIEW, uri)
                            internet.addCategory(Intent.CATEGORY_BROWSABLE)
                            startActivity(internet)
                            appDialog.hide()
                        }
                        appView.findViewById<AppButton>(R.id.dialog_info_button2)
                            .setOnClickListener {
                                appDialog.hide()
                            }
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    binding.setInfoApp.setTitle(resources.getString(R.string.info_app_version))
                }
            }
        }

        // Show update database dialog.
        binding.setInfoDb.setOnClickListener {
            if (dbDialog.isShowing) {
                return@setOnClickListener
            }
            dbDialog.show()

            val clearDialog = AlertDialog.Builder(requireContext()).create()
            val clearView: View = View.inflate(requireContext(), R.layout.dialog_info, null)
            clearDialog.setView(clearView)

            val radioGroup = dbView.findViewById<RadioGroup>(R.id.db_update_group)
            val confirm = dbView.findViewById<AppButton>(R.id.db_update_confirm)
            val cancel = dbView.findViewById<AppButton>(R.id.db_update_cancel)
            val clear = dbView.findViewById<AppButton>(R.id.db_update_clear)
            val custom = dbView.findViewById<EditText>(R.id.db_update_custom_input)
            var channel = preferences.getInt("db_update_channel", 0)

            radioGroup.check(
                when (channel) {
                    0 -> R.id.db_update_hd2
                    1 -> R.id.db_update_hd
                    2 -> R.id.db_update_custom
                    else -> R.id.db_update_hd2
                }
            )
            custom.setText(preferences.getString("db_update_channel_custom", ""))
            custom.isEnabled = channel == 2

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                channel = when (checkedId) {
                    R.id.db_update_hd2 -> 0
                    R.id.db_update_hd -> 1
                    R.id.db_update_custom -> 2
                    else -> 0
                }
                custom.isEnabled = channel == 2
            }

            // Set custom url.
            custom.addTextChangedListener {
                preferences.edit().putString("db_update_channel_custom", custom.text.toString())
                    .apply()
            }

            // Clear cache.
            clear.setAlert(true)
            clear.setOnClickListener {
                if (!clearDialog.isShowing) {
                    clearDialog.show()

                    val title = clearView.findViewById<TextView>(R.id.dialog_info_title)
                    title.setText(R.string.info_db_update_clear)
                    clearView.findViewById<TextView>(R.id.dialog_info_msg)
                        .setText(R.string.info_db_update_clear_desc)
                    val button1 = clearView.findViewById<AppButton>(R.id.dialog_info_button1)
                    button1.setAlert(true)
                    button1.setOnClickListener {
                        val path = context?.filesDir?.path + "/icons"
                        File(path).deleteRecursively()
                        preferences.edit().putString("db_version", "0").apply()
                        preferences.edit().putString("db_name", getString(R.string.default_string))
                            .apply()
                        preferences.edit().putInt("db_update_channel", 0).apply()
                        channel = 0
                        radioGroup.check(R.id.db_update_hd2)
                        stratagemViewModel.deleteAll()

                        Toast.makeText(
                            context,
                            getString(R.string.toast_complete),
                            Toast.LENGTH_SHORT
                        ).show()

                        lifecycleScope.launch {
                            checkDBUpdate()
                        }
                        clearDialog.hide()
                    }
                    clearView.findViewById<AppButton>(R.id.dialog_info_button2).setOnClickListener {
                        clearDialog.hide()
                    }
                }
            }

            // Cancel update.
            cancel.setOnClickListener {
                dbDialog.hide()
            }

            // Update database.
            confirm.setOnClickListener {
                dbDialog.hide()
                preferences.edit().putInt("db_update_channel", channel).apply()
                dbVer = preferences.getString("db_version", "0")!!
                dbName =
                    preferences.getString("db_name", resources.getString(R.string.db_hd2_name))!!

                preferences.edit()
                    .putString("db_name", resources.getString(R.string.default_string)).apply()

                var url: String = when (preferences.getInt("db_update_channel", 0)) {
                    0 -> getString(R.string.db_hd2_url)
                    1 -> getString(R.string.db_hd_url)
                    2 -> preferences.getString("db_update_channel_custom", "")!!
                    else -> getString(R.string.db_hd2_url)
                }
                if (url.isEmpty()) {
                    url = getString(R.string.default_string)
                }
                if (url.substring(url.length - 1) != "/" && url.substring(url.length - 1) != "\\") {
                    url = "$url/"
                }

                // Download database.
                lifecycleScope.launch {
                    binding.setInfoDb.isEnabled = false
                    withContext(Dispatchers.Main) {
                        binding.setInfoDb.setTitle(resources.getString(R.string.info_db_version_updating))
                        binding.setInfoDb.setHint(resources.getString(R.string.info_db_version_updating_index_desc))
                    }

                    try {
                        preferences.edit().putString("db_version", "1").apply()

                        // Download index.
                        val indexObj = JSONObject(Util.downloadToStr(url + "index.json"))
                        val date = indexObj.getString("date")
                        val dbUrl = url + indexObj.getString("db_path")
                        val iconsUrl = url + indexObj.getString("icons_path")
                        val iconsList: MutableList<String> = emptyList<String>().toMutableList()
                        val name = indexObj.getString("name")
                        val iconsPath = context?.filesDir?.path + "/icons/$name/"

                        // Analyze the index.
                        preferences.edit().putString("db_name", name).apply()

                        // Download database.
                        withContext(Dispatchers.Main) {
                            binding.setInfoDb.setHint(
                                String.format(
                                    resources.getString(R.string.info_db_version_updating_db_desc),
                                    name
                                )
                            )
                            binding.setInfoDb.setTitle(resources.getString(R.string.info_db_version_updating))
                        }
                        val dbObj = JSONObject(Util.downloadToStr(dbUrl))
                        // Regenerate database.
                        stratagemViewModel.deleteAll()
                        val rows = dbObj.getJSONArray("objects")
                            .getJSONObject(0)
                            .getJSONArray("rows")
                        for (i in 0 until rows.length()) {
                            val row = rows.getJSONArray(i)
                            val stepsArray = JSONArray(row.getString(4))
                            val steps: MutableList<Int> = emptyList<Int>().toMutableList()
                            for (j in 0 until stepsArray.length()) {
                                steps.add(stepsArray.getInt(j))
                            }
                            var item: StratagemData
                            if (row.length() == 6) {
                                item = StratagemData(
                                    row.getInt(0),
                                    row.getString(1),
                                    row.getString(2),
                                    row.getString(3),
                                    steps,
                                    row.getInt(5)
                                )
                            } else {
                                item = StratagemData(
                                    row.getInt(0),
                                    row.getString(1),
                                    row.getString(2),
                                    row.getString(3),
                                    steps
                                )
                            }
                            iconsList.add(row.getString(3))
                            stratagemViewModel.insertItem(item)
                        }

                        // Download icons.
                        for (i in 0 until iconsList.size) {
                            withContext(Dispatchers.Main) {
                                binding.setInfoDb.setHint(
                                    String.format(
                                        resources.getString(R.string.info_db_version_updating_icons_desc),
                                        name,
                                        i,
                                        iconsList.size
                                    )
                                )
                                binding.setInfoDb.setTitle(resources.getString(R.string.info_db_version_updating))
                            }
                            Util.download(
                                iconsUrl + iconsList[i] + ".svg",
                                iconsPath + iconsList[i] + ".svg",
                                false
                            )
                        }

                        preferences.edit().putString("db_version", date).apply()
                        withContext(Dispatchers.Main) {
                            binding.setInfoDb.setHint(
                                String.format(
                                    resources.getString(R.string.info_db_version_update_complete_desc),
                                    name
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                        withContext(Dispatchers.Main) {
                            binding.setInfoDb.setHint(resources.getString(R.string.info_db_version_update_failed_desc))
                            binding.setInfoDb.isEnabled = true
                        }
                        preferences.edit().putBoolean("hint_db_incomplete", false).apply()
                    }
                    withContext(Dispatchers.Main) {
                        binding.setInfoDb.setTitle(resources.getString(R.string.info_db_version))
                    }
                }
            }
        }
    }

    /**
     * Check if the database is updatable
     */
    private suspend fun checkDBUpdate() {
        binding.setInfoDb.setTitle(resources.getString(R.string.info_db_version_check))
        try {
            var url: String = when (preferences.getInt("db_update_channel", 0)) {
                0 -> getString(R.string.db_hd2_url)
                1 -> getString(R.string.db_hd_url)
                2 -> preferences.getString("db_update_channel_custom", "")!!
                else -> getString(R.string.db_hd2_url)
            }
            if (url.isEmpty()) {
                url = getString(R.string.default_string)
            }
            if (url.substring(url.length - 1) != "/" && url.substring(url.length - 1) != "\\") {
                url = "$url/"
            }

            val json = JSONObject(Util.downloadToStr(url + "index.json"))
            val newVer = json.getString("date")
            dbVer = preferences.getString("db_version", "0")!!
            withContext(Dispatchers.Main) {
                if (dbVer != newVer) {
                    binding.setInfoDb.setHint(
                        String.format(
                            resources.getString(R.string.info_db_version_updatable_desc),
                            preferences.getString("db_name", getString(R.string.db_hd2_name)),
                            when (dbVer) {
                                "0" -> resources.getString(R.string.info_db_version_empty)
                                "1" -> resources.getString(R.string.info_db_version_incomplete)
                                else -> dbVer
                            },
                            json.getString("name"),
                            newVer
                        )
                    )
                    binding.setInfoDb.setTitle(resources.getString(R.string.info_db_version_updatable))
                } else {
                    binding.setInfoDb.setTitle(resources.getString(R.string.info_db_version))
                    binding.setInfoDb.setHint(
                        String.format(
                            resources.getString(R.string.info_db_version_latest),
                            preferences.getString("db_name", getString(R.string.db_hd2_name)),
                            when (dbVer) {
                                "0" -> resources.getString(R.string.info_db_version_empty)
                                "1" -> resources.getString(R.string.info_db_version_incomplete)
                                else -> dbVer
                            }
                        )
                    )
                }
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                binding.setInfoDb.setTitle(resources.getString(R.string.info_db_version))
            }
        }
    }
}