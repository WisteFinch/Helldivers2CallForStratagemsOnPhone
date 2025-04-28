package indie.wistefinch.callforstratagems.data

import androidx.annotation.Keep
import indie.wistefinch.callforstratagems.Constants
import indie.wistefinch.callforstratagems.data.models.GroupData
import indie.wistefinch.callforstratagems.socket.ServerConfigData

@Keep
data class BackupFileData(
    var ver: Int = Constants.API_VERSION,
    var sync: ServerConfigData,
    var settings: AppSettingsData,
    var groups: List<GroupData>
)

@Keep
data class AppSettingsData(
    var conn: AppSettingsConnData,
    var ctrl: AppSettingsCtrlData,
    var db: AppSettingsDBData
)

@Keep
data class AppSettingsConnData(
    var addr: String,
    var port: Int,
    var retry: Int,
)

data class AppSettingsCtrlData(
    var simplified: Boolean,
    var fastboot: Boolean,
    var sfx: Boolean,
    var vibrator: Boolean,
    var sdt: Float,
    var svt: Float,
    var lang: String,
)

data class AppSettingsDBData(
    var channel: Int,
    var custom: String
)