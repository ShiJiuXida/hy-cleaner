# 欢游净化

欢游（`com.sabac.hy`）的 LSPosed 界面精简模块，基于 libxposed **API 102**。

把不想看到的入口、广告位、推广横幅收掉，不改动任何数据。

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
  Installer.kt        hook 安装工具
  Windows.kt          窗口根视图枚举
feature/
  HomeFeatures.kt     主页 6 项
  MessageFeatures.kt  消息 2 项
  MineFeatures.kt     我的 2 项
  RoomFeatures.kt     房间 4 项
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

## 构建

需要 JDK 21、Android SDK（compileSdk 37、build-tools 37.0.0）。

```bash
gradle :app:assembleDebug
```

产物在 `app/build/outputs/apk/debug/`。

> arm64 设备本地构建时 `aapt2` 只有 x86_64 版本，需要 `qemu-user-static` + binfmt。

## 安装

1. 装 APK，在 LSPosed 里启用模块
2. 作用域勾选 **欢游**（`com.sabac.hy`）
3. 强制停止欢游再打开

改完开关后在欢游里切一次页面就会重读配置，不用重启。

## 日志

LSPosed 管理器 → 日志，标记 `HYClean`。模块 App 自身看不到这些日志。

## 版本

规则基于欢游 **2.88.5 (19623)**。版本变化后某个开关失效时，
看日志里的 `规则异常` / `id 找不到` 能定位到是哪一项。

## 许可

仅供个人学习使用。不修改欢游本体，不涉及绕过任何付费或权限校验。
