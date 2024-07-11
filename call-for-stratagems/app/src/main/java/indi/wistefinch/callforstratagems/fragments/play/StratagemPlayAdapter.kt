package indi.wistefinch.callforstratagems.fragments.play

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import indi.wistefinch.callforstratagems.R
import indi.wistefinch.callforstratagems.data.models.StratagemData

class StratagemPlayAdapter: RecyclerView.Adapter<StratagemPlayAdapter.ListViewHolder>() {

    var onItemClick: ((StratagemData) -> Unit)? = null

    private var dataList = emptyList<StratagemData>()
    private lateinit var context: Context

    class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_stratagem_play, parent, false))
    }

    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        // For compatibility with lower SDKs, ignore the warning
        val res = context.resources.getIdentifier(
            dataList[pos].icon,
            "drawable",
            context.packageName
        )
        if (res != 0) {
            holder.itemView.findViewById<ImageView>(R.id.stratagem_play_button).setImageResource(res)
            holder.itemView.findViewById<ImageView>(R.id.stratagem_play_button).setOnClickListener {
                onItemClick?.invoke(dataList[pos])
            }
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    /**
     * Set the adapter data
     * Because the data won't change, there is no need to compare the difference
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<StratagemData>) {
        this.dataList = list
        notifyDataSetChanged()
    }
}