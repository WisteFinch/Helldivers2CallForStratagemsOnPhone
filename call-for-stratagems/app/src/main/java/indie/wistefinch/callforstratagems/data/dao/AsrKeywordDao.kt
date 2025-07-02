package indie.wistefinch.callforstratagems.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import indie.wistefinch.callforstratagems.data.models.AsrKeywordData

/**
 * Provide data access for [AsrKeywordData].
 */
@Dao
interface AsrKeywordDao {
    /**
     * Get all the entries in the database.
     */
    @Query("SELECT * FROM asr_keyword_table ORDER BY id ASC")
    fun getItems(): List<AsrKeywordData>

    /**
     * Get all the entries in the database by dbName.
     */
    @Query("SELECT * FROM asr_keyword_table WHERE dbName = :dbName ORDER BY id ASC")
    fun getItems(dbName: String): List<AsrKeywordData>

    /**
     * Get the specified entry from the database by stratagem & dbName.
     */
    @Query("SELECT * from asr_keyword_table WHERE stratagem = :stratagem and dbName = :dbName")
    fun getItem(stratagem: Int, dbName: String): AsrKeywordData

    /**
     * Check whether the specified entry exists in the database by stratagem & dbName.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM asr_keyword_table WHERE stratagem = :stratagem and dbName = :dbName)")
    fun valid(stratagem: Int, dbName: String): Boolean

    /**
     * Insert the entry into the database, ignoring the conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: AsrKeywordData)

    /**
     * Update the entry in the database.
     */
    @Update
    suspend fun update(item: AsrKeywordData)

    /**
     * Delete the entry in the database.
     */
    @Delete
    suspend fun delete(item: AsrKeywordData)

    /**
     * Delete all the entry in the database.
     */
    @Query("DELETE FROM asr_keyword_table")
    suspend fun deleteAll()

    /**
     * Delete all the entry in the database by dbName.
     */
    @Query("DELETE FROM asr_keyword_table WHERE dbName = :dbName")
    suspend fun deleteAll(dbName: String)
}