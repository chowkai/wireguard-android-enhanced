package com.wireguard.android.socks5

data class Socks5Config(
    var enabled: Boolean = false,
    var server: String = "",
    var port: Int = 1080,
    var username: String = "",
    var password: String = ""
) {
    fun isValid(): Boolean = server.isNotBlank() && port in 1..65535
    fun requiresAuthentication(): Boolean = username.isNotBlank() && password.isNotBlank()
    fun getProxyAddress(): String = "$server:$port"
}
