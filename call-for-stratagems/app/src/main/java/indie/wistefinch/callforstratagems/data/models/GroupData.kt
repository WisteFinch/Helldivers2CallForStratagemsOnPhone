package indie.wistefinch.callforstratagems.data.models

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * A structure that stores group information, including id, name, and stratagems within the group.
 */
@Entity(tableName = "group_table")
@Parcelize
@Keep
data class GroupData(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var title: String,
    var list: List<Int>,
    @ColumnInfo(name = "dbName", defaultValue = "0")
    var dbName: String,
    @ColumnInfo(name = "idx", defaultValue = Int.MAX_VALUE.toString())
    var idx: Int = Int.MAX_VALUE
): Parcelable
