package com.wireguard.android.socks5

import android.util.Log
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket

/**
 * SOCKS5 代理服务 - 本地转发器
 * 
 * 工作原理:
 * 1. 在本地 (127.0.0.1:11080) 启动一个 SOCKS5 代理服务器
 * 2. 接收应用的 SOCKS5 请求
 * 3. 通过 WireGuard 隧道转发到内网 SOCKS5 服务器 (10.0.0.11:1080)
 * 4. 返回响应给应用
 * 
 * 流量路径:
 * App → 本地 SOCKS5(127.0.0.1:11080) → WireGuard 隧道 → 内网 SOCKS5(10.0.0.11:1080) → 互联网
 */
class Socks5ProxyService {
    
    companion object {
        private const val TAG = "Socks5ProxyService"
        private const val LOCAL_PORT = 11080  // 本地 SOCKS5 端口
    }
    
    private var config: Socks5Config? = null
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var acceptThread: Thread? = null
    
    /**
     * 配置 SOCKS5 转发
     */
    fun configure(newConfig: Socks5Config) {
        Log.d(TAG, "Configuring: remote=${newConfig.getProxyAddress()}, local=127.0.0.1:$LOCAL_PORT")
        this.config = newConfig
    }
    
    /**
     * 启动本地 SOCKS5 代理转发器
     */
    fun start(): Boolean {
        val cfg = config ?: run {
            Log.e(TAG, "No configuration")
            return false
        }
        
        if (!cfg.isValid() || !cfg.enabled) {
            Log.d(TAG, "SOCKS5 is disabled or invalid")
            return false
        }
        
        if (isRunning) {
            Log.w(TAG, "Already running")
            return true
        }
        
        try {
            // 在本地启动 SOCKS5 代理服务器
            serverSocket = ServerSocket(LOCAL_PORT)
            isRunning = true
            
            Log.i(TAG, "=========================================")
            Log.i(TAG, "✓ LOCAL SOCKS5 PROXY STARTED")
            Log.i(TAG, "✓ Listen: 127.0.0.1:$LOCAL_PORT")
            Log.i(TAG, "✓ Remote: ${cfg.getProxyAddress()}")
            Log.i(TAG, "✓ Traffic: App → Local:11080 → WireGuard → Remote:1080 → Internet")
            Log.i(TAG, "=========================================")
            
            // 启动接受连接线程
            acceptThread = Thread {
                while (isRunning && !Thread.currentThread().isInterrupted) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        if (clientSocket != null) {
                            Log.d(TAG, "Client connected: ${clientSocket.remoteAddress}")
                            handleClient(clientSocket, cfg)
                        }
                    } catch (e: IOException) {
                        if (isRunning) {
                            Log.e(TAG, "Accept error", e)
                        }
                    }
                }
            }
            acceptThread?.start()
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SOCKS5 proxy", e)
            isRunning = false
            return false
        }
    }
    
    /**
     * 处理客户端连接
     */
    private fun handleClient(clientSocket: Socket, config: Socks5Config) {
        var remoteSocket: Socket? = null
        try {
            // 连接到远程 SOCKS5 服务器 (通过 WireGuard 隧道)
            remoteSocket = Socket(config.server, config.port)
            Log.d(TAG, "Connected to remote SOCKS5: ${config.getProxyAddress()}")
            
            // 简单的 SOCKS5 握手转发
            val clientIn = clientSocket.getInputStream()
            val clientOut = clientSocket.getOutputStream()
            val remoteIn = remoteSocket.getInputStream()
            val remoteOut = remoteSocket.getOutputStream()
            
            // 转发握手请求
            val version = clientIn.read()
            val nmethods = clientIn.read()
            val methods = ByteArray(nmethods)
            clientIn.read(methods)
            
            // 发送握手响应 (选择无认证)
            clientOut.write(byteArrayOf(version.toByte(), 0.toByte()))
            clientOut.flush()
            
            // 读取连接请求
            val cmd = clientIn.read()
            clientIn.read() // rsv
            clientIn.read() // atyp
            
            // 读取目标地址
            val atyp = clientIn.read()
            val destAddress: String
            when (atyp) {
                1 -> { // IPv4
                    val addr = ByteArray(4)
                    clientIn.read(addr)
                    destAddress = addr.joinToString(".") { it.toUByte().toString() }
                }
                3 -> { // Domain
                    val len = clientIn.read()
                    val domain = ByteArray(len)
                    clientIn.read(domain)
                    destAddress = String(domain, Charsets.UTF_8)
                }
                4 -> { // IPv6
                    val addr = ByteArray(16)
                    clientIn.read(addr)
                    destAddress = addr.joinToString(":") { it.toUByte().toString(16) }
                }
                else -> {
                    Log.e(TAG, "Unsupported address type: $atyp")
                    return
                }
            }
            
            val destPort = ((clientIn.read() shl 8) or clientIn.read()) and 0xFFFF
            
            Log.d(TAG, "Client wants to connect to: $destAddress:$destPort")
            
            // 转发连接请求到远程 SOCKS5
            remoteOut.write(byteArrayOf(5.toByte(), cmd.toByte(), 0.toByte(), atyp.toByte()))
            when (atyp) {
                1 -> remoteOut.write(ByteArray(4)) // IPv4 placeholder
                3 -> {
                    val domainBytes = destAddress.toByteArray(Charsets.UTF_8)
                    remoteOut.write(domainBytes.size)
                    remoteOut.write(domainBytes)
                }
                4 -> remoteOut.write(ByteArray(16)) // IPv6 placeholder
            }
            remoteOut.write((destPort shr 8) and 0xFF)
            remoteOut.write(destPort and 0xFF)
            remoteOut.flush()
            
            // 读取远程响应
            val response = ByteArray(10)
            val responseLen = remoteIn.read(response)
            if (responseLen >= 10 && response[1] == 0.toByte()) {
                // 成功，转发响应给客户端
                clientOut.write(response, 0, responseLen)
                clientOut.flush()
                
                Log.i(TAG, "✓ Connection established: $destAddress:$destPort")
                
                // 双向转发数据
                val clientToRemote = Thread {
                    try {
                        clientIn.copyTo(remoteOut)
                    } catch (e: IOException) {
                        Log.d(TAG, "Client to remote ended")
                    }
                }
                val remoteToClient = Thread {
                    try {
                        remoteIn.copyTo(clientOut)
                    } catch (e: IOException) {
                        Log.d(TAG, "Remote to client ended")
                    }
                }
                clientToRemote.start()
                remoteToClient.start()
                clientToRemote.join()
                remoteToClient.join()
            } else {
                Log.e(TAG, "Remote SOCKS5 connection failed")
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Connection error", e)
        } finally {
            try { clientSocket.close() } catch (e: IOException) {}
            try { remoteSocket?.close() } catch (e: IOException) {}
        }
    }
    
    /**
     * 停止 SOCKS5 代理
     */
    fun stop() {
        Log.i(TAG, "Stopping SOCKS5 proxy...")
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: IOException) {}
        serverSocket = null
        acceptThread?.interrupt()
        acceptThread = null
        Log.i(TAG, "SOCKS5 proxy stopped")
    }
    
    fun isRunning(): Boolean = isRunning
    fun getLocalAddress(): String = "127.0.0.1:$LOCAL_PORT"
}
