package indi.wistefinch.callforstratagems.fragments.root

import androidx.recyclerview.widget.DiffUtil
import indi.wistefinch.callforstratagems.data.models.GroupData

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
                && newList[newItemPosition].title == oldList[oldItemPosition].title
                && newList[newItemPosition].list == oldList[oldItemPosition].list
    }
}