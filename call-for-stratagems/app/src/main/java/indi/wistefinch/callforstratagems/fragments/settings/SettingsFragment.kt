package indi.wistefinch.callforstratagems.fragments.settings

import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import indi.wistefinch.callforstratagems.R
import indi.wistefinch.callforstratagems.socket.Client

class SettingsFragment : PreferenceFragmentCompat() {

    private val client: Client = Client()

    private lateinit var tcpAddress: EditTextPreference
    private lateinit var tcpPort: EditTextPreference
    private lateinit var tcpTest: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        tcpAddress = preferenceManager.findPreference("tcp_add")!!
        tcpPort = preferenceManager.findPreference("tcp_port")!!
        tcpTest = preferenceManager.findPreference("tcp_test")!!

        // Only numeric value for port
        tcpPort.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        // Set click listener
        tcpTest.setOnPreferenceClickListener {
            tcpTest.summary = resources.getText(R.string.tcp_test_connecting)
            val add = tcpAddress.text!!
            val port: Int = tcpPort.text?.toInt()!!
            Thread {
                val res = client.connect(add, port)
                activity?.runOnUiThread {
                    if(res) {
                        tcpTest.summary = resources.getText(R.string.tcp_test_success)
                    }
                    else {
                        tcpTest.summary = resources.getText(R.string.tcp_test_failed)
                    }
                }
            }.start()
            true
        }

    }

    override fun onDestroy() {
        client.disconnect()
        super.onDestroy()
    }
}