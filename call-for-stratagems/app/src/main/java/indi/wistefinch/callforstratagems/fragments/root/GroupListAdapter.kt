package indi.wistefinch.callforstratagems.fragments.root

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
import indi.wistefinch.callforstratagems.R
import indi.wistefinch.callforstratagems.data.models.GroupData
import indi.wistefinch.callforstratagems.data.models.StratagemData
import indi.wistefinch.callforstratagems.data.viewmodel.StratagemViewModel
import java.util.Vector

class GroupListAdapter: RecyclerView.Adapter<GroupListAdapter.ListViewHolder>() {

    private var groupList = emptyList<GroupData>()
    private lateinit var stratagemViewModel: StratagemViewModel
    private lateinit var context: Context

    class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {}

    // Init the stratagem thumbnail recycler view adapter
    private val adapter: StratagemThumbnailAdapter by lazy { StratagemThumbnailAdapter() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context
        return ListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_group, parent, false))
    }

    override fun onBindViewHolder(holder: ListViewHolder, pos: Int) {
        holder.itemView.findViewById<TextView>(R.id.group_title).text = groupList[pos].title

        val bundle = bundleOf(Pair("currentItem", groupList[pos]))
        holder.itemView.findViewById<CardView>(R.id.group_cardView).setOnClickListener {
            holder.itemView.findNavController().navigate(R.id.action_rootFragment_to_viewGroupFragment, bundle)
        }

        // Setup stratagem thumbnail recycler view
        val recyclerView = holder.itemView.findViewById<RecyclerView>(R.id.group_recyclerView)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val list: Vector<StratagemData> = Vector()
        for (i in groupList[pos].list) {
            if (stratagemViewModel.isIdValid(i)) {
                list.add(stratagemViewModel.retrieveItem(i))
            }
        }
        adapter.setData(list.toList())
        holder.itemView.findViewById<RecyclerView>(R.id.group_recyclerView).suppressLayout(true)
    }

    override fun getItemCount(): Int {
        return groupList.size
    }

    fun setStratagemViewModel(viewModel: StratagemViewModel) {
        stratagemViewModel = viewModel
    }

    fun setData(list: List<GroupData>) {
        val groupListDiffUtil = GroupListDiffUtil(groupList, list)
        val groupListDiffResult = DiffUtil.calculateDiff(groupListDiffUtil)
        this.groupList = list
        groupListDiffResult.dispatchUpdatesTo(this)
        //notifyDataSetChanged()
    }
}