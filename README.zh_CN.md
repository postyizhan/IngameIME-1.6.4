# IngameIME for Minecraft Forge 1.6.4

IngameIME 是一个让 Minecraft 支持输入法输入的客户端 Mod。本分支将 IngameIME 的核心功能移植到 **Minecraft 1.6.4 + Forge 9.11.1.1345**，用于在窗口化或全屏 Minecraft 中输入中文等需要 IME 的文字。

这个 1.6.4 移植版使用 Forge 早期的 coremod/ASM 注入方式，而不是新版 IngameIME 使用的 Mixin 架构。

## 当前状态

当前范围：**核心稳定版 1.6.4 移植**。

本移植版已验证：

- Minecraft Forge 1.6.4 客户端可正常加载
- jar manifest 中的 coremod transformer 可正常加载
- Windows 原生 IME 动态库可正常加载
- TSF/Imm32 输入上下文可创建、激活、关闭和复用
- 原版 `GuiTextField` 输入可用，包括聊天框和创造模式搜索框
- 告示牌输入可用
- 可书写书本输入可用
- 拼音/预编辑文本可在光标附近显示
- 默认使用 Windows 原生候选框
- 全屏/窗口切换时会重置 IME 上下文
- 已过滤控制字符，创造模式搜索框按 ESC/回车不会再插入异常字符

当前 1.6.4 MVP **不包含**：

- 1.12.2 版本中的主题编辑器和配置 GUI
- 大范围第三方 Mod 文本框兼容层
- 新版 Mixin 资源和 Mixin 架构回迁

## 环境要求

- Windows
- Minecraft `1.6.4`
- Forge `1.6.4-9.11.1.1345`
- Java 8（用于构建和运行旧版 Forge 客户端）

本项目编译时仍使用 Java 7 字节码：

```text
sourceCompatibility = 1.7
targetCompatibility = 1.7
```

原因是 Forge 1.6.4 在 Mod 扫描阶段使用 ASM 4.1，无法可靠解析 Java 8 class 文件。

## 构建

请使用 Java 8，不要使用 Java 21 或更新版本：

```bash
JAVA_HOME='C:/Program Files/Zulu/zulu-8' ./gradlew clean assemble --console=plain --no-daemon
```

构建产物：

```text
build/libs/IngameIME-1.0.0-1.6.4.jar
```

开发环境启动客户端：

```bash
JAVA_HOME='C:/Program Files/Zulu/zulu-8' ./gradlew runClient --console=plain --no-daemon
```

## 安装

将发布 jar 复制到 Forge 1.6.4 实例的 `mods/` 目录：

```text
IngameIME-1.0.0-1.6.4.jar
```

jar manifest 中已经包含 coremod 所需属性：

```text
FMLCorePlugin: com.dhj.ingameime.core.IngameIMECorePlugin
FMLCorePluginContainsFMLMod: true
ForceLoadAsMod: true
```

## 配置

配置文件生成位置：

```text
config/ingameime.cfg
```

重要配置项示例：

```text
S:Windows=TextServiceFramework
B:Windows=false
B:DebugLog=false
B:VerboseLog=false
```

推荐配置：

- `S:Windows=TextServiceFramework`：使用 Windows TSF 后端。
- `B:Windows=false`：这里指 `uiless` 分类下的 `Windows` 配置项。`false` 表示使用 Windows 原生候选框，同时由 IngameIME 在游戏内绘制预编辑文本。
- `B:DebugLog=false`、`B:VerboseLog=false`：日常使用建议关闭；排查问题时再打开。

`uiless` 分类下的 `B:Windows=true` 会请求 IngameIME 在游戏内自行绘制候选框。它可作为全屏或原生候选框异常时的备用方案，但本移植版默认推荐使用 Windows 原生候选框。

## 功能说明

### 原版文本框

支持原版 `GuiTextField`，包括：

- 聊天输入框
- 创造模式物品搜索框
- 其他基于原版 `GuiTextField` 的界面

创造模式搜索框已经额外处理 ESC 和回车：

- 按 ESC 不应插入控制字符或 `esc` 文本
- 按回车不应插入控制字符或 `cr` 文本
- 正常中文输入仍应提交到搜索框

### 告示牌和书本

本移植版包含 1.6.4 专用控制器：

- `SignControl`：支持告示牌输入
- `BookControl`：支持可书写书本输入

### 游戏内输入状态

当没有 GUI 打开时，IngameIME 会主动让原生输入上下文保持非激活状态，避免中文输入法吞掉 WASD 等游戏控制键。

## 维护者说明

- `IngameIMETransformer` 用 LaunchWrapper 时代的 ASM 注入替代新版 Mixin hook。
- `Internal` 负责原生库加载、HWND 获取、IME 激活状态、回调队列、commit 过滤和游戏状态下强制非激活。
- 当前 MVP 支持的控制器是：
  - `VanillaTextFieldControl`
  - `SignControl`
  - `BookControl`
- `ChatAllowedCharacters` 被补丁修改为允许非 ASCII 文本，同时拒绝 ESC、回车等控制字符。
- 请保持输出 class 文件兼容 Java 7。
- 不要直接引入 1.12.2 的 Mixin 架构；1.6.4 端口应继续使用现有 coremod/ASM 路线。

## Credits

基于 IngameIME 项目及其原生 JNI 绑定。mod 元数据中列出的致谢：

- Windmill_City
- DHJComical
- RuiXuqi
- Andrea Frederica
- Circulate233
