package indie.wistefinch.callforstratagems.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import indie.wistefinch.callforstratagems.data.dao.AsrKeywordDao
import indie.wistefinch.callforstratagems.data.models.AsrKeywordData

/**
 * ASR keyword database.
 */
@Database(
    entities = [AsrKeywordData::class],
    version = 1,
    exportSchema = true,
    autoMigrations = [
    ]
)
@TypeConverters(Converters::class)
abstract class AsrKeywordDatabase: RoomDatabase() {

    abstract fun asrKeywordDao(): AsrKeywordDao

    companion object {
        @Volatile
        private var INSTANCE: AsrKeywordDatabase? = null

        fun getDatabase(context: Context): AsrKeywordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AsrKeywordDatabase::class.java,
                    "asr_keyword_database"
                )
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}