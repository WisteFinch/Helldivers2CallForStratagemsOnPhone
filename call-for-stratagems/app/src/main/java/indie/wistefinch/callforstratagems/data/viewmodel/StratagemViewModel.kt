package indie.wistefinch.callforstratagems.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import indie.wistefinch.callforstratagems.data.dao.StratagemDao
import indie.wistefinch.callforstratagems.data.models.StratagemData

/**
 * View Model to keep a reference to the database.
 */
class StratagemViewModel(private val stratagemDao: StratagemDao) : ViewModel() {

    /**
     * Cache all items form the database.
     */
    val allItems: List<StratagemData> = stratagemDao.getItems()

    fun retrieveItem(id: Int): StratagemData {
        return stratagemDao.getItem(id)
    }

    fun isIdValid(id: Int): Boolean {
        return stratagemDao.valid(id)
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

