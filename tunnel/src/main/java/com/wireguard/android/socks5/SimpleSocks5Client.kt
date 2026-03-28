package com.wireguard.android.socks5

import android.util.Log
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket

/**
 * 简化的 SOCKS5 客户端/服务器
 * 
 * 工作原理:
 * 1. 在本地 (127.0.0.1:11080) 启动 SOCKS5 服务器
 * 2. 接收应用的 SOCKS5 请求
 * 3. 通过 WireGuard 隧道转发到 10.0.0.11:1080
 * 4. 返回响应
 * 
 * 流量路径:
 * App → 本地 127.0.0.1:11080 → WireGuard 隧道 → 10.0.0.11:1080 → 互联网
 */
class SimpleSocks5Client(
    private val remoteServer: String,
    private val remotePort: Int,
    private val localPort: Int = 11080
) {
    
    companion object {
        private const val TAG = "SimpleSocks5Client"
        
        // SOCKS5 常量
        private const val SOCKS_VERSION = 5
        private const val CMD_CONNECT = 1
        private const val ATYP_IPV4 = 1
        private const val ATYP_DOMAIN = 3
        private const val ATYP_IPV6 = 4
        private const val REP_SUCCESS = 0
    }
    
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var acceptThread: Thread? = null
    
    /**
     * 启动 SOCKS5 服务器
     */
    fun start() {
        if (isRunning) {
            Log.w(TAG, "Already running")
            return
        }
        
        try {
            serverSocket = ServerSocket(localPort)
            isRunning = true
            
            Log.i(TAG, "=========================================")
            Log.i(TAG, "✓ SIMPLE SOCKS5 SERVER STARTED")
            Log.i(TAG, "✓ Listen: 127.0.0.1:$localPort")
            Log.i(TAG, "✓ Remote: $remoteServer:$remotePort")
            Log.i(TAG, "✓ Traffic: App → Local:$localPort → WireGuard → Remote:$remotePort → Internet")
            Log.i(TAG, "=========================================")
            
            acceptThread = Thread {
                while (isRunning && !Thread.currentThread().isInterrupted) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        if (clientSocket != null) {
                            Log.d(TAG, "Client connected")
                            Thread {
                                handleClient(clientSocket)
                            }.start()
                        }
                    } catch (e: IOException) {
                        if (isRunning) {
                            Log.e(TAG, "Accept error", e)
                        }
                    }
                }
            }
            acceptThread?.start()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SOCKS5 server", e)
            isRunning = false
        }
    }
    
    /**
     * 处理客户端连接
     */
    private fun handleClient(clientSocket: Socket) {
        var remoteSocket: Socket? = null
        try {
            val clientIn = clientSocket.getInputStream()
            val clientOut = clientSocket.getOutputStream()
            
            // 1. 读取客户端的 SOCKS5 握手
            val version = clientIn.read()
            val nmethods = clientIn.read()
            val methods = ByteArray(nmethods)
            clientIn.read(methods)
            
            Log.d(TAG, "SOCKS5 handshake: version=$version, methods=$nmethods")
            
            // 2. 回复握手 (选择无认证)
            clientOut.write(byteArrayOf(SOCKS_VERSION.toByte(), 0.toByte()))
            clientOut.flush()
            
            // 3. 读取连接请求
            val cmd = clientIn.read()
            clientIn.read() // rsv
            val atyp = clientIn.read()
            
            // 4. 读取目标地址
            val destAddress: String
            val destPort: Int
            when (atyp) {
                ATYP_IPV4 -> {
                    val addr = ByteArray(4)
                    clientIn.read(addr)
                    destAddress = addr.joinToString(".") { it.toUByte().toString() }
                    destPort = ((clientIn.read() shl 8) or clientIn.read()) and 0xFFFF
                }
                ATYP_DOMAIN -> {
                    val len = clientIn.read()
                    val domain = ByteArray(len)
                    clientIn.read(domain)
                    destAddress = String(domain, Charsets.UTF_8)
                    destPort = ((clientIn.read() shl 8) or clientIn.read()) and 0xFFFF
                }
                ATYP_IPV6 -> {
                    val addr = ByteArray(16)
                    clientIn.read(addr)
                    destAddress = addr.joinToString(":") { it.toUByte().toString(16) }
                    destPort = ((clientIn.read() shl 8) or clientIn.read()) and 0xFFFF
                }
                else -> {
                    Log.e(TAG, "Unsupported address type: $atyp")
                    return
                }
            }
            
            Log.i(TAG, "✓ Client wants to connect: $destAddress:$destPort")
            
            // 5. 连接远程 SOCKS5 服务器 (通过 WireGuard 隧道)
            Log.d(TAG, "Connecting to remote SOCKS5: $remoteServer:$remotePort")
            remoteSocket = Socket(remoteServer, remotePort)
            val remoteIn = remoteSocket.getInputStream()
            val remoteOut = remoteSocket.getOutputStream()
            
            // 6. 发送连接请求到远程 SOCKS5
            remoteOut.write(byteArrayOf(
                SOCKS_VERSION.toByte(),
                cmd.toByte(),
                0.toByte(), // rsv
                atyp.toByte()
            ))
            
            // 写入目标地址
            when (atyp) {
                ATYP_IPV4 -> {
                    val addr = destAddress.split(".").map { it.toInt().toByte() }.toByteArray()
                    remoteOut.write(addr)
                }
                ATYP_DOMAIN -> {
                    val domainBytes = destAddress.toByteArray(Charsets.UTF_8)
                    remoteOut.write(domainBytes.size)
                    remoteOut.write(domainBytes)
                }
                ATYP_IPV6 -> {
                    val addr = destAddress.split(":").flatMap { 
                        it.toIntOrNull(16)?.toByte()?.let { b -> listOf(0.toByte(), b) } ?: emptyList() 
                    }.toByteArray()
                    remoteOut.write(addr)
                }
            }
            
            // 写入端口
            remoteOut.write((destPort shr 8) and 0xFF)
            remoteOut.write(destPort and 0xFF)
            remoteOut.flush()
            
            // 7. 读取远程响应
            val response = ByteArray(10)
            val responseLen = remoteIn.read(response)
            
            if (responseLen >= 10 && response[1] == REP_SUCCESS.toByte()) {
                // 8. 转发响应给客户端
                clientOut.write(response, 0, responseLen)
                clientOut.flush()
                
                Log.i(TAG, "✓ Connection established: $destAddress:$destPort")
                
                // 9. 双向转发数据
                val clientToRemote = Thread {
                    try {
                        val buffer = ByteArray(4096)
                        while (isRunning) {
                            val len = clientIn.read(buffer)
                            if (len <= 0) break
                            remoteOut.write(buffer, 0, len)
                            remoteOut.flush()
                        }
                    } catch (e: IOException) {
                        Log.d(TAG, "Client to remote ended")
                    }
                }
                
                val remoteToClient = Thread {
                    try {
                        val buffer = ByteArray(4096)
                        while (isRunning) {
                            val len = remoteIn.read(buffer)
                            if (len <= 0) break
                            clientOut.write(buffer, 0, len)
                            clientOut.flush()
                        }
                    } catch (e: IOException) {
                        Log.d(TAG, "Remote to client ended")
                    }
                }
                
                clientToRemote.start()
                remoteToClient.start()
                clientToRemote.join()
                remoteToClient.join()
                
            } else {
                Log.e(TAG, "Remote SOCKS5 connection failed: responseLen=$responseLen, rep=${response.getOrNull(1)}")
                
                // 发送失败响应给客户端
                clientOut.write(byteArrayOf(
                    SOCKS_VERSION.toByte(),
                    1.toByte(), // 失败
                    0.toByte()
                ))
                clientOut.flush()
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Connection error", e)
        } finally {
            try { clientSocket.close() } catch (e: IOException) {}
            try { remoteSocket?.close() } catch (e: IOException) {}
        }
    }
    
    /**
     * 停止 SOCKS5 服务器
     */
    fun stop() {
        Log.i(TAG, "Stopping SOCKS5 server...")
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: IOException) {}
        serverSocket = null
        acceptThread?.interrupt()
        acceptThread = null
        Log.i(TAG, "SOCKS5 server stopped")
    }
    
    fun isRunning(): Boolean = isRunning
}
