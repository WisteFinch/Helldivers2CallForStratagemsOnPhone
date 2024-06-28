package indi.wistefinch.callforstratagems.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import indi.wistefinch.callforstratagems.data.models.GroupData

@Dao
interface GroupDao {

    @Query("SELECT * FROM group_table ORDER BY id ASC")
    fun getAllData(): LiveData<List<GroupData>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertData(groupData: GroupData)
}