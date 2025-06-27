package indie.wistefinch.callforstratagems.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import indie.wistefinch.callforstratagems.databinding.FragmentSettingsSyncBinding
import kotlin.math.max

class SettingsSyncFragment : Fragment() {
    // View binding.
    private var _binding: FragmentSettingsSyncBinding? = null
    private val binding get() = _binding!!

    // Preference.
    private lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSettingsSyncBinding.inflate(inflater, container, false)
        val view = binding.root

        // Init menu
        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }

        preferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }!!

        setupContent()
        setupEventListener()

        return view
    }

    /**
     * Setup the content of the views
     */
    private fun setupContent() {
        binding.setSyncIp.setText(preferences.getString("sync_ip", ""))
        binding.setSyncAuth.isChecked =
            preferences.getBoolean("sync_auth", true)
        binding.setSyncAuthTimeout.setText(
            preferences.getInt(
                "sync_auth_timeout",
                3
            ).toString()
        )
        binding.setSyncDebug.isChecked =
            preferences.getBoolean("sync_debug", false)
    }


    /**
     * Setup event listener
     */
    private fun setupEventListener() {
        binding.setSyncIp.addTextChangedListener { text ->
            with(preferences.edit()) {
                putString("sync_ip", text.toString().trim())
                apply()
            }
        }
        binding.setSyncAuth.setOnCheckedChangeListener { _, isChecked ->
            with(preferences.edit()) {
                putBoolean("sync_auth", isChecked)
                apply()
            }
        }
        binding.setSyncAuthTimeout.addTextChangedListener { text ->
            try {
                with(preferences.edit()) {
                    putInt(
                        "sync_auth_timeout",
                        max(1, if (text.toString().isEmpty()) 3 else text.toString().toInt())
                    )
                    apply()
                }
            } catch (_: Exception) {
                with(preferences.edit()) {
                    putInt("sync_auth_timeout", 3)
                    apply()
                }
            }
        }
        binding.setSyncDebug.setOnCheckedChangeListener { _, isChecked ->
            with(preferences.edit()) {
                putBoolean("sync_debug", isChecked)
                apply()
            }
        }
    }
}