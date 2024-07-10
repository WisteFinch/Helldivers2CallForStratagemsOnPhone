package indi.wistefinch.callforstratagems.fragments.play

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import indi.wistefinch.callforstratagems.R
import indi.wistefinch.callforstratagems.data.models.StratagemData

class StepPlayAdapter: RecyclerView.Adapter<StepPlayAdapter.ListViewHolder>() {

    private var dataList = emptyList<Int>()
    private lateinit var context: Context

    class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_step_play, parent, false))
    }

    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        when(dataList[pos]) {
            1 -> holder.itemView.findViewById<ImageView>(R.id.step_imageView).setImageResource(R.drawable.ic_arrow_upward)
            2 -> holder.itemView.findViewById<ImageView>(R.id.step_imageView).setImageResource(R.drawable.ic_arrow_downward)
            3 -> holder.itemView.findViewById<ImageView>(R.id.step_imageView).setImageResource(R.drawable.ic_arrow_backward)
            4 -> holder.itemView.findViewById<ImageView>(R.id.step_imageView).setImageResource(R.drawable.ic_arrow_forward)
            else -> holder.itemView.findViewById<ImageView>(R.id.step_imageView).setImageResource(R.drawable.ic_arrow_upward)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun setData(list: List<Int>) {
        this.dataList = list
        notifyDataSetChanged()
    }
}