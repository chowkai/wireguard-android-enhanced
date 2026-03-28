# WireGuard Android SOCKS5 增强版 - 代码修改说明

**项目地址**: https://github.com/chowkai/wireguard-android-enhanced

**基于版本**: WireGuard Android 官方源码

**修改日期**: 2026-03-28

---

## 📋 修改概述

在 WireGuard Android 官方客户端基础上，添加 SOCKS5 代理功能，实现内网穿透上网。

### 核心功能
- 在 WireGuard 连接成功后，自动启动 SOCKS5 代理服务
- 本地监听 `127.0.0.1:11080`
- 转发流量到内网 SOCKS5 服务器 `10.0.0.11:1080`
- 通过 WireGuard 隧道访问互联网

---

## 📁 新增文件

### 1. `tunnel/src/main/java/com/wireguard/android/socks5/SimpleSocks5Client.kt`

**功能**: 简化的 SOCKS5 服务器/客户端实现

**核心逻辑**:
```kotlin
// 在本地启动 SOCKS5 服务器
ServerSocket(11080)

// 接收应用连接
acceptClient()

// 转发到远程 SOCKS5 (通过 WireGuard 隧道)
Socket("10.0.0.11", 1080)

// 双向转发数据
client ↔ remote
```

---

### 2. `tunnel/src/main/java/com/wireguard/android/tunnel/Socks5Initializer.kt`

**功能**: SOCKS5 初始化器

**调用时机**: WireGuard 连接成功后（UI 进程）

---

## 🔧 修改文件

### 1. `ui/src/main/java/com/wireguard/android/model/ObservableTunnel.kt`

**修改位置**: `onStateChanged()` 方法

**修改内容**: 在 UI 进程检测到 Tunnel UP 时，启动 SOCKS5

**修改原因**: 
- 之前尝试在 VPN 服务进程中启动 SOCKS5，但流量不走系统路由
- 移到 UI 进程（普通应用进程），流量自动走系统路由（已被 WireGuard 接管）

---

### 2. `tunnel/src/main/java/com/wireguard/android/backend/GoBackend.java`

**修改位置**: `setTunnel()` 方法

**修改内容**: 移除 VPN 服务进程中的 SOCKS5 调用（移到 UI 进程）

---

## 🔄 工作流程

```
1. 用户点击 WireGuard 连接
2. VpnService 启动，建立隧道
3. ObservableTunnel 检测到 Tunnel.State.UP
4. UI 进程调用 Socks5Initializer.onTunnelStarted()
5. 异步线程延迟 2 秒
6. 启动 SimpleSocks5Client
7. 监听 127.0.0.1:11080
8. SOCKS5 服务就绪
```

### 流量路径

```
应用 → 127.0.0.1:11080 → WireGuard 隧道 → 10.0.0.11:1080 → 互联网
```

---

## 📝 配置说明

**硬编码配置** (位置：`Socks5Initializer.kt`):
```kotlin
private const val REMOTE_SERVER = "10.0.0.11"  // 内网 SOCKS5 服务器
private const val REMOTE_PORT = 1080
private const val LOCAL_PORT = 11080
```

---

## 🧪 测试方法

### 1. 查看日志
```bash
adb logcat | grep -i "SOCKS5"
```

**期望输出**:
```
✓ SIMPLE SOCKS5 CLIENT STARTED
✓ Listen: 127.0.0.1:11080
✓ Remote: 10.0.0.11:1080
```

### 2. 配置应用代理
- 服务器：`127.0.0.1`
- 端口：`11080`
- 类型：`SOCKS5`

---

## ⚠️ 已知问题

### 架构限制

**问题**: 同一个应用的 UI 进程流量不一定走 VPN 隧道

**原因**: Android VPN 架构限制，VpnService 只接管系统其他应用的流量，不接管自己的流量

**影响**: SOCKS5 服务可能无法通过 WireGuard 隧道访问 10.0.0.11

---

## 📚 编译历史

| 次数 | 状态 | 说明 |
|------|------|------|
| 1-14 | ❌ | 各种编译错误 |
| 15 | ✅ | Phase 2a 核心功能 |
| 16-20 | ❌ | 集成代码问题 |
| 21 | ✅ | 编译成功 (功能不工作) |
| 22-26 | ❌ | 架构调整和编译错误 |
| 27 | ✅ | 简化 SOCKS5 客户端 |

---

**最后更新**: 2026-03-28  
**版本**: v2.0 (简化 SOCKS5 客户端)
