package com.wireguard.android.socks5

import android.util.Log

/**
 * SOCKS5 管理器
 * 
 * 使用场景:
 * 1. 先连接 WireGuard 隧道
 * 2. WireGuard 连通后，启动 SOCKS5 代理
 * 3. 流量通过 SOCKS5 → WireGuard → 内网 → 互联网
 */
object Socks5Manager {
    
    private const val TAG = "Socks5Manager"
    
    // 默认配置 (可在 ConfigActivity 中修改)
    private var config = Socks5Config(
        enabled = true,
        server = "10.0.0.11",  // 内网 SOCKS5 服务器
        port = 1080,
        username = "",
        password = ""
    )
    
    private val proxyService = Socks5ProxyService()
    
    /**
     * WireGuard 连接成功后调用
     */
    fun onWireGuardConnected() {
        Log.i(TAG, "WireGuard connected, starting SOCKS5 proxy...")
        
        if (config.enabled && config.isValid()) {
            proxyService.configure(config)
            val success = proxyService.start()
            if (success) {
                Log.i(TAG, "SOCKS5 proxy started: ${config.getProxyAddress()}")
            } else {
                Log.e(TAG, "Failed to start SOCKS5 proxy")
            }
        } else {
            Log.d(TAG, "SOCKS5 proxy is disabled or invalid config")
        }
    }
    
    /**
     * WireGuard 断开时调用
     */
    fun onWireGuardDisconnected() {
        Log.i(TAG, "WireGuard disconnected, stopping SOCKS5 proxy...")
        proxyService.stop()
    }
    
    /**
     * 更新配置
     */
    fun updateConfig(newConfig: Socks5Config) {
        config = newConfig
        Log.d(TAG, "Config updated: ${config.getProxyAddress()}")
    }
    
    /**
     * 获取当前配置
     */
    fun getConfig(): Socks5Config = config
    
    /**
     * 检查 SOCKS5 是否已启用
     */
    fun isSocks5Enabled(): Boolean = config.enabled && proxyService.isConnected()
}
