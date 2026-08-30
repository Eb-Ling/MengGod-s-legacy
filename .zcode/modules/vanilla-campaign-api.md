# 生涯 API

来源：原版 Starsector。

## 定义

生涯 API 是 Sector、时钟、脚本、插件、UI 和 listener 的原版接口集合。

## 参考

- `com.fs.starfarer.api.EveryFrameScript`：定义生涯按帧脚本入口。
- `com.fs.starfarer.api.EveryFrameScriptWithCleanup`：定义附着实体移除时的清理回调。
- `com.fs.starfarer.api.campaign.BaseCampaignEventListener`：提供生涯事件监听器空实现。
- `com.fs.starfarer.api.campaign.BaseCampaignPlugin`：提供生涯插件 picker 和 memory fact 更新入口。
- `com.fs.starfarer.api.campaign.CampaignClockAPI`：提供生涯日期、timestamp 和实时时间转换入口。
- `com.fs.starfarer.api.campaign.CampaignEventListener`：提供 Sector 级事件上报回调入口。
- `com.fs.starfarer.api.campaign.CampaignUIAPI`：提供生涯消息区、core tab、确认框、航线和 UI 状态入口。
- `com.fs.starfarer.api.campaign.GenericPluginManagerAPI`：提供通用插件注册、移除和按 priority 选择入口。
- `com.fs.starfarer.api.campaign.MessageDisplayAPI`：提供生涯左侧消息区文本显示入口。
- `com.fs.starfarer.api.campaign.PluginPickerAPI`：提供 AI core、fleet stub、immigration 和 inflater 的聚合 picker 入口。
- `com.fs.starfarer.api.campaign.SectorAPI`：提供生涯全局状态、脚本、插件、经济和 memory 入口。
- `com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI`：提供 Sector 通用 listener 注册、移除和按类型查询入口。

## 边界

- `BaseCampaignEventListener` 归属 Sector 事件回调。
- `BaseCampaignPlugin` 归属生涯插件 picker 和 memory fact 更新。
- `CampaignClockAPI` 归属生涯日期、cycle、timestamp、秒到天和秒到月的转换。
- `CampaignEventListener` 归属 Sector 级事件上报回调。
- `CampaignUIAPI` 归属消息区、core tab、确认框、航线、缩放、保存读取命令和当前 UI 状态。
- `CampaignPlugin.isTransient()` 归属插件是否进入存档的保存边界。
- `EveryFrameScript` 归属生涯帧推进脚本。
- `EveryFrameScriptWithCleanup` 归属附着实体移除时的清理回调。
- `GenericPluginManagerAPI` 归属通用插件对象的注册、移除、按类查询和按 priority 选择。
- `ListenerManagerAPI` 归属通用 listener 对象的注册、移除和按接口类型查询。
- `MessageDisplayAPI` 归属生涯 UI 消息区文本的添加和移除。
- `PluginPickerAPI` 归属 AI core、fleet stub、immigration、fleet inflater 等生涯插件聚合选择入口。
- `SectorAPI` 拥有 persistent data、rules、scripts、listeners、generic plugins、economy 和 player memory。
- `SectorAPI.addListener(...)` 注册 Sector 级事件监听器。
- `SectorAPI.addScript(...)` 注册可存档脚本。
- `SectorAPI.addTransientScript(...)` 注册临时脚本。
- `SectorAPI.getClock()` 提供当前生涯时钟入口。
- `SectorAPI.getGenericPlugins()` 提供通用插件管理入口。
- `SectorAPI.getUI()` 提供生涯 UI 外壳入口。
- `SectorAPI.registerPlugin(...)` 注册生涯插件。
- `update...Facts(...)` 归属每次 memory 更新时的短期 fact 写入。

## 链路

### Sector 脚本链路

1. 代码取得 `Global.getSector()`。
2. 调用 `SectorAPI.addScript(script)` 或 `addTransientScript(script)`。
3. 生涯引擎每帧检查 `runWhilePaused()`。
4. 生涯引擎调用 `advance(amount)`。
5. `isDone()` 返回 true 后脚本进入清理。
6. 附着实体移除时 `EveryFrameScriptWithCleanup.cleanup()` 可被调用，但不会被自动调用。
7. 代码可调用 `removeScript(script)` 或 `removeScriptsOfClass(clazz)`。

### 生涯插件链路

1. 代码创建 `CampaignPlugin`。
2. 调用 `SectorAPI.registerPlugin(plugin)`。
3. 生涯系统在对话、声望、AI、autofit、fleet inflater 等选择点调用 picker。
4. 插件返回 `PluginPick<T>` 或 `null`。
5. 规则系统更新 facts 时调用 `update...Facts(...)`。
6. transient 插件在存档载入后需要重新注册。
7. 代码可调用 `unregisterPlugin(pluginId)`。

