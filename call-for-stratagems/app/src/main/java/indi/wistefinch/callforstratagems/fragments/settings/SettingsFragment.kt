package indi.wistefinch.callforstratagems.fragments.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.gson.Gson
import indi.wistefinch.callforstratagems.R
import indi.wistefinch.callforstratagems.socket.Client
import indi.wistefinch.callforstratagems.socket.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : PreferenceFragmentCompat() {

    private val client = Client()

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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

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

        setupFormat()
        setupEventListener()
        setupContent()

    }

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

    private fun setupEventListener() {
        // Test connection
        tcpTest.setOnPreferenceClickListener {
            tcpTest.summary = resources.getText(R.string.tcp_test_connecting)
            tcpTest.isEnabled = false
            val add = tcpAddress.text!!
            val port: Int = tcpPort.text?.toInt()!!
            // launch coroutine
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val connected = client.connect(add, port)
                    if (connected) {
                        client.send("{\"operation\":0}")
                        val res = client.receive()
                        withContext(Dispatchers.Main) {
                            if (res == "ready") {
                                tcpTest.summary = resources.getText(R.string.tcp_test_success)
                            }
                            else {
                                tcpTest.summary = resources.getText(R.string.tcp_test_response_error)
                            }
                        }
                    }
                    else {
                        withContext(Dispatchers.Main) {
                            tcpTest.summary = resources.getText(R.string.tcp_test_failed)
                        }
                    }
                    client.disconnect()
                    withContext(Dispatchers.Main) {
                        tcpTest.isEnabled = true
                    }
                }
            }
            true
        }
        // Sync config
        syncConfig.setOnPreferenceClickListener {
            syncConfig.summary = resources.getText(R.string.tcp_test_connecting)
            syncConfig.isEnabled = false
            val config = ServerConfig(
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
                    val connected = client.connect(add, port)
                    if (connected) {
                        client.send("{\"operation\":4,\"configuration\":" + Gson().toJson(config).toString() + "}")
                        withContext(Dispatchers.Main) {
                            tcpPort.text = config.port.toString()
                            syncConfig.summary = resources.getText(R.string.sync_config_finished)
                        }
                    }
                    else {
                        withContext(Dispatchers.Main) {
                            syncConfig.summary = resources.getText(R.string.tcp_test_failed)
                        }
                    }
                    client.disconnect()
                    withContext(Dispatchers.Main) {
                        syncConfig.isEnabled = true
                    }
                }
            }
            true
        }
        // Open repository
        infoRepo.setOnPreferenceClickListener {
            val uri = Uri.parse(resources.getString(R.string.repo_url))
            val internet = Intent(Intent.ACTION_VIEW, uri)
            internet.addCategory(Intent.CATEGORY_BROWSABLE)
            startActivity(internet)
            true
        }
    }

    private fun setupContent() {
        val pkgName = context?.packageName!!
        val pkgInfo = context?.applicationContext?.packageManager?.getPackageInfo(pkgName, 0)!!
        infoVersion.summary = pkgInfo.versionName + "(" + pkgInfo.versionCode + ")"
    }

    override fun onDestroy() {
        client.disconnect()
        super.onDestroy()
    }

}