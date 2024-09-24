package indie.wistefinch.callforstratagems.fragments.play

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.data.models.StratagemData

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

    class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_stratagem_simplified_play, parent, false))
    }

    @SuppressLint("DiscouragedApi")
    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        // For compatibility with lower SDKs, ignore the discouraged warning.
        val res = context.resources.getIdentifier(
            dataList[pos].icon,
            "drawable",
            context.packageName
        )
        if (res != 0) {
            holder.itemView.findViewById<ImageView>(R.id.stratagem_simplified_play_button).setImageResource(res)
            holder.itemView.findViewById<ImageView>(R.id.stratagem_simplified_play_button).setOnClickListener {
                onItemClick?.invoke(dataList[pos])
            }
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
    fun setData(list: List<StratagemData>) {
        this.dataList = list
        notifyDataSetChanged()
    }
}