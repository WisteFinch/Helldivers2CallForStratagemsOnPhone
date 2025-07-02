package indie.wistefinch.callforstratagems.fragments.stratagemslist

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVGImageView
import com.google.gson.Gson
import indie.wistefinch.callforstratagems.Constants
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.data.models.AsrKeywordData
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.data.viewmodel.AsrKeywordViewModel
import indie.wistefinch.callforstratagems.utils.AppButton
import indie.wistefinch.callforstratagems.utils.EditListDialog
import java.io.File

class StratagemInfoDialog(
    context: Context,
    private val activity: Activity,
    private val asrKeywordViewModel: AsrKeywordViewModel
) :
    Dialog(context) {

    /**
     * The step recycler view's adapter.
     */
    private val stepAdapter: StratagemInfoStepAdapter by lazy { StratagemInfoStepAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_stratagem_info)
    }

    fun setData(data: StratagemData, dbName: String, lang: String) {
        // Set card view text.
        val displayName = when (lang) {
            "zh-CN" -> data.nameZh
            else -> data.name
        }
        findViewById<TextView>(R.id.stratagem_info_title).text = displayName

        // Set icon resources.
        try {
            findViewById<SVGImageView>(R.id.stratagem_info_imageView)
                .setImageURI(
                    Uri.fromFile(
                        File(
                            context.filesDir.path +
                                    Constants.PATH_DB_ICONS +
                                    "$dbName/" +
                                    data.icon + ".svg"
                        )
                    )
                )
        } catch (_: Exception) {
        }

        // Set recycler view
        val view = findViewById<RecyclerView>(R.id.stratagem_info_steps_recyclerView)
        view.adapter = stepAdapter
        view.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        stepAdapter.setData(data.steps)

        // Set ASR keyword
        refreshKeyword(data.id, dbName)

        // Set ASR keyword edit dialog
        findViewById<AppButton>(R.id.stratagem_info_asr_edit).setOnClickListener {
            hide()
            var list: MutableList<String> = emptyList<String>().toMutableList()
            val valid = asrKeywordViewModel.isIdValid(data.id, dbName)
            if (valid) {
                val str = asrKeywordViewModel.retrieveItem(data.id, dbName).keywords
                list = Gson().fromJson(str, list.javaClass)
            }
            val dialog = EditListDialog(
                context,
                activity,
                list,
                displayName
            )
            dialog.onEditFinished { d ->
                val res: MutableList<String> = emptyList<String>().toMutableList()
                for (i in d) {
                    if (i.isNotBlank()) {
                        res.add(i)
                    }
                }
                if (valid) {
                    val newData = asrKeywordViewModel.retrieveItem(data.id, dbName)
                    newData.keywords = Gson().toJson(res).toString()
                    asrKeywordViewModel.updateItem(newData)
                } else {
                    asrKeywordViewModel.insertItem(
                        AsrKeywordData(
                            dbName = dbName,
                            stratagem = data.id,
                            keywords = Gson().toJson(res).toString()
                        )
                    )
                }
                refreshKeyword(data.id, dbName)
            }
        }
    }

    private fun refreshKeyword(id: Int, dbName: String) {
        if (asrKeywordViewModel.isIdValid(id, dbName)) {
            val asrKeyword = asrKeywordViewModel.retrieveItem(id, dbName)
            var displayKeyword: String
            var list: MutableList<String> = emptyList<String>().toMutableList()
            list = Gson().fromJson(asrKeyword.keywords, list.javaClass)
            if (list.isEmpty()) {
                findViewById<TextView>(R.id.stratagem_info_asr_keywords).setText(R.string.dlg_stratagem_info_keyword_empty)
            } else {
                displayKeyword = list[0]
                for (i in 1..<list.size) {
                    displayKeyword = "${displayKeyword}, ${list[i]}"
                }
                findViewById<TextView>(R.id.stratagem_info_asr_keywords).text = String.format(
                    context.getString(R.string.dlg_stratagem_info_keyword),
                    displayKeyword
                )
            }
        } else {
            findViewById<TextView>(R.id.stratagem_info_asr_keywords).setText(R.string.dlg_stratagem_info_keyword_empty)
        }
    }
}