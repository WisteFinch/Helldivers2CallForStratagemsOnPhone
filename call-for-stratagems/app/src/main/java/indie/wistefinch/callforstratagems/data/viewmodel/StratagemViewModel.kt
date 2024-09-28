package indie.wistefinch.callforstratagems.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import indie.wistefinch.callforstratagems.data.dao.StratagemDao
import indie.wistefinch.callforstratagems.data.models.GroupData
import indie.wistefinch.callforstratagems.data.models.StratagemData
import kotlinx.coroutines.launch

/**
 * View Model to keep a reference to the database.
 */
class StratagemViewModel(private val stratagemDao: StratagemDao) : ViewModel() {

    /**
     * Get all [StratagemData] form the database.
     */
    fun getAllItems(): List<StratagemData> {
        return stratagemDao.getItems()
    }

    fun retrieveItem(id: Int): StratagemData {
        return stratagemDao.getItem(id)
    }

    fun isIdValid(id: Int): Boolean {
        return stratagemDao.valid(id)
    }

    fun deleteAll() {
        viewModelScope.launch {
            stratagemDao.deleteAll()
        }
    }

    fun insertItem(item: StratagemData) {
        viewModelScope.launch {
            stratagemDao.insert(item)
        }
    }

}

/**
 * Factory to instantiate the [StratagemViewModel].
 */
class StratagemViewModelFactory(private val stratagemDao: StratagemDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StratagemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StratagemViewModel(stratagemDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

