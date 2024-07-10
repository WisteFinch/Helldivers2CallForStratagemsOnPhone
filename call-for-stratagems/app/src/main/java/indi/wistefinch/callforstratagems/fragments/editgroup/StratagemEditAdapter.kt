package indi.wistefinch.callforstratagems.fragments.editgroup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import indi.wistefinch.callforstratagems.R
import indi.wistefinch.callforstratagems.data.models.StratagemData

class StratagemEditAdapter: RecyclerView.Adapter<StratagemEditAdapter.ListViewHolder>() {

    private var dataList = emptyList<StratagemData>()
    private lateinit var context: Context
    lateinit var set: MutableSet<Int>

    class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_stratagem_edit, parent, false))
    }

    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        holder.itemView.findViewById<TextView>(R.id.stratagem_edit_title).text = dataList[pos].name
        val res = context.resources.getIdentifier(
            dataList[pos].icon,
            "drawable",
            context.packageName
        )
        if(res != 0) {
            holder.itemView.findViewById<ImageView>(R.id.stratagem_edit_imageView).setImageResource(res)
        }
        val cardView = holder.itemView.findViewById<CardView>(R.id.stratagem_edit_cardView)
        setCardViewBg(cardView, dataList[pos].id)
        cardView.setOnClickListener {
            if (set.contains(dataList[pos].id)) {
                set.remove(dataList[pos].id)
            }
            else {
                set.add(dataList[pos].id)
            }
            setCardViewBg(cardView, dataList[pos].id)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    private fun setCardViewBg(cardView: CardView, id: Int)
    {
        if(set.contains(id)) {
            cardView.setCardBackgroundColor(context.resources.getColor(R.color.darkBlue, context.theme))
        }
        else {
            cardView.setCardBackgroundColor(context.resources.getColor(R.color.colorBackground, context.theme))
        }
    }

    fun setData(list: List<StratagemData>, set: MutableSet<Int>) {
        this.set = set
        this.dataList = list
        notifyDataSetChanged()
    }
}