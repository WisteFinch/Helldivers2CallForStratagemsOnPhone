package indi.wistefinch.callforstratagems.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import indi.wistefinch.callforstratagems.data.models.GroupData
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT * FROM group_table ORDER BY id ASC")
    fun getItems(): Flow<List<GroupData>>

    @Query("SELECT * from group_table WHERE id = :id")
    fun getItem(id: Int): Flow<GroupData>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: GroupData)

    @Update
    suspend fun update(item: GroupData)

    @Delete
    suspend fun delete(item: GroupData)
}