package indi.wistefinch.callforstratagems.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import indi.wistefinch.callforstratagems.data.dao.StratagemDao
import indi.wistefinch.callforstratagems.data.models.StratagemData
import kotlinx.coroutines.launch

/**
 * View Model to keep a reference to the Inventory repository and an up-to-date list of all items.
 *
 */
class StratagemViewModel(private val stratagemDao: StratagemDao) : ViewModel() {

    // Cache all items form the database using LiveData.
    val allItems: LiveData<List<StratagemData>> = stratagemDao.getItems().asLiveData()

    fun updateItem(
        id: Int,
        title: String,
        list: List<Int>
    ) {
        val updatedItem = getUpdatedItemEntry(id, title, list)
        updateItem(updatedItem)
    }

    private fun updateItem(item: StratagemData) {
        viewModelScope.launch {
            stratagemDao.update(item)
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

    private fun insertItem(item: StratagemData) {
        viewModelScope.launch {
            stratagemDao.insert(item)
        }
    }

    fun deleteItem(item: StratagemData) {
        viewModelScope.launch {
            stratagemDao.delete(item)
        }
    }

    fun retrieveItem(id: Int): LiveData<StratagemData> {
        return stratagemDao.getItem(id).asLiveData()
    }

    fun isEntryValid(title: String): Boolean {
        return title.isNotBlank()
    }

    private fun getNewItemEntry(
        name: String,
        list: List<Int>
    ): StratagemData {
        return StratagemData(
            0,
            name = name,
            steps = list
        )
    }

    private fun getUpdatedItemEntry(
        id: Int,
        name: String,
        list: List<Int>
    ): StratagemData {
        return StratagemData(
            id = id,
            name = name,
            steps = list
        )
    }
}

class StratagemViewModelFactory(private val stratagemDao: StratagemDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StratagemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StratagemViewModel(stratagemDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

