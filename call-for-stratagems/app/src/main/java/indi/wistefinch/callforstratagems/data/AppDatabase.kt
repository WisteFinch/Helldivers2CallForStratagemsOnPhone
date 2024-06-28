package indi.wistefinch.callforstratagems.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import indi.wistefinch.callforstratagems.data.models.GroupData

@Database(entities = [GroupData::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {

    abstract fun groupDao(): GroupDao

    companion object {
        @Volatile
        private  var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val instance = INSTANCE
            if (instance != null) {
                return instance
            }
            synchronized(this) {
                val newInstacne = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "group_table"
                ).build()
                INSTANCE = newInstacne
                return newInstacne
            }
        }
    }

}