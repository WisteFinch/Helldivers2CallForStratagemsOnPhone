package indi.wistefinch.callforstratagems.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import indi.wistefinch.callforstratagems.data.models.GroupData

class SharedViewModel(application: Application): AndroidViewModel(application) {

    // Check if the database is empty
    val emptyDatabase: MutableLiveData<Boolean> = MutableLiveData(true)

    fun checkIfDbIsEmpty(list: List<GroupData>) {
        emptyDatabase.value = list.isEmpty()
    }

}
