package indie.wistefinch.callforstratagems.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import indie.wistefinch.callforstratagems.data.dao.AsrKeywordDao
import indie.wistefinch.callforstratagems.data.models.AsrKeywordData
import kotlinx.coroutines.launch

/**
 * View Model to keep a reference to the database.
 */
class AsrKeywordViewModel(private val asrKeywordDao: AsrKeywordDao) : ViewModel() {

    fun getAllItems(): List<AsrKeywordData> {
        return asrKeywordDao.getItems()
    }

    fun getAllItems(dbName: String): List<AsrKeywordData> {
        return asrKeywordDao.getItems(dbName)
    }

    fun retrieveItem(id: Int, dbName: String): AsrKeywordData {
        return asrKeywordDao.getItem(id, dbName)
    }

    fun isIdValid(id: Int, dbName: String): Boolean {
        return asrKeywordDao.valid(id, dbName)
    }

    fun deleteAll() {
        viewModelScope.launch {
            asrKeywordDao.deleteAll()
        }
    }

    fun deleteAll(dbName: String) {
        viewModelScope.launch {
            asrKeywordDao.deleteAll(dbName)
        }
    }

    fun insertItem(item: AsrKeywordData) {
        viewModelScope.launch {
            asrKeywordDao.insert(item)
        }
    }

    fun updateItem(item: AsrKeywordData) {
        viewModelScope.launch {
            asrKeywordDao.update(item)
        }
    }

}

/**
 * Factory to instantiate the [AsrKeywordViewModel].
 */
class AsrKeywordViewModelFactory(private val asrKeywordDao: AsrKeywordDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AsrKeywordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AsrKeywordViewModel(asrKeywordDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

