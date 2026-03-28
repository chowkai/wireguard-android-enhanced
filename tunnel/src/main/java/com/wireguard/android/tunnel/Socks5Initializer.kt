package com.wireguard.android.tunnel

import android.util.Log
import com.wireguard.android.socks5.SimpleSocks5Client

/**
 * SOCKS5 初始化器
 * 
 * 在 WireGuard 隧道启动时自动启动简化的 SOCKS5 客户端
 */
object Socks5Initializer {
    
    private const val TAG = "Socks5Initializer"
    
    // SOCKS5 配置 - 硬编码
    private const val REMOTE_SERVER = "10.0.0.11"  // 内网 SOCKS5 服务器
    private const val REMOTE_PORT = 1080
    private const val LOCAL_PORT = 11080
    
    private var socks5Client: SimpleSocks5Client? = null
    
    /**
     * 在 WireGuard 隧道启动后调用
     * 
     * 调用时机：WireGuard 连接成功，隧道建立后
     */
    fun onTunnelStarted() {
        Log.i(TAG, "Tunnel started, initializing SOCKS5...")
        
        // 延迟 2 秒启动 SOCKS5，确保 WireGuard 完全就绪
        Thread {
            try {
                Thread.sleep(2000)
                
                // 创建并启动简化的 SOCKS5 客户端
                socks5Client = SimpleSocks5Client(
                    remoteServer = REMOTE_SERVER,
                    remotePort = REMOTE_PORT,
                    localPort = LOCAL_PORT
                )
                socks5Client?.start()
                
                Log.i(TAG, "=========================================")
                Log.i(TAG, "✓ SIMPLE SOCKS5 CLIENT STARTED")
                Log.i(TAG, "✓ Listen: 127.0.0.1:$LOCAL_PORT")
                Log.i(TAG, "✓ Remote: $REMOTE_SERVER:$REMOTE_PORT")
                Log.i(TAG, "✓ Traffic: App → Local:$LOCAL_PORT → WireGuard → Remote:$REMOTE_PORT → Internet")
                Log.i(TAG, "=========================================")
                Log.i(TAG, "")
                Log.i(TAG, "使用方法:")
                Log.i(TAG, "在应用中配置 SOCKS5 代理:")
                Log.i(TAG, "  服务器：127.0.0.1")
                Log.i(TAG, "  端口：$LOCAL_PORT")
                Log.i(TAG, "=========================================")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting SOCKS5", e)
            }
        }.start()
    }
    
    /**
     * 在 WireGuard 隧道停止时调用
     */
    fun onTunnelStopped() {
        Log.i(TAG, "Tunnel stopped, stopping SOCKS5...")
        socks5Client?.stop()
        socks5Client = null
    }
}
