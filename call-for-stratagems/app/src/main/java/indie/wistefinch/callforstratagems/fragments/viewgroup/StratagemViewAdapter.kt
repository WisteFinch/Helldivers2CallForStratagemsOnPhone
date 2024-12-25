package indie.wistefinch.callforstratagems.fragments.viewgroup

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVGImageView
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.fragments.stratagemslist.StratagemInfoDialog
import java.io.File

/**
 * Adapter for the stratagem recycler view in [ViewGroupFragment]
 */
class StratagemViewAdapter: RecyclerView.Adapter<StratagemViewAdapter.ListViewHolder>() {

    /**
     * All data in the adapter.
     */
    private var dataList = emptyList<StratagemData>()

    /**
     * Context, used to obtain external information.
     */
    private lateinit var context: Context

    private lateinit var dbName: String

    private var lang: String = "auto"

    class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_stratagem_view, parent, false))
    }

    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        // Set card view text.
        holder.itemView.findViewById<TextView>(R.id.stratagem_view_title).text = when (lang) {
            "zh-CN" -> dataList[pos].nameZh
            else -> dataList[pos].name
        }

        // Set icon resources.
        try {
            holder.itemView.findViewById<SVGImageView>(R.id.stratagem_view_imageView)
                .setImageURI(
                    Uri.fromFile(
                        File(context.filesDir.path +
                            context.resources.getString(R.string.icons_path) +
                            "$dbName/" +
                            dataList[pos].icon + ".svg")))
        }
        catch (_: Exception) {}

        // Set long click listener.
        holder.itemView.findViewById<CardView>(R.id.stratagem_view_cardView).setOnLongClickListener {
            val dialog = StratagemInfoDialog(context)
            dialog.show()
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setData(dataList[pos], dbName, lang)
            true
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    /**
     * Set the adapter data.
     *
     * Because the data won't change, there is no need to compare the difference, ignore the warning.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<StratagemData>, name: String, lang: String) {
        this.dataList = list
        dbName = name
        this.lang = lang
        notifyDataSetChanged()
    }
}