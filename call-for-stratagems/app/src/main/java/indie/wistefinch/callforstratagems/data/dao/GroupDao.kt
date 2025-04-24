package indie.wistefinch.callforstratagems.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import indie.wistefinch.callforstratagems.data.models.GroupData
import kotlinx.coroutines.flow.Flow

/**
 * Provide data access for [GroupData].
 */
@Dao
interface GroupDao {

    /**
     * Get all the entries in the database.
     */
    @Query("SELECT * FROM group_table ORDER BY idx ASC, id ASC")
    fun getItems(): Flow<List<GroupData>>

    /**
     * Get all the entries in the database.
     */
    @Query("SELECT * FROM group_table ORDER BY idx ASC, id ASC")
    fun getSyncItems(): List<GroupData>

    /**
     * Get the specified entry from the database by id.
     */
    @Query("SELECT * from group_table WHERE id = :id")
    fun getItem(id: Int): Flow<GroupData>

    /**
     * Insert the entry into the database, ignoring the conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: GroupData)

    /**
     * Update the entry in the database.
     */
    @Update
    suspend fun update(item: GroupData)

    /**
     * Delete the entry in the database.
     */
    @Delete
    suspend fun delete(item: GroupData)
}