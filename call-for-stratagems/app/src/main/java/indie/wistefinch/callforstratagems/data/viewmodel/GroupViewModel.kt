package indie.wistefinch.callforstratagems.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import indie.wistefinch.callforstratagems.data.dao.GroupDao
import indie.wistefinch.callforstratagems.data.models.GroupData
import kotlinx.coroutines.launch

/**
 * View Model to keep a reference to the database and an up-to-date list of all items.
 */
class GroupViewModel(private val groupDao: GroupDao) : ViewModel() {

    /**
     * Cache all [GroupData] form the database using LiveData.
     */
    val allItems: LiveData<List<GroupData>> = groupDao.getItems().asLiveData()

    val allItemsSync: List<GroupData> = groupDao.getItemsSync()

    fun updateItem(
        id: Int,
        title: String,
        list: List<Int>,
        dbName: String,
        idx: Int
    ) {
        val updatedItem = getUpdatedItemEntry(id, title, list, dbName, idx)
        updateItem(updatedItem)
    }

    private fun updateItem(item: GroupData) {
        viewModelScope.launch {
            groupDao.update(item)
        }
    }

    fun addItem(
        title: String,
        list: List<Int>,
        dbName: String
    ) {
        val newItem = getNewItemEntry(title, list, dbName)
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

    fun initIdx() {
        val list = groupDao.getItemsSync()
        for (i in list.indices) {
            if (i != list[i].idx) {
                list[i].idx = i
                updateItem(list[i])
            }
        }
    }

    fun updateIdx(item: GroupData) {
        updateItem(item)
    }

    private fun getNewItemEntry(
        title: String,
        list: List<Int>,
        dbName: String
    ): GroupData {
        return GroupData(
            title = title,
            list = list,
            dbName = dbName
        )
    }

    private fun getUpdatedItemEntry(
        id: Int,
        title: String,
        list: List<Int>,
        dbName: String,
        idx: Int
    ): GroupData {
        return GroupData(
            id = id,
            title = title,
            list = list,
            dbName = dbName,
            idx = idx
        )
    }
}

/**
 * Factory to instantiate the [GroupViewModel].
 */
class GroupViewModelFactory(private val groupDao: GroupDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupViewModel(groupDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