### 生涯时钟转换链路

1. 代码取得 `Global.getSector()`。
2. 调用 `SectorAPI.getClock()`。
3. 代码读取 `getCycle()`、`getMonth()`、`getDay()`、`getHour()` 或 `getTimestamp()`。
4. 帧推进逻辑把秒数传给 `convertToDays(amount)` 或 `convertToMonths(amount)`。
5. 需要基于 timestamp 计算间隔时调用 `getElapsedDaysSince(timestamp)`。

### 通用插件选择链路

1. 代码取得 `SectorAPI.getGenericPlugins()`。
2. 调用 `GenericPluginManagerAPI.addPlugin(plugin, isTransient)` 注册通用插件。
3. 生涯系统按插件类调用 `pickPlugin(clazz, params)`。
4. 插件通过 `getHandlingPriority(params)` 返回处理优先级。
5. 选中的插件实例进入对应生涯功能入口。
6. `PluginPickerAPI` 在 AI core、fleet stub、immigration 和 fleet inflater 场景返回聚合选择结果。

### 生涯 UI 外壳链路

1. 代码取得 `SectorAPI.getUI()`。
2. 调用 `CampaignUIAPI.addMessage(...)` 或 `getMessageDisplay().addMessage(...)` 写入消息区。
3. 调用 `showCoreUITab(tab, custom)` 打开 core UI 页签。
4. 调用 `showConfirmDialog(...)` 创建确认框。
5. 调用航线、缩放、保存读取和显示状态方法读取或改变 UI 外壳状态。

### 监听器注册链路

1. 代码取得 `Global.getSector()` 或 `SectorAPI.getListenerManager()`。
2. 调用 `addListener(...)`、`addTransientListener(...)` 或 `ListenerManagerAPI.addListener(...)`。
3. Sector 事件发生时按接口类型分发到已注册 listener。
4. 代码可调用 `removeListener(...)` 或 `removeListenerOfClass(...)` 移除 listener。

## 规范

- `BaseCampaignPlugin.isTransient()` 默认返回 true。
- `BaseCampaignPlugin` 的 picker 默认返回 null。
- `CampaignClockAPI.convertToDays(amount)` 接收实时时间秒数并返回生涯天数。
- `CampaignUIAPI.getCurrentCoreTab()` 返回当前 core UI 页签。
- `GenericPluginManagerAPI` 的 priority 数值越高，处理优先级越高。
- `PluginPickerAPI.pickAICoreOfficerPlugin(...)` 按 commodity id 返回 AI core officer plugin。
- `EveryFrameScript.advance(amount)` 的 amount 是上一帧秒数。
- `EveryFrameScript.isDone()` 返回 true 后脚本可被生涯引擎清理。
- `EveryFrameScript.runWhilePaused()` 控制暂停时推进。
- `EveryFrameScriptWithCleanup.cleanup()` 在附着实体被移除时调用。
- `ListenerManagerAPI.getListeners(clazz)` 按传入接口或类返回已注册 listener。
- `SectorAPI.addTransientScript(...)` 注册的脚本通过 transient script 集合管理。
- `SectorAPI.getEconomy()` 返回生涯经济入口。
- `SectorAPI.getMemoryWithoutUpdate()` 返回 global memory。
- `SectorAPI.getPersistentData()` 可用于跨会话保存模组级数据。
- `SectorAPI.getPlayerMemoryWithoutUpdate()` 等同玩家角色 memory。

## 陷阱

- `CampaignClockAPI.getTimestamp()` 表示生涯时钟时间点，跨存档保存时应按 timestamp 计算间隔。
- `CampaignUIAPI.showConfirmDialog(...)` 只表达 UI 确认框，确认后的行为由传入 `Script` 执行。
- `CampaignPlugin.update...Facts(...)` 注释要求写入过期时间为 `0` 的临时变量。
- `EveryFrameScriptWithCleanup.cleanup()` 不会被自动调用，需要手动调用。
- `GenericPluginManagerAPI.GenericPlugin.getHandlingPriority(params)` 返回负数时表示该插件不处理本次参数。
- `ListenerManagerAPI.addListener(listener, true)` 注册 transient listener，存档载入后需要重新注册。
- `SectorAPI.addScript(...)` 注册的是可存档脚本，临时推进逻辑应使用 transient script 入口。
- `SectorAPI.getPersistentData()` 保存运行时对象时要考虑 XStream 存档边界。
