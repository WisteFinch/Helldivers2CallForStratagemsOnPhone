package indi.wistefinch.callforstratagems.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_table")
data class GroupData(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var title: String,
    var list: List<Int>
)