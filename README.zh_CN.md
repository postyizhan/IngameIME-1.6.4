# IngameIME for Minecraft Forge 1.6.4

[[English](README.md) / 简体中文]

IngameIME 是一个让 Minecraft 支持输入法输入的客户端 Mod。本分支将 [IngameIME-1.12.2](https://github.com/DHJComical/IngameIME-1.12.2) 的核心功能移植到 **Minecraft 1.6.4 + Forge 9.11.1.1345**

- **✓** 中文输入
- **✓** 输入法候选框
- **✓** 小窗/全屏支持
- **✓** 不卡输入法

>当没有 GUI 打开时，IngameIME 会自动停用原生输入上下文，避免中文输入法拦截WASD、空格等游戏按键。因此即使系统当前处于中文输入法模式，关闭聊天框或其他输入界面后也可以正常移动、跑动和操作，不会出现按键被输入法吞掉的情况。

## 展示

- **请求 IngameIME 在游戏内自行绘制候选框和预编辑文本（默认）**

配置文件中设置：

```
uiless {
    # Config if render in-game candidate list.
    B:Windows=true
}
```

![](show-1.png)

- **使用 Windows 原生候选框**

配置文件中设置：

```
uiless {
    # Config if render in-game candidate list.
    B:Windows=false
}
```

![](show-2.png)

## 当前状态

本移植版已验证：

- Minecraft Forge 1.6.4 客户端可正常加载
- jar manifest 中的 coremod transformer 可正常加载
- Windows 原生 IME 动态库可正常加载
- TSF/Imm32 输入上下文可创建、激活、关闭和复用
- 原版 `GuiTextField` 输入可用，包括聊天框和创造模式搜索框
- 告示牌输入可用
- 可书写书本输入可用
- 拼音/预编辑文本可在光标附近显示
- 默认在游戏内自行绘制候选框
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

> 此处假设你使用 zulu-8 并使用默认安装路径

请使用 Java 8，不要使用 Java 21 或更新版本：

```bash
JAVA_HOME='C:/Program Files/Zulu/zulu-8' ./gradlew clean assemble --console=plain --no-daemon
```

构建产物：

```text
build/libs/IngameIME-x.x.x-1.6.4.jar
```

开发环境启动客户端：

```bash
JAVA_HOME='C:/Program Files/Zulu/zulu-8' ./gradlew runClient --console=plain --no-daemon
```

本仓库已内置 CME 补丁启动器，`runClient` / `runServer` 在 Java 8u20 及以上的 JVM 上可直接运行，无需手动往 `runs/main/client/mods/` 放 LegacyJavaFixer。详见下文「开发环境的 CME 补丁」。

### 开发环境的 CME 补丁（`src/launchPatch/`）

在 **Java 8u20 及以上**的 JVM 上，原版 1.6.4 启动链必崩于：

```text
java.util.ConcurrentModificationException
    at java.util.ArrayList$Itr.checkForComodification(ArrayList.java:911)
    at java.util.ArrayList$Itr.remove(ArrayList.java:875)
    at net.minecraft.launchwrapper.Launch.launch(Launch.java:114)
```

成因是三方共同作用：

1. launchwrapper 1.8 的 `Launch.launch()` 在循环体内调 `tweaker.acceptOptions(...)`，紧接着调 `it.remove()`——迭代器跨回调存活。
2. 1.6.4 FML 的 `CoreModManager.sortTweakList()` 里是裸的 `Collections.sort(tweakers, cmp)`，而这个 `tweakers` 正是上面那个迭代器遍历的 list。
3. [JDK-8030848](https://bugs.openjdk.org/browse/JDK-8030848) 在 **8u20** 改了 `Collections.sort`：此前是拷进临时数组排完写回、不动 `modCount`，改成委托 `List.sort()` 后，`ArrayList.sort()` 结尾无条件 `modCount++`。

于是 `it.remove()` 在 `checkForComodification()` 抛 CME。分界线是 8u20 而非“Java 8”，早期 8u5 之类是能跑的。Forge 在 1.7.10 时代修了（`sortTweakList` 改用 `Arrays.sort` + `set` 回写），1.6.4 已停止维护没拿到。

本仓库在 launchwrapper 一侧修：`src/launchPatch/java/dev/launchfix/CmeSafeLaunch.java` 是 `Launch` 的等价实现，把「持迭代器跨回调」改成「每轮取列表头、处理完按引用移除」，回调里怎么排序增删都不会炸。接入方式：

```groovy
sourceSets.register('launchPatch')
sourceSets.launchPatch.compileClasspath = sourceSets.main.compileClasspath
sourceSets.main.runtimeClasspath += sourceSets.launchPatch.output

// run 块内
environment.put("mainClass", "dev.launchfix.CmeSafeLaunch")
```

三个细节：

- **为什么改 env 的 `mainClass` 而不是 run DSL 的 `mainClass`**：slime-launcher 的 `LegacyDev` 只在外层 main 以 `net.minecraftforge.legacydev.` 开头时才生效，由它读 env `mainClass` 取真实入口，并负责补 `--tweakClass`/`--version`/`--assetsDir` 等参数。改 DSL 的 `mainClass` 会绕过整条 legacydev 路径，导致 `--tweakClass` 丢失、退回 `VanillaTweaker`。
- **为什么挂到 `main.runtimeClasspath`**：run 任务的 JVM classpath = slime-launcher 工具 jar + `main.runtimeClasspath`。run DSL 的 `classpath` 是**整体覆盖**语义，用它会把 slime-launcher 自己挤掉，报找不到主类 `net.minecraftforge.launcher.Main`。
- **为什么用独立 sourceSet**：`jar` 任务打的是 `sourceSets.main.output`，补丁在 `launchPatch` 里，两者输出目录分离，**不会进发布 jar**（已验证）。追加 `runtimeClasspath` 只影响 run/test，不是 `jar` 的输入。顺带也避免 FML 在 dev 下把它当 mod 类扫描。

**发布须知**：本补丁只覆盖开发环境的 `runClient`/`runServer`。玩家在生产 Forge 上用 8u20+ 运行本 mod 时，仍需 [`legacyjavafixer-1.0.jar`](https://github.com/MinecraftForge/LegacyJavaFixer)（它在 FML 一侧替换 `sortTweakList`，与本补丁修的是同一个 bug 的两端）。

## 配置

配置文件生成位置：

```text
config/ingameime.cfg
```

配置文件采用 Forge 1.6.4 的 `Configuration` 格式：

- `S:` 表示字符串（String）
- `B:` 表示布尔值（Boolean），只能填 `true` 或 `false`
- `I:` 表示整数（Integer）
- 颜色使用 ARGB 十六进制格式，例如 `0xFF000000`

<details>
<summary>完整配置文件示例（点击展开）</summary>

```cfg
# Configuration file

####################
# api
####################

api {
    # Windows 平台使用的输入法 API。
    # 可选值：
    # - TextServiceFramework：推荐，使用 TSF，适合现代 Windows 输入法。
    # - Imm32：旧式 IMM32 后端，作为兼容性备用。
    S:Windows=TextServiceFramework
}


####################
# debug
####################

debug {
    # 是否输出普通调试日志。
    # 日常使用建议 false；排查问题时可改为 true。
    B:DebugLog=false

    # 是否输出更详细的排障日志。
    # 会记录 IME 回调、候选框、预编辑文本、光标位置等信息；日志较多。
    # 日常使用建议 false；只有需要提交日志或定位问题时再打开。
    B:VerboseLog=false
}


####################
# general
####################

general {
    # 鼠标移动时是否自动关闭输入法。
    # true：移动鼠标后关闭 IME。
    # false：移动鼠标不影响 IME 状态，推荐默认值。
    B:TurnOffOnMouseMove=false
}


####################
# modetext
####################

modetext {
    # 英文/字母模式提示文字。
    S:AlphaMode=A

    # 中文/本地输入模式提示文字。
    S:NativeMode=中
}


####################
# theme
####################

theme {
    # 预编辑文本/候选框背景色，ARGB。
    S:BackgroundColor=0xEBEBEBEB

    # 边框颜色，ARGB。
    S:BorderColor=0x80000000

    # 边框宽度，单位为游戏 GUI 像素。
    I:BorderWidth=1

    # 候选框内部边距，单位为游戏 GUI 像素。
    I:CandidatePadding=5

    # 预编辑光标颜色，ARGB。
    S:CursorColor=0xFF000000

    # 候选序号颜色，ARGB。
    S:IndexColor=0xFF555555

    # 普通控件内部边距，单位为游戏 GUI 像素。
    I:Padding=3

    # 当前选中候选项背景色，ARGB。
    S:SelectedBackgroundColor=0xEBEBEBEB

    # 文本颜色，ARGB。
    S:TextColor=0xFF000000
}


####################
# uiless
####################

uiless {
    # Windows 候选框渲染模式。
    # true：默认值。请求 IngameIME 在游戏内自行绘制候选框和预编辑文本。
    # false：使用 Windows 原生候选框，同时由 IngameIME 在游戏内绘制预编辑文本；可作为游戏内候选框异常时的备用方案。
    B:Windows=true
}
```

</details>

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
- 请保持输出 class 文件兼容 Java 7。字节码上限是 Java 7（class 版本 51），因为 1.6.4 FML 内置 ASM 4.1 解析不了 Java 8 字节码（52），会抛 `IllegalArgumentException` 并丢弃整个 mod。这条约束只针对**编译产物**，与跑 Gradle 用的 JDK 无关。
- 不要直接引入 1.12.2 的 Mixin 架构；1.6.4 端口应继续使用现有 coremod/ASM 路线。
- `src/launchPatch/` 只服务于开发环境运行，不会打进发布 jar，改动时注意别把它挪进 `src/main/`。

## 常见问题

**启动时报 `ConcurrentModificationException` at `net.minecraft.launchwrapper.Launch.launch`**
用的 JVM 是 8u20+ 且没走到 CME 补丁启动器。检查 `build.gradle` 的 run 块里是否有 `environment.put("mainClass", "dev.launchfix.CmeSafeLaunch")`，以及 `sourceSets.main.runtimeClasspath += sourceSets.launchPatch.output` 是否还在。

**`runServer` 报 `UnsatisfiedLinkError: no lwjgl in java.library.path`**
server run 块的 `tweakClass` 写成了客户端的 `FMLTweaker`。它的 `getLaunchTarget()` 返回 `net.minecraft.client.main.Main`，于是 `runServer` 实际去启动了客户端，倒在 LWJGL 原生库上。应为 `FMLServerTweaker`。

**FML 日志报 `Unable to read a class file correctly` / `IllegalArgumentException`**
classpath 上有 Java 8+ 字节码的 jar，ASM 4.1 读不了。FML 会跳过该条目继续，通常不致命；若丢的是本 mod 自己的类，检查 `-source/-target` 是否被提到了 1.8。

## Credits

基于 IngameIME 项目及其原生 JNI 绑定。mod 元数据中列出的致谢：

- Windmill_City
- DHJComical
- RuiXuqi
- Andrea Frederica
- Circulate233
