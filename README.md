# JLXC 提词器遥控器桌面版（Windows / macOS）

这是安卓“提词器遥控器 APP”的桌面版控制端，面向 Windows 和 macOS。它不负责显示提词文本，只通过局域网控制另一台手机 / 平板上的 JLXC Teleprompter 提词端。

## 已实现功能

- 输入提词端 IP 和端口连接，默认端口 `47230`
- HTTP `GET /api/ping` 连接检测
- 识别提词端 `scriptUpload=true`，显示是否支持远程发送文稿
- 键盘 / 音量键遥控模式
  - `↓` / `PageDown` / `S` / 音量下：继续往后滚动
  - `↑` / `PageUp` / `W` / 音量上：向前回退
  - 长按连续发送，间隔约 70ms
  - `Space`：暂停 / 继续
  - `T`：回到顶部
  - 全屏防误触界面，底部滑动解锁退出
- 虚拟触控板模式
  - 全屏触控板
  - 鼠标 / 触控板向上滑动：继续往后读
  - 鼠标 / 触控板向下滑动：回退
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

## 桌面端关于“音量键”的说明

Windows / macOS 对音量键的处理和 Android 不一样。桌面版会尝试接收系统传递到窗口的音量键码：

- Volume Down：继续往后滚动
- Volume Up：向前回退

但部分键盘或系统会优先把音量键交给系统音量服务，Java/Swing 窗口无法 100% 阻止系统音量变化。因此桌面版同时提供稳定的键盘替代：

- `↓` / `PageDown` / `S`：继续往后滚动
- `↑` / `PageUp` / `W`：向前回退

## 构建环境

- JDK 17
- Gradle 8.x

## 本地运行

```bash
gradle clean run --no-daemon --stacktrace
```

## 构建可执行分发包

构建 Jar：

```bash
gradle clean build --no-daemon --stacktrace
```

构建 Windows / macOS app-image：

```bash
gradle clean build packageNative --no-daemon --stacktrace
```

输出位置：

- Jar：`build/libs/`
- app-image：`build/jpackage/`

## GitHub Actions

项目已包含：

```text
.github/workflows/desktop-build.yml
```

Actions 会在：

- `windows-latest`
- `macos-latest`

分别执行：

```bash
gradle clean build packageNative --no-daemon --stacktrace
```

然后上传：

- Jar artifact
- Windows / macOS app-image artifact

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

{
  "title": "文稿标题",
  "content": "完整提词内容"
}
```

备用新增文稿：

```http
POST /api/remote/scripts/add?title=URL编码后的标题
Content-Type: text/plain; charset=utf-8

完整提词内容
```

文稿列表：

```http
GET /api/remote/scripts
```
