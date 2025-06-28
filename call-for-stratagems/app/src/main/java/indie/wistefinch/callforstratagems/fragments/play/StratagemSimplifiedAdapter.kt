package indie.wistefinch.callforstratagems.fragments.play

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVGImageView
import indie.wistefinch.callforstratagems.Constants
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.utils.Utils
import java.io.File

/**
 * Adapter for the simplified stratagem recycler view in [PlayFragment]
 */
class StratagemSimplifiedAdapter: RecyclerView.Adapter<StratagemSimplifiedAdapter.ListViewHolder>() {

    /**
     * Item click callback, transmit the stratagem data.
     */
    var onItemClick: ((StratagemData) -> Unit)? = null

    /**
     * All data in the adapter.
     */
    var dataList = emptyList<StratagemData>()

    /**
     * Context, used to obtain external information.
     */
    private lateinit var context: Context

    private lateinit var dbName: String

    private var size: Int = 100

    class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_stratagem_simplified_play, parent, false))
    }

    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        // Set icon resources.
        try {
            val view = holder.itemView.findViewById<SVGImageView>(R.id.stratagem_simplified_play_button)
            view.setImageURI(
                Uri.fromFile(
                    File(
                        context.filesDir.path +
                                Constants.PATH_DB_ICONS +
                                "$dbName/" +
                                dataList[pos].icon + ".svg")))
            val params = view.layoutParams
            params.width = Utils.dpToPx(context, size)
            params.height = Utils.dpToPx(context, size)
            view.layoutParams = params
        }
        catch (_: Exception) {}
        holder.itemView.findViewById<SVGImageView>(R.id.stratagem_simplified_play_button).setOnClickListener {
            onItemClick?.invoke(dataList[pos])
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
    fun setData(list: List<StratagemData>, name: String, size: Int = 100) {
        this.dataList = list
        dbName = name
        this.size = size
        notifyDataSetChanged()
    }
}