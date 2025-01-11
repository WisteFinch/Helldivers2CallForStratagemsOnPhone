package indie.wistefinch.callforstratagems.fragments.editgroup

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class ItemTouchHelperCallback(private val adapter: ItemTouchHelperAdapter): ItemTouchHelper.Callback() {

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val flags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        return makeMovementFlags(flags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        source: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        adapter.onItemMove(source, target)
        return true
    }

    override fun onSwiped(p0: RecyclerView.ViewHolder, p1: Int) {
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            adapter.onItemSelect(viewHolder!!)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (!recyclerView.isComputingLayout) {
            adapter.onItemClear(viewHolder)
        }
    }
}