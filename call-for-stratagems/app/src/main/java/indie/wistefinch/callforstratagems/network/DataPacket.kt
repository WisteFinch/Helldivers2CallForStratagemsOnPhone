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
    var server: SyncConfigServerData,
    var input: SyncConfigInputData,
    var auth: SyncConfigAuthData,
    var debug: Boolean,
    var records: List<Int> = listOf(),
)

@Keep
data class SyncConfigServerData (
    var port: Int,
    var ip: String,
)

@Keep
data class SyncConfigAuthData (
    var enabled: Boolean,
    var timeout: Int,
)

@Keep
data class SyncConfigInputData (
    var delay: Int,
    var open: String,
    var keytype: String,
    var up: String,
    var down: String,
    var left: String,
    var right: String,
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