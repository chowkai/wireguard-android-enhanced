# WireGuard + SOCKS5 增强版 - 使用说明

**版本**: Phase 2 完成版  
**编译时间**: 2026-03-28  
**GitHub**: https://github.com/chowkai/wireguard-android-enhanced/actions

---

## 📋 功能说明

本版本在 WireGuard 官方客户端基础上添加了 SOCKS5 代理功能：

### 工作流程

```
手机 App → SOCKS5(10.0.0.11:1080) → WireGuard 隧道 → 内网 → 互联网
```

1. 用户连接 WireGuard
2. WireGuard 连接成功后，自动启动 SOCKS5 代理
3. 所有流量通过 SOCKS5 → WireGuard → 互联网

---

## 🔧 配置说明

### 当前版本配置方式

**配置文件**: `tunnel/src/main/java/com/wireguard/android/socks5/WireGuardSocks5Integration.kt`

**修改位置**:
```kotlin
val config = Socks5Config(
    enabled = true,           // 是否启用 SOCKS5
    server = "10.0.0.11",     // SOCKS5 服务器 IP
    port = 1080,              // SOCKS5 端口
    username = "",            // 用户名 (可选)
    password = ""             // 密码 (可选)
)
```

### 修改步骤

1. 打开 `WireGuardSocks5Integration.kt`
2. 修改 `server` 为你的 SOCKS5 服务器 IP
3. 修改 `port` 为你的 SOCKS5 端口
4. 如有认证，填写 `username` 和 `password`
5. 重新编译 APK

---

## 📥 下载安装

### 下载 APK

1. 访问：https://github.com/chowkai/wireguard-android-enhanced/actions
2. 点击最新编译成功 (绿色 ✓) 的运行
3. 下载 Artifacts → `wireguard-debug-apks`
4. 解压后得到 `ui-debug.apk` 和 `tunnel-debug.apk`

### 安装

```bash
adb install ui-debug.apk
adb install tunnel-debug.apk
```

或在手机上直接安装 APK 文件。

---

## 🚀 使用方法

1. **打开 WireGuard App**
2. **导入/创建 WireGuard 配置**
3. **点击连接**
4. **WireGuard 连接成功后，SOCKS5 自动启动**
5. **所有流量自动通过 SOCKS5 代理**

---

## 🔍 验证 SOCKS5 是否工作

### 方法 1: 查看日志

```bash
adb logcat | grep "Socks5"
```

**正常输出**:
```
I/Socks5Manager: Tunnel connected, checking SOCKS5 config...
I/Socks5Manager: ✓ SOCKS5 proxy started: 10.0.0.11:1080
I/Socks5Manager: ✓ Traffic flow: App → SOCKS5 → WireGuard → Internet
```

### 方法 2: 测试网络访问

1. 连接 WireGuard
2. 打开浏览器访问仅内网可访问的网站
3. 如果能访问，说明 SOCKS5 工作正常

---

## ⚠️ 注意事项

1. **SOCKS5 服务器必须在 WireGuard 内网中可达**
2. **先连接 WireGuard，SOCKS5 才会启动**
3. **WireGuard 断开后，SOCKS5 自动停止**

---

## 📝 下一步 (Phase 2b)

计划添加 UI 配置界面：
- SOCKS5 配置界面
- 启用/禁用开关
- 服务器/端口输入
- 保存/连接按钮

---

**技术支持**: https://github.com/chowkai/wireguard-android-enhanced/issues
