/*
 * WireGuard for Android
 * Copyright (C) 2026 WireGuard Team & SOCKS5 Enhancement
 *
 * SOCKS5 Settings Fragment
 */

package com.wireguard.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.wireguard.android.socks5.Socks5Config
import com.wireguard.android.socks5.Socks5ProxyService

/**
 * SOCKS5 代理设置界面
 */
class Socks5SettingsFragment : Fragment() {
    
    companion object {
        private const val TAG = "Socks5SettingsFragment"
        
        fun newInstance(): Socks5SettingsFragment = Socks5SettingsFragment()
    }
    
    private val proxyService = Socks5ProxyService()
    private var config: Socks5Config = Socks5Config.default()
    
    // UI Views
    private lateinit var switchEnable: SwitchMaterial
    private lateinit var editServer: TextInputEditText
    private lateinit var editPort: TextInputEditText
    private lateinit var editUsername: TextInputEditText
    private lateinit var editPassword: TextInputEditText
    private lateinit var buttonSave: MaterialButton
    private lateinit var textStatus: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_socks5, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        switchEnable = view.findViewById(R.id.switch_enable_socks5)
        editServer = view.findViewById(R.id.edit_server)
        editPort = view.findViewById(R.id.edit_port)
        editUsername = view.findViewById(R.id.edit_username)
        editPassword = view.findViewById(R.id.edit_password)
        buttonSave = view.findViewById(R.id.button_save)
        textStatus = view.findViewById(R.id.text_status)
        
        // Load saved config
        loadConfig()
        
        // Set listeners
        buttonSave.setOnClickListener { onSaveClicked() }
    }
    
    /**
     * 加载保存的配置
     */
    private fun loadConfig() {
        // TODO: 从 SharedPreferences 加载配置
        config = Socks5Config.default()
        
        // Update UI
        switchEnable.isChecked = config.enabled
        editServer.setText(config.server)
        editPort.setText(config.port.toString())
        editUsername.setText(config.username)
        editPassword.setText(config.password)
        
        updateStatus()
    }
    
    /**
     * 保存配置
     */
    private fun onSaveClicked() {
        // Read from UI
        config.enabled = switchEnable.isChecked
        config.server = editServer.text?.toString() ?: ""
        config.port = editPort.text?.toString()?.toIntOrNull() ?: 1080
        config.username = editUsername.text?.toString() ?: ""
        config.password = editPassword.text?.toString() ?: ""
        
        // Validate
        if (config.enabled && !config.isValid()) {
            textStatus.text = "错误：服务器地址不能为空"
            return
        }
        
        // Save config
        saveConfig()
        
        // Start/Stop proxy
        if (config.enabled) {
            proxyService.configure(config)
            val success = proxyService.start()
            if (success) {
                textStatus.text = "已连接：${config.getProxyAddress()}"
            } else {
                textStatus.text = "连接失败"
            }
        } else {
            proxyService.stop()
            textStatus.text = "SOCKS5 已禁用"
        }
    }
    
    /**
     * 保存配置到存储
     */
    private fun saveConfig() {
        // TODO: 保存到 SharedPreferences
        // 临时实现：仅内存保存
    }
    
    /**
     * 更新状态显示
     */
    private fun updateStatus() {
        if (proxyService.isConnected()) {
            textStatus.text = "已连接：${config.getProxyAddress()}"
        } else {
            textStatus.text = "未连接"
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        proxyService.stop()
    }
}
