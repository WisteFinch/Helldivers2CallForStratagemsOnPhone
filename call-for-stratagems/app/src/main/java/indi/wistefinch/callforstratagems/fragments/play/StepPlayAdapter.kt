package indi.wistefinch.callforstratagems.fragments.play

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import indi.wistefinch.callforstratagems.R

class StepPlayAdapter: RecyclerView.Adapter<StepPlayAdapter.ListViewHolder>() {

    private var dataList = emptyList<Int>()
    private lateinit var context: Context

    class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_step_play, parent, false))
    }

    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        when (dataList[pos]) {
            1 -> holder.itemView.findViewById<ImageView>(R.id.step_imageView).setImageResource(R.drawable.ic_arrow_upward)
            2 -> holder.itemView.findViewById<ImageView>(R.id.step_imageView).setImageResource(R.drawable.ic_arrow_downward)
            3 -> holder.itemView.findViewById<ImageView>(R.id.step_imageView).setImageResource(R.drawable.ic_arrow_backward)
            4 -> holder.itemView.findViewById<ImageView>(R.id.step_imageView).setImageResource(R.drawable.ic_arrow_forward)
            5 -> { holder.itemView.findViewById<ImageView>(R.id.step_imageView).setImageResource(R.drawable.ic_arrow_upward)
                holder.itemView.findViewById<ImageView>(R.id.step_imageView)
                    .drawable
                    .setTintList(context.resources.getColorStateList(R.color.yellow, context.theme))
            }
            6 -> { holder.itemView.findViewById<ImageView>(R.id.step_imageView).setImageResource(R.drawable.ic_arrow_downward)
                holder.itemView.findViewById<ImageView>(R.id.step_imageView)
                    .drawable
                    .setTintList(context.resources.getColorStateList(R.color.yellow, context.theme))
            }
            7 -> { holder.itemView.findViewById<ImageView>(R.id.step_imageView).setImageResource(R.drawable.ic_arrow_backward)
                holder.itemView.findViewById<ImageView>(R.id.step_imageView)
                    .drawable
                    .setTintList(context.resources.getColorStateList(R.color.yellow, context.theme))
            }
            8 -> { holder.itemView.findViewById<ImageView>(R.id.step_imageView).setImageResource(R.drawable.ic_arrow_forward)
                holder.itemView.findViewById<ImageView>(R.id.step_imageView)
                    .drawable
                    .setTintList(context.resources.getColorStateList(R.color.yellow, context.theme))
            }
            else -> holder.itemView.findViewById<ImageView>(R.id.step_imageView).setImageResource(R.drawable.ic_arrow_upward)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    /**
     * Set the adapter data
     * Because of the small amount of data, there is no need to compare the difference
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<Int>) {
        this.dataList = list
        notifyDataSetChanged()
    }
}