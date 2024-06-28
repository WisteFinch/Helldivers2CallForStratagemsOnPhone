package indi.wistefinch.callforstratagems.data.models

import android.graphics.drawable.Drawable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_table")
data class GroupData(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    var title: String,
    var icon: Drawable,
    var list: List<String>
)
