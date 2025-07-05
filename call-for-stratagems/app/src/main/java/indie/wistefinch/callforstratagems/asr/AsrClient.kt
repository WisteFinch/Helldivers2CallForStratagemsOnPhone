package indie.wistefinch.callforstratagems.asr

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.asr.AsrService.ASRErrType
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.data.viewmodel.AsrKeywordViewModel
import org.apache.commons.text.similarity.LevenshteinDistance


class AsrClient(
    private val context: Context,
    private val activity: Activity,
    asrModelName: String,
    stratagems: List<StratagemData>,
    dbName: String,
    lang: String,
    keywordsViewModel: AsrKeywordViewModel,
    private val activateWords: List<String>,
    private val similarityThreshold: Float,
    useGPU: Boolean,
    private val onProcess: (txt: String) -> Unit = {},
    private val onEndPoint: (txt: String) -> Unit = {},
    private val onStarted: () -> Unit = {},
    private val onStopped: () -> Unit = {},
    private val onError: (e: ASRErrType) -> Unit = {},
    private val onReady: () -> Unit = {},
    private val onCalculated: (list: List<Triple<Int, String, Float>>, txt: String) -> Unit = { _, _ -> }
) {
    private val isActivateWordsEnabled: Boolean
    private var keywords: MutableList<Pair<Int, String>>

    @Volatile
    var isRecording: Boolean = false

    init {
        AsrService.initModel(
            name = asrModelName,
            context = context,
            activity = activity,
            useGPU = useGPU,
            onProcess = { txt ->
                onProcess(txt)
            },
            onEndPoint = { txt ->
                onEndPoint(txt)
                val list = calcSimilarity(txt)
                Log.i(
                    "[ASR Client]", "Calc: " + if (list.isEmpty()) context.getString(
                        R.string.asr_model_no_result
                    ) else String.format(
                        context.getString(
                            R.string.asr_model_result_item
                        ), list.first().second, list.first().third
                    )
                )
                onCalculated(list, txt)
            }, onStarted = {
                onStarted()
            }, onStopped = {
                onStopped()
            },
            onError = { e ->
                onError(e)
            },
            onReady = {
                onReady()
            })
        isActivateWordsEnabled = activateWords.isNotEmpty()
        // Generate stratagems keywords
        keywords = emptyList<Pair<Int, String>>().toMutableList()
        for (s in stratagems) {
            keywords.add(
                Pair(
                    s.id, strFilter(
                        when (lang) {
                            "zh-CN" -> s.nameZh
                            else -> s.name
                        }
                    )
                )
            )
            if (keywordsViewModel.isIdValid(s.id, dbName)) {
                var l: MutableList<String> = emptyList<String>().toMutableList()
                l = Gson().fromJson(
                    keywordsViewModel.retrieveItem(s.id, dbName).keywords,
                    l.javaClass
                )
                for (i in l) {
                    keywords.add(Pair(s.id, strFilter(i)))
                }
            }
        }
    }

    fun startRecord(): Boolean {
        isRecording = AsrService.startRecord(context, activity)
        return isRecording
    }

    fun stopRecord() {
        AsrService.stopRecord()
        isRecording = false
    }

    fun destroy() {
        AsrService.destroyModel()
    }

    private fun calcSimilarity(txt: String): List<Triple<Int, String, Float>> {
        val list = emptyList<Triple<Int, String, Float>>().toMutableList()
        val txtLen = txt.length
        for (i in keywords) {
            val len = i.second.length.coerceAtLeast(txtLen)
            val s = (len - LevenshteinDistance.getDefaultInstance().apply(i.second, txt)
                .toFloat()) / len
            if (s >= similarityThreshold) {
                list.add(Triple(i.first, i.second, s))
            }
        }
        if (list.isEmpty()) {
            return list
        } else {
            list.sortBy { it.third }
            list.reverse()
            return list
        }
    }

    private fun strFilter(str: String): String {
        return str.replace(Regex("[-\"“”]"), "").uppercase()
    }

}