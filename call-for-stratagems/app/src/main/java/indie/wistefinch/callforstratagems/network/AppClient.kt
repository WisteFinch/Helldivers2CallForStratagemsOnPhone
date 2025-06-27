package indie.wistefinch.callforstratagems.network

import com.google.gson.Gson
import indie.wistefinch.callforstratagems.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class AppClientEvent {
    CONNECTED,
    DISCONNECTED,
    FAILED,
    CONNECTING,
    RETRYING,
    SENT,
    RECEIVED,
    AUTHING,
    AUTH_FAILED,
    API_MISMATCH,
    SERVER_ERR,
}

object AppClient {
    /**
     * The socket.
     */
    private var socket: AppSocket = AppSocket()

    /**
     * Client coroutineScope.
     */
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Client coroutine job.
     */
    private lateinit var clientJob: Job

    /**
     * Connect retry times.
     */
    private var retry: Int = 5

    /**
     * Connect retried times.
     */
    private var retried: Int = 0

    /**
     * Server ip address.
     */
    private var addr: String = "127.0.0.1"

    /**
     * Server port.
     */
    private var port: Int = 23333

    /**
     * Socket timeout.
     */
    private var timeout: Int = 5000

    /**
     * Whether the client is connected.
     */
    private var connected: Boolean = false

    /**
     * Client security id.
     */
    private var sid: String = "NULL"

    /**
     * Client security token.
     */
    private var token: String = "NULL"

    /**
     * Reflect whether network communication is currently in progress.
     *
     * Lock during any network communication.
     */
    private var networkLock = Mutex()

    /**
     * Reflect whether it is currently attempting to connect to the server.
     *
     * Lock during the connect process.
     */
    private var connectingLock = Mutex()

    /**
     * Client event callback.
     */
    private lateinit var eventListener: (ev: AppClientEvent, opt: Int) -> Unit

    /**
     * Whether the client is connected.
     */
    var isConnected: () -> Boolean = { connected }

    /**
     * Connect retried times.
     */
    var retriedTimes: () -> Int = { retried }


    /**
     * Init app client.
     */
    fun initClient(addr: String, port: Int, sid: String, retry: Int = 5, timeout: Int = 5000) {
        this.addr = addr
        this.port = port
        this.sid = sid
        this.retry = retry
        this.timeout = timeout

        socket = AppSocket()
        networkLock = Mutex()
        connectingLock = Mutex()

        if (connected || socket.isConnected()) {
            closeClient()
        }
        clientJob = scope.launch(Dispatchers.IO) {
            setupSocket()
            keepAlive()
        }
    }

    /**
     * Close app client.
     */
    fun closeClient() {
        if (this::clientJob.isInitialized) {
            clientJob.cancel()
        }
        socket.forceClose()
        connected = false
        emitEvent(AppClientEvent.DISCONNECTED)
        eventListener = { _, _ -> }
    }

    /**
     * Setup the tcp client.
     */
    private suspend fun setupSocket() {
        withContext(Dispatchers.IO) {
            connectingLock.withLock {
                retried = 0

                // Connect to server
                emitEvent(AppClientEvent.CONNECTING)
                networkLock.withLock {
                    socket.connect(addr, port, timeout)
                }
                // Check auth and server status
                optCheckStatus()

                // If the connection is not successful, retry.
                while (!connected && retried < retry && isActive) {
                    emitEvent(AppClientEvent.RETRYING)
                    delay(2000)
                    // Connect to server
                    retried++
                    emitEvent(AppClientEvent.CONNECTING)
                    networkLock.withLock {
                        socket.connect(addr, port, timeout)
                    }
                    // Check auth and server status
                    optCheckStatus()
                }
            }
        }
    }

