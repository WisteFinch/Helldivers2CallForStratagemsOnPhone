package indie.wistefinch.callforstratagems.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import indie.wistefinch.callforstratagems.data.dao.StratagemDao
import indie.wistefinch.callforstratagems.data.models.StratagemData

/**
 * Stratagem database.
 *
 * Is a read-only database, the contents are generated from the default file when the program is first run.
 */
@Database(
    entities = [StratagemData::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 1, to = 2)
    ]
)
@TypeConverters(Converters::class)
abstract class StratagemDatabase: RoomDatabase() {

    abstract fun stratagemDao(): StratagemDao

    companion object {
        @Volatile
        private var INSTANCE: StratagemDatabase? = null

        fun getDatabase(context: Context): StratagemDatabase {
            return INSTANCE ?: synchronized(this) {
                var instance: StratagemDatabase ?
                try {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        StratagemDatabase::class.java,
                        "stratagem_database"
                    )
                        .allowMainThreadQueries()
                        .createFromAsset("database/stratagem_db.db")
                        .build()
                } catch (_: Exception) {
                    context.deleteDatabase("stratagem_database")
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        StratagemDatabase::class.java,
                        "stratagem_database"
                    )
                        .allowMainThreadQueries()
                        .createFromAsset("database/stratagem_db.db")
                        .build()
                }
                INSTANCE = instance!!
                instance
            }
        }
    }
}