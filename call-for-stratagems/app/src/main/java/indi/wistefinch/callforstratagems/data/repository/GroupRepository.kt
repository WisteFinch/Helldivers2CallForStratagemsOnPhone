package indi.wistefinch.callforstratagems.data.repository

import androidx.lifecycle.LiveData
import indi.wistefinch.callforstratagems.data.GroupDao
import indi.wistefinch.callforstratagems.data.models.GroupData

class GroupRepository(private val groupDao: GroupDao) {

    val getAllData: LiveData<List<GroupData>> = groupDao.getAllData()

    suspend fun insertData(groupData: GroupData) {
        groupDao.insertData(groupData)
    }
}