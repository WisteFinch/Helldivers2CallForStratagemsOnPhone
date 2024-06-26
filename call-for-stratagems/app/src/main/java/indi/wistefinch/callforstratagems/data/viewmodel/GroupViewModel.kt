package indi.wistefinch.callforstratagems.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import indi.wistefinch.callforstratagems.data.dao.GroupDao
import indi.wistefinch.callforstratagems.data.models.GroupData
import kotlinx.coroutines.launch

/**
 * View Model to keep a reference to the Inventory repository and an up-to-date list of all items.
 *
 */
class GroupViewModel(private val groupDao: GroupDao) : ViewModel() {

    // Cache all items form the database using LiveData.
    val allItems: LiveData<List<GroupData>> = groupDao.getItems().asLiveData()

    fun updateItem(
        id: Int,
        title: String,
        list: List<Int>
    ) {
        val updatedItem = getUpdatedItemEntry(id, title, list)
        updateItem(updatedItem)
    }

    private fun updateItem(item: GroupData) {
        viewModelScope.launch {
            groupDao.update(item)
        }
    }

    fun addItem(
        id: Int,
        title: String,
        list: List<Int>
    ) {
        val newItem = getNewItemEntry(title, list)
        insertItem(newItem)
    }

    private fun insertItem(item: GroupData) {
        viewModelScope.launch {
            groupDao.insert(item)
        }
    }

    fun deleteItem(item: GroupData) {
        viewModelScope.launch {
            groupDao.delete(item)
        }
    }

    fun retrieveItem(id: Int): LiveData<GroupData> {
        return groupDao.getItem(id).asLiveData()
    }

    fun isEntryValid(title: String): Boolean {
        return title.isNotBlank()
    }

    private fun getNewItemEntry(
        title: String,
        list: List<Int>
    ): GroupData {
        return GroupData(
            title = title,
            list = list
        )
    }

    private fun getUpdatedItemEntry(
        id: Int,
        title: String,
        list: List<Int>
    ): GroupData {
        return GroupData(
            id = id,
            title = title,
            list = list
        )
    }
}

class GroupViewModelFactory(private val groupDao: GroupDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupViewModel(groupDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

