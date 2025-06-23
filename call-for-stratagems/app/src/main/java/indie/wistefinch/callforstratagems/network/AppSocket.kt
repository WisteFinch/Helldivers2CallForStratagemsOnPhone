package indie.wistefinch.callforstratagems.network

import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Scanner

/**
 * App socket client.
 */
class AppSocket {

    /**
     * Main socket.
     */
    private lateinit var socket: Socket

    /**
     * Reader, receive data from server.
     */
    private lateinit var reader: Scanner

    /**
     * Print writer, send data to server.
     */
    private lateinit var writer: PrintWriter

    /**
     * Whether the socket is connected to the server.
     */
    private var connected: Boolean = false

    /**
     * Socket operation timeout.
     */
    private var soTimeout: Int = 5000

    /**
     * Whether the socket is connected to the server.
     */
    var isConnected: () -> Boolean = { connected }

    /**
     * Connect to the server.
     */
    fun connect(address: String, port: Int, timeout: Int = 5000): Boolean {
        soTimeout = timeout
        if (connected) {
            disconnect()
        }
        socket = Socket()
        try {
            socket.connect(InetSocketAddress(address, port), soTimeout)
            reader = Scanner(InputStreamReader(socket.getInputStream()))
            writer = PrintWriter(socket.getOutputStream(), true)
        } catch (_: IOException) {
            return false
        }
        toggleTimeout()
        connected = true
        return true
    }

    /**
     * Disconnect the current connection.
     */
    fun disconnect() {
        if (connected) {
            reader.close()
            writer.close()
            connected = false
            socket.close()
        }
    }

    /**
     * Force close the current connection.
     */
    fun forceClose() {
        try {
            socket.close()
            reader.close()
            writer.close()
        } catch (_: Exception) {
        }
    }

    /**
     * Send data to the server.
     */
    fun send(msg: String) {
        writer.println(msg)
    }

    /**
     * Receive data to the server.
     */
    fun receive(): String {
        return reader.nextLine()
    }

    /**
     * Set whether to enable timeout.
     */
    fun toggleTimeout(flag: Boolean = true) {
        if (flag) {
            socket.soTimeout = soTimeout
        } else {
            socket.soTimeout = 0
        }
    }
}