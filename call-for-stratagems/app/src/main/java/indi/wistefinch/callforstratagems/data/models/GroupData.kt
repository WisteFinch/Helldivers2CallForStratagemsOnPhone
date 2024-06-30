package indi.wistefinch.callforstratagems.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "group_table")
@Parcelize
data class GroupData(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var title: String,
    var list: List<Int>
): Parcelable
