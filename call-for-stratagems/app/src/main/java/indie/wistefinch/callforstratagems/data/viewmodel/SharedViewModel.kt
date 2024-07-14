package indie.wistefinch.callforstratagems.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

/**
 * A shared view model tool.
 */
class SharedViewModel(application: Application): AndroidViewModel(application) {

    /**
     * Check if the database is empty.
     */
    val emptyDatabase: MutableLiveData<Boolean> = MutableLiveData(true)

    /**
     * Check if the database is empty.
     */
    fun checkIfDbIsEmpty(list: List<Any>) {
        emptyDatabase.value = list.isEmpty()
    }

}
