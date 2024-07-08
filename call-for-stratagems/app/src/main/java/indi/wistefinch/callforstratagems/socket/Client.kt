package indi.wistefinch.callforstratagems.socket

import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Scanner

class Client {

    private lateinit var socket: Socket

    private lateinit var reader: Scanner
    private lateinit var writer: PrintWriter

    private var isConnected: Boolean = false

    fun connect(address: String, port: Int):Boolean {
        if(isConnected) {
          disconnect()
        }
        socket = Socket()
        try {
            socket.connect(InetSocketAddress(address, port))
            reader = Scanner(InputStreamReader(socket.getInputStream()))
            writer = PrintWriter(socket.getOutputStream(), true)
        } catch (_: IOException) {
            return false
        }
        isConnected = true
        return true
    }

    fun disconnect() {
        if(isConnected) {
            reader.close()
            writer.close()
            socket.close()
            isConnected = false
        }
    }

    fun send(msg: String) {
        writer.println(msg)
    }

    fun receive(): String {
        return reader.nextLine()
    }

}