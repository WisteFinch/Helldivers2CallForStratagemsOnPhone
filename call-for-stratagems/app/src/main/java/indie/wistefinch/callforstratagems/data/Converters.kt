package indie.wistefinch.callforstratagems.data

import androidx.room.TypeConverter
import com.google.gson.Gson

/**
 * Provide data converters for room database.
 */
class Converters {

    /**
     * Serialize the integer list to json string.
     */
    @TypeConverter
    fun fromList(list: List<Int>): String {
        return Gson().toJson(list).toString()
    }

    /**
     * Deserialize the json string to integer list.
     */
    @TypeConverter
    fun toList(json: String): List<Int> {
        return Gson().fromJson(json, Array<Int>::class.java).asList()
    }
}