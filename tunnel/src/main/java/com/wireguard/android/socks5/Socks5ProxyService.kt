/*
 * WireGuard for Android
 * Copyright (C) 2026 WireGuard Team & SOCKS5 Enhancement
 *
 * SOCKS5 Proxy Service - Pure Java Implementation
 */

package com.wireguard.android.socks5

import android.util.Log
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket

/**
 * SOCKS5 代理服务 - 纯 Java 实现
 * 
 * 功能:
 * - 管理 SOCKS5 连接
 * - 代理转发
 * - 生命周期管理
 */
class Socks5ProxyService {
    
    companion object {
        private const val TAG = "Socks5ProxyService"
        private const val SOCKS_VERSION = 5
        private const val AUTH_NONE = 0
        private const val AUTH_GSSAPI = 1
        private const val AUTH_PASSWORD = 2
        private const val CMD_CONNECT = 1
        private const val ATYP_IPV4 = 1
        private const val ATYP_DOMAIN = 3
        private const val ATYP_IPV6 = 4
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
            // 创建 SOCKS5 代理连接
            val address = InetSocketAddress(cfg.server, cfg.port)
            this.proxy = Proxy(Proxy.Type.SOCKS, address)
            
            // 如果需要认证，进行认证握手
            if (cfg.requiresAuthentication()) {
                if (!authenticate(cfg)) {
                    Log.e(TAG, "SOCKS5 authentication failed")
                    return false
                }
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
     * SOCKS5 认证握手
     */
    private fun authenticate(config: Socks5Config): Boolean {
        return try {
            val socket = Socket(config.server, config.port)
            val out = socket.getOutputStream()
            val `in` = socket.getInputStream()
            
            // 发送认证方法声明
            val username = config.username.toByteArray(Charsets.UTF_8)
            val password = config.password.toByteArray(Charsets.UTF_8)
            
            out.write(byteArrayOf(
                SOCKS_VERSION.toByte(),
                2.toByte(), // 2 种认证方法
                AUTH_NONE.toByte(),
                AUTH_PASSWORD.toByte()
            ))
            out.flush()
            
            // 读取服务器响应
            val response = ByteArray(2)
            val read = `in`.read(response)
            if (read != 2 || response[1].toInt() != AUTH_PASSWORD) {
                socket.close()
                return false
            }
            
            // 发送用户名密码
            out.write(byteArrayOf(1.toByte(), username.size.toByte()))
            out.write(username)
            out.write(password.size.toByte())
            out.write(password)
            out.flush()
            
            // 读取认证结果
            val authResponse = ByteArray(2)
            `in`.read(authResponse)
            socket.close()
            
            authResponse[1] == 0.toByte() // 0 表示成功
            
        } catch (e: IOException) {
            Log.e(TAG, "Authentication failed", e)
            false
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
