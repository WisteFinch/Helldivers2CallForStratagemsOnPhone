package indie.wistefinch.callforstratagems.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import indie.wistefinch.callforstratagems.CFSApplication
import indie.wistefinch.callforstratagems.Constants
import indie.wistefinch.callforstratagems.Constants.PATH_ASR_MODELS
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.asr.AsrClient
import indie.wistefinch.callforstratagems.asr.AsrService
import indie.wistefinch.callforstratagems.data.viewmodel.AsrKeywordViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.AsrKeywordViewModelFactory
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModelFactory
import indie.wistefinch.callforstratagems.databinding.FragmentSettingsAsrBinding
import indie.wistefinch.callforstratagems.utils.AppButton
import indie.wistefinch.callforstratagems.utils.AppProgressBar
import indie.wistefinch.callforstratagems.utils.EditListDialog
import indie.wistefinch.callforstratagems.utils.DownloadService
import indie.wistefinch.callforstratagems.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class SettingsAsrFragment : Fragment() {
    // View binding.
    private var _binding: FragmentSettingsAsrBinding? = null
    private val binding get() = _binding!!

    // Dialogs
    private lateinit var modelDialog: AlertDialog
    private lateinit var modelView: View

    // Preference.
    private lateinit var preferences: SharedPreferences

    /**
     * The stratagem view model.
     */
    private val stratagemViewModel: StratagemViewModel by activityViewModels {
        StratagemViewModelFactory(
            (activity?.application as CFSApplication).stratagemDb.stratagemDao()
        )
    }

    /**
     * The Asr keyword view model.
     */
    private val asrKeywordViewModel: AsrKeywordViewModel by activityViewModels {
        AsrKeywordViewModelFactory(
            (activity?.application as CFSApplication).asrKeywordDb.asrKeywordDao()
        )
    }

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

        preferences = requireContext().let { PreferenceManager.getDefaultSharedPreferences(it) }

        // Setup dialogs
        modelDialog = AlertDialog.Builder(requireContext()).create()
        modelView = View.inflate(requireContext(), R.layout.dialog_asr_models, null)
        modelDialog.setView(modelView)

        setupContent()
        setupEventListener()

        checkAsrModelStatus()

        return view
    }

    /**
     * Setup the content of the views
     */
    private fun setupContent() {
        binding.setCtrlAsrSwitch.isChecked = preferences.getBoolean("ctrl_asr_enabled", false)
        binding.setCtrlAsrSimilarity.setText(
            preferences.getInt(
                "ctrl_asr_similarity",
                50
            ).toString()
        )
        binding.setCtrlAsrGpu.isChecked = preferences.getBoolean("ctrl_asr_gpu", true)
    }


    /**
     * Setup event listener
     */
    private fun setupEventListener() {
        binding.setCtrlAsrSwitch.setOnCheckedChangeListener { _, isChecked ->
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

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                model = when (checkedId) {
                    R.id.dlg_asr_model_en -> 0
                    R.id.dlg_asr_model_zh -> 1
                    R.id.dlg_asr_model_custom -> 2
                    else -> -1
                }
            }

            // Set custom url.
            custom.addTextChangedListener { txt ->
                preferences.edit().putString("ctrl_asr_custom", txt.toString().trim())
                    .apply()
            }

            // Clear cache.
            clear.setAlert(true)
            clear.setOnClickListener {
                if (!clearDialog.isShowing) {
                    clearDialog.show()

                    val title = clearView.findViewById<TextView>(R.id.dlg_info_title)
                    title.setText(R.string.dlg_asr_model_clear)
                    clearView.findViewById<TextView>(R.id.dlg_info_msg)
                        .setText(R.string.dlg_asr_model_clear_desc)
                    val button1 = clearView.findViewById<AppButton>(R.id.dlg_info_button1)
                    button1.setAlert(true)
                    button1.setOnClickListener {
                        val path = requireContext().filesDir.path + PATH_ASR_MODELS
                        File(path).deleteRecursively()

                        Toast.makeText(
                            context,
                            getString(R.string.toast_complete),
                            Toast.LENGTH_SHORT
                        ).show()

                        checkAsrModelStatus()
                        clearDialog.hide()
                    }
                    clearView.findViewById<AppButton>(R.id.dlg_info_button2).setOnClickListener {
                        clearDialog.hide()
                    }
                }
            }

            // Cancel.
            cancel.setOnClickListener {
                modelDialog.hide()
            }

            // Select model
            confirm.setOnClickListener Confirm@{
                modelDialog.hide()

                // Get index url.
                var rawUrl = when (model) {
                    0 -> Constants.URL_ASR_MODEL_EN
                    1 -> Constants.URL_ASR_MODEL_ZH
                    2 -> preferences.getString("ctrl_asr_custom", "")!!
                    else -> return@Confirm
                }
                if (rawUrl.isEmpty()) {
                    rawUrl = getString(R.string.default_string)
                }

                // Init download dialog.
                val downloadDialog = AlertDialog.Builder(requireContext()).create()
                val downloadView: View =
                    View.inflate(requireContext(), R.layout.dialog_download, null)
                downloadDialog.setView(downloadView)
                downloadDialog.setCanceledOnTouchOutside(false)
                val indexView = downloadView.findViewById<LinearLayout>(R.id.dlg_download_index)
                val filesView = downloadView.findViewById<LinearLayout>(R.id.dlg_download_files)
                val titleView = downloadView.findViewById<TextView>(R.id.dlg_download_title)
                val idxTxtView = downloadView.findViewById<TextView>(R.id.dlg_download_index_text)
                val totalPB =
                    downloadView.findViewById<AppProgressBar>(R.id.dlg_download_files_total)
                val itemPB = downloadView.findViewById<AppProgressBar>(R.id.dlg_download_files_item)
                val infoView = downloadView.findViewById<TextView>(R.id.dlg_download_info)
                val buttonView = downloadView.findViewById<AppButton>(R.id.dlg_download_button)
                itemPB.enableHint()

                filesView.visibility = GONE
                indexView.visibility = VISIBLE
                titleView.setText(R.string.set_ctrl_asr_model_dl_title)
                idxTxtView.setText(R.string.set_ctrl_asr_model_dl_idx)
                downloadDialog.show()

                val parsedUrl: Utils.Companion.UrlParts
                try {
                    parsedUrl = Utils.parseUrl(rawUrl, "index.json")
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
                        infoView.text =
                            String.format(getString(R.string.utils_parse_url_failed), rawUrl)
                    }
                    return@Confirm
                }

                // Download model.
                val downloadJob = lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // Download index.
                        ensureActive()
                        val indexObj =
                            JSONObject(DownloadService().downloadAsStr(parsedUrl.dir + parsedUrl.fileName))
                        val name = indexObj.getString("name")
                        val nameEn = indexObj.getString("nameEn")
                        val nameZh = indexObj.getString("nameZh")
                        val modelsPath = requireContext().filesDir.path + PATH_ASR_MODELS + "$name/"
                        val displayName =
                            when (requireContext().resources.configuration.locales.get(0)
                                .toLanguageTag()) {
                                "zh-CN" -> nameZh
                                else -> nameEn
                            }
                        val downloadInfo = listOf(
                            listOf(
                                indexObj.getString("encoderParam"),
                                "encoder_jit_trace-pnnx.ncnn.param"
                            ),
                            listOf(
                                indexObj.getString("encoderBin"),
                                "encoder_jit_trace-pnnx.ncnn.bin"
                            ),
                            listOf(
                                indexObj.getString("decoderParam"),
                                "decoder_jit_trace-pnnx.ncnn.param"
                            ),
                            listOf(
                                indexObj.getString("decoderBin"),
                                "decoder_jit_trace-pnnx.ncnn.bin"
                            ),
                            listOf(
                                indexObj.getString("joinerParam"),
                                "joiner_jit_trace-pnnx.ncnn.param"
                            ),
                            listOf(
                                indexObj.getString("joinerBin"),
                                "joiner_jit_trace-pnnx.ncnn.bin"
                            ),
                            listOf(indexObj.getString("tokens"), "tokens.txt"),
                        )

                        // Download model files.
                        ensureActive()
                        var index = 0
                        withContext(Dispatchers.Main) {
                            filesView.visibility = VISIBLE
                            indexView.visibility = GONE
                            titleView.text = String.format(
                                getString(R.string.set_ctrl_asr_model_dl_title2),
                                displayName
                            )
                            totalPB.setText(
                                String.format(
                                    getString(R.string.set_ctrl_asr_model_dl_files),
                                    index + 1,
                                    downloadInfo.size
                                )
                            )
                            totalPB.setValue((index.toFloat() / downloadInfo.size * 100).toInt())
                            itemPB.setText(parsedUrl.dir + downloadInfo[index][0])
                            itemPB.setHint(
                                String.format(
                                    getString(R.string.set_ctrl_asr_model_dl_item),
                                    0f,
                                    "0",
                                    "?"
                                )
                            )
                            itemPB.setValue(0)
                        }
                        val service = DownloadService()
                        service.onComplete = {
                            ensureActive()
                            index++
                            if (index == downloadInfo.size) {
                                preferences.edit().putInt("ctrl_asr_model", model).apply()
                                preferences.edit().putString("asr_model_name", name).apply()
                                preferences.edit().putString("asr_model_name_en", nameEn).apply()
                                preferences.edit().putString("asr_model_name_zh", nameZh).apply()
                                buttonView.setTitle(getString(R.string.dlg_comm_confirm))
                                buttonView.setOnClickListener {
                                    downloadDialog.hide()
                                }
                                filesView.visibility = GONE
                                infoView.visibility = VISIBLE
                                infoView.setText(R.string.set_ctrl_asr_model_dl_complete)
                                checkAsrModelStatus()
                            } else {
                                totalPB.setText(
                                    String.format(
                                        getString(R.string.set_ctrl_asr_model_dl_files),
                                        index + 1,
                                        downloadInfo.size
                                    )
                                )
                                totalPB.setValue((index.toFloat() / downloadInfo.size * 100).toInt())
                                service.downloadToFile(
                                    parsedUrl.dir + downloadInfo[index][0],
                                    modelsPath + downloadInfo[index][1],
                                    this
                                )
                            }
                        }
                        service.onProgress = { d, t ->
                            val p = if (t.toInt() == -1) 0f else (d.toFloat() / t * 100)
                            itemPB.setText(parsedUrl.dir + downloadInfo[index][0])
                            itemPB.setHint(
                                String.format(
                                    getString(R.string.set_ctrl_asr_model_dl_item),
                                    p,
                                    d.toString(),
                                    if (t.toInt() == -1) "?" else t.toString()
                                )
                            )
                            itemPB.setValue(p.toInt())
                        }
                        service.onError = { e ->
                            Log.e("[Settings ASR] Download Model", e.toString())
                            buttonView.setTitle(getString(R.string.dlg_comm_confirm))
                            buttonView.setOnClickListener {
                                downloadDialog.hide()
                            }
                            indexView.visibility = GONE
                            filesView.visibility = GONE
                            infoView.visibility = VISIBLE
                            infoView.text = String.format(
                                getString(R.string.set_ctrl_asr_model_dl_failed),
                                e.toString()
                            )
                        }
                        service.downloadToFile(
                            parsedUrl.dir + downloadInfo[index][0],
                            modelsPath + downloadInfo[index][1],
                            this
                        )
                    } catch (e: Exception) {
                        Log.e("[Settings ASR] Download Model", e.toString())
                        lifecycleScope.launch(Dispatchers.Main) {
                            buttonView.setTitle(getString(R.string.dlg_comm_confirm))
                            buttonView.setOnClickListener {
                                downloadDialog.hide()
                            }
                            indexView.visibility = GONE
                            filesView.visibility = GONE
                            infoView.visibility = VISIBLE
                            infoView.text = String.format(
                                getString(R.string.set_ctrl_asr_model_dl_failed),
                                e.toString()
                            )
                        }
                    }
                }

                buttonView.setOnClickListener {
                    downloadDialog.hide()
                    downloadJob.cancel()
                }

                downloadDialog.setOnCancelListener {
                    downloadJob.cancel()
                }
            }
        }

        // Edit activation words.
        binding.setCtrlAsrActivate.setOnClickListener {
            val list = Utils.getPreferenceList(preferences, "ctrl_asr_activate")
            val dialog = EditListDialog(
                requireContext(),
                requireActivity(),
                list,
                getString(R.string.set_ctrl_asr_activate_edit)
            )
            dialog.onEditFinished { data ->
                val res: MutableList<String> = emptyList<String>().toMutableList()
                for (i in data) {
                    if (i.isNotBlank()) {
                        res.add(i)
                    }
                }
                Utils.setPreferenceList(preferences, "ctrl_asr_activate", res)
            }
        }

        // Test ASR.
        binding.setCtrlAsrTest.setOnClickListener {
            // Setup dialog.
            val dialog = AlertDialog.Builder(requireContext()).create()
            val view: View = View.inflate(requireContext(), R.layout.dialog_info, null)
            dialog.setView(view)
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()

            view.findViewById<TextView>(R.id.dlg_info_title)
                .setText(R.string.set_ctrl_asr_test)
            val textView = view.findViewById<TextView>(R.id.dlg_info_msg)
            textView.setText(R.string.asr_model_loading)
            view.findViewById<ImageView>(R.id.dlg_info_icon).setImageResource(R.drawable.ic_mic)
            view.findViewById<AppButton>(R.id.dlg_info_button2).visibility = GONE
            view.findViewById<AppButton>(R.id.dlg_info_button1).setOnClickListener {
                dialog.cancel()
            }

            var lastRes = ""
            var lang = preferences.getString("ctrl_lang", "auto")!!
            if (lang == "auto") {
                lang = requireContext().resources.configuration.locales.get(0).toLanguageTag()
            }
            val dbName = preferences.getString("db_name", Constants.ID_DB_HD2)!!
            val client = AsrClient(asrModelName = preferences.getString("asr_model_name", "")!!,
                context = requireContext(),
                activity = requireActivity(),
                keywordsViewModel = asrKeywordViewModel,
                activateWords = emptyList(),
                dbName = dbName,
                lang = lang,
                similarityThreshold = preferences.getInt("ctrl_asr_similarity", 50).toFloat() / 100,
                useGPU = preferences.getBoolean("ctrl_asr_gpu", true),
                stratagems = stratagemViewModel.getAllItems(),
                onError = { e ->
                    textView.setText(
                        when (e) {
                            AsrService.ASRErrType.ASR_MODEL_INIT_FAILED -> R.string.asr_model_init_failed
                            AsrService.ASRErrType.ASR_MODEL_FILE_CHECK_FAILED -> R.string.asr_model_file_check_failed
                            AsrService.ASRErrType.ASR_MIC_PERMISSION_DENIED -> R.string.asr_mic_permission_denied
                        }
                    )
                },
                onProcess = { txt ->
                    if (txt.isNotBlank()) {
                        val display = "${txt}\n\n${lastRes}"
                        textView.text = display
                    }
                },
                onCalculated = { l, txt ->
                    val list = l.subList(0, l.size.coerceAtMost(10))
                    if (txt.isNotBlank()) {
                        lastRes = ""
                        if (list.isEmpty()) {
                            lastRes = getString(R.string.asr_model_no_result)
                        } else {
                            for (i in list) {
                                lastRes = String.format(
                                    getString(R.string.asr_model_result_item),
                                    "${lastRes}${i.second}",
                                    i.third
                                )
                            }
                        }
                        val display = "${txt}\n\n${lastRes}"
                        textView.text = display
                    }
                },
                onStarted = {
                    textView.text = null
                })

            dialog.setOnCancelListener {
                client.destroy()
            }

            lifecycleScope.launch(Dispatchers.IO) {
                client.startRecord()
            }
        }
    }

    /**
     * Check ASR Model Status
     */
    private fun checkAsrModelStatus() {
        var flag = 0 // 0-null, 1-complete, 2-incomplete
        val name = preferences.getString("asr_model_name", "")!!
        val nameEn = preferences.getString("asr_model_name_en", name)!!
        val nameZh = preferences.getString("asr_model_name_zh", name)!!
        val displayName =
            when (requireContext().resources.configuration.locales.get(0).toLanguageTag()) {
                "zh-CN" -> nameZh
                else -> nameEn
            }
        if (name.isNotEmpty()) {
            flag = if (AsrService.checkAsrModelFiles(requireContext(), name)) 1 else 2
        }

        binding.setCtrlAsrModels.setHint(
            when (flag) {
                1 -> String.format(getString(R.string.set_ctrl_asr_model_ready), displayName)
                2 -> String.format(getString(R.string.set_ctrl_asr_model_incomplete), displayName)
                else -> getString(R.string.set_ctrl_asr_model_null)
            }
        )
        binding.setCtrlAsrSwitch.isEnabled = flag == 1
        binding.setCtrlAsrSwitchHint.setText(
            if (flag == 1) {
                R.string.set_ctrl_asr_enable_success
            } else {
                R.string.set_ctrl_asr_enable_failed
            }
        )
        binding.setCtrlAsrTest.setEnable(flag == 1)
        binding.setCtrlAsrTest.setHint(
            if (flag == 1) {
                getString(R.string.set_ctrl_asr_enable_success)
            } else {
                getString(R.string.set_ctrl_asr_enable_failed)
            }
        )
    }
}