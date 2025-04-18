package indie.wistefinch.callforstratagems.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A structure that stores stratagem information, including id, name, name translation,
 * icon and stratagem steps.
 */
@Entity(tableName = "stratagem_table")
data class StratagemData(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(name = "name", defaultValue = "")
    var name: String = "",
    @ColumnInfo(name = "nameZh", defaultValue = "")
    var nameZh: String = "",
    @ColumnInfo(name = "icon", defaultValue = "")
    var icon: String = "",
    var steps: List<Int> = listOf(),
    @ColumnInfo(name = "idx", defaultValue = "0")
    var idx: Int = 0
)