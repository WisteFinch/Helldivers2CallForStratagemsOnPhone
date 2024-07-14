package indie.wistefinch.callforstratagems.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A structure that stores stratagem information, including id, name, icon and stratagem steps.
 */
@Entity(tableName = "stratagem_table")
data class StratagemData(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var name: String,
    var icon: String,
    var steps: List<Int>
)