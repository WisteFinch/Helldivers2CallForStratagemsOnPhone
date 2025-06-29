package indie.wistefinch.callforstratagems.data

import androidx.annotation.Keep
import com.google.gson.Gson
import indie.wistefinch.callforstratagems.Constants
import indie.wistefinch.callforstratagems.data.models.GroupData
import indie.wistefinch.callforstratagems.network.SyncConfigData

@Keep
data class BackupFileData(
    var ver: Int = 1,
    var sync: SyncConfigData = SyncConfigData(),
    var settings: AppSettingsData = AppSettingsData(),
    var groups: List<GroupData> = listOf(),
)

@Keep
data class AppSettingsData(
    var conn: AppSettingsConnData = AppSettingsConnData(),
    var ctrl: AppSettingsCtrlData = AppSettingsCtrlData(),
    var db: AppSettingsDBData = AppSettingsDBData(),
)

@Keep
data class AppSettingsConnData(
    var addr: String = "127.0.0.1",
    var port: Int = 23333,
    var retry: Int = 5,
)

data class AppSettingsCtrlData(
    var simplified: Boolean = false,
    var stratagemSize: Int = 100,
    var fastboot: Boolean = false,
    var sfx: Boolean = false,
    var vibrator: Boolean = false,
    var sdt: Int = 100,
    var svt: Int = 50,
    var lang: String = "en",
)

data class AppSettingsDBData(
    var channel: Int = 0,
    var custom: String = ""
)

class BackupFileDataUtils {
    companion object{
        fun fromVer1(data: String): BackupFileData {
            return Gson().fromJson(data, BackupFileData::class.java)
        }
    }
}