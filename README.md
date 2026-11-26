# Mirror

欢游（`com.sabac.hy`）的 LSPosed 界面精简模块，基于 libxposed **API 102**。

把不想看到的入口、广告位、推广横幅收掉，不改动任何数据。

包名 `com.halo.mirror`、应用名 `Mirror` 刻意中性，不在包列表/应用列表里直接暴露用途。

## 设计取舍

欢游的 dex 被加固壳保护，静态拿不到业务类，也没有稳定方法可 hook。
所以模块只 hook **框架层**（`Activity` / `Dialog` / `PopupWindow` / `View`），
拿到已建好的 View 树，按资源 id 与文案定位目标后收掉。

跨版本相对稳（布局 id 不变就一直有效），代价是要和布局时机打交道。

## 两个关键约束

### 1. 列表项必须清尺寸，普通容器不能清

`RecyclerView` 的 `LayoutManager` 对 `GONE` 子项**照样测量排布** ——
列表项只 `GONE` 会留下原尺寸空白（「广告没了但位置还在」）。

反过来，自定义 TabLayout 那种高度写死的行，清了尺寸会整行空掉。

所以只有两种动作，按目标是不是列表项严格区分：

| 动作 | 用途 | 行为 |
|---|---|---|
| `hide` | 普通容器里的部件 | 只 `GONE` |
| `drop` / `dropFully` | 列表项 | `GONE` + 清零对应方向尺寸 |

### 2. 扫描对象是「所有窗口的根视图」

不能用 `Activity.getWindow().getDecorView()`：欢游会把页面内容挂在额外窗口里，
只扫 Activity 的 decor 会拿到**空列表**（子项 0），广告永远收不掉。

统一走 `WindowManagerGlobal.mViews`（`hook/Windows.kt`），拿不到再退回 decor。

## 防闪烁

切页 / 列表复用时欢游会先把控件设回 `VISIBLE`，等下一帧才被清掉，中间那帧就露出来了。
三层一起用：

1. **`View.setVisibility` 设卡**（`UiHooks.tryVisibilityHook`）——
   名单里的控件一旦被设成可见立刻改回 `GONE`，根本没机会显示。治本的一层。
2. **`RecyclerView.onLayout` 返回后清扫** —— 列表项在绘制之前收掉。
3. **每个根视图的 `onPreDraw`** —— 不节流，切页那一帧也要赶上。

再加 500ms 常驻循环兜底：屏幕静止时没有绘制回调，只靠 preDraw 会漏掉
「加载完成后才出现的列表」。循环在模块加载时启动，不随页面切换而停。

> 名单（`Feature.blockIds`）只放「一个 id 只服务一种用途」的控件。
> `item_layout_root`、`tab_filter_item_root` 这类被大量复用的 id 放进去会把正常内容一起收掉。

## 减少被检测的痕迹

模块本身是被注入进程里的一段外来代码，目标应用若主动排查是能看到痕迹的。
**LSPosed 负责隐藏注入与栈帧，但不负责隐藏「方法被改过」**，所以下面这些是模块侧能做的：

| 措施 | 做法 | 效果 |
|---|---|---|
| 关日志 | `HyLog.ENABLED = BuildConfig.DEBUG`，release 里 R8 折算掉全部分支，并用 `-assumenosideeffects` 兜底 | logcat 里不存在任何日志字面量。目标应用持有 `READ_LOGS`，日志就是自曝 |
| 类名混淆 | release 开 R8 | 入口 `HyModule` 混淆成两个字母，`feature.*` / `hook.*` 全部消失 |
| 诊断代码不进包 | `Probe` 加 `-assumenosideeffects` | 排错用的树导出逻辑不会残留 |
| 不调 `deoptimize` | `Installer` 默认跳过，只在 `DEOPT_TARGETS` 里的方法才调 | 方法不脱离 AOT 优化、entrypoint 不改，抹掉最直接的「方法被动过」证据 |
| 中性命名 | 包名 / 应用名 / prefs 分组名都不带 cleaner、hook 字样 | 关键词扫描失效 |

**不要加 `-adaptclassstrings`。** 它会把「看起来像类名」的字符串一起改写，而 AIDL
的接口描述符就是这样一个字符串：`io.github.libxposed.service.IXposedService` 被改成
混淆名之后，模块 App 调框架的 `getRemotePreferences` 时 binder 里 `enforceInterface`
对不上（logcat 会报 `expected '...IXposedService' but read 'du'`），异常被吞掉，
界面就误报成「写入失败」。服务库的类名必须原样保留，见 `proguard-rules.pro`。

构建产物验证（`dexdump -s` 搜索关键字全为 0）：

```
HYClean: 0    已挂载: 0    收起: 0    cleaner: 0    hy_cleaner: 0
```

> 剩下的痕迹只有目标包名 `com.sabac.hy`（必须留着才能匹配），以及框架自身暴露的东西。

### 做不到的

- 无法让 `ArtMethod` 完全看不出被 hook：只要 hook 了框架方法就一定留下 entrypoint 改动。
- 无法对抗主动式反调试/反注入（易盾那类 SO 层检测）：模块跑在 Java 层，够不到那边。
- 无法隐藏模块 APK 在 `/proc/self/maps` 里的映射：那是框架的行为。

