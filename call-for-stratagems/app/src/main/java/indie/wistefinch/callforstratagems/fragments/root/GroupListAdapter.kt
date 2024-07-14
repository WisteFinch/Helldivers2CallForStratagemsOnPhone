package indie.wistefinch.callforstratagems.fragments.root

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import indie.wistefinch.callforstratagems.R
import indie.wistefinch.callforstratagems.data.models.GroupData
import indie.wistefinch.callforstratagems.data.models.StratagemData
import indie.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import java.util.Vector

/**
 * Adapter for the group recycler view in [RootFragment]
 */
class GroupListAdapter: RecyclerView.Adapter<GroupListAdapter.ListViewHolder>() {

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

    class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    /**
     * The stratagem thumbnail recycler view's adapter.
     */
    private val adapter: StratagemThumbnailAdapter by lazy { StratagemThumbnailAdapter() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_group, parent, false))
    }

    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        holder.itemView.findViewById<TextView>(R.id.group_title).text = dataList[pos].title

        val bundle = bundleOf(Pair("currentItem", dataList[pos]))
        holder.itemView.findViewById<CardView>(R.id.group_cardView).setOnClickListener {
            holder.itemView.findNavController().navigate(R.id.action_rootFragment_to_viewGroupFragment, bundle)
        }

        // Setup stratagem thumbnail recycler view.
        val recyclerView = holder.itemView.findViewById<RecyclerView>(R.id.group_recyclerView)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val list: Vector<StratagemData> = Vector()
        for (i in dataList[pos].list) {
            if (stratagemViewModel.isIdValid(i)) {
                list.add(stratagemViewModel.retrieveItem(i))
            }
        }
        adapter.setData(list.toList())
        holder.itemView.findViewById<RecyclerView>(R.id.group_recyclerView).suppressLayout(true)
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
    fun setData(list: List<GroupData>) {
        // Check difference.
        val groupListDiffUtil = GroupListDiffUtil(dataList, list)
        val groupListDiffResult = DiffUtil.calculateDiff(groupListDiffUtil)
        this.dataList = list
        groupListDiffResult.dispatchUpdatesTo(this)
    }
}