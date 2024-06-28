package indi.wistefinch.callforstratagems.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import indi.wistefinch.callforstratagems.data.AppDatabase
import indi.wistefinch.callforstratagems.data.models.GroupData
import indi.wistefinch.callforstratagems.data.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GroupViewModel(application: Application): AndroidViewModel(application) {

    private val groupDao = AppDatabase.getDatabase(application).groupDao()
    private val repository: GroupRepository

    private val getAllData: LiveData<List<GroupData>>

    init {
        repository = GroupRepository(groupDao)
        getAllData = repository.getAllData
    }

    fun insertData(groupData: GroupData) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertData(groupData)
        }
    }

}