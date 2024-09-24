package indie.wistefinch.callforstratagems.socket


/**
 * Request server status.
 */
data class RequestStatusPacket (
    var ver: String,
    var opt: Int = 0,
)

/**
 * Receive server status data.
 */
data class ReceiveStatusData (
    var status: Int,
    var ver: String,
)

/**
 * Stratagem macro data.
 */
data class StratagemMacroData (
    var name: String,
    var steps: List<Int>,
)

/**
 * Activate stratagem macro.
 */
data class StratagemMacroPacket (
    var macro: StratagemMacroData,
    var token: String,
    var opt: Int = 1,
)

/**
 * Stratagem input data.
 */
data class StratagemInputData (
    var step: Int,
    var type: Int,
)

/**
 * Activate stratagem input.
 */
data class StratagemInputPacket (
    var input: StratagemInputData,
    var token: String,
    var opt: Int = 2,
)

/**
 * Server configuration data.
 */
data class ServerConfigData (
    var port: Int,
    var delay: Int,
    var open: String,
    var up: String,
    var down: String,
    var left: String,
    var right: String,
)

/**
 * Synchronize server configuration.
 */
data class SyncConfigPacket (
    var config: ServerConfigData,
    var token: String,
    var opt: Int = 4,
)

/**
 * Request authentication.
 */
data class RequestAuthPacket (
    var sid: String,
    var opt: Int = 5,
)

/**
 * Receive authentication token.
 */
data class ReceiveAuthData (
    var auth: Boolean,
    var token: String,
)

/**
 * Address data.
 */
data class AddressData (
    var add: String,
    var port: Int,
)