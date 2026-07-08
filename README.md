# JLXC 提词器遥控器桌面版（Windows / macOS）

这是安卓“提词器遥控器 APP”的桌面版控制端，面向 Windows 和 macOS。它不负责显示提词文本，只通过局域网控制另一台手机 / 平板上的 JLXC Teleprompter 提词端。

## 已实现功能

- 输入提词端 IP 和端口连接，默认端口 `47230`
- HTTP `GET /api/ping` 连接检测
- 识别提词端 `scriptUpload=true`，显示是否支持远程发送文稿
- 键盘控制模式
  - `↓` / `→` / `PageDown` / `S`：继续往后滚动
  - `↑` / `←` / `PageUp` / `W`：向前回退
  - 长按连续发送，间隔约 70ms
  - `Space`：快速回到文档开头
  - `T`：回到顶部备用快捷键
  - 全屏防误触界面，底部滑动解锁退出
- 滑动控制模式
  - 全屏滑动控制区
  - 按住鼠标 / 触控板向上滑动：继续往后读
  - 按住鼠标 / 触控板向下滑动：回退
  - 支持鼠标滚轮控制：滚轮向下继续，滚轮向上回退
  - 约 24ms 节流发送，优先 UDP
  - 左侧滑动退出，右侧滑动回到顶部
- UDP 高频滚动
  - `SCROLL 80`
  - `SCROLL -80`
- HTTP 兼容滚动
  - `POST /api/remote/scroll?dy=80`
- 暂停 / 继续
  - UDP：`PAUSE true` / `PAUSE false`
  - HTTP：`POST /api/remote/pause?paused=true`
- 回到顶部
  - UDP：`TOP`
  - HTTP：`POST /api/remote/top`
- 远程发送文稿到提词端
  - JSON：`POST /api/remote/scripts/add`
  - 备用纯文本：`POST /api/remote/scripts/add?title=...`
- 查看提词端文稿列表
  - `GET /api/remote/scripts`
- 设置保存
  - IP
  - 端口
  - 滚动灵敏度
  - 方向反转
  - UDP 开关
  - HTTP 兼容模式开关
- 深色 UI + 青绿色强调色 `#39c5bb`
- 不使用公网服务器，不依赖 Google 服务，不需要账号登录
- 主窗口启动时自动按当前屏幕尺寸适配，禁用横向滚动条，避免打开后内容显示不全

## macOS 版本怎么用

现在工程已经支持通过 GitHub Actions 直接打包 macOS 安装包：

- Apple 芯片版：`macos-apple-silicon-arm64`
- Intel 芯片版：`macos-intel-x64`

因为 `jpackage` 会把 Java 运行时一起打进 `.app` / `.dmg` / `.pkg` 里，所以独立安装包是按 CPU 架构区分的：

- Apple Silicon Mac，下载 `apple-silicon-arm64` 版本。
- Intel Mac，下载 `intel-x64` 版本。
- 如果只运行 `.jar`，一套 Jar 可以跨 Intel / Apple 芯片使用，但电脑上需要安装 Java 17 或更高版本。
- 如果要“直接安装就能用”，推荐发两份 DMG / PKG；这样将来 Apple 芯片环境不再依赖 x86 转译时，也可以直接用 arm64 版本。

## GitHub Actions 打包 macOS DMG / PKG

工程已包含：

```text
.github/workflows/desktop-build.yml
```

推送到 GitHub 后，进入：

```text
Actions → Build Desktop Remote → Artifacts
```

可以下载这些产物：

```text
JLXC-Teleprompter-Desktop-Remote-macos-apple-silicon-arm64-dmg
JLXC-Teleprompter-Desktop-Remote-macos-apple-silicon-arm64-pkg
JLXC-Teleprompter-Desktop-Remote-macos-intel-x64-dmg
JLXC-Teleprompter-Desktop-Remote-macos-intel-x64-pkg
```

同时也会输出：

```text
JLXC-Teleprompter-Desktop-Remote-macos-apple-silicon-arm64-app
JLXC-Teleprompter-Desktop-Remote-macos-intel-x64-app
JLXC-Teleprompter-Desktop-Remote-windows-app-image
```

## macOS 安装方式

DMG：

1. 下载对应架构的 DMG。
2. 双击打开 DMG。
3. 把 `JLXCTeleprompterRemote.app` 拖到“应用程序”。
4. 第一次打开如果提示来自未知开发者，右键点击 App，选择“打开”。

PKG：

1. 下载对应架构的 PKG。
2. 双击运行安装器。
3. 按提示安装。
4. 安装完成后从“应用程序”启动。

当前工程默认生成的是未签名、未公证的安装包。个人测试可以直接用；如果要正式分发，建议后续加 Apple Developer ID 签名和 Notarization 公证。

如果 macOS 拦截得比较严格，可以在终端执行一次：

```bash
xattr -dr com.apple.quarantine /Applications/JLXCTeleprompterRemote.app
```

## 桌面端键盘控制说明

桌面版以键盘控制为主，避免 Windows / macOS 抢占音量键导致系统音量变化。

- `↓` / `→` / `PageDown` / `S`：继续往后滚动
- `↑` / `←` / `PageUp` / `W`：向前回退
- `Space`：快速回到文档开头

部分键盘的系统音量键如果能被 Java/Swing 窗口接收到，仍会兼容发送滚动指令，但界面文案不再把它作为主要操作方式。

## 构建环境

- JDK 17
- Gradle 8.x
- macOS 打 DMG / PKG 时，需要在 macOS 上执行，因为 `jpackage --type dmg/pkg` 只能在 macOS 环境生成 macOS 安装包。

## 本地运行

```bash
gradle clean run --no-daemon --stacktrace
```

## 构建 Jar

```bash
gradle clean build --no-daemon --stacktrace
```

输出位置：

```text
build/libs/
```

## 构建当前系统 app-image

```bash
gradle clean build packageNative --no-daemon --stacktrace
```

输出位置：

```text
build/jpackage/
```

## 在 macOS 本地构建 DMG / PKG

Intel Mac 会打出 Intel x64 包，Apple 芯片 Mac 会打出 arm64 包。

```bash
gradle clean build packageMacAppImage packageMacDmg packageMacPkg --no-daemon --stacktrace
```

或：

```bash
./scripts/build_macos_packages.sh
```

输出位置：

```text
build/jpackage-macos-app/
build/jpackage-macos-dmg/
build/jpackage-macos-pkg/
```

## 协议兼容

连接检测：

```http
GET http://提词器IP:47230/api/ping
```

滚动：

```text
UDP: SCROLL 80
UDP: SCROLL -80
```

```http
POST /api/remote/scroll?dy=80
POST /api/remote/scroll?dy=-80
```

暂停：

```text
UDP: PAUSE true
UDP: PAUSE false
```

```http
POST /api/remote/pause?paused=true
POST /api/remote/pause?paused=false
```

回到顶部：

```text
UDP: TOP
```

```http
POST /api/remote/top
```

新增文稿：

```http
POST /api/remote/scripts/add
Content-Type: application/json; charset=utf-8
```

备用纯文本：

```http
POST /api/remote/scripts/add?title=URL编码标题
Content-Type: text/plain; charset=utf-8
```

查看文稿列表：

```http
GET /api/remote/scripts
```
