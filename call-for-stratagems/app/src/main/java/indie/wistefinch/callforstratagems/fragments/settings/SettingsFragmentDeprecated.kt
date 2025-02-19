package indie.wistefinch.callforstratagems.fragments.settings

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityOptionsCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.king.camera.scan.CameraScan
import indie.wistefinch.callforstratagems.CFSApplication
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.Util
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModelFactory
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


class SettingsFragmentDeprecated: PreferenceFragmentCompat() {

    /**
     * The socket client.
     */
    private val client = Client()

    /**
     * The stratagem view model.
     */
    private val stratagemViewModel: StratagemViewModel by activityViewModels {
        StratagemViewModelFactory(
            (activity?.application as CFSApplication).stratagemDb.stratagemDao()
        )
    }

    // View binding.
    // Connection preference
    private lateinit var tcpAddress: EditTextPreference
    private lateinit var tcpPort: EditTextPreference
    private lateinit var tcpScanner: Preference
    private lateinit var tcpTest: Preference

    // Sync preference
    private lateinit var serverPort: EditTextPreference
    private lateinit var inputDelay: EditTextPreference
    private lateinit var inputOpen: ListPreference
    private lateinit var inputTypeOpen: ListPreference
    private lateinit var inputUp: ListPreference
    private lateinit var inputDown: ListPreference
    private lateinit var inputLeft: ListPreference
    private lateinit var inputRight: ListPreference
    private lateinit var syncConfig: Preference

    // Control preference
    private lateinit var swipeDistance: EditTextPreference
    private lateinit var swipeVelocity: EditTextPreference

    // Info preference
    private lateinit var infoDbVersion: Preference
    private lateinit var infoAppVersion: Preference
    private lateinit var infoAbout: Preference

    // Environment
    private lateinit var sid: String
    private lateinit var dbVer: String
    private lateinit var dbName: String

    private lateinit var preferences: SharedPreferences