## 代码结构

```
core/
  Keys.kt             全局常量（分组名、总开关）
  HyLog.kt            日志，只走框架
  Settings.kt         Settings 只读快照 + Live 节流读 remote preferences
  Feature.kt          Feature 接口 + 分组枚举
  FeatureRegistry.kt  全部功能的唯一清单
  Process.kt          进程名，把 hook 限制在界面进程
view/
  Dom.kt              View 树查询（只读）
  Mutate.kt           View 变更（hide / drop / restore）
  Block.kt            可见性拦截名单
  Layouts.kt          ConstraintLayout 反射（底栏平分用）
hook/
  Hooks.kt            编排：常驻循环 + 装配
  UiHooks.kt          页面 / 弹层 / 列表 / 可见性 / 手势
  PopupGuard.kt       弹窗拦截（Dialog.show 设卡 + 已显示窗口清扫）
  Installer.kt        hook 安装工具
  Windows.kt          窗口根视图枚举
feature/
  HomeFeatures.kt     主页 6 项
  MessageFeatures.kt  消息 2 项
  MineFeatures.kt     我的 2 项
  RoomFeatures.kt     房间 4 项
  PopupFeatures.kt    弹窗 2 项（更新提示 / 登录抽奖广告）
diag/
  Probe.kt            诊断输出（面板项 / 窗口概况 / 整树导出）
Sweep.kt              一次清扫，按配置逐条执行
HyModule.kt           模块入口
Bridge.kt             模块 App 与框架（写配置）
ui/                   设置界面（Compose）
```

**加功能** = 写一个 `Feature` 实现 + 在 `FeatureRegistry` 加一行。
设置界面、清扫流程、可见性名单都从注册表读，不需要别处同步。

## 功能清单

| 分组 | 功能 |
|---|---|
| 主页 | 顶部「娱乐」页签、顶栏右侧抽奖位、入口整排、为你推荐、滚动横幅、底栏广场与开房 |
| 消息 | 「我的房间」右侧频道卡、好友印记 / 活动中心 |
| 我的 | 贵族特权、开通超级玩家 |
| 房间 | 全服公告条、输入栏右侧广告、三条杠底部付费入口、麦位名称上方广告 |
| 弹窗 | 版本更新提示、登录抽奖广告 |

### 弹窗和别的功能不是一回事

其余功能都是「找到控件收起来」，弹窗是独立窗口 —— 只把控件 `GONE` 会留下一层
挡住点击的透明窗口，所以弹窗走 `hook/PopupGuard.kt`：

| 时机 | 动作 |
|---|---|
| `Dialog.show()` 之前 | 命中规则就**不调用原方法**，弹窗连一帧都不出现 |
| 常驻 500ms 循环 | 扫所有窗口根，对「已经显示出来的」实例点它自己的关闭按钮 |

第二条是为了覆盖「模块装上之前就弹出来」的情况（热重载后立刻生效）。
不用 `dismiss()` 也不用从 `WindowManagerGlobal` 删窗口：前者拿不到 Dialog 实例，
后者会让应用之后的 `dismiss()` 抛 `View not attached`。点关闭按钮走的是应用自己的路径。

识别方式：更新提示按文案（`版本更新` + `立刻更新` 一类按钮）认，抽奖广告按 id
（`loginLotteryLayout` + `loginLotteryClose`）认 —— 主页顶栏那个抽奖入口是另一个 id
（`loginLotteryEntranceLayout`），不会误伤。

## 安装

1. 装 APK（包名 `com.halo.mirror`），在 LSPosed 里启用模块
2. 作用域勾选 **欢游**（`com.sabac.hy`）
3. 强制停止欢游再打开

改完开关后在欢游里切一次页面就会重读配置，不用重启。

### 模块本身更新也不用重启欢游

`module.prop` 里开着 `autoHotReload=true`，LSPosed 在模块 APK 更新后会带着新代码
重新进 `onHotReloaded`。这里不能只摘旧 hook —— 摘完要用新 classLoader 重装一遍
（见 `HyModule.onHotReloaded`），否则热重载等于把模块卸掉。

热重载之后 `Hooks.install` 会立刻扫一遍窗口，所以「更新前就已经弹在屏幕上」的
目标弹窗也会被直接关掉，不用等下次启动。

## 日志

**release 包没有日志**（见上面「减少被检测的痕迹」）。要排错只能自己编一个不混淆的 debug 变体。

拿到之后看 LSPosed 管理器 → 日志，标记 `Mirror`。模块 App 自身看不到这些日志。

## 版本

规则基于欢游 **2.88.5 (19623)**。版本变化后某个开关失效时，
看日志里的 `规则异常` / `id 找不到` 能定位到是哪一项。

## 赞赏支持

如果这个模块对你有帮助，欢迎请作者喝杯奶茶 ₍ᐢ..ᐢ₎♡

<p align="center">
  <img src="donate/donate_pair.png" width="400" alt="赞赏码" />
</p>

## 许可

仅供个人学习使用。不修改欢游本体，不涉及绕过任何付费或权限校验。
