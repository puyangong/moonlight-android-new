# Moonlight 增强版 — AI 开发规范

本文档定义了 AI 助手在本项目中进行代码开发时必须遵循的规范和约定。

## 响应语言

- **始终使用中文**进行对话和注释说明
- 代码标识符（类名、方法名、变量名）使用英文
- 提交信息使用中文

## 技术约束

- **语言版本**: Java 11（sourceCompatibility & targetCompatibility）
- **最低 API**: 21 (Android 5.0)
- **目标 API**: 34 (Android 14)
- **NDK 版本**: 27.0.12077973
- **编译编码**: UTF-8
- **严禁使用**: Java 17+ 特性（如 Records, Sealed Classes, Pattern Matching for Switch）

## 代码风格

### Java 编码规范
- **缩进**: 4 空格缩进，不使用 Tab
- **命名约定**:
  - 类名: `UpperCamelCase`（如 `NvConnection`, `StreamView`）
  - 方法名: `lowerCamelCase`（如 `startStreaming`, `onConnectionEstablished`）
  - 常量: `UPPER_SNAKE_CASE`（如 `MAX_RETRY_COUNT`）
  - 成员变量: `mCamelCase`（如 `mConnectionContext`, `mVideoDecoder`）— 遵循 AOSP 风格
  - 静态变量: `sCamelCase`（如 `sInstance`）
- **注解**: 使用 `@Override` 标注所有重写方法
- **空值处理**: 使用 `@Nullable` / `@NonNull` 注解明确可空性

### C/C++ 编码规范 (JNI)
- **命名**: JNI 方法遵循 `Java_<package>_<Class>_<method>` 格式
- **缩进**: 4 空格
- **错误处理**: 所有 JNI 调用必须检查 `env->ExceptionCheck()`
- **内存管理**: `NewGlobalRef` 必须配对 `DeleteGlobalRef`，`GetStringUTFChars` 配对 `ReleaseStringUTFChars`

### XML 资源规范
- **多语言**: 
  - 默认字符串在 `res/values/strings.xml`
  - 中文翻译在 `res/values-zh-rCN/strings.xml`
  - 新增用户可见文本必须提供中英文版本
- **命名**: 使用 `snake_case`（如 `app_label`, `action_settings`）
- **布局**: 
  - 竖屏布局在 `res/layout/`
  - 横屏布局在 `res/layout-land/`
  - 命名使用 `snake_case`

## 项目架构约束

### 包结构
```
com.limelight
├── binding/         # 平台绑定 → 不在此层写业务逻辑
├── computers/       # 电脑管理
├── discovery/       # 服务发现
├── grid/            # 网格布局
├── nvstream/        # NVStream 核心协议 → 核心逻辑在此
│   ├── av/          # 音视频编解码
│   ├── http/        # HTTP 通信
│   ├── input/       # 输入处理
│   ├── jni/         # JNI 桥接
│   ├── mdns/        # mDNS 发现
│   └── wol/         # 网络唤醒
├── preferences/     # 偏好设置
├── ui/              # UI 组件
└── utils/           # 工具类
```

### 架构规则
1. **UI 层不直接调用 JNI**: 必须通过 `nvstream/` 或 `binding/` 层
2. **新增 Activity**: 必须在 `AndroidManifest.xml` 中注册
3. **Flavor 感知**: 注意区分 root/nonRoot flavor，条件代码使用 `BuildConfig.ROOT_BUILD`
4. **线程安全**: 网络操作在后台线程，UI 更新通过 `runOnUiThread()` / `Handler(Looper.getMainLooper())`

## 构建与测试

### 构建验证
- 修改代码后必须确保项目可编译
- 涉及 NDK 修改时，必须验证两种 flavor 均可构建
- 命令: `./gradlew assembleNonRootDebug assembleRootDebug`

### Lint 检查
- 运行 `./gradlew lint` 检查代码质量
- 忽略规则: `MissingTranslation` (允许部分翻译缺失)
- 不引入新的 Lint 警告

### ProGuard
- **不要混淆代码** (`-dontobfuscate`)，仅压缩和优化
- 新增使用反射的类必须在 `proguard-rules.pro` 中添加 `-keep` 规则
- 重点关注: `com.limelight.binding.input.evdev.*`, `com.limelight.nvstream.jni.*`

## 文件命名

- Java 源文件: `UpperCamelCase.java`
- 布局文件: `snake_case.xml`
- 资源文件: `snake_case.xml`
- JNI/C 源文件: `snake_case.c`
- NDK makefile: `Android.mk`, `Application.mk`

## 版本管理

- `versionCode`: 整数递增（当前 314）
- `versionName`: 语义化版本（当前 "12.1"）
- 修改 `versionCode` 时必须同步更新 `versionName`

## 禁止操作

1. ❌ 不要更改 `applicationId`（`com.limelight` / `com.limelight.root`）
2. ❌ 不要修改 release signingConfig
3. ❌ 不要移除 ProGuard `-dontobfuscate` 规则
4. ❌ 不要在 UI 线程执行网络 I/O
5. ❌ 不要在主分支直接提交构建产物（APK/AAB）
6. ❌ 不要修改 `moonlight-common-c` 子模块（上游仓库）

## 代码审查要点

- 新增功能是否兼容两种 flavor
- 多语言字符串是否已添加到对应 values 目录
- JNI 代码是否处理异常和内存泄漏
- 新 Activity 是否已注册到 AndroidManifest
- 是否需要更新 ProGuard 规则
