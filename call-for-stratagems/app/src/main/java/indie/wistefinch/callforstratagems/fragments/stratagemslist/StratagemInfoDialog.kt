package indie.wistefinch.callforstratagems.fragments.stratagemslist

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVGImageView
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.data.models.StratagemData
import java.io.File

class StratagemInfoDialog(context: Context) : Dialog(context) {

    /**
     * The step recycler view's adapter.
     */
    private val stepAdapter: StratagemInfoStepAdapter by lazy { StratagemInfoStepAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_stratagem_info)
    }

    fun setData(data: StratagemData, dbName: String, lang: String)
    {
        // Set card view text.
        findViewById<TextView>(R.id.stratagem_info_title).text = when (lang) {
            "zh-CN" -> data.nameZh
            else -> data.name
        }

        // Set icon resources.
        try {
            findViewById<SVGImageView>(R.id.stratagem_info_imageView)
                .setImageURI(
                    Uri.fromFile(
                        File(context.filesDir.path +
                                context.resources.getString(R.string.icons_path) +
                                "$dbName/" +
                                data.icon + ".svg")
                    ))
        }
        catch (_: Exception) {}

        // Set recycler view
        val view = findViewById<RecyclerView>(R.id.stratagem_info_steps_recyclerView)
        view.adapter = stepAdapter
        view.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        stepAdapter.setData(data.steps)
    }
}