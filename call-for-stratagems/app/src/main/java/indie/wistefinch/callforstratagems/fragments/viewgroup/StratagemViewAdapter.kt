package indie.wistefinch.callforstratagems.fragments.viewgroup

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVGImageView
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.fragments.stratagemslist.StratagemInfoDialog
import java.io.File

/**
 * Adapter for the stratagem recycler view in [ViewGroupFragment]
 */
class StratagemViewAdapter: RecyclerView.Adapter<StratagemViewAdapter.ListViewHolder>() {

    /**
     * All data in the adapter.
     */
    private var dataList = emptyList<StratagemData>()

    /**
     * Context, used to obtain external information.
     */
    private lateinit var context: Context

    private lateinit var dbName: String

    private var lang: String = "auto"

    private lateinit var dialog: StratagemInfoDialog

    class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        dialog = StratagemInfoDialog(context)
        return ListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_stratagem_view, parent, false))
    }

    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        val borderTopView = holder.itemView.findViewById<View>(R.id.stratagem_view_border_top)
        val borderBottomView = holder.itemView.findViewById<View>(R.id.stratagem_view_border_bottom)
        val imageView = holder.itemView.findViewById<SVGImageView>(R.id.stratagem_view_image)

        // Set icon resources.
        try {
            imageView.setImageURI(
                    Uri.fromFile(
                        File(context.filesDir.path +
                            context.resources.getString(R.string.icons_path) +
                            "$dbName/" +
                            dataList[pos].icon + ".svg")))
        }
        catch (_: Exception) {}

        // Set long click listener.
        holder.itemView.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    borderTopView.setBackgroundResource(R.drawable.clickable_bg_top)
                    borderBottomView.setBackgroundResource(R.drawable.clickable_bg_bottom)
                    imageView.setBackgroundColor(
                        context.getColor(
                            R.color.buttonBackground
                        )
                    )
                    view.performClick()
                }

                MotionEvent.ACTION_DOWN -> {
                    borderTopView.setBackgroundResource(R.drawable.clickable_bg_top_pressed)
                    borderBottomView.setBackgroundResource(R.drawable.clickable_bg_bottom_pressed)
                    imageView.setBackgroundColor(
                        context.getColor(
                            R.color.buttonBackgroundPressed
                        )
                    )
                }

                MotionEvent.ACTION_CANCEL -> {
                    borderTopView.setBackgroundResource(R.drawable.clickable_bg_top)
                    borderBottomView.setBackgroundResource(R.drawable.clickable_bg_bottom)
                    imageView.setBackgroundColor(
                        context.getColor(
                            R.color.buttonBackground
                        )
                    )
                }
            }
            true
        }
        holder.itemView.setOnClickListener {
            if (!dialog.isShowing) {
                dialog.show()
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.setData(dataList[pos], dbName, lang)
            }
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    /**
     * Set the adapter data.
     *
     * Because the data won't change, there is no need to compare the difference, ignore the warning.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<StratagemData>, name: String, lang: String) {
        this.dataList = list
        dbName = name
        this.lang = lang
        notifyDataSetChanged()
    }
}