    /**
     * Check if the server is valid.
     */
    private suspend fun optCheckStatus() {
        withContext(Dispatchers.IO) {
            // If socket connect failed, skip server status check.
            if (!socket.isConnected()) {
                connected = false
                emitEvent(AppClientEvent.FAILED)
                return@withContext
            }
            // Check server status
            var flag: Int
            if (networkLock.isLocked) {
                return@withContext
            }
            networkLock.withLock {
                try {
                    // Send status request
                    socket.send(Gson().toJson(RequestStatusPacket(Constants.API_VERSION)).toString())
                    // Receive status
                    val res: ReceiveStatusData =
                        Gson().fromJson(socket.receive(), ReceiveStatusData::class.java)
                    // Check API version
                    if (res.api != Constants.API_VERSION) {
                        flag = 2
                    }
                    // Check the server status
                    else {
                        when (res.status) {
                            0 -> flag = 1
                            1 -> flag = 2
                            2 -> {
                                // Request authentication
                                emitEvent(AppClientEvent.AUTHING)
                                socket.send(Gson().toJson(RequestAuthPacket(sid)).toString())
                                socket.toggleTimeout(false)
                                val auth = Gson().fromJson(socket.receive(), ReceiveAuthData::class.java)
                                socket.toggleTimeout(true)
                                if (auth.auth) {
                                    token = auth.token
                                    flag = 1
                                } else {
                                    flag = 3
                                }
                            }
                            else -> flag = 4
                        }
                    }
                } catch (_: Exception) { // Json convert error & socket timeout
                    flag = 0
                }
            }
            connected = flag == 1
            when (flag) {
                1 -> emitEvent(AppClientEvent.CONNECTED)
                2 -> emitEvent(AppClientEvent.API_MISMATCH)
                3 -> emitEvent(AppClientEvent.AUTH_FAILED)
                4 -> emitEvent(AppClientEvent.SERVER_ERR)
                else -> emitEvent(AppClientEvent.FAILED)
            }
        }
    }

    /**
     * Check if the server is valid every 10s, if not, reconnect.
     */
    private suspend fun keepAlive() {
        withContext(Dispatchers.IO) {
            while (true) {
                ensureActive()
                delay(10000)
                if (connected && !connectingLock.isLocked) {
                    optCheckStatus()
                    if (!connected && !connectingLock.isLocked) {
                        setupSocket()
                    }
                }
            }
        }
    }

    /**
     * Activate stratagem, send stratagem macro data to the server.
     */
    suspend fun optMacro(data: StratagemMacroData) {
        withContext(Dispatchers.IO) {
            // Check the socket status
            if (!connected) {
                if (connectingLock.isLocked) {
                    return@withContext
                }
                setupSocket()
                if (!connected) {
                    return@withContext
                }
            }
            // Send data
            val packet = RequestMacroPacket(data, token)
            if (networkLock.isLocked) {
                return@withContext
            }
            networkLock.withLock {
                try {
                    socket.send(Gson().toJson(packet).toString())
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Activate step, send input data to the server.
     */
    suspend fun optInput(data: StratagemInputData) {
        withContext(Dispatchers.IO) {
            // Check the socket status
            if (!connected) {
                if (connectingLock.isLocked) {
                    return@withContext
                }
                setupSocket()
                if (!connected) {
                    return@withContext
                }
            }
            // Send data
            val packet = RequestInputPacket(data, token)
            if (networkLock.isLocked) {
                return@withContext
            }
            networkLock.withLock {
                try {
                    socket.send(Gson().toJson(packet).toString())
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Activate step, send input data to the server.
     */
    suspend fun optSync(data: SyncConfigData) {
        withContext(Dispatchers.IO) {
            // Check the socket status
            if (!connected) {
                if (connectingLock.isLocked) {
                    return@withContext
                }
                setupSocket()
                if (!connected) {
                    return@withContext
                }
            }
            // Send data
            val packet = RequestSyncPacket(data, token)
            if (networkLock.isLocked) {
                return@withContext
            }
            networkLock.withLock {
                try {
                    socket.send(Gson().toJson(packet).toString())
                    emitEvent(AppClientEvent.SENT, 4)
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Change client status.
     */
    private fun emitEvent(ev: AppClientEvent, opt: Int = -1) {
        if (this::eventListener.isInitialized) {
            eventListener(ev, opt)
        }
    }

    /**
     * Set client status change callback.
     */
    fun setEventListener(listener: (ev: AppClientEvent, opt: Int) -> Unit) {
        eventListener = listener
    }
}