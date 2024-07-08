package indi.wistefinch.callforstratagems.socket

data class ServerConfig (
    var port: Int,
    var delay: Int,
    var open: String,
    var up: String,
    var down: String,
    var left: String,
    var right: String
)