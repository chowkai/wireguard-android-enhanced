package com.wireguard.android.socks5

import android.util.Log

/**
 * WireGuard + SOCKS5 集成
 * 
 * 工作流程:
 * 1. 用户连接 WireGuard
 * 2. WireGuard 连接成功后，自动启动 SOCKS5 代理
 * 3. 所有流量 → SOCKS5(10.0.0.11:1080) → WireGuard 隧道 → 内网 → 互联网
 */
object WireGuardSocks5Integration {
    
    private const val TAG = "WireGuardSocks5"
    
    /**
     * 在 WireGuard 连接成功后调用
     * 
     * 调用位置: WireGuard 状态变为 CONNECTED 时
     */
    fun onTunnelConnected() {
        Log.i(TAG, "Tunnel connected, checking SOCKS5 config...")
        
        val config = Socks5Config(
            enabled = true,
            server = "10.0.0.11",  // 内网 SOCKS5 服务器
            port = 1080,
            username = "",
            password = ""
        )
        
        if (config.enabled && config.isValid()) {
            val proxyService = Socks5ProxyService()
            proxyService.configure(config)
            val success = proxyService.start()
            
            if (success) {
                Log.i(TAG, "✓ SOCKS5 proxy started: ${config.getProxyAddress()}")
                Log.i(TAG, "✓ Traffic flow: App → SOCKS5 → WireGuard → Internet")
            } else {
                Log.e(TAG, "✗ Failed to start SOCKS5 proxy")
            }
        } else {
            Log.d(TAG, "SOCKS5 proxy is disabled")
        }
    }
    
    /**
     * 在 WireGuard 断开连接时调用
     * 
     * 调用位置: WireGuard 状态变为 DISCONNECTED 时
     */
    fun onTunnelDisconnected() {
        Log.i(TAG, "Tunnel disconnected, stopping SOCKS5 proxy...")
        // SOCKS5 服务会自动停止，因为底层连接已断开
    }
}
