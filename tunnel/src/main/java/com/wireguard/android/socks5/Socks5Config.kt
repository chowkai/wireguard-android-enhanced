/*
 * WireGuard for Android
 * Copyright (C) 2026 WireGuard Team & SOCKS5 Enhancement
 *
 * SOCKS5 Configuration Data Class
 */

package com.wireguard.android.socks5

/**
 * SOCKS5 代理配置
 * 
 * @property enabled 是否启用 SOCKS5 代理
 * @property server 服务器地址 (IP 或域名)
 * @property port 服务器端口 (1080 默认)
 * @property username 用户名 (可选，用于认证)
 * @property password 密码 (可选，用于认证)
 */
data class Socks5Config(
    var enabled: Boolean = false,
    var server: String = "",
    var port: Int = 1080,
    var username: String = "",
    var password: String = ""
) {
    /**
     * 验证配置是否有效
     */
    fun isValid(): Boolean {
        return server.isNotBlank() && port in 1..65535
    }
    
    /**
     * 是否需要认证
     */
    fun requiresAuthentication(): Boolean {
        return username.isNotBlank() && password.isNotBlank()
    }
    
    /**
     * 获取完整的代理地址 (用于日志)
     */
    fun getProxyAddress(): String {
        return "$server:$port"
    }
    
    companion object {
        /**
         * 默认配置
         */
        fun default(): Socks5Config = Socks5Config()
    }
}
