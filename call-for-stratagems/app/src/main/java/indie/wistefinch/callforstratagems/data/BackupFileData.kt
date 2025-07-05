package indie.wistefinch.callforstratagems.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.google.gson.Gson
import indie.wistefinch.callforstratagems.data.models.AsrKeywordData
import indie.wistefinch.callforstratagems.data.models.GroupData
import indie.wistefinch.callforstratagems.network.SyncConfigData

@Keep
data class BackupFileData(
    var ver: Int = 1,
    var sync: SyncConfigData = SyncConfigData(),
    var settings: BackupSettingsData = BackupSettingsData(),
    var groups: List<GroupData> = listOf(),
    var keywords: List<BackupAsrKeywordData> = listOf(),
)

@Keep
data class BackupAsrKeywordData(
    var dbName: String,
    var stratagem: Int,
    var keywords: List<String>,
)

@Keep
data class BackupSettingsData(
    var conn: BackupSettingsConnData = BackupSettingsConnData(),
    var ctrl: BackupSettingsCtrlData = BackupSettingsCtrlData(),
    var asr: BackupSettingsAsrData = BackupSettingsAsrData(),
    var db: BackupSettingsDBData = BackupSettingsDBData(),
)

@Keep
data class BackupSettingsConnData(
    var addr: String = "127.0.0.1",
    var port: Int = 23333,
    var retry: Int = 5,
)

@Keep
data class BackupSettingsCtrlData(
    var simplified: Boolean = false,
    var stratagemSize: Int = 100,
    var fastboot: Boolean = false,
    var sfx: Boolean = false,
    var vibrator: Boolean = false,
    var sdt: Int = 100,
    var svt: Int = 50,
    var lang: String = "en",
)

@Keep
data class BackupSettingsAsrData(
    var model: Int = -1,
    var custom: String = "",
    var enabled: Boolean = false,
    var similarity: Int = 50,
    var gpu: Boolean = true,
    var activate: List<String> = listOf(),
    var autoKeywords: Boolean = true,
)

@Keep
data class BackupSettingsDBData(
    var channel: Int = 0,
    var custom: String = "",
)

class BackupFileDataUtils {
    companion object{
        fun fromVer1(data: String): BackupFileData {
            return Gson().fromJson(data, BackupFileData::class.java)
        }
    }
}