package indi.wistefinch.callforstratagems.fragments.root

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import indi.wistefinch.callforstratagems.R
import indi.wistefinch.callforstratagems.data.models.GroupData

class GroupListAdapter: RecyclerView.Adapter<GroupListAdapter.GroupViewHolder>() {

    private var dataList = emptyList<GroupData>()

    class GroupViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        return GroupViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.group_layout, parent, false))
    }

    override fun onBindViewHolder(holder: GroupViewHolder, pos: Int) {
        holder.itemView.findViewById<TextView>(R.id.group_title).text = dataList[pos].title
        // TODO: Setup stratagem icons

        val bundle = bundleOf(Pair("currentItem", dataList[pos]))
        holder.itemView.findViewById<CardView>(R.id.group_cardView).setOnClickListener {
            holder.itemView.findNavController().navigate(R.id.action_rootFragment_to_viewGroupFragment, bundle)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun setData(list: List<GroupData>) {
        this.dataList = list
        notifyDataSetChanged()
    }
}