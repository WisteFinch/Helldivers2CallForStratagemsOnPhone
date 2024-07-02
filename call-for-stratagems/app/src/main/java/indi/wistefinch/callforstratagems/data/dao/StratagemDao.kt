package indi.wistefinch.callforstratagems.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import indi.wistefinch.callforstratagems.data.models.StratagemData
import kotlinx.coroutines.flow.Flow

@Dao
interface StratagemDao {

    @Query("SELECT * FROM stratagem_table ORDER BY id ASC")
    fun getItems(): List<StratagemData>

    @Query("SELECT * from stratagem_table WHERE id = :id")
    fun getItem(id: Int): StratagemData

    @Query("SELECT EXISTS(SELECT 1 FROM stratagem_table WHERE id = :id)")
    fun valid(id: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: StratagemData)

    @Update
    suspend fun update(item: StratagemData)

    @Delete
    suspend fun delete(item: StratagemData)
}