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
    val allItems: List<StratagemData> = stratagemDao.getItems()

    fun updateItem(
        id: Int,
        title: String,
        icon: String,
        list: List<Int>
    ) {
        val updatedItem = getUpdatedItemEntry(id, title, icon, list)
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
        icon: String,
        list: List<Int>
    ) {
        val newItem = getNewItemEntry(title, icon, list)
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

    fun retrieveItem(id: Int): StratagemData {
        return stratagemDao.getItem(id)
    }

    fun isIdValid(id: Int): Boolean {
        return stratagemDao.valid(id)
    }

    fun isEntryValid(title: String): Boolean {
        return title.isNotBlank()
    }

    private fun getNewItemEntry(
        name: String,
        icon: String,
        steps: List<Int>
    ): StratagemData {
        return StratagemData(
            0,
            name = name,
            icon = icon,
            steps = steps
        )
    }

    private fun getUpdatedItemEntry(
        id: Int,
        name: String,
        icon: String,
        steps: List<Int>
    ): StratagemData {
        return StratagemData(
            id = id,
            name = name,
            icon = icon,
            steps = steps
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

