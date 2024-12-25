package indie.wistefinch.callforstratagems.fragments.editgroup

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVGImageView
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.fragments.stratagemslist.StratagemInfoDialog
import java.io.File

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

    private lateinit var dbName: String

    private var lang: String = "auto"

    class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_stratagem_edit, parent, false))
    }

    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        // Set card view text.
        holder.itemView.findViewById<TextView>(R.id.stratagem_edit_title).text = when (lang) {
            "zh-CN" -> dataList[pos].nameZh
            else -> dataList[pos].name
        }

        // Set icon resources.
        try {
            holder.itemView.findViewById<SVGImageView>(R.id.stratagem_edit_imageView)
                .setImageURI(
                    Uri.fromFile(
                        File(context.filesDir.path +
                                context.resources.getString(R.string.icons_path) +
                                "$dbName/" +
                                dataList[pos].icon + ".svg")))
        }
        catch (_: Exception) {}

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

        // Set long click listener.
        holder.itemView.findViewById<CardView>(R.id.stratagem_edit_cardView).setOnLongClickListener {
            val dialog = StratagemInfoDialog(context)
            dialog.show()
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setData(dataList[pos], dbName, lang)
            true
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
    fun setData(list: List<StratagemData>, set: MutableSet<Int>, name: String, lang: String) {
        this.enabledStratagem = set
        this.dataList = list
        dbName = name
        this.lang = lang
        notifyDataSetChanged()
    }
}