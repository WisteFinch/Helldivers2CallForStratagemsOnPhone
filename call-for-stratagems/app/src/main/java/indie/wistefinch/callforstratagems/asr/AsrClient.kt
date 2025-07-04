package indie.wistefinch.callforstratagems.asr

import android.app.Activity
import android.content.Context
import com.google.gson.Gson
import indie.wistefinch.callforstratagems.asr.AsrService.ASRErrType
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.data.viewmodel.AsrKeywordViewModel
import org.apache.commons.text.similarity.LevenshteinDistance


class AsrClient(
    private val context: Context,
    private val activity: Activity,
    private val asrModelName: String,
    private val stratagems: List<StratagemData>,
    private val dbName: String,
    private val lang: String,
    private val keywordsViewModel: AsrKeywordViewModel,
    private val activateWords: List<String>,
    private val similarityThreshold: Float,
    private val onProcess: (txt: String) -> Unit = {},
    private val onEndPoint: (txt: String) -> Unit = {},
    private val onStarted: () -> Unit = {},
    private val onStopped: () -> Unit = {},
    private val onError: (e: ASRErrType) -> Unit = {},
    private val onReady: () -> Unit = {},
    private val onCalculated: (list: List<Triple<Int, String, Float>>, txt: String) -> Unit = {_, _ ->}
) {
    private val isActivateWordsEnabled: Boolean
    private var keywords: MutableList<Pair<Int, String>>

    init {
        AsrService.initModel(
            name = asrModelName,
            context = context,
            activity = activity,
            onProcess = { txt ->
                onProcess(txt)
            },
            onEndPoint = { txt ->
                onEndPoint(txt)
                onCalculated(calcSimilarity(txt), txt)
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
            keywords.add(Pair(s.id, strFilter(when (lang) {
                "zh-CN" -> s.nameZh
                else -> s.name
            })))
            if (keywordsViewModel.isIdValid(s.id, dbName)) {
                var l: MutableList<String> = emptyList<String>().toMutableList()
                l = Gson().fromJson(keywordsViewModel.retrieveItem(s.id, dbName).keywords, l.javaClass)
                for (i in l) {
                    keywords.add(Pair(s.id, strFilter(i)))
                }
            }
        }
    }

    fun startRecord() {
        AsrService.startRecord(context, activity)
    }

    fun stopRecord() {
        AsrService.stopRecord()
    }

    fun destroy() {
        AsrService.destroyModel()
    }

    private fun calcSimilarity(txt: String): List<Triple<Int, String, Float>> {
        val list = emptyList<Triple<Int, String, Float>>().toMutableList()
        val txtLen = txt.length
        for (i in keywords) {
            val len = i.second.length.coerceAtLeast(txtLen)
            val s = (len - LevenshteinDistance.getDefaultInstance().apply(i.second, txt).toFloat()) / len
            if (s >= similarityThreshold) {
                list.add(Triple(i.first, i.second, s))
            }
        }
        if (list.isEmpty()) {
            return list
        }
        else {
            list.sortBy { it.third }
            list.reverse()
            return list
        }
    }

    private fun strFilter(str: String): String {
        return str.replace(Regex("[-\"“”]"), "").uppercase()
    }

}