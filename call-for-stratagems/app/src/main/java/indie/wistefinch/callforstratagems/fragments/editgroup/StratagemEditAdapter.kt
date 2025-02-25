package indie.wistefinch.callforstratagems.fragments.editgroup

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVGImageView
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.data.models.StratagemData
import java.io.File

/**
 * Adapter for the recycler view in [EditGroupFragment]
 */
class StratagemEditAdapter : RecyclerView.Adapter<StratagemEditAdapter.ListViewHolder>(),
    ItemTouchHelperAdapter {

    /**
     * All data in the adapter.
     */
    private var dataList = emptyList<StratagemData>().toMutableList()

    /**
     * Context, used to obtain external information.
     */
    private lateinit var context: Context

    /**
     * A set of all the enabled stratagems.
     */
    private lateinit var enabledStratagem: MutableSet<Int>

    private lateinit var dbName: String

    private var lang: String = "auto"

    class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_stratagem_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        val borderTopView = holder.itemView.findViewById<View>(R.id.stratagem_item_border_top)
        val borderBottomView = holder.itemView.findViewById<View>(R.id.stratagem_item_border_bottom)
        val imageView = holder.itemView.findViewById<SVGImageView>(R.id.stratagem_item_image)

        // Set icon resources.
        try {
            imageView.setImageURI(
                Uri.fromFile(
                    File(
                        context.filesDir.path +
                                context.resources.getString(R.string.icons_path) +
                                "$dbName/" +
                                dataList[pos].icon + ".svg"
                    )
                )
            )
        } catch (_: Exception) {
        }

        // Set view background.
        val setViewBg = { id: Int ->
            if (enabledStratagem.contains(id)) {
                borderTopView.setBackgroundResource(R.drawable.clickable_bg_top_pressed)
                borderBottomView.setBackgroundResource(R.drawable.clickable_bg_bottom_pressed)
                imageView.setBackgroundColor(
                    context.getColor(
                        R.color.buttonBackgroundPressed
                    )
                )
            } else {
                borderTopView.setBackgroundResource(R.drawable.clickable_bg_top)
                borderBottomView.setBackgroundResource(R.drawable.clickable_bg_bottom)
                imageView.setBackgroundColor(
                    context.getColor(
                        R.color.buttonBackground
                    )
                )
            }
        }
        setViewBg(dataList[holder.adapterPosition].id)

        // Setup click listener, the selected status and background color will change after clicking.
        holder.itemView.setOnClickListener {
            val index = holder.adapterPosition
            if (enabledStratagem.contains(dataList[index].id)) {
                enabledStratagem.remove(dataList[index].id)
            } else {
                enabledStratagem.add(dataList[index].id)
            }
            setViewBg(dataList[index].id)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    /**
     * Set the adapter data.
     *
     * The content of the view won't change after initialization, so there is no need to consider performance impact.
     * ignore the warning.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<StratagemData>, set: MutableSet<Int>, name: String, lang: String) {
        this.dataList = list.toMutableList()
        this.enabledStratagem = set
        dbName = name
        this.lang = lang
        notifyDataSetChanged()
    }

    override fun onItemMove(source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) {
        val fromPos = source.adapterPosition
        val toPos = target.adapterPosition
        if (fromPos < dataList.size && toPos < dataList.size) {
            val ori = dataList[fromPos]
            dataList.removeAt(fromPos)
            dataList.add(toPos, ori)
            notifyItemMoved(fromPos, toPos)
        }
        onItemClear(source)
    }

    override fun onItemSelect(source: RecyclerView.ViewHolder) {
        source.itemView.scaleX = 1.2f
        source.itemView.scaleY = 1.2f
    }

    override fun onItemClear(source: RecyclerView.ViewHolder) {
        source.itemView.scaleX = 1.0f
        source.itemView.scaleY = 1.0f
    }

    fun getEnabledStratagems(): List<Int> {
        val list: MutableSet<Int> = emptySet<Int>().toMutableSet()
        for (i in dataList) {
            if (enabledStratagem.contains(i.id))
                list.add(i.id)
        }
        return list.toList()
    }
}