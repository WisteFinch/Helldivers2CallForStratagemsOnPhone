package indie.wistefinch.callforstratagems.fragments.settings

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import indie.wistefinch.callforstratagems.CFSApplication
import indie.wistefinch.callforstratagems.Constants
import indie.wistefinch.callforstratagems.Constants.PATH_DB_ICONS
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.data.AppSettingsConnData
import indie.wistefinch.callforstratagems.data.AppSettingsCtrlData
import indie.wistefinch.callforstratagems.data.AppSettingsDBData
import indie.wistefinch.callforstratagems.data.AppSettingsData
import indie.wistefinch.callforstratagems.data.BackupFileData
import indie.wistefinch.callforstratagems.data.BackupFileDataUtils
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.data.viewmodel.GroupViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.GroupViewModelFactory
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModelFactory
import indie.wistefinch.callforstratagems.databinding.FragmentSettingsBinding
import indie.wistefinch.callforstratagems.network.AddressData
import indie.wistefinch.callforstratagems.network.AppClient
import indie.wistefinch.callforstratagems.network.AppClientEvent
import indie.wistefinch.callforstratagems.network.SyncConfigAuthData
import indie.wistefinch.callforstratagems.network.SyncConfigData
import indie.wistefinch.callforstratagems.network.SyncConfigInputData
import indie.wistefinch.callforstratagems.network.SyncConfigServerData
import indie.wistefinch.callforstratagems.scanner.QRCodeScanActivity
import indie.wistefinch.callforstratagems.utils.AppButton
import indie.wistefinch.callforstratagems.utils.AppProgressBar
import indie.wistefinch.callforstratagems.utils.DownloadService
import indie.wistefinch.callforstratagems.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream


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
     * Check database update coroutine job
     */
    private lateinit var checkDBUpdateJob: Job

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
                            putString("conn_addr", add.add)
                            putInt("conn_port", add.port)
                            apply()
                        }
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.tcp_scan_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e("[Settings] QRCode Scanner", e.toString())
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
                        getString(R.string.tcp_scan_cancel),
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
     * Export activity launcher
     */
    private val requestExportLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    val uri = result.data?.data!!
                    val sync = SyncConfigData(
                        server = SyncConfigServerData(
                            port = preferences.getInt("sync_server_port", 23333),
                            ip = preferences.getString("sync_server_ip", "")!!,
                        ),
                        input = SyncConfigInputData(
                            delay = preferences.getInt("sync_input_delay", 25),
                            open = preferences.getString("sync_input_open", "ctrl_left")!!,
                            keytype = preferences.getString("input_type_open", "hold")!!,
                            up = preferences.getString("sync_input_up", "w")!!,
                            down = preferences.getString("sync_input_down", "s")!!,
                            left = preferences.getString("sync_input_left", "a")!!,
                            right = preferences.getString("sync_input_right", "d")!!,
                        ),
                        auth = SyncConfigAuthData(
                            enabled = preferences.getBoolean("sync_auth", true),
                            timeout = preferences.getInt(
                                "sync_auth_timeout",
                                3
                            )
                        ),
                        debug = preferences.getBoolean("sync_debug", true),
                    )
                    val conn = AppSettingsConnData(
                        preferences.getString("conn_addr", "127.0.0.1")!!,
                        preferences.getInt("conn_port", 23333),
                        preferences.getInt("conn_retry", 5)
                    )
                    val ctrl = AppSettingsCtrlData(
                        preferences.getBoolean("ctrl_simplified", false),
                        preferences.getInt("ctrl_stratagem_size", 100),
                        preferences.getBoolean("ctrl_fastboot", false),
                        preferences.getBoolean("ctrl_sfx", false),
                        preferences.getBoolean("ctrl_vibrator", false),
                        preferences.getInt(
                            "ctrl_sdt",
                            100
                        ),
                        preferences.getInt(
                            "ctrl_svt",
                            50
                        ),
                        preferences.getString(
                            "ctrl_lang",
                            "auto"
                        )!!
                    )
                    val db = AppSettingsDBData(
                        preferences.getInt("db_channel", 0),
                        preferences.getString("db_custom", "")!!
                    )
                    val settings = AppSettingsData(conn, ctrl, db)
                    try {
                        val json = Gson().toJson(
                            BackupFileData(
                                Constants.API_VERSION,
                                sync,
                                settings,
                                groupViewModel.allItemsSync
                            )
                        )
                        val cr = requireContext().contentResolver
                        val os: OutputStream = cr.openOutputStream(uri)!!
                        os.write(json.toByteArray())
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.set_info_export_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e("[Settings] Export User Data", e.toString())
                        Toast.makeText(
                            requireContext(),
                            String.format(getString(R.string.set_info_export_failed), e.toString()),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                RESULT_CANCELED -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.set_info_export_cancel),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    Toast.makeText(
                        requireContext(),
                        String.format(getString(R.string.set_info_export_failed), "NULL"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private val requestImportLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    val uri = result.data?.data!!
                    try {
                        // Read file
                        val cr = requireContext().contentResolver
                        val os: InputStream = cr.openInputStream(uri)!!
                        val buffer = ByteArray(1024)
                        var len: Int
                        val bos = ByteArrayOutputStream()
                        while ((os.read(buffer).also { len = it }) != -1) {
                            bos.write(buffer, 0, len)
                        }
                        bos.close()
                        val bytes = bos.toString()
                        val json = JSONObject(bytes)
                        val data: BackupFileData = when (json.getInt("ver")) {
                            1 -> {
                                BackupFileDataUtils.fromVer1(bytes)
                            }

                            else -> {
                                BackupFileData(ver = -1)
                            }
                        }
                        // Restore data
                        if (data.ver != -1) {
                            val sync = data.sync
                            val set = data.settings
                            val groups = data.groups
                            val server = sync.server
                            val input = sync.input
                            val auth = sync.auth
                            val ctrl = set.ctrl
                            val conn = set.conn
                            val db = set.db

                            with(preferences.edit()) {
                                putInt("sync_server_port", server.port)
                                putString("sync_server_ip", server.ip)
                                putInt("sync_input_delay", input.delay)
                                putString("sync_input_open", input.open)
                                putString("sync_input_up", input.up)
                                putString("sync_input_down", input.down)
                                putString("sync_input_left", input.left)
                                putString("sync_input_right", input.right)
                                putBoolean("sync_auth", auth.enabled)
                                putInt("sync_auth_timeout", auth.timeout)
                                putBoolean("sync_debug", sync.debug)
                                putString("conn_addr", conn.addr)
                                putInt("conn_port", conn.port)
                                putInt("conn_retry", conn.retry)
                                putBoolean("ctrl_simplified", ctrl.simplified)
                                putInt("ctrl_stratagem_size", ctrl.stratagemSize)
                                putBoolean("ctrl_fastboot", ctrl.fastboot)
                                putBoolean("ctrl_sfx", ctrl.sfx)
                                putBoolean("ctrl_vibrator", ctrl.vibrator)
                                putInt("ctrl_sdt", ctrl.sdt)
                                putInt("ctrl_svt", ctrl.svt)
                                putString("ctrl_lang", ctrl.lang)
                                putInt("db_channel", db.channel)
                                putString("db_custom", db.custom)
                                apply()
                            }

                            // Restore group database
                            for (g in groups) {
                                groupViewModel.addItem(g.title, g.list, g.dbName)
                            }

                            // Refresh content
                            setupContent()

                            Toast.makeText(
                                requireContext(),
                                getString(R.string.set_info_export_success),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.set_info_import_failed_ver),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e("[Settings] Import User Data", e.toString())
                        Toast.makeText(
                            requireContext(),
                            String.format(getString(R.string.set_info_import_failed), e.toString()),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                RESULT_CANCELED -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.set_info_export_cancel),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    Toast.makeText(
                        requireContext(),
                        String.format(getString(R.string.set_info_export_failed), "NULL"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    /**
     * Stratagem view model
     */
    private val stratagemViewModel: StratagemViewModel by activityViewModels {
        StratagemViewModelFactory(
            (requireActivity().application as CFSApplication).stratagemDb.stratagemDao()
        )
    }

    /**
     * Group view model
     */
    private val groupViewModel: GroupViewModel by activityViewModels {
        GroupViewModelFactory(
            (requireActivity().application as CFSApplication).groupDb.groupDao()
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

        preferences = requireContext().let { PreferenceManager.getDefaultSharedPreferences(it) }
        sid = preferences.getString("sid", "0")!!
        dbVer = preferences.getString("db_version", "0")!!
        dbName = preferences.getString("db_name", Constants.ID_DB_HD2)!!

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
        binding.setConnAddr.setText(preferences.getString("conn_addr", "127.0.0.1"))
        binding.setConnPort.setText(preferences.getInt("conn_port", 23333).toString())
        binding.setConnRetry.setText(preferences.getInt("conn_retry", 5).toString())
        // Sync
        binding.setSyncPort.setText(preferences.getInt("sync_server_port", 23333).toString())
        binding.setSyncDelay.setText(preferences.getInt("sync_input_delay", 25).toString())
        binding.setSyncInputOpen.setSelection(
            inputValues.indexOf(
                preferences.getString(
                    "sync_input_open",
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
                    "sync_input_up",
                    "w"
                )
            )
        )
        binding.setSyncInputDown.setSelection(
            inputValues.indexOf(
                preferences.getString(
                    "sync_input_down",
                    "s"
                )
            )
        )
        binding.setSyncInputLeft.setSelection(
            inputValues.indexOf(
                preferences.getString(
                    "sync_input_left",
                    "a"
                )
            )
        )
        binding.setSyncInputRight.setSelection(
            inputValues.indexOf(
                preferences.getString(
                    "sync_input_right",
                    "d"
                )
            )
        )
        binding.setSyncAdvance.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_settingsSyncFragment)
        }
        // Control
        binding.setCtrlSimplifiedMode.isChecked =
            preferences.getBoolean("ctrl_simplified", false)
        binding.setCtrlSimplifiedStratagemSize.setText(
            preferences.getInt(
                "ctrl_stratagem_size",
                100
            ).toString()
        )
        binding.setCtrlFastbootMode.isChecked =
            preferences.getBoolean("ctrl_fastboot", false)
        binding.setCtrlSfx.isChecked =
            preferences.getBoolean("ctrl_sfx", false)
        binding.setCtrlVibrator.isChecked =
            preferences.getBoolean("ctrl_vibrator", false)
        binding.setCtrlGstSwpDistanceThreshold.setText(
            preferences.getInt(
                "ctrl_sdt",
                100
            ).toString()
        )
        binding.setCtrlGstSwpVelocityThreshold.setText(
            preferences.getInt(
                "ctrl_svt",
                50
            ).toString()
        )
        binding.setCtrlLangStratagem.setSelection(
            langValues.indexOf(
                preferences.getString(
                    "ctrl_lang",
                    "auto"
                )
            )
        )
        binding.setCtrlAsr.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_settingsASRFragment)
        }
        // Info
        // Set database version
        binding.setInfoDb.setHint(
            String.format(
                resources.getString(R.string.set_info_db_ver),
                dbName,
                when (dbVer) {
                    "0" -> resources.getString(R.string.set_info_db_empty)
                    "1" -> resources.getString(R.string.set_info_db_incomplete)
                    else -> dbVer
                }
            )
        )
        // Set app version.
        val pkgName = requireContext().packageName
        val pkgInfo = requireContext().applicationContext.packageManager.getPackageInfo(pkgName, 0)
        binding.setInfoApp.setHint(
            String.format(
                getString(R.string.set_info_app_ver),
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
                putString("conn_addr", text.toString().trim())
                apply()
            }
        }
        binding.setConnPort.addTextChangedListener { text ->
            with(preferences.edit()) {
                putInt(
                    "conn_port",
                    (if (text.toString().isEmpty()) 23333 else text.toString().toInt()).coerceIn(0, 65535)
                )
                apply()
            }
        }
        binding.setConnRetry.addTextChangedListener { text ->
            with(preferences.edit()) {
                putInt("conn_retry", (if (text.toString().isEmpty()) 5 else text.toString().toInt()).coerceIn(0, Int.MAX_VALUE))
                apply()
            }
        }
        // Sync
        binding.setSyncPort.addTextChangedListener { text ->
            with(preferences.edit()) {
                putInt(
                    "sync_server_port",
                    (if (text.toString().isEmpty()) 23333 else text.toString().toInt()).coerceIn(0, 65535)
                )
                apply()
            }
        }
        binding.setSyncDelay.addTextChangedListener { text ->
            with(preferences.edit()) {
                putInt(
                    "sync_input_delay",
                    (if (text.toString().isEmpty()) 25 else text.toString().toInt()).coerceIn(0, Int.MAX_VALUE)
                )
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
                        putString("sync_input_open", inputValues[pos])
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
                        putString("sync_input_up", inputValues[pos])
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
                        putString("sync_input_down", inputValues[pos])
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
                        putString("sync_input_left", inputValues[pos])
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
                        putString("sync_input_right", inputValues[pos])
                        apply()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        // Control
        binding.setCtrlSimplifiedMode.setOnCheckedChangeListener { _, isChecked ->
            with(preferences.edit()) {
                putBoolean("ctrl_simplified", isChecked)
                apply()
            }
        }
        binding.setCtrlSimplifiedStratagemSize.addTextChangedListener { text ->
            with(preferences.edit()) {
                putInt(
                    "ctrl_stratagem_size",
                    (if (text.toString().isEmpty()) 100 else text.toString().toInt()).coerceIn(1, 1000)
                )
                apply()
            }
        }
        binding.setCtrlFastbootMode.setOnCheckedChangeListener { _, isChecked ->
            with(preferences.edit()) {
                putBoolean("ctrl_fastboot", isChecked)
                apply()
            }
        }
        binding.setCtrlSfx.setOnCheckedChangeListener { _, isChecked ->
            with(preferences.edit()) {
                putBoolean("ctrl_sfx", isChecked)
                apply()
            }
        }
        binding.setCtrlVibrator.setOnCheckedChangeListener { _, isChecked ->
            with(preferences.edit()) {
                putBoolean("ctrl_vibrator", isChecked)
                apply()
            }
        }
        binding.setCtrlGstSwpDistanceThreshold.addTextChangedListener { text ->
            with(preferences.edit()) {
                putInt(
                    "ctrl_sdt",
                    (if (text.toString().isEmpty()) 100 else text.toString().toInt()).coerceIn(0, Int.MAX_VALUE)
                )
                apply()
            }
        }
        binding.setCtrlGstSwpVelocityThreshold.addTextChangedListener { text ->
            with(preferences.edit()) {
                putInt(
                    "ctrl_svt",
                    (if (text.toString().isEmpty()) 50 else text.toString().toInt()).coerceIn(0, Int.MAX_VALUE)
                )
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
                        putString("ctrl_lang", langValues[pos])
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
            connView.findViewById<TextView>(R.id.dlg_conn_title).text =
                resources.getString(R.string.dlg_conn_test_title)
            val progress = connView.findViewById<LinearLayout>(R.id.dlg_conn_progress)
            val msg = connView.findViewById<TextView>(R.id.dlg_conn_msg)
            progress.visibility = VISIBLE
            msg.visibility = GONE
            val button = connView.findViewById<AppButton>(R.id.dlg_conn_button)
            button.setTitle(resources.getString(R.string.dlg_comm_cancel))
            val connFinish = { str: String ->
                progress.visibility = GONE
                msg.visibility = VISIBLE
                msg.text = str
                button.setTitle(resources.getString(R.string.dlg_comm_confirm))
            }

            // Prepare socket and data.
            val addr = binding.setConnAddr.text.toString()
            val port: Int = binding.setConnPort.text.toString().toInt()

            // Launch coroutine.
            lifecycleScope.launch(Dispatchers.IO) {
                AppClient.setEventListener { ev, _ ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        when (ev) {
                            AppClientEvent.CONNECTED -> {
                                connFinish(
                                    resources.getText(R.string.dlg_conn_test_success).toString()
                                )
                                AppClient.closeClient()
                            }

                            AppClientEvent.AUTHING -> {
                                connFinish(
                                    String.format(getString(R.string.network_auth), sid)
                                )
                            }

                            AppClientEvent.AUTH_FAILED -> {
                                connFinish(
                                    String.format(getString(R.string.network_auth_failed))
                                )
                                AppClient.closeClient()
                            }

                            AppClientEvent.SERVER_ERR -> {
                                connFinish(
                                    String.format(
                                        getString(R.string.network_status_error)
                                    )
                                )
                                AppClient.closeClient()
                            }

                            AppClientEvent.FAILED -> {
                                connFinish(
                                    resources.getText(R.string.dlg_conn_test_failed).toString()
                                )
                                AppClient.closeClient()
                            }

                            AppClientEvent.API_MISMATCH -> {
                                connFinish(
                                    String.format(
                                        getString(R.string.network_status_1),
                                        Constants.API_VERSION.toString()
                                    )
                                )
                                AppClient.closeClient()
                            }

                            else -> {}
                        }
                    }
                }
                AppClient.initClient(addr, port, sid, 0)
            }
            button.setOnClickListener {
                AppClient.closeClient()
                connDialog.hide()
            }
        }

        // Apply config
        binding.setSyncApply.setOnClickListener {
            if (connDialog.isShowing) {
                return@setOnClickListener
            }
            connDialog.show()
            connView.findViewById<TextView>(R.id.dlg_conn_title).text =
                resources.getString(R.string.set_sync_apply)
            val progress = connView.findViewById<LinearLayout>(R.id.dlg_conn_progress)
            val msg = connView.findViewById<TextView>(R.id.dlg_conn_msg)
            progress.visibility = VISIBLE
            msg.visibility = GONE
            val button = connView.findViewById<AppButton>(R.id.dlg_conn_button)
            button.setTitle(resources.getString(R.string.dlg_comm_cancel))
            button.requestLayout()
            val connFinish = { str: String ->
                progress.visibility = GONE
                msg.visibility = VISIBLE
                msg.text = str
                button.setTitle(resources.getString(R.string.dlg_comm_confirm))
                button.requestLayout()
            }

            // Prepare socket and data.
            val config = SyncConfigData(
                server = SyncConfigServerData(
                    port = preferences.getInt("sync_server_port", 23333),
                    ip = preferences.getString("sync_server_ip", "")!!,
                ),
                input = SyncConfigInputData(
                    delay = preferences.getInt("sync_input_delay", 25),
                    open = preferences.getString("sync_input_open", "ctrl_left")!!,
                    keytype = preferences.getString("input_type_open", "hold")!!,
                    up = preferences.getString("sync_input_up", "w")!!,
                    down = preferences.getString("sync_input_down", "s")!!,
                    left = preferences.getString("sync_input_left", "a")!!,
                    right = preferences.getString("sync_input_right", "d")!!,
                ),
                auth = SyncConfigAuthData(
                    enabled = preferences.getBoolean("sync_auth", true),
                    timeout = preferences.getInt(
                        "sync_auth_timeout",
                        3
                    )
                ),
                debug = preferences.getBoolean("sync_debug", true),
            )
            val addr = binding.setConnAddr.text.toString()
            val port: Int = binding.setConnPort.text.toString().toInt()
            // Launch coroutine.
            lifecycleScope.launch(Dispatchers.IO) {
                AppClient.setEventListener { ev, opt ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        when (ev) {
                            AppClientEvent.CONNECTED -> {
                                withContext(Dispatchers.IO) {
                                    AppClient.optSync(config)
                                }
                            }

                            AppClientEvent.AUTHING -> {
                                connFinish(
                                    String.format(getString(R.string.network_auth), sid)
                                )
                            }

                            AppClientEvent.AUTH_FAILED -> {
                                connFinish(
                                    String.format(getString(R.string.network_auth_failed))
                                )
                                AppClient.closeClient()
                            }

                            AppClientEvent.SERVER_ERR -> {
                                connFinish(
                                    String.format(
                                        getString(R.string.network_status_error)
                                    )
                                )
                                AppClient.closeClient()
                            }

                            AppClientEvent.FAILED -> {
                                connFinish(
                                    resources.getText(R.string.dlg_conn_test_failed).toString()
                                )
                                AppClient.closeClient()
                            }

                            AppClientEvent.SENT -> {
                                if (opt == 4) {
                                    connFinish(
                                        resources.getText(R.string.set_sync_apply_finished)
                                            .toString()
                                    )
                                    AppClient.closeClient()
                                }
                            }

                            AppClientEvent.API_MISMATCH -> {
                                connFinish(
                                    String.format(
                                        getString(R.string.network_status_1),
                                        Constants.API_VERSION.toString()
                                    )
                                )
                                AppClient.closeClient()
                            }

                            else -> {}
                        }
                    }
                }
                AppClient.initClient(addr, port, sid, 0)
            }
            button.setOnClickListener {
                AppClient.closeClient()
                connDialog.hide()
            }
        }

        // Open about dialog
        binding.setInfoAbout.setOnClickListener {
            if (aboutDialog.isShowing) {
                return@setOnClickListener
            }
            aboutDialog.show()

            val pkgName = requireContext().packageName
            val pkgInfo = requireContext().applicationContext.packageManager.getPackageInfo(pkgName, 0)
            val curVer = pkgInfo.versionName

            aboutView.findViewById<TextView>(R.id.dlg_info_title).text = String.format(
                resources.getString(R.string.dlg_about_title),
                curVer
            )
            aboutView.findViewById<ImageView>(R.id.dlg_info_icon)
                .setImageResource(R.drawable.ic_launcher_foreground)
            aboutView.findViewById<TextView>(R.id.dlg_info_msg).setText(R.string.dlg_about_desc)
            val button1 = aboutView.findViewById<AppButton>(R.id.dlg_info_button1)
            button1.setTitle(resources.getString(R.string.dlg_about_usage))
            button1.setOnClickListener {
                val uri = Uri.parse(Constants.URL_APP_USAGE)
                val internet = Intent(Intent.ACTION_VIEW, uri)
                internet.addCategory(Intent.CATEGORY_BROWSABLE)
                startActivity(internet)
                aboutDialog.hide()
            }
            val button2 = aboutView.findViewById<AppButton>(R.id.dlg_info_button2)
            button2.setTitle(resources.getString(R.string.dlg_about_license))
            button2.setOnClickListener {
                val uri = Uri.parse(Constants.URL_APP_LICENSE)
                val internet = Intent(Intent.ACTION_VIEW, uri)
                internet.addCategory(Intent.CATEGORY_BROWSABLE)
                startActivity(internet)
                aboutDialog.hide()
            }
            val button3 = aboutView.findViewById<AppButton>(R.id.dlg_info_button3)
            button3.setTitle(resources.getString(R.string.dlg_about_repo))
            button3.visibility = VISIBLE
            button3.setOnClickListener {
                val uri = Uri.parse(Constants.URL_APP_REPO)
                val internet = Intent(Intent.ACTION_VIEW, uri)
                internet.addCategory(Intent.CATEGORY_BROWSABLE)
                startActivity(internet)
                aboutDialog.hide()
            }
        }

        // Check database update.
        // Launch coroutine
        checkDBUpdateJob = lifecycleScope.launch {
            checkDBUpdate()
        }

        // Check app update.
        // Launch coroutine
        lifecycleScope.launch {
            binding.setInfoApp.setTitle(resources.getString(R.string.set_info_app_chk))
            try {
                val json = JSONObject(DownloadService().downloadAsStr(Constants.URL_APP_RELEASE_API))
                val newVer = json.getString("tag_name").substring(1)
                withContext(Dispatchers.Main) {
                    // Set version.
                    val pkgName = requireContext().packageName
                    val pkgInfo =
                        requireContext().applicationContext.packageManager.getPackageInfo(pkgName, 0)
                    val curVer = pkgInfo.versionName
                    val title: String
                    if (curVer != newVer) {
                        binding.setInfoApp.setTitle(resources.getString(R.string.set_info_app_updatable))
                        binding.setInfoApp.setHint(
                            String.format(
                                resources.getString(R.string.set_info_app_ver_diff),
                                pkgInfo.versionName,
                                newVer
                            )
                        )
                        title = String.format(
                            resources.getString(R.string.dlg_app_ver_log_updatable),
                            newVer
                        )
                    } else {
                        binding.setInfoApp.setTitle(resources.getString(R.string.set_info_app))
                        binding.setInfoApp.setHint(
                            String.format(
                                resources.getString(R.string.set_info_app_latest),
                                pkgInfo.versionName
                            )
                        )
                        title = String.format(
                            resources.getString(R.string.dlg_app_ver_log_latest),
                            curVer
                        )
                    }
                    // Open version log.
                    binding.setInfoApp.setOnClickListener {
                        if (appDialog.isShowing) {
                            return@setOnClickListener
                        }
                        appDialog.show()

                        appView.findViewById<TextView>(R.id.dlg_info_title).text = title
                        appView.findViewById<ImageView>(R.id.dlg_info_icon)
                            .setImageResource(R.drawable.ic_launcher_foreground)
                        appView.findViewById<TextView>(R.id.dlg_info_msg).text =
                            json.getString("body")
                        val button1 = appView.findViewById<AppButton>(R.id.dlg_info_button1)
                        button1.setTitle(resources.getString(R.string.dlg_comm_download))
                        button1.setOnClickListener {
                            val uri = Uri.parse(Constants.URL_APP_RELEASE)
                            val internet = Intent(Intent.ACTION_VIEW, uri)
                            internet.addCategory(Intent.CATEGORY_BROWSABLE)
                            startActivity(internet)
                            appDialog.hide()
                        }
                        appView.findViewById<AppButton>(R.id.dlg_info_button2)
                            .setOnClickListener {
                                appDialog.hide()
                            }
                    }
                }
            } catch (e: Exception) {
                Log.e("[Settings] Check App Ver", e.toString())
                withContext(Dispatchers.Main) {
                    binding.setInfoApp.setTitle(resources.getString(R.string.set_info_app))
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

            val radioGroup = dbView.findViewById<RadioGroup>(R.id.dlg_db_update_group)
            val confirm = dbView.findViewById<AppButton>(R.id.dlg_db_update_confirm)
            val cancel = dbView.findViewById<AppButton>(R.id.dlg_db_update_cancel)
            val clear = dbView.findViewById<AppButton>(R.id.dlg_db_update_clear)
            val custom = dbView.findViewById<EditText>(R.id.dlg_db_update_custom_input)
            var channel = preferences.getInt("db_channel", 0)

            radioGroup.check(
                when (channel) {
                    0 -> R.id.dlg_db_update_hd2
                    1 -> R.id.dlg_db_update_hd
                    2 -> R.id.dlg_db_update_custom
                    else -> R.id.dlg_db_update_hd2
                }
            )
            custom.setText(preferences.getString("db_custom", ""))
            custom.isEnabled = channel == 2

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                channel = when (checkedId) {
                    R.id.dlg_db_update_hd2 -> 0
                    R.id.dlg_db_update_hd -> 1
                    R.id.dlg_db_update_custom -> 2
                    else -> 0
                }
                custom.isEnabled = channel == 2
            }

            // Set custom url.
            custom.addTextChangedListener {
                preferences.edit().putString("db_custom", custom.text.toString().trim())
                    .apply()
            }

            // Clear cache.
            clear.setAlert(true)
            clear.setOnClickListener {
                if (!clearDialog.isShowing) {
                    clearDialog.show()

                    val title = clearView.findViewById<TextView>(R.id.dlg_info_title)
                    title.setText(R.string.dlg_db_updt_clear)
                    clearView.findViewById<TextView>(R.id.dlg_info_msg)
                        .setText(R.string.dlg_db_updt_clear_desc)
                    val button1 = clearView.findViewById<AppButton>(R.id.dlg_info_button1)
                    button1.setAlert(true)
                    button1.setOnClickListener {
                        val path = requireContext().filesDir.path + PATH_DB_ICONS
                        File(path).deleteRecursively()
                        preferences.edit().putString("db_version", "0").apply()
                        preferences.edit().putString("db_name", getString(R.string.default_string))
                            .apply()
                        preferences.edit().putInt("db_channel", 0).apply()
                        channel = 0
                        radioGroup.check(R.id.dlg_db_update_hd2)
                        stratagemViewModel.deleteAll()

                        Toast.makeText(
                            context,
                            getString(R.string.toast_complete),
                            Toast.LENGTH_SHORT
                        ).show()

                        if (checkDBUpdateJob.isActive) {
                            checkDBUpdateJob.cancel()
                        }
                        checkDBUpdateJob = lifecycleScope.launch { checkDBUpdate() }
                        clearDialog.hide()
                    }
                    clearView.findViewById<AppButton>(R.id.dlg_info_button2).setOnClickListener {
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
                if (checkDBUpdateJob.isActive) {
                    checkDBUpdateJob.cancel()
                }

                dbDialog.hide()
                preferences.edit().putInt("db_channel", channel).apply()
                dbVer = preferences.getString("db_version", "0")!!
                dbName =
                    preferences.getString("db_name", Constants.ID_DB_HD2)!!

                preferences.edit()
                    .putString("db_name", resources.getString(R.string.default_string)).apply()

                // Get index url.
                var rawUrl: String = when (preferences.getInt("db_channel", 0)) {
                    0 -> Constants.URL_DB_HD2
                    1 -> Constants.URL_DB_HD
                    2 -> preferences.getString("db_custom", "")!!
                    else -> Constants.URL_DB_HD2
                }
                if (rawUrl.isEmpty()) {
                    rawUrl = getString(R.string.default_string)
                }
                val parsedUrl = Utils.parseUrl(rawUrl, "index.json")

                // Init download dialog.
                val downloadDialog = AlertDialog.Builder(requireContext()).create()
                val downloadView: View = View.inflate(requireContext(), R.layout.dialog_download, null)
                downloadDialog.setView(downloadView)
                downloadDialog.setCanceledOnTouchOutside(false)
                val indexView = downloadView.findViewById<LinearLayout>(R.id.dlg_download_index)
                val filesView = downloadView.findViewById<LinearLayout>(R.id.dlg_download_files)
                val titleView = downloadView.findViewById<TextView>(R.id.dlg_download_title)
                val idxTxtView = downloadView.findViewById<TextView>(R.id.dlg_download_index_text)
                val totalPB = downloadView.findViewById<AppProgressBar>(R.id.dlg_download_files_total)
                val itemPB = downloadView.findViewById<AppProgressBar>(R.id.dlg_download_files_item)
                val infoView = downloadView.findViewById<TextView>(R.id.dlg_download_info)
                val buttonView = downloadView.findViewById<AppButton>(R.id.dlg_download_button)
                itemPB.enableHint()

                var isFinished = false

                // Download database.
                val downloadJob = lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        withContext(Dispatchers.Main) {
                            filesView.visibility = GONE
                            indexView.visibility = VISIBLE
                            titleView.setText(R.string.set_info_db_updt_title)
                            idxTxtView.setText(R.string.set_info_db_updt_idx)
                            downloadDialog.show()
                        }

                        preferences.edit().putString("db_version", "1").apply()
                        withContext(Dispatchers.Main) {
                            binding.setInfoDb.setHint(
                                resources.getString(R.string.set_info_db_incomplete)
                            )
                        }

                        // Download index.
                        ensureActive()
                        val indexObj = JSONObject(DownloadService().downloadAsStr(parsedUrl.dir + parsedUrl.fileName))
                        val date = indexObj.getString("date")
                        val dbUrl = parsedUrl.dir + indexObj.getString("db_path")
                        val iconsUrl = parsedUrl.dir + indexObj.getString("icons_path")
                        val iconsList: MutableList<String> = emptyList<String>().toMutableList()
                        val name = indexObj.getString("name")
                        val nameEn = indexObj.getString("nameEn")
                        val nameZh = indexObj.getString("nameZh")
                        val iconsPath = requireContext().filesDir.path + PATH_DB_ICONS + "$name/"
                        val displayName = when (requireContext().resources.configuration.locales.get(0).toLanguageTag()) {
                            "zh-CN" -> nameZh
                            else -> nameEn
                        }

                        // Analyze the index.
                        preferences.edit().putString("db_name", name).apply()
                        preferences.edit().putString("db_name_en", nameEn).apply()
                        preferences.edit().putString("db_name_zh", nameZh).apply()

                        // Download database.
                        withContext(Dispatchers.Main) {
                            titleView.text = String.format(getString(R.string.set_info_db_updt_title2), displayName)
                            idxTxtView.setText(R.string.set_info_db_updt_db)
                        }
                        ensureActive()
                        val dbObj = JSONObject(DownloadService().downloadAsStr(dbUrl))
                        // Regenerate database.
                        ensureActive()
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
                        ensureActive()
                        var index = 0
                        withContext(Dispatchers.Main) {
                            filesView.visibility = VISIBLE
                            indexView.visibility = GONE
                            totalPB.setText(String.format(getString(R.string.set_info_db_updt_icons), index + 1, iconsList.size))
                            totalPB.setValue((index.toFloat() / iconsList.size * 100).toInt())
                            itemPB.setText(iconsUrl + iconsList[index] + ".svg")
                            itemPB.setHint(String.format(getString(R.string.set_info_db_updt_icons_item), 0, "0", "?"))
                            itemPB.setValue(0)
                        }
                        val service = DownloadService()
                        service.onComplete = {
                            ensureActive()
                            index++
                            if (index == iconsList.size) {
                                preferences.edit().putString("db_version", date).apply()
                                buttonView.setTitle(getString(R.string.dlg_comm_confirm))
                                buttonView.setOnClickListener {
                                    downloadDialog.hide()
                                }
                                filesView.visibility = GONE
                                infoView.visibility = VISIBLE
                                infoView.setText(R.string.set_info_db_updt_complete)
                                isFinished = true

                                if (checkDBUpdateJob.isActive) {
                                    checkDBUpdateJob.cancel()
                                }
                                checkDBUpdateJob = lifecycleScope.launch { checkDBUpdate() }
                            } else {
                                totalPB.setText(String.format(getString(R.string.set_info_db_updt_icons), index + 1, iconsList.size))
                                totalPB.setValue((index.toFloat() / iconsList.size * 100).toInt())
                                service.downloadToFile(iconsUrl + iconsList[index] + ".svg", iconsPath + iconsList[index] + ".svg", this)
                            }
                        }
                        service.onProgress = {d, t->
                            val p = if(t.toInt() == -1) 0 else (d.toFloat() / t *100).toInt()
                            itemPB.setText(iconsUrl + iconsList[index] + ".svg")
                            itemPB.setHint(String.format(getString(R.string.set_info_db_updt_icons_item), p, d.toString(), if(t.toInt() == -1) "?" else t.toString()))
                            itemPB.setValue(p)
                        }
                        service.onError = { e ->
                            Log.e("[Settings] Update DB", e.toString())
                            buttonView.setTitle(getString(R.string.dlg_comm_confirm))
                            buttonView.setOnClickListener {
                                downloadDialog.hide()
                            }
                            indexView.visibility = GONE
                            filesView.visibility = GONE
                            infoView.visibility = VISIBLE
                            infoView.text = String.format(getString(R.string.set_info_db_updt_failed), e.toString())
                        }
                        service.downloadToFile(iconsUrl + iconsList[index] + ".svg", iconsPath + iconsList[index] + ".svg", this)
                    } catch (e: Exception) {
                        Log.e("[Settings] Update DB", e.toString())
                        lifecycleScope.launch(Dispatchers.Main) {
                            buttonView.setTitle(getString(R.string.dlg_comm_confirm))
                            buttonView.setOnClickListener {
                                downloadDialog.hide()
                            }
                            indexView.visibility = GONE
                            filesView.visibility = GONE
                            infoView.visibility = VISIBLE
                            infoView.text = String.format(getString(R.string.set_info_db_updt_failed), e.toString())
                        }
                        preferences.edit().putBoolean("hint_db_incomplete", false).apply()
                    }
                }

                buttonView.setOnClickListener {
                    downloadDialog.hide()
                    downloadJob.cancel()
                    preferences.edit().putBoolean("hint_db_incomplete", false).apply()
                    if (checkDBUpdateJob.isActive) {
                        checkDBUpdateJob.cancel()
                    }
                    if (checkDBUpdateJob.isActive) {
                        checkDBUpdateJob.cancel()
                    }
                    checkDBUpdateJob = lifecycleScope.launch { checkDBUpdate() }
                }

                downloadDialog.setOnCancelListener {
                    downloadJob.cancel()
                    if (!isFinished) {
                        preferences.edit().putBoolean("hint_db_incomplete", false).apply()
                        if (checkDBUpdateJob.isActive) {
                            checkDBUpdateJob.cancel()
                        }
                        if (checkDBUpdateJob.isActive) {
                            checkDBUpdateJob.cancel()
                        }
                        checkDBUpdateJob = lifecycleScope.launch { checkDBUpdate() }
                    }
                }
            }
        }

        binding.setInfoExport.setOnClickListener {
            val fileName = "${Constants.BACKUP_FILE_NAME}.json"
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            requestExportLauncher.launch(intent)
        }

        binding.setInfoImport.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            requestImportLauncher.launch(intent)
        }
    }

    /**
     * Check if the database is updatable
     */
    private suspend fun checkDBUpdate() {
        binding.setInfoDb.setTitle(resources.getString(R.string.set_info_db_chk))
        binding.setInfoDb.setHint(
            String.format(
                resources.getString(R.string.set_info_db_ver),
                dbName,
                when (dbVer) {
                    "0" -> resources.getString(R.string.set_info_db_empty)
                    "1" -> resources.getString(R.string.set_info_db_incomplete)
                    else -> dbVer
                }
            )
        )
        try {
            // Get index url.
            var rawUrl: String = when (preferences.getInt("db_channel", 0)) {
                0 -> Constants.URL_DB_HD2
                1 -> Constants.URL_DB_HD
                2 -> preferences.getString("db_custom", "")!!
                else -> Constants.URL_DB_HD2
            }
            if (rawUrl.isEmpty()) {
                rawUrl = getString(R.string.default_string)
            }
            val parsedUrl = Utils.parseUrl(rawUrl, "index.json")

            val json = JSONObject(DownloadService().downloadAsStr(parsedUrl.dir + parsedUrl.fileName))
            val newVer = json.getString("date")
            dbVer = preferences.getString("db_version", "0")!!
            withContext(Dispatchers.Main) {
                if (dbVer != newVer) {
                    binding.setInfoDb.setHint(
                        String.format(
                            resources.getString(R.string.set_info_db_ver_diff),
                            preferences.getString("db_name", Constants.ID_DB_HD2),
                            when (dbVer) {
                                "0" -> resources.getString(R.string.set_info_db_empty)
                                "1" -> resources.getString(R.string.set_info_db_incomplete)
                                else -> dbVer
                            },
                            json.getString("name"),
                            newVer
                        )
                    )
                    binding.setInfoDb.setTitle(resources.getString(R.string.set_info_db_updatable))
                } else {
                    binding.setInfoDb.setTitle(resources.getString(R.string.set_info_db))
                    binding.setInfoDb.setHint(
                        String.format(
                            resources.getString(R.string.set_info_db_latest),
                            preferences.getString("db_name", Constants.ID_DB_HD2),
                            when (dbVer) {
                                "0" -> resources.getString(R.string.set_info_db_empty)
                                "1" -> resources.getString(R.string.set_info_db_incomplete)
                                else -> dbVer
                            }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("[Settings] Check DB Ver", e.toString())
            withContext(Dispatchers.Main) {
                binding.setInfoDb.setTitle(resources.getString(R.string.set_info_db))
            }
        }
    }
}