package indie.wistefinch.callforstratagems.fragments.root

import androidx.recyclerview.widget.DiffUtil
import indie.wistefinch.callforstratagems.data.models.GroupData

/**
 * A utility to tell the difference between two group list data.
 */
class GroupListDiffUtil(
    private val oldList: List<GroupData>,
    private val newList: List<GroupData>
): DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return newList[newItemPosition] === oldList[oldItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return newList[newItemPosition].id == oldList[oldItemPosition].id
    }
}