package com.wireguard.android.tunnel

import android.util.Log
import com.wireguard.android.socks5.Socks5Config
import com.wireguard.android.socks5.Socks5ProxyService

/**
 * SOCKS5 初始化器
 * 
 * 在 WireGuard 隧道启动时自动初始化 SOCKS5 代理
 */
object Socks5Initializer {
    
    private const val TAG = "Socks5Initializer"
    
    // SOCKS5 配置 - 可在编译时修改
    private val config = Socks5Config(
        enabled = true,
        server = "10.0.0.11",  // 内网 SOCKS5 服务器
        port = 1080,
        username = "",
        password = ""
    )
    
    private val proxyService = Socks5ProxyService()
    
    /**
     * 在 WireGuard 隧道启动后调用
     * 
     * 调用时机：WireGuard 连接成功，隧道建立后
     */
    fun onTunnelStarted() {
        Log.i(TAG, "Tunnel started, initializing SOCKS5...")
        
        if (!config.enabled) {
            Log.d(TAG, "SOCKS5 is disabled")
            return
        }
        
        if (!config.isValid()) {
            Log.e(TAG, "Invalid SOCKS5 config: ${config.getProxyAddress()}")
            return
        }
        
        // 延迟 2 秒启动 SOCKS5，确保 WireGuard 完全就绪
        Thread {
            try {
                Thread.sleep(2000)
                
                proxyService.configure(config)
                val success = proxyService.start()
                
                if (success) {
                    Log.i(TAG, "=========================================")
                    Log.i(TAG, "✓ LOCAL SOCKS5 PROXY STARTED")
                    Log.i(TAG, "✓ Listen: 127.0.0.1:11080")
                    Log.i(TAG, "✓ Remote: ${config.getProxyAddress()}")
                    Log.i(TAG, "✓ Traffic: App → Local:11080 → WireGuard → Remote:1080 → Internet")
                    Log.i(TAG, "=========================================")
                    Log.i(TAG, "")
                    Log.i(TAG, "配置说明:")
                    Log.i(TAG, "1. 在支持 SOCKS5 的应用中配置代理:")
                    Log.i(TAG, "   服务器：127.0.0.1")
                    Log.i(TAG, "   端口：11080")
                    Log.i(TAG, "2. 或使用支持系统代理的应用")
                    Log.i(TAG, "=========================================")
                } else {
                    Log.e(TAG, "✗ Failed to start SOCKS5 proxy")
                }
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
        proxyService.stop()
        
        // 清除系统属性
        System.clearProperty("socksProxy")
        System.clearProperty("socksPort")
        System.clearProperty("java.net.socks.username")
        System.clearProperty("java.net.socks.password")
    }
}
