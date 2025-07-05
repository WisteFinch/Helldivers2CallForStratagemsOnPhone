package indie.wistefinch.callforstratagems.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import indie.wistefinch.callforstratagems.data.models.StratagemData

/**
 * Provide data access for [StratagemData].
 */
@Dao
interface StratagemDao {

    /**
     * Get all the entries in the database.
     */
    @Query("SELECT * FROM stratagem_table ORDER BY idx ASC, id ASC")
    fun getItems(): List<StratagemData>

    /**
     * Get the specified entry from the database by id.
     */
    @Query("SELECT * from stratagem_table WHERE id = :id")
    fun getItem(id: Int): StratagemData

    /**
     * Check whether the specified entry exists in the database by id.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM stratagem_table WHERE id = :id)")
    fun valid(id: Int): Boolean

    /**
     * Insert the entry into the database, ignoring the conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: StratagemData)

    /**
     * Update the entry in the database.
     */
    @Update
    suspend fun update(item: StratagemData)

    /**
     * Delete the entry in the database.
     */
    @Delete
    suspend fun delete(item: StratagemData)

    /**
     * Delete all the entry in the database.
     */
    @Query("DELETE FROM stratagem_table")
    suspend fun deleteAll()
}