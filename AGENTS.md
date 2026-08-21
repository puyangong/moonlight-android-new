# Moonlight 增强版 (Moonlight Enhanced)

基于开源项目 [Moonlight Android](https://github.com/moonlight-stream/moonlight-android) 二次开发的 Android 远程桌面应用，将 Moonlight 从单纯的游戏串流工具扩展为手机远程办公平台。

## 概述

Moonlight 增强版是一个 Android 平台的远程控制客户端，配合 Sunshine 服务端使用，支持通过 GameStream 协议对远程电脑进行低延迟的桌面控制。项目在保留原版 Moonlight 核心串流能力的基础上，新增了大量面向生产力场景的功能：竖屏显示适配、可贴边悬浮窗控制面板、内置虚拟键盘、屏幕缩放与拖动，以及断线自动重连等。

本项目为个人二次开发版本，以 root 和非 root 两种 flavor 构建。非 root 版本面向普通设备，root 版本利用系统权限提供原生鼠标捕获功能。项目同时维护了 Lua 脚本工具，用于网络数据包分析和解码纠错。

## 技术栈

- **语言/运行时**: Java 11, C/C++ (JNI/NDK), Lua
- **构建系统**: Gradle 8.5.1, Android Gradle Plugin (AGP) 8.5.1
- **最低 SDK**: 21 (Android 5.0)
- **目标/编译 SDK**: 34 (Android 14)
- **NDK 版本**: 27.0.12077973
- **关键依赖**:
  - BouncyCastle (加密, jdk18on 1.77)
  - OkHttp 4.12.0 (网络通信)
  - JCodec 0.2.5 (视频编解码)
  - jmDNS 3.5.9 (局域网服务发现)
  - ShieldControllerExtensions (控制器扩展)
- **子模块**: moonlight-common-c (C 语言公共库，从 GitHub 引入)
- **代码混淆**: ProGuard (不混淆代码，仅保留规则)
- **CI/CD**: AppVeyor (Windows CI)

## 项目结构

```
moonlight-android-new-main/
├── app/                          # 主应用模块
│   ├── build.gradle              # 应用级构建配置 (productFlavors, signingConfigs, NDK)
│   ├── proguard-rules.pro        # ProGuard 混淆保留规则
│   ├── lint.xml                  # Lint 配置
│   └── src/
│       ├── main/                 # 主源码
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/limelight/
│       │   │   ├── Game.java                     # 游戏/串流会话入口 Activity
│       │   │   ├── PcView.java                   # 电脑列表视图
│       │   │   ├── AppView.java                  # 应用列表视图
│       │   │   ├── LimeLog.java                  # 日志工具类
│       │   │   ├── PosterContentProvider.java    # 海报内容提供者
│       │   │   ├── ShortcutTrampoline.java       # 快捷方式跳转
│       │   │   ├── HelpActivity.java             # 帮助页面
│       │   │   ├── binding/                      # 平台绑定层
│       │   │   │   ├── PlatformBinding.java      # 平台绑定接口
│       │   │   │   ├── audio/                    # 音频绑定
│       │   │   │   ├── crypto/                   # 加密绑定 (AES等)
│       │   │   │   ├── input/                    # 输入绑定 (触摸、键盘、鼠标)
│       │   │   │   └── video/                    # 视频解码绑定
│       │   │   ├── computers/                    # 电脑管理
│       │   │   ├── discovery/                    # 局域网发现服务
│       │   │   ├── grid/                         # 网格布局相关
│       │   │   ├── nvstream/                     # NVStream 协议实现
│       │   │   │   ├── NvConnection.java         # 核心连接管理
│       │   │   │   ├── NvConnectionListener.java # 连接事件回调
│       │   │   │   ├── StreamConfiguration.java  # 串流配置
│       │   │   │   ├── av/                       # 音视频编解码
│       │   │   │   ├── http/                     # HTTP 通信层
│       │   │   │   ├── input/                    # 输入处理
│       │   │   │   ├── jni/                      # JNI 桥接层
│       │   │   │   ├── mdns/                     # mDNS 服务发现
│       │   │   │   └── wol/                      # Wake-on-LAN
│       │   │   ├── preferences/                  # 用户偏好设置
│       │   │   ├── ui/                           # UI 组件
│       │   │   │   ├── StreamView.java           # 串流视图 (SurfaceView)
│       │   │   │   ├── GameGestures.java         # 游戏手势控制
│       │   │   │   ├── FloatingWindow1.java      # 悬浮窗 V1 (贴边悬浮窗)
│       │   │   │   ├── FloatingWindow2.java      # 悬浮窗 V2
│       │   │   │   └── AdapterFragment.java      # 适配器 Fragment
│       │   │   └── utils/                        # 工具类
│       │   ├── jni/                              # 原生代码
│       │   │   ├── Android.mk                    # NDK Makefile
│       │   │   ├── Application.mk                # NDK 应用配置
│       │   │   ├── evdev_reader/                 # Linux evdev 输入读取
│       │   │   └── moonlight-core/               # Moonlight 核心 C 库 (子模块)
│       │   │       └── moonlight-common-c/       # 公共 C 库 (git submodule)
│       │   └── res/                              # 资源文件 (多语言)
│       ├── nonRoot/              # 非 root flavor 源码覆盖
│       └── root/                 # root flavor 源码覆盖
├── LuaScripts/                   # Lua 脚本工具
│   ├── NALParser.lua             # H.264/H.265 NAL 单元解析器
│   ├── NVStreamVideoPacket.lua   # NVStream 视频包分析
│   └── gridctl.lua               # 网格控制脚本
├── fastlane/                     # Fastlane 供应 (Google Play 元数据)
├── store-assets/                 # 应用商店截图素材
├── pic/                          # README 文档图片
├── build.gradle                  # 顶级 Gradle 构建脚本
├── settings.gradle               # Gradle 设置 (include ':app')
├── gradle.properties             # Gradle JVM 配置
├── gradlew / gradlew.bat         # Gradle Wrapper
├── appveyor.yml                  # AppVeyor CI 配置
├── LICENSE.txt                   # 许可证
├── .github/                      # GitHub 配置
├── .snow/                        # Snow AI CLI 配置
└── README.md                     # 中文项目说明
```

## 核心功能

### 原有 Moonlight 功能
- **GameStream 协议支持**: 通过 NVIDIA GameStream 或 Sunshine 协议进行低延迟桌面串流
- **音视频编解码**: H.264/H.265 视频硬件解码，音频编解码
- **输入转发**: 触摸、键盘、鼠标输入实时转发到远程主机
- **局域网自动发现**: 通过 mDNS 自动发现局域网内的 GameStream/Sunshine 主机
- **Wake-on-LAN**: 远程唤醒功能

### 增强功能（本版本新增）
- **竖屏显示**: 适配手机竖屏模式，方便单手操作远程桌面
- **贴边悬浮窗**: 可拖动的悬浮控制面板，自动贴边隐藏，支持移动、屏幕操作和键盘切换
- **屏幕缩放与拖动**: 双指缩放 + 单指拖动屏幕画面，双击恢复默认
- **内置虚拟键盘**: 悬浮窗集成虚拟键盘，点击即可打开/关闭
- **断线自动重连**: 切出应用后返回时自动重连，恢复屏幕缩放、位置、悬浮窗状态和键盘状态

## 开发入门

### 先决条件
- JDK 17+
- Android SDK (compileSdk 34)
- NDK 27.0.12077973
- Git (用于初始化子模块)

### 安装与构建
```bash
# 克隆仓库
git clone <repository-url>
cd moonlight-android-new-main

# 初始化子模块
git submodule update --init --recursive

# 构建 Debug 版本（非 root flavor）
./gradlew assembleNonRootDebug

# 构建 Release 版本
./gradlew assembleNonRootRelease

# 构建 Root 版本
./gradlew assembleRootDebug
```

### 使用说明
1. 在电脑端安装并配置 [Sunshine](https://app.lizardbyte.dev/Sunshine/?lng=zh-CN)
2. 配置内网穿透或异地组网（推荐异地组网，需同时打开 TCP 和 UDP）
3. 在 Moonlight 中搜索并配对电脑
4. 连接后使用悬浮窗进行常用操作：
   - **推荐设置**: 输入设置中开启触摸屏模式
   - **右键点击**: 一指长按 + 一指点击
   - **滚动屏幕**: 两指滑动

## 开发

### 可用脚本
| 命令 | 说明 |
|------|------|
| `./gradlew assembleNonRootDebug` | 构建非 root Debug APK |
| `./gradlew assembleNonRootRelease` | 构建非 root Release APK |
| `./gradlew assembleRootDebug` | 构建 root Debug APK |
| `./gradlew assembleRootRelease` | 构建 root Release APK |
| `./gradlew connectedCheck` | 运行连接设备测试 |
| `./gradlew lint` | 运行 Lint 代码检查 |

### 开发工作流
1. 修改 Java/Kotlin 源码（`app/src/main/java/com/limelight/`）
2. 如涉及原生代码，修改 `app/src/main/jni/` 下的 C 文件
3. 使用 Android Studio 或命令行构建测试
4. 使用 AppVeyor CI 进行自动化构建

### Flavor 说明
- **nonRoot**: 面向普通设备，使用标准 Android 输入 API
- **root**: 利用 root 权限使用 evdev 接口实现原生鼠标捕获（限制 maxSdk 25）

## 架构

### 分层架构
```
┌─────────────────────────────────────────┐
│              UI Layer (ui/)              │
│  StreamView, FloatingWindow,            │
│  GameGestures, AdapterFragment          │
├─────────────────────────────────────────┤
│          Business Layer                  │
│  Game.java, PcView.java, AppView.java    │
├─────────────────────────────────────────┤
│         NVStream Protocol (nvstream/)    │
│  NvConnection, StreamConfiguration,      │
│  av/, http/, input/, mdns/, wol/         │
├─────────────────────────────────────────┤
│         Platform Binding (binding/)      │
│  PlatformBinding, audio/, crypto/,       │
│  input/, video/                          │
├─────────────────────────────────────────┤
│         JNI / Native Layer (jni/)        │
│  moonlight-core, evdev_reader            │
├─────────────────────────────────────────┤
│        Android Framework                 │
│  SurfaceView, AudioTrack, MediaCodec     │
└─────────────────────────────────────────┘
```

### 核心数据流
1. **发现**: mDNS → 扫描局域网 → 获取主机列表
2. **配对**: HTTP → Sunshine/GS 服务端 → 证书交换 (BouncyCastle)
3. **串流**: NvConnection 建立连接 → 音视频流通过 JNI 解码 → SurfaceView 渲染
4. **输入**: 触摸/键盘事件 → binding/input → JNI 编码 → 网络发送

## 配置

### 签名配置
签名密钥信息存储在 `app/build.gradle` 的 `signingConfigs` 中（仅供本地开发使用）。

### ProGuard
- 不使用代码混淆 (`-dontobfuscate`)
- 保留关键类：`com.limelight.binding.input.evdev.*`, `com.limelight.nvstream.jni.*`
- 第三方库保留规则：Okio, BouncyCastle, jMDNS

## 许可证

基于 Moonlight Android 开源项目，遵循 GPL-3.0 许可证。详见 `LICENSE.txt`。

## 致谢

- [Moonlight Android](https://github.com/moonlight-stream/moonlight-android) - 原始开源项目
- [Sunshine](https://app.lizardbyte.dev/Sunshine/) - 开源 GameStream 服务端
- [Moonlight Common C](https://github.com/moonlight-stream/moonlight-common-c) - C 语言公共库
