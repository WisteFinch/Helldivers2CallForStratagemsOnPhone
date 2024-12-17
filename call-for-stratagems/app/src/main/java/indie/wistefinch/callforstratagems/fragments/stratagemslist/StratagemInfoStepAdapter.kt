package indie.wistefinch.callforstratagems.fragments.stratagemslist

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import indie.wistefinch.callforstratagems.R

/**
 * Adapter for the step recycler view in [StratagemInfoDialog]
 */
class StratagemInfoStepAdapter: RecyclerView.Adapter<StratagemInfoStepAdapter.ListViewHolder>() {

    /**
     * All data in the adapter.
     */
    private var dataList = emptyList<Int>()

    /**
     * Context, used to obtain external information.
     */
    private lateinit var context: Context

    class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_step_info, parent, false))
    }

    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        // Set icon for the view.
        when (dataList[pos]) {
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

    /**
     * Set the adapter data
     *
     * Because of the small amount of data, there is no need to compare the difference, ignore the warning.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<Int>) {
        this.dataList = list
        notifyDataSetChanged()
    }

    /**
     * Clear the adapter data
     *
     * Because of the small amount of data, there is no need to compare the difference, ignore the warning.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        setData(listOf())
    }
}