package indie.wistefinch.callforstratagems.socket

/**
 * Server configuration data.
 */
data class ServerConfig (
    var port: Int,
    var delay: Int,
    var open: String,
    var up: String,
    var down: String,
    var left: String,
    var right: String
)