package indi.wistefinch.callforstratagems.fragments.viewgroup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import indi.wistefinch.callforstratagems.R
import indi.wistefinch.callforstratagems.data.models.StratagemData

class StratagemViewAdapter: RecyclerView.Adapter<StratagemViewAdapter.ListViewHolder>() {

    private var dataList = emptyList<StratagemData>()
    private lateinit var context: Context

    class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.stratagem_view_layout, parent, false))
    }

    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        holder.itemView.findViewById<TextView>(R.id.stratagem_view_title).text = dataList[pos].name
        val res = context.resources.getIdentifier(
            dataList[pos].icon,
            "drawable",
            context.packageName
        )
        if(res != 0) {
            holder.itemView.findViewById<ImageView>(R.id.stratagem_view_imageView).setImageResource(res)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun setData(list: List<StratagemData>) {
        this.dataList = list
        notifyDataSetChanged()
    }
}