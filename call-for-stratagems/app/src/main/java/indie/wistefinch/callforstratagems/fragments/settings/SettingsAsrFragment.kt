package indie.wistefinch.callforstratagems.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.databinding.FragmentSettingsAsrBinding
import indie.wistefinch.callforstratagems.utils.AppButton

class SettingsAsrFragment : Fragment() {
    // View binding.
    private var _binding: FragmentSettingsAsrBinding? = null
    private val binding get() = _binding!!

    // Dialogs
    private lateinit var modelDialog: AlertDialog
    private lateinit var modelView: View

    // Preference.
    private lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSettingsAsrBinding.inflate(inflater, container, false)
        val view = binding.root

        // Init menu
        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }

        preferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }!!

        // Setup dialogs
        modelDialog = AlertDialog.Builder(requireContext()).create()
        modelView = View.inflate(requireContext(), R.layout.dialog_asr_models, null)
        modelDialog.setView(modelView)

        setupContent()
        setupEventListener()

        return view
    }

    /**
     * Setup the content of the views
     */
    private fun setupContent() {
        binding.setCtrlAsr.isChecked = preferences.getBoolean("ctrl_asr_enabled", false)
        binding.setCtrlAsrSimilarity.setText(
            preferences.getInt(
                "ctrl_asr_similarity",
                50
            ).toString()
        )
        binding.setCtrlAsrGpu.isChecked = preferences.getBoolean("ctrl_asr_gpu", false)
    }


    /**
     * Setup event listener
     */
    private fun setupEventListener() {
        binding.setCtrlAsr.setOnCheckedChangeListener { _, isChecked ->
            with(preferences.edit()) {
                putBoolean("ctrl_asr_enabled", isChecked)
                apply()
            }
        }
        binding.setCtrlAsrSimilarity.addTextChangedListener { text ->
            try {
                with(preferences.edit()) {
                    putInt(
                        "ctrl_asr_similarity",
                        (if (text.toString().isEmpty()) 50 else text.toString().toInt()).coerceIn(
                            0,
                            100
                        )
                    )
                    apply()
                }
            } catch (_: Exception) {
                with(preferences.edit()) {
                    putInt("ctrl_asr_similarity", 50)
                    apply()
                }
            }
        }
        binding.setCtrlAsrGpu.setOnCheckedChangeListener { _, isChecked ->
            with(preferences.edit()) {
                putBoolean("ctrl_asr_gpu", isChecked)
                apply()
            }
        }

        // Show select model dialog.
        binding.setCtrlAsrModels.setOnClickListener {
            if (modelDialog.isShowing) {
                return@setOnClickListener
            }
            modelDialog.show()

            val clearDialog = AlertDialog.Builder(requireContext()).create()
            val clearView: View = View.inflate(requireContext(), R.layout.dialog_info, null)
            clearDialog.setView(clearView)

            val radioGroup = modelView.findViewById<RadioGroup>(R.id.dlg_asr_model_group)
            val confirm = modelView.findViewById<AppButton>(R.id.dlg_asr_model_confirm)
            val cancel = modelView.findViewById<AppButton>(R.id.dlg_asr_model_cancel)
            val clear = modelView.findViewById<AppButton>(R.id.dlg_asr_model_clear)
            val custom = modelView.findViewById<EditText>(R.id.dlg_asr_model_custom_input)
            var model = preferences.getInt("ctrl_asr_model", -1)

            radioGroup.check(
                when (model) {
                    0 -> R.id.dlg_asr_model_en
                    1 -> R.id.dlg_asr_model_zh
                    2 -> R.id.dlg_asr_model_custom
                    else -> -1
                }
            )
            custom.setText(preferences.getString("ctrl_asr_custom", ""))
            custom.isEnabled = model == 2

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                model = when (checkedId) {
                    R.id.dlg_asr_model_en -> 0
                    R.id.dlg_asr_model_zh -> 1
                    R.id.dlg_asr_model_custom -> 2
                    else -> -1
                }
                custom.isEnabled = model == 2
            }

            // Set custom url.
            custom.addTextChangedListener {
                preferences.edit().putString("ctrl_asr_custom", custom.text.toString().trim())
                    .apply()
            }

            // Clear cache.
            clear.setAlert(true)
            clear.setOnClickListener {
            }

            // Cancel.
            cancel.setOnClickListener {
                modelDialog.hide()
            }

            // Select model.
            confirm.setOnClickListener {
                modelDialog.hide()
            }
        }
    }
}