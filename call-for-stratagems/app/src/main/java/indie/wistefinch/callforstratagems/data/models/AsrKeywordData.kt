package indie.wistefinch.callforstratagems.data.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * A structure that stores stratagem ASR keyword information.
 */
@Entity(tableName = "asr_keyword_table")
@Parcelize
data class AsrKeywordData(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(name = "dbName", defaultValue = "0")
    var dbName: String,
    var stratagem: Int,
    @ColumnInfo(name = "keywords", defaultValue = "[]")
    var keywords: String
) : Parcelable
