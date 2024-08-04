package indie.wistefinch.callforstratagems.fragments.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import indie.wistefinch.callforstratagems.R
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

class SettingsFragment : PreferenceFragmentCompat() {

    /**
     * The socket client.
     */
    private val client = Client()

    // View binding.
    // Connection preference
    private lateinit var tcpAddress: EditTextPreference
    private lateinit var tcpPort: EditTextPreference
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
    private lateinit var infoVersion: Preference
    private lateinit var infoRepo: Preference

    private lateinit var sid: String
    private lateinit var token: String

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Setup binding.
        tcpAddress = preferenceManager.findPreference("tcp_add")!!
        tcpPort = preferenceManager.findPreference("tcp_port")!!
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

        infoVersion = preferenceManager.findPreference("info_version")!!
        infoRepo = preferenceManager.findPreference("info_repo")!!

        val preferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
        sid = preferences?.getString("sid", "0")!!

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
            infoVersion.summary = pkgInfo.versionName
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
        infoVersion.setOnPreferenceClickListener {
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
    }

    /**
     * Setup the content of some preference
     */
    private fun setupContent() {
        // Set version.
        val pkgName = context?.packageName!!
        val pkgInfo = context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
        // For compatibility with lower SDKs, ignore the discouraged warning.
        @Suppress("DEPRECATION")
        infoVersion.summary = pkgInfo.versionName + "(" + pkgInfo.versionCode + ")"
    }

    override fun onDestroy() {
        client.disconnect()
        super.onDestroy()
    }

}