/*
 * WireGuard for Android
 * Copyright (C) 2026 WireGuard Team & SOCKS5 Enhancement
 *
 * SOCKS5 Proxy Service
 */

package com.wireguard.android.socks5

import android.util.Log
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * SOCKS5 代理服务
 * 
 * 功能:
 * - 管理 SOCKS5 连接
 * - 代理转发
 * - 生命周期管理
 */
class Socks5ProxyService {
    
    companion object {
        private const val TAG = "Socks5ProxyService"
    }
    
    private var config: Socks5Config? = null
    private var proxy: Proxy? = null
    private var isConnected: Boolean = false
    
    /**
     * 配置 SOCKS5 代理
     */
    fun configure(newConfig: Socks5Config) {
        Log.d(TAG, "Configuring SOCKS5 proxy: ${newConfig.getProxyAddress()}")
        this.config = newConfig
    }
    
    /**
     * 启动 SOCKS5 代理
     */
    fun start(): Boolean {
        val cfg = config ?: run {
            Log.e(TAG, "Cannot start: no configuration")
            return false
        }
        
        if (!cfg.isValid()) {
            Log.e(TAG, "Cannot start: invalid configuration")
            return false
        }
        
        if (!cfg.enabled) {
            Log.d(TAG, "SOCKS5 proxy is disabled")
            return false
        }
        
        try {
            // 创建 SOCKS5 代理
            val address = InetSocketAddress(cfg.server, cfg.port)
            this.proxy = if (cfg.requiresAuthentication()) {
                // TODO: 实现带认证的 SOCKS5
                Log.d(TAG, "SOCKS5 with authentication (not fully implemented)")
                Proxy(Proxy.Type.SOCKS, address)
            } else {
                Proxy(Proxy.Type.SOCKS, address)
            }
            
            this.isConnected = true
            Log.i(TAG, "SOCKS5 proxy started: ${cfg.getProxyAddress()}")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SOCKS5 proxy", e)
            this.isConnected = false
            return false
        }
    }
    
    /**
     * 停止 SOCKS5 代理
     */
    fun stop() {
        Log.d(TAG, "Stopping SOCKS5 proxy")
        this.proxy = null
        this.isConnected = false
    }
    
    /**
     * 获取代理实例
     */
    fun getProxy(): Proxy? {
        return proxy
    }
    
    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean {
        return isConnected && config?.isValid() == true && config?.enabled == true
    }
    
    /**
     * 获取当前配置
     */
    fun getConfig(): Socks5Config? {
        return config
    }
}
