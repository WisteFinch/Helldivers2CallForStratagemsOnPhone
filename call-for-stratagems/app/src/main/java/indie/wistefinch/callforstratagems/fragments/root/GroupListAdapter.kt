package indie.wistefinch.callforstratagems.fragments.root

import android.content.Context
import android.net.Uri
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVGImageView
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.data.models.GroupData
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import java.io.File
import kotlin.math.min

/**
 * Adapter for the group recycler view in [RootFragment]
 */
class GroupListAdapter : RecyclerView.Adapter<GroupListAdapter.ListViewHolder>() {

    /**
     * All data in the adapter.
     */
    private var dataList = emptyList<GroupData>()

    /**
     * The stratagem view model,.
     */
    private lateinit var stratagemViewModel: StratagemViewModel

    /**
     * Context, used to obtain external information.
     */
    private lateinit var context: Context

    /**
     * Enable fastboot mode.
     */
    private var fastboot = false


    class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.layout_group, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        holder.itemView.findViewById<TextView>(R.id.group_title).text = dataList[pos].title

        val bundle = bundleOf(Pair("currentItem", dataList[pos]))
        if (fastboot) {
            holder.itemView.findViewById<ConstraintLayout>(R.id.group_cardView).setOnClickListener {
                holder.itemView.findNavController()
                    .navigate(R.id.action_rootFragment_to_playFragment, bundle)
            }
            holder.itemView.findViewById<ConstraintLayout>(R.id.group_cardView)
                .setOnLongClickListener {
                    holder.itemView.findNavController()
                        .navigate(R.id.action_rootFragment_to_viewGroupFragment, bundle)
                    true
                }
        } else {
            holder.itemView.findViewById<ConstraintLayout>(R.id.group_cardView).setOnClickListener {
                holder.itemView.findNavController()
                    .navigate(R.id.action_rootFragment_to_viewGroupFragment, bundle)
            }
        }

        // Setup stratagem thumbnail view.
        val layout = holder.itemView.findViewById<LinearLayout>(R.id.group_stratagem_list)
        layout.removeAllViews()
        layout.post {
            val preference = context.let { PreferenceManager.getDefaultSharedPreferences(it) }!!
            val dbName =
                preference.getString("db_name", context.resources.getString(R.string.db_hd2_name))!!
            val maxCount =
                (holder.itemView.width / (context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT) - 50) / 34
            if (dataList.size <= pos) {
                return@post
            }
            if (dataList[pos].list.isEmpty()) {
                layout.addView(
                    LayoutInflater.from(context)
                        .inflate(R.layout.layout_stratagem_thumbnail, layout, false)
                )
            } else {
                for (i in 0..<min(maxCount, dataList[pos].list.size)) {
                    val item = dataList[pos].list[i]
                    val itemView = LayoutInflater.from(context)
                        .inflate(R.layout.layout_stratagem_thumbnail, layout, false)
                    if (stratagemViewModel.isIdValid(item)) {
                        val data = stratagemViewModel.retrieveItem(item)
                        try {
                            itemView.findViewById<SVGImageView>(R.id.stratagem_thumbnail_imageView)
                                .setImageURI(
                                    Uri.fromFile(
                                        File(
                                            context.filesDir.path +
                                                    context.resources.getString(R.string.icons_path) +
                                                    "$dbName/" +
                                                    data.icon + ".svg"
                                        )
                                    )
                                )
                        } catch (_: Exception) {
                        }
                    }
                    layout.addView(itemView)
                }
                if (dataList[pos].list.size > maxCount) {
                    val msg = holder.itemView.findViewById<TextView>(R.id.group_overflow)
                    msg.visibility = View.VISIBLE
                    msg.text = String.format(
                        context.resources.getString(R.string.root_group_overflow),
                        (dataList[pos].list.size - maxCount).toString()
                    )
                } else {
                    holder.itemView.findViewById<TextView>(R.id.group_overflow).visibility =
                        View.GONE
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    /**
     * Set stratagem view model.
     */
    fun setStratagemViewModel(viewModel: StratagemViewModel) {
        stratagemViewModel = viewModel
    }

    /**
     * Set the adapter data.
     */
    fun setData(list: List<GroupData>, fastboot: Boolean) {
        this.fastboot = fastboot
        // Check difference.
        val groupListDiffUtil = GroupListDiffUtil(dataList, list)
        val groupListDiffResult = DiffUtil.calculateDiff(groupListDiffUtil)
        this.dataList = list
        groupListDiffResult.dispatchUpdatesTo(this)
    }
}