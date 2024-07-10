package indi.wistefinch.callforstratagems.fragments.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.gson.Gson
import indi.wistefinch.callforstratagems.R
import indi.wistefinch.callforstratagems.socket.Client
import indi.wistefinch.callforstratagems.socket.ServerConfig

class SettingsFragment : PreferenceFragmentCompat() {

    private val client: Client = Client()

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
    }

    private fun setupEventListener() {
        // Test connection
        tcpTest.setOnPreferenceClickListener {
            tcpTest.summary = resources.getText(R.string.tcp_test_connecting)
            val add = tcpAddress.text!!
            val port: Int = tcpPort.text?.toInt()!!
            Thread {
                activity?.runOnUiThread {
                    tcpTest.isEnabled = false
                }
                val connected = client.connect(add, port)
                if(connected) {
                    client.send("{\"operation\":0}")
                    val res = client.receive()
                    activity?.runOnUiThread {
                        if(res == "ready") {
                            tcpTest.summary = resources.getText(R.string.tcp_test_success)
                        }
                        else {
                            tcpTest.summary = resources.getText(R.string.tcp_test_response_error)
                        }
                    }
                }
                else {
                    activity?.runOnUiThread {
                        tcpTest.summary = resources.getText(R.string.tcp_test_failed)
                    }
                }
                client.disconnect()
                activity?.runOnUiThread {
                    tcpTest.isEnabled = true
                }
            }.start()
            true
        }
        // Sync config
        syncConfig.setOnPreferenceClickListener {
            syncConfig.summary = resources.getText(R.string.tcp_test_connecting)
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
            Thread {
                activity?.runOnUiThread {
                    syncConfig.isEnabled = false
                }
                val connected = client.connect(add, port)
                if(connected) {
                    client.send("{\"operation\":4,\"configuration\":" + Gson().toJson(config).toString() + "}")
                    activity?.runOnUiThread {
                        tcpPort.text = config.port.toString()
                        syncConfig.summary = resources.getText(R.string.sync_config_finished)
                    }
                }
                else {
                    activity?.runOnUiThread {
                        syncConfig.summary = resources.getText(R.string.tcp_test_failed)
                    }
                }
                client.disconnect()
                activity?.runOnUiThread {
                    syncConfig.isEnabled = true
                }
            }.start()
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