    // Activity launcher.
    private val requestQRScanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                val data = result.data?.getStringExtra(CameraScan.SCAN_RESULT)
                try {
                    val add = Gson().fromJson(data, AddressData::class.java)
                    tcpAddress.text = add.add
                    tcpPort.text = add.port.toString()
                    Toast.makeText(requireContext(), getString(R.string.tcp_scan_success), Toast.LENGTH_SHORT).show()
                }
                catch (_: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.tcp_scan_failed), Toast.LENGTH_SHORT).show()
                }
            }
            RESULT_CANCELED -> {
                Toast.makeText(requireContext(), getString(R.string.tcp_scan_canceled), Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(requireContext(), getString(R.string.tcp_scan_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Setup binding.
        tcpAddress = preferenceManager.findPreference("tcp_add")!!
        tcpPort = preferenceManager.findPreference("tcp_port")!!
        tcpScanner = preferenceManager.findPreference("tcp_scanner")!!
        tcpTest = preferenceManager.findPreference("tcp_test")!!

        serverPort = preferenceManager.findPreference("server_port")!!
        inputDelay = preferenceManager.findPreference("input_delay")!!
        inputOpen = preferenceManager.findPreference("input_open")!!
        inputTypeOpen = preferenceManager.findPreference("input_type_open")!!
        inputUp = preferenceManager.findPreference("input_up")!!
        inputDown = preferenceManager.findPreference("input_down")!!
        inputLeft = preferenceManager.findPreference("input_left")!!
        inputRight = preferenceManager.findPreference("input_right")!!
        syncConfig = preferenceManager.findPreference("sync_config")!!

        swipeDistance = preferenceManager.findPreference("swipe_distance_threshold")!!
        swipeVelocity = preferenceManager.findPreference("swipe_velocity_threshold")!!

        infoDbVersion = preferenceManager.findPreference("info_db_version")!!
        infoAppVersion = preferenceManager.findPreference("info_app_version")!!
        infoAbout = preferenceManager.findPreference("info_about")!!

        preferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }!!
        sid = preferences.getString("sid", "0")!!
        dbVer = preferences.getString("db_version", "0")!!
        dbName = preferences.getString("db_name", resources.getString(R.string.db_hd2_name))!!

        setupFormat()
        setupEventListener()
        setupContent()
    }

    /**
     * Setup the format of some preferences.
     */
    private fun setupFormat() {
        // Only numeric value
        tcpPort.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        serverPort.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        inputDelay.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        swipeDistance.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        swipeVelocity.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
    }

    /**
     * Setup preference event listener.
     */
    private fun setupEventListener() {
        // Scan QR Code.
        tcpScanner.setOnPreferenceClickListener {
            val optionsCompat = ActivityOptionsCompat.makeCustomAnimation(requireContext(), R.anim.from_right, R.anim.to_left)
            val intent = Intent(requireContext(), QRCodeScanActivity::class.java)
            requestQRScanLauncher.launch(intent, optionsCompat)
            true
        }


        // Test connection preference.
        tcpTest.setOnPreferenceClickListener {
            tcpTest.summary = resources.getText(R.string.tcp_test_connecting)
            // Disable the preference to prevent conflict.
            tcpTest.isEnabled = false
            syncConfig.isEnabled = false
            // Prepare socket and data.
            val add = tcpAddress.text!!
            val port: Int = tcpPort.text?.toInt()!!
            val pkgName = context?.packageName!!
            val pkgInfo = context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
            val version = pkgInfo.versionName
            infoAppVersion.summary = pkgInfo.versionName
            // Launch coroutine.
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    // Connect to the server.
                    val connected = client.connect(add, port)
                    if (connected) { // Connection successful, request server status.
                        withContext(Dispatchers.Main) {
                            tcpTest.summary = resources.getText(R.string.tcp_test_waiting)
                        }
                        client.send(Gson().toJson(RequestStatusPacket(version)).toString())
                        try {
                            val res: ReceiveStatusData = Gson().fromJson(client.receive(), ReceiveStatusData::class.java)
                            // Check the server status.
                            when (res.status) {
                                0 -> {
                                    withContext(Dispatchers.Main) {
                                        tcpTest.summary =
                                            resources.getText(R.string.tcp_test_success)
                                    }
                                }
                                1 -> {
                                    withContext(Dispatchers.Main) {
                                        tcpTest.summary = String.format(
                                            getString(R.string.network_status_1),
                                            version.substring(0, version.lastIndexOf(".")),
                                            res.ver
                                        )
                                    }
                                }
                                2 -> {
                                    withContext(Dispatchers.Main) {
                                        tcpTest.summary =
                                            String.format(getString(R.string.network_auth), sid)
                                    }
                                    // Request authentication.
                                    client.send(Gson().toJson(RequestAuthPacket(sid)).toString())
                                    client.toggleTimeout(false)
                                    val auth = Gson().fromJson(client.receive(), ReceiveAuthData::class.java)
                                    client.toggleTimeout(true)
                                    withContext(Dispatchers.Main) {
                                        if (auth.auth) {
                                            tcpTest.summary =
                                                String.format(getString(R.string.tcp_test_success))
                                        }
                                        else {
                                            tcpTest.summary =
                                                String.format(getString(R.string.network_auth_failed))
                                        }
                                    }
                                }
                                else -> {
                                    tcpTest.summary = String.format(getString(R.string.network_status_error),
                                        res.status
                                    )
                                }
                            }
                        } catch (_: Exception) { // Json convert error & timeout.
                            withContext(Dispatchers.Main) {
                                tcpTest.summary = resources.getText(R.string.network_response_error)
                            }
                        }
                    }
                    else { // Connection failed.
                        withContext(Dispatchers.Main) {
                            tcpTest.summary = resources.getText(R.string.tcp_test_failed)
                        }
                    }
                    // Disconnect.
                    client.disconnect()
                    // Enable the preference.
                    withContext(Dispatchers.Main) {
                        tcpTest.isEnabled = true
                        syncConfig.isEnabled = true
                    }
                }
            }
            true
        }

        // Sync config preference.
        syncConfig.setOnPreferenceClickListener {
            syncConfig.summary = resources.getText(R.string.tcp_test_connecting)
            // Disable the preference to prevent conflict.
            tcpTest.isEnabled = false
            syncConfig.isEnabled = false
            // Prepare socket and data.
            val config = ServerConfigData(
                port = serverPort.text?.toInt()!!,
                delay = inputDelay.text?.toInt()!!,
                open = inputOpen.value!!,
                openType = inputTypeOpen.value!!,
                up = inputUp.value!!,
                down = inputDown.value!!,
                left = inputLeft.value!!,
                right = inputRight.value!!,
                ip = "",
            )
            val add = tcpAddress.text!!
            val port: Int = tcpPort.text?.toInt()!!
            // Launch coroutine
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    // Connect to the server.
                    val connected = client.connect(add, port)
                    if (connected) { // Connection successful.
                        withContext(Dispatchers.Main) {
                            syncConfig.summary =
                                String.format(getString(R.string.network_auth), sid)
                        }
                        // Request authentication.
                        client.send(Gson().toJson(RequestAuthPacket(sid)).toString())
                        client.toggleTimeout(false)
                        try {
                            val auth = Gson().fromJson(client.receive(), ReceiveAuthData::class.java)
                            client.toggleTimeout(true)
                            if (auth.auth) {
                                client.send(Gson().toJson(SyncConfigPacket(config, auth.token)).toString())
                                withContext(Dispatchers.Main) {
                                    syncConfig.summary = resources.getText(R.string.sync_config_finished)
                                }
                            }
                            else {
                                withContext(Dispatchers.Main) {
                                    syncConfig.summary =
                                        String.format(getString(R.string.network_auth_failed))
                                }
                            }
                        } catch (_: Exception) { // Json convert error & timeout.
                            withContext(Dispatchers.Main) {
                                syncConfig.summary = resources.getText(R.string.network_response_error)
                            }
                        }
                    }
                    else { // Connection failed.
                        withContext(Dispatchers.Main) {
                            syncConfig.summary = resources.getText(R.string.tcp_test_failed)
                        }
                    }
                    // Disconnect.
                    client.disconnect()
                    // Enable the preference.
                    withContext(Dispatchers.Main) {
                        tcpTest.isEnabled = true
                        syncConfig.isEnabled = true
                    }
                }
            }
            true
        }

        // Open repository preference.
        infoAbout.setOnPreferenceClickListener {
            val pkgName = context?.packageName!!
            val pkgInfo = context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
            val curVer = pkgInfo.versionName
            val dialog: AlertDialog = AlertDialog.Builder(requireContext())
                .setTitle(String.format(
                    resources.getString(R.string.info_about_title),
                    curVer
                ))
                .setMessage(R.string.info_about_desc)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(R.string.info_about_repo) { _, _ ->
                    val uri = Uri.parse(resources.getString(R.string.repo_url))
                    val internet = Intent(Intent.ACTION_VIEW, uri)
                    internet.addCategory(Intent.CATEGORY_BROWSABLE)
                    startActivity(internet)
                }.setNegativeButton(R.string.info_about_license) { _, _ ->
                    val uri = Uri.parse(resources.getString(R.string.license_url))
                    val internet = Intent(Intent.ACTION_VIEW, uri)
                    internet.addCategory(Intent.CATEGORY_BROWSABLE)
                    startActivity(internet)
                }.setNeutralButton(R.string.info_about_usage) { _, _ ->
                    val uri = Uri.parse(resources.getString(R.string.usage_url))
                    val internet = Intent(Intent.ACTION_VIEW, uri)
                    internet.addCategory(Intent.CATEGORY_BROWSABLE)
                    startActivity(internet)
                }.create()
            dialog.show()
            true
        }

        // Check database update.
        // Launch coroutine
        lifecycleScope.launch {
            checkDBUpdate()
        }

        // Check app update.
        // Launch coroutine
        lifecycleScope.launch {
            infoAppVersion.title = resources.getString(R.string.info_app_version_check)
            try {
                val json = JSONObject(Util.downloadToStr(resources.getString(R.string.release_api_url)))
                val newVer = json.getString("tag_name").substring(1)
                withContext(Dispatchers.Main) {
                    // Set version.
                    val pkgName = context?.packageName!!
                    val pkgInfo = context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
                    val curVer = pkgInfo.versionName
                    var title: String
                    if (curVer != newVer) {
                        infoAppVersion.summary = String.format(
                            resources.getString(R.string.info_app_version_updatable_desc),
                            pkgInfo.versionName,
                            newVer
                        )
                        infoAppVersion.title = resources.getString(R.string.info_app_version_updatable)
                        title = String.format(
                            resources.getString(R.string.info_app_version_log_updatable),
                            newVer
                        )
                    }
                    else {
                        infoAppVersion.title = resources.getString(R.string.info_app_version)
                        infoAppVersion.summary = String.format(
                            resources.getString(R.string.info_app_version_latest),
                            pkgInfo.versionName
                        )
                        title = String.format(
                            resources.getString(R.string.info_app_version_log_latest),
                            curVer
                        )
                    }
                    // Open version log.
                    infoAppVersion.setOnPreferenceClickListener {
                        val dialog: AlertDialog = AlertDialog.Builder(requireContext())
                            .setTitle(title)
                            .setMessage(json.getString("body"))
                            .setIcon(R.mipmap.ic_launcher)
                            .setPositiveButton(R.string.dialog_download) { _, _ ->
                                val uri = Uri.parse(resources.getString(R.string.release_url))
                                val internet = Intent(Intent.ACTION_VIEW, uri)
                                internet.addCategory(Intent.CATEGORY_BROWSABLE)
                                startActivity(internet)
                            }.setNegativeButton(R.string.dialog_cancel) { _, _ ->

                            }.create()
                        dialog.show()
                        true
                    }
                }
            }
            catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    infoAppVersion.title = resources.getString(R.string.info_app_version)
                }
            }
        }

        // Show update database dialog.
        infoDbVersion.setOnPreferenceClickListener {
            // Setup dialog.
            val dialog = AlertDialog.Builder(requireContext()).create()
            val view: View = View.inflate(requireContext(), R.layout.dialog_db_update_channel, null)
            dialog.setView(view)
            dialog.show()

            val radioGroup = view.findViewById<RadioGroup>(R.id.db_update_group)
            val confirm = view.findViewById<Button>(R.id.db_update_confirm)
            val cancel = view.findViewById<Button>(R.id.db_update_cancel)
            val clear = view.findViewById<Button>(R.id.db_update_clear)
            val custom = view.findViewById<EditText>(R.id.db_update_custom_input)
            var channel = preferences.getInt("db_update_channel", 0)

            radioGroup.check(when (channel) {
                0 -> R.id.db_update_hd2
                1 -> R.id.db_update_hd
                2 -> R.id.db_update_custom
                else -> R.id.db_update_hd2
            })
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
                preferences.edit().putString("db_update_channel_custom", custom.text.toString()).apply()
            }

            // Clear cache.
            clear.setOnClickListener {
                val clearDialog = AlertDialog.Builder(requireContext())
                    .setTitle(R.string.info_db_update_clear)
                    .setMessage(R.string.info_db_update_clear_desc)
                    .setIcon(R.drawable.ic_info)
                    .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                        val path = context?.filesDir?.path + "/icons"
                        File(path).deleteRecursively()
                        preferences.edit().putString("db_version", "0").apply()
                        preferences.edit().putString("db_name", getString(R.string.default_string)).apply()
                        preferences.edit().putInt("db_update_channel", 0).apply()
                        channel = 0
                        radioGroup.check(R.id.db_update_hd2)
                        stratagemViewModel.deleteAll()

                        Toast.makeText(context, getString(R.string.toast_complete), Toast.LENGTH_SHORT).show()

                        lifecycleScope.launch {
                            checkDBUpdate()
                        }
                    }.setNegativeButton(R.string.dialog_cancel) { _, _ ->

                    }.create()
                clearDialog.show()
            }

            // Cancel update.
            cancel.setOnClickListener {
                dialog.hide()
            }

            // Update database.
            confirm.setOnClickListener {
                dialog.hide()
                preferences.edit().putInt("db_update_channel", channel).apply()
                dbVer = preferences.getString("db_version", "0")!!
                dbName = preferences.getString("db_name", resources.getString(R.string.db_hd2_name))!!

                preferences.edit().putString("db_name", resources.getString(R.string.default_string)).apply()

                var url: String = when (preferences.getInt("db_update_channel", 0)) {
                    0 -> getString(R.string.db_hd2_url)
                    1 -> getString(R.string.db_hd_url)
                    2 -> preferences.getString("db_update_channel_custom", "")!!
                    else -> getString(R.string.db_hd2_url)
                }
                if (url.isEmpty()) {
                    url = getString(R.string.default_string)
                }
                else if (url.substring(url.length - 1) != "/" && url.substring(url.length - 1) != "\\") {
                    url = "$url/"
                }

                // Download database.
                lifecycleScope.launch {
                    infoDbVersion.isEnabled = false
                    withContext(Dispatchers.Main) {
                        infoDbVersion.title = resources.getString(R.string.info_db_version_updating)
                        infoDbVersion.summary = resources.getString(R.string.info_db_version_updating_index_desc)
                    }

                    try {
                        preferences.edit().putString("db_version", "1").apply()

                        // Download index.
                        val indexObj = JSONObject(Util.downloadToStr(url))
                        val date = indexObj.getString("date")
                        val dbUrl = url + indexObj.getString("db_path")
                        val iconsUrl = url + indexObj.getString("icons_path")
                        val iconsList: MutableList<String> = emptyList<String>().toMutableList()
                        val name = indexObj.getString("name")
                        val iconsPath = context?.filesDir?.path + "/icons/$name/"

                        // Analyze the index.
                        preferences.edit().putString("db_name", name).apply()
                        if (date == dbVer && name == dbName) {
                            // Database is latest, no need to update.
                            withContext(Dispatchers.Main) {
                                infoDbVersion.summary = String.format(
                                    resources.getString(R.string.info_db_version_update_latest),
                                    name
                                )
                                infoDbVersion.title = resources.getString(R.string.info_db_version)
                            }
                            preferences.edit().putString("db_version", date).apply()
                            return@launch
                        }

                        // Download database.
                        withContext(Dispatchers.Main) {
                            infoDbVersion.summary = String.format(
                                resources.getString(R.string.info_db_version_updating_db_desc),
                                name
                            )
                            infoDbVersion.title = resources.getString(R.string.info_db_version_updating)
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
                            val item = StratagemData(
                                row.getInt(0),
                                row.getString(1),
                                row.getString(2),
                                row.getString(3),
                                steps
                            )
                            iconsList.add(row.getString(3))
                            stratagemViewModel.insertItem(item)
                        }

                        // Download icons.
                        for (i in 0 until iconsList.size) {
                            withContext(Dispatchers.Main) {
                                infoDbVersion.summary = String.format(
                                    resources.getString(R.string.info_db_version_updating_icons_desc),
                                    name,
                                    i,
                                    iconsList.size
                                )
                                infoDbVersion.title = resources.getString(R.string.info_db_version_updating)
                            }
                            Util.download(iconsUrl + iconsList[i] + ".svg",
                                iconsPath + iconsList[i] + ".svg",
                                false
                            )
                        }

                        preferences.edit().putString("db_version", date).apply()
                        withContext(Dispatchers.Main) {
                            infoDbVersion.summary = String.format(
                                resources.getString(R.string.info_db_version_update_complete_desc),
                                name
                            )
                        }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) {
                            infoDbVersion.summary = resources.getString(R.string.info_db_version_update_failed_desc)
                            infoDbVersion.isEnabled = true
                        }
                        preferences.edit().putBoolean("hint_db_incomplete", false).apply()
                    }
                    withContext(Dispatchers.Main) {
                        infoDbVersion.title = resources.getString(R.string.info_db_version)
                    }
                }
            }

            true
        }
    }

    /**
     * Setup the content of some preference
     */
    private fun setupContent() {
        // Set database version
        infoDbVersion.summary = String.format(
            resources.getString(R.string.info_db_version_desc),
            dbName,
            when (dbVer) {
                "0" -> resources.getString(R.string.info_db_version_empty)
                "1" -> resources.getString(R.string.info_db_version_incomplete)
                else -> dbVer
            }
        )

        // Set app version.
        val pkgName = context?.packageName!!
        val pkgInfo = context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
        infoAppVersion.summary = String.format(
            getString(R.string.info_app_version_desc),
            pkgInfo.versionName
        )
    }

    override fun onDestroy() {
        client.disconnect()
        super.onDestroy()
    }

    /**
     * Check if the database is updatable.
     */
    private suspend fun checkDBUpdate() {
        infoDbVersion.title = resources.getString(R.string.info_db_version_check)
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
            else if (url.substring(url.length - 1) != "/" && url.substring(url.length - 1) != "\\") {
                url = "$url/"
            }

            val json = JSONObject(Util.downloadToStr(url))
            val newVer = json.getString("date")
            dbVer = preferences.getString("db_version", "0")!!
            withContext(Dispatchers.Main) {
                if (dbVer != newVer) {
                    infoDbVersion.summary =
                        String.format(resources.getString(R.string.info_db_version_updatable_desc),
                            preferences.getString("db_name", getString(R.string.db_hd2_name)),
                            when (dbVer) {
                                "0" -> resources.getString(R.string.info_db_version_empty)
                                "1" -> resources.getString(R.string.info_db_version_incomplete)
                                else -> dbVer
                            },
                            json.getString("name"),
                            newVer
                        )
                    infoDbVersion.title =
                        resources.getString(R.string.info_db_version_updatable)
                }
                else {
                    infoDbVersion.title = resources.getString(R.string.info_db_version)
                    infoDbVersion.summary =
                        String.format(resources.getString(R.string.info_db_version_latest),
                            preferences.getString("db_name", getString(R.string.db_hd2_name)),
                            when (dbVer) {
                                "0" -> resources.getString(R.string.info_db_version_empty)
                                "1" -> resources.getString(R.string.info_db_version_incomplete)
                                else -> dbVer
                            }
                        )
                }
            }
        }
        catch (_: Exception) {
            withContext(Dispatchers.Main) {
                infoDbVersion.title = resources.getString(R.string.info_db_version)
            }
        }
    }

}