package indi.wistefinch.callforstratagems.data.models

import android.graphics.drawable.Drawable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stratagem_table")
data class StratagemData(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var name: String,
    var icon: String,
    var steps: List<Int>
)