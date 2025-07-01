package indie.wistefinch.callforstratagems.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import indie.wistefinch.callforstratagems.R

class DialogEditListAdapter : RecyclerView.Adapter<DialogEditListAdapter.ListViewHolder>() {

    private var dataList = emptyList<String>().toMutableList()

    private lateinit var context: Context

    class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_edit_list_item, parent, false)
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        holder.itemView.findViewById<TextView>(R.id.edit_list_item_input).text = dataList[pos]
        holder.itemView.findViewById<TextView>(R.id.edit_list_item_input)
            .addTextChangedListener { txt ->
                dataList[pos] = txt.toString()
            }
        holder.itemView.findViewById<AppButton>(R.id.edit_list_item_delete).setOnClickListener {
            dataList.removeAt(pos)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }


    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<String>) {
        this.dataList = list.toMutableList()
        notifyDataSetChanged()
    }

    fun getData(): List<String> {
        return dataList.toList()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun add(txt: String) {
        dataList.add(txt)
        notifyDataSetChanged()
    }
}