package indie.wistefinch.callforstratagems.network

import androidx.annotation.Keep

/**
 * Request server status.
 */
@Keep
data class RequestStatusPacket (
    var api: Int,
    var opt: Int = 0,
)

/**
 * Receive server status data.
 */
@Keep
data class ReceiveStatusData (
    var status: Int,
    var api: Int,
)

/**
 * Stratagem macro data.
 */
@Keep
data class StratagemMacroData (
    var name: String,
    var steps: List<Int>,
)

/**
 * Activate stratagem macro.
 */
@Keep
data class RequestMacroPacket (
    var macro: StratagemMacroData,
    var token: String,
    var opt: Int = 1,
)

/**
 * Stratagem input data.
 */
@Keep
data class StratagemInputData (
    var step: Int,
    var type: Int,
)

/**
 * Activate stratagem input.
 */
@Keep
data class RequestInputPacket (
    var input: StratagemInputData,
    var token: String,
    var opt: Int = 2,
)

/**
 * Server configuration data.
 */
@Keep
data class SyncConfigData (
    var server: SyncConfigServerData = SyncConfigServerData(),
    var input: SyncConfigInputData = SyncConfigInputData(),
    var auth: SyncConfigAuthData = SyncConfigAuthData(),
    var debug: Boolean = false,
    var records: List<Int> = listOf(),
)

@Keep
data class SyncConfigServerData (
    var port: Int = 23333,
    var ip: String = "",
)

@Keep
data class SyncConfigAuthData (
    var enabled: Boolean = true,
    var timeout: Int = 3,
)

@Keep
data class SyncConfigInputData (
    var delay: Int = 25,
    var open: String = "ctrl_left",
    var keytype: String = "hold",
    var up: String = "w",
    var down: String = "s",
    var left: String = "a",
    var right: String = "d",
)

/**
 * Synchronize server configuration.
 */
@Keep
data class RequestSyncPacket (
    var config: SyncConfigData,
    var token: String,
    var opt: Int = 4,
)

/**
 * Request authentication.
 */
@Keep
data class RequestAuthPacket (
    var sid: String,
    var opt: Int = 5,
)

/**
 * Receive authentication token.
 */
@Keep
data class ReceiveAuthData (
    var auth: Boolean,
    var token: String,
)

/**
 * Address data.
 */
@Keep
data class AddressData (
    var add: String,
    var port: Int,
)