package indi.wistefinch.callforstratagems.data

import androidx.room.TypeConverter
import com.google.gson.Gson

class Converters {

    @TypeConverter
    fun fromList(list: List<Int>): String {
        return Gson().toJson(list).toString()
    }

    @TypeConverter
    fun toList(json: String): List<Int> {
        return Gson().fromJson(json, Array<Int>::class.java).asList()
    }
}