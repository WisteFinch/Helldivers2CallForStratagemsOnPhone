package indie.wistefinch.callforstratagems.fragments.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_CANCELED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_FAILED
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import indie.wistefinch.callforstratagems.CFSApplication
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.Util
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModelFactory
import indie.wistefinch.callforstratagems.socket.AddressData
import indie.wistefinch.callforstratagems.socket.RequestAuthPacket
import indie.wistefinch.callforstratagems.socket.Client
import indie.wistefinch.callforstratagems.socket.ReceiveAuthData
import indie.wistefinch.callforstratagems.socket.ReceiveStatusData
import indie.wistefinch.callforstratagems.socket.RequestStatusPacket
import indie.wistefinch.callforstratagems.socket.ServerConfigData
import indie.wistefinch.callforstratagems.socket.SyncConfigPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SettingsFragment: PreferenceFragmentCompat() {

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
    private lateinit var infoRepo: Preference

    private lateinit var sid: String
    private lateinit var dbVer: String

    private lateinit var preferences: SharedPreferences

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
        inputUp = preferenceManager.findPreference("input_up")!!
        inputDown = preferenceManager.findPreference("input_down")!!
        inputLeft = preferenceManager.findPreference("input_left")!!
        inputRight = preferenceManager.findPreference("input_right")!!
        syncConfig = preferenceManager.findPreference("sync_config")!!

        swipeDistance = preferenceManager.findPreference("swipe_distance_threshold")!!
        swipeVelocity = preferenceManager.findPreference("swipe_velocity_threshold")!!

        infoDbVersion = preferenceManager.findPreference("info_db_version")!!
        infoAppVersion = preferenceManager.findPreference("info_app_version")!!
        infoRepo = preferenceManager.findPreference("info_repo")!!

        preferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }!!
        sid = preferences.getString("sid", "0")!!
        dbVer = preferences.getString("db_version", "0")!!

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
        // Check dependency.
        val moduleInstallClient = context?.let { ModuleInstall.getClient(it) }
        val optionalModuleApi = context?.let { GmsBarcodeScanning.getClient(it) }
        moduleInstallClient
            ?.areModulesAvailable(optionalModuleApi)
            ?.addOnSuccessListener { it ->
                val success = it
                if (success.areModulesAvailable()) {
                    tcpScanner.summary = getString(R.string.tcp_scan_desc)
                    // Dependency available.
                    tcpScanner.setOnPreferenceClickListener {
                        val options = GmsBarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build()
                        val scanner = context?.let { GmsBarcodeScanning.getClient(it, options) }
                        scanner?.startScan()
                            ?.addOnSuccessListener { barcode ->
                                try {
                                    val rawValue: String? = barcode.rawValue
                                    val add = Gson().fromJson(rawValue, AddressData::class.java)
                                    tcpAddress.text = add.add
                                    tcpPort.text = add.port.toString()
                                    Toast.makeText(context, getString(R.string.tcp_scan_success), Toast.LENGTH_SHORT).show()
                                }
                                catch (_: Exception) {
                                    Toast.makeText(context, getString(R.string.tcp_scan_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        true
                    }
                }
                else {
                    // Dependency Unavailable.
                    tcpScanner.summary = getString(R.string.tcp_scan_unavailable)
                    tcpScanner.setOnPreferenceClickListener {
                        tcpScanner.isEnabled = false
                        tcpScanner.summary = getString(R.string.tcp_scan_downloading)
                        val options = GmsBarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build()
                        val scanner = context?.let { GmsBarcodeScanning.getClient(it, options) }!!

                        // Setup install status listener.
                        class ModuleInstallProgressListener: InstallStatusListener {
                            override fun onInstallStatusUpdated(update: ModuleInstallStatusUpdate) {
                                update.progressInfo?.let {
                                    tcpScanner.summary = getString(R.string.tcp_scan_downloading) + (it.bytesDownloaded * 100 / it.totalBytesToDownload).toInt() + "%"
                                }
                                if (isTerminateState(update.installState)) {
                                    moduleInstallClient.unregisterListener(this)
                                }
                            }

                            fun isTerminateState(@ModuleInstallStatusUpdate.InstallState state: Int): Boolean {
                                return state == STATE_CANCELED || state == STATE_COMPLETED || state == STATE_FAILED
                            }
                        }
                        val listener = ModuleInstallProgressListener()
                        // Setup install request.
                        val moduleInstallRequest =
                            ModuleInstallRequest.newBuilder()
                                .setListener(listener)
                                .addApi(scanner)
                                .build()
                        // Setup install client and add listener.
                        moduleInstallClient
                            .installModules(moduleInstallRequest)
                            .addOnSuccessListener {
                                tcpScanner.isEnabled = true
                                if (success.areModulesAvailable()) {
                                    tcpScanner.summary = getString(R.string.tcp_scan_desc)
                                }
                                else {
                                    tcpScanner.summary = getString(R.string.tcp_scan_download_failed)
                                }
                            }
                            .addOnFailureListener {
                                tcpScanner.isEnabled = true
                                tcpScanner.summary = getString(R.string.tcp_scan_download_failed)
                            }
                        true
                    }
                }
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
                                            version,
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
                up = inputUp.value!!,
                down = inputDown.value!!,
                left = inputLeft.value!!,
                right = inputRight.value!!,
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

        // Open release url preference.
        infoAppVersion.setOnPreferenceClickListener {
            val uri = Uri.parse(resources.getString(R.string.release_url))
            val internet = Intent(Intent.ACTION_VIEW, uri)
            internet.addCategory(Intent.CATEGORY_BROWSABLE)
            startActivity(internet)
            true
        }

        // Open repository preference.
        infoRepo.setOnPreferenceClickListener {
            val uri = Uri.parse(resources.getString(R.string.repo_url))
            val internet = Intent(Intent.ACTION_VIEW, uri)
            internet.addCategory(Intent.CATEGORY_BROWSABLE)
            startActivity(internet)
            true
        }

        // Check database update.
        // Launch coroutine
        lifecycleScope.launch {
            val json = JSONObject(Util.downloadToStr(resources.getString(R.string.db_index_url)))
            val newVer = json.getString("date")
            withContext(Dispatchers.Main) {
                if (dbVer != newVer) {
                    infoDbVersion.summary =
                        String.format(resources.getString(R.string.db_version_updatable_desc),
                            infoDbVersion.summary,
                            newVer
                        )
                    infoDbVersion.title =
                        resources.getString(R.string.db_version_updatable)
                }
            }
        }

        // Check app update.
        // Launch coroutine
        lifecycleScope.launch {
            val json = JSONObject(Util.downloadToStr(resources.getString(R.string.release_api_url)))
            val newVer = json.getString("tag_name").substring(1)
            withContext(Dispatchers.Main) {
                // Set version.
                val pkgName = context?.packageName!!
                val pkgInfo = context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
                val curVer = pkgInfo.versionName
                if (curVer != newVer) {
                    // For compatibility with lower SDKs, ignore the discouraged warning.
                    @Suppress("DEPRECATION")
                    infoAppVersion.summary = pkgInfo.versionName + "(" + pkgInfo.versionCode + ") ➡ " + newVer
                    infoAppVersion.title = resources.getString(R.string.info_version_updatable)
                }
            }
        }

        // Update database.
        infoDbVersion.setOnPreferenceClickListener {
            lifecycleScope.launch {
                infoDbVersion.isEnabled = false
                withContext(Dispatchers.Main) {
                    infoDbVersion.title = resources.getString(R.string.db_version_updating)
                    infoDbVersion.summary = resources.getString(R.string.db_version_updating_index_desc)
                }

                try {
                    val iconsPath = context?.filesDir?.path + "/icons/"
                    preferences.edit().putString("db_version", "1").apply()

                    // Download index.
                    val indexObj = JSONObject(Util.downloadToStr(resources.getString(R.string.db_index_url)))
                    val date = indexObj.getString("date")
                    val dbUrl = resources.getString(R.string.db_index_url) + indexObj.getString("db_path")
                    val iconsUrl = resources.getString(R.string.db_index_url) + indexObj.getString("icons_path")
                    val iconsList: MutableList<String> = emptyList<String>().toMutableList()

                    // Download database.
                    withContext(Dispatchers.Main) {
                        infoDbVersion.summary = resources.getString(R.string.db_version_updating_db_desc)
                        infoDbVersion.title = resources.getString(R.string.db_version_updating)
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
                                resources.getString(R.string.db_version_updating_icons_desc),
                                i,
                                iconsList.size
                            )
                            infoDbVersion.title = resources.getString(R.string.db_version_updating)
                        }
                        Util.download(iconsUrl + iconsList[i] + ".svg",
                            iconsPath + iconsList[i] + ".svg",
                            false
                        )
                    }

                    preferences.edit().putString("db_version", date).apply()
                    withContext(Dispatchers.Main) {
                        infoDbVersion.summary = resources.getString(R.string.db_version_update_complete_desc)
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        infoDbVersion.summary = resources.getString(R.string.db_version_update_failed_desc)
                    }
                }
                withContext(Dispatchers.Main) {
                    infoDbVersion.title = resources.getString(R.string.db_version)
                }
                infoDbVersion.isEnabled = true
            }
            true
        }
    }

    /**
     * Setup the content of some preference
     */
    private fun setupContent() {
        // Set database version
        when (dbVer) {
            "0" -> infoDbVersion.summary = resources.getString(R.string.db_version_empty)
            "1" -> infoDbVersion.summary = resources.getString(R.string.db_version_incomplete)
            else -> infoDbVersion.summary = dbVer
        }

        // Set app version.
        val pkgName = context?.packageName!!
        val pkgInfo = context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
        // For compatibility with lower SDKs, ignore the discouraged warning.
        @Suppress("DEPRECATION")
        infoAppVersion.summary = pkgInfo.versionName + "(" + pkgInfo.versionCode + ")"
    }

    override fun onDestroy() {
        client.disconnect()
        super.onDestroy()
    }

}