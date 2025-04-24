package indie.wistefinch.callforstratagems.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import indie.wistefinch.callforstratagems.data.models.GroupData
import indie.wistefinch.callforstratagems.data.dao.GroupDao

/**
 * Group database.
 */
@Database(
    entities = [GroupData::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 1, to = 2),
        AutoMigration (from = 2, to = 3)
    ]
)
@TypeConverters(Converters::class)
abstract class GroupDatabase: RoomDatabase() {

    abstract fun groupDao(): GroupDao

    companion object {
        @Volatile
        private var INSTANCE: GroupDatabase? = null

        fun getDatabase(context: Context): GroupDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GroupDatabase::class.java,
                    "group_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}