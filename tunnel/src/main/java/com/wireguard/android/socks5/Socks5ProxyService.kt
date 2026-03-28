package com.wireguard.android.socks5

import android.util.Log
import java.net.InetSocketAddress
import java.net.Proxy

class Socks5ProxyService {
    companion object {
        private const val TAG = "Socks5ProxyService"
    }
    
    private var config: Socks5Config? = null
    private var proxy: Proxy? = null
    private var isConnected: Boolean = false
    
    fun configure(newConfig: Socks5Config) {
        Log.d(TAG, "Configuring: ${newConfig.getProxyAddress()}")
        this.config = newConfig
    }
    
    fun start(): Boolean {
        val cfg = config ?: return false
        if (!cfg.isValid() || !cfg.enabled) return false
        try {
            this.proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(cfg.server, cfg.port))
            this.isConnected = true
            Log.i(TAG, "Started: ${cfg.getProxyAddress()}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed", e)
            this.isConnected = false
            return false
        }
    }
    
    fun stop() {
        this.proxy = null
        this.isConnected = false
    }
    
    fun getProxy(): Proxy? = proxy
    fun isConnected(): Boolean = isConnected
}
