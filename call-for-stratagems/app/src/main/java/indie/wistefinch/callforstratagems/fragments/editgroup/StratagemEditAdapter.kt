package indie.wistefinch.callforstratagems.fragments.editgroup

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.data.models.StratagemData

/**
 * Adapter for the recycler view in [EditGroupFragment]
 */
class StratagemEditAdapter: RecyclerView.Adapter<StratagemEditAdapter.ListViewHolder>() {

    /**
     * All data in the adapter.
     */
    private var dataList = emptyList<StratagemData>()

    /**
     * Context, used to obtain external information.
     */
    private lateinit var context: Context

    /**
     * A set of all the enabled stratagems.
     */
    lateinit var enabledStratagem: MutableSet<Int>

    class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_stratagem_edit, parent, false))
    }

    @SuppressLint("DiscouragedApi")
    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        // Set card view text.
        holder.itemView.findViewById<TextView>(R.id.stratagem_edit_title).text = dataList[pos].name

        // Get icon resources.
        // The resource must be obtained by name, so ignore the discouraged warning.
        val res = context.resources.getIdentifier(
            dataList[pos].icon,
            "drawable",
            context.packageName
        )
        if (res != 0) {
            holder.itemView.findViewById<ImageView>(R.id.stratagem_edit_imageView).setImageResource(res)
        }

        // Set card view background.
        val cardView = holder.itemView.findViewById<CardView>(R.id.stratagem_edit_cardView)
        setCardViewBg(cardView, dataList[pos].id)

        // Setup click listener, the selected status and background color will change after clicking.
        cardView.setOnClickListener {
            if (enabledStratagem.contains(dataList[pos].id)) {
                enabledStratagem.remove(dataList[pos].id)
            }
            else {
                enabledStratagem.add(dataList[pos].id)
            }
            setCardViewBg(cardView, dataList[pos].id)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    /**
     * Change the card view background if the stratagem is enabled.
     */
    private fun setCardViewBg(cardView: CardView, id: Int)
    {
        if (enabledStratagem.contains(id)) {
            cardView.setCardBackgroundColor(context.resources.getColor(R.color.darkBlue, context.theme))
        }
        else {
            cardView.setCardBackgroundColor(context.resources.getColor(R.color.colorBackground, context.theme))
        }
    }

    /**
     * Set the adapter data.
     *
     * The content of the view won't change after initialization, so there is no need to consider performance impact.
     * ignore the warning.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<StratagemData>, set: MutableSet<Int>) {
        this.enabledStratagem = set
        this.dataList = list
        notifyDataSetChanged()
    }
}