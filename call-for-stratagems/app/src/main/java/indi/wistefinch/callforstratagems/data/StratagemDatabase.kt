package indi.wistefinch.callforstratagems.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import indi.wistefinch.callforstratagems.data.dao.StratagemDao
import indi.wistefinch.callforstratagems.data.models.StratagemData

@Database(entities = [StratagemData::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class StratagemDatabase: RoomDatabase() {

    abstract fun stratagemDao(): StratagemDao

    companion object {
        @Volatile
        private var INSTANCE: StratagemDatabase? = null

        fun getDatabase(context: Context): StratagemDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StratagemDatabase::class.java,
                    "stratagem_database"
                )
                    .allowMainThreadQueries()
                    .createFromAsset("database/stratagem_initial_db.db")
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}