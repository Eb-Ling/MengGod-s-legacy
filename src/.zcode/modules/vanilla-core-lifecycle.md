# 核心生命周期

来源：原版 Starsector。

## 定义

核心生命周期 API 是模组 Java 入口、全局服务访问和运行时插件选择的原版接口集合。

## 参考

- `com.fs.starfarer.api.BaseModPlugin`：提供 `ModPlugin` 的空实现基类。
- `com.fs.starfarer.api.Global.getCombatEngine()`：提供当前战斗引擎引用。
- `com.fs.starfarer.api.Global.getSector()`：提供当前生涯 Sector 引用。
- `com.fs.starfarer.api.Global.getSettings()`：提供已加载设置、资源、spec 和脚本类加载器。
- `com.fs.starfarer.api.Global`：提供 settings、sector、combat engine、logger、sound player 和 factory 的静态访问入口。
- `com.fs.starfarer.api.ModPlugin`：定义模组实例从应用加载到 Codex 生成的生命周期回调。
- `com.fs.starfarer.api.PluginPick`：携带被选择插件实例和 `PickPriority`。
- `com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority`：定义 picker 返回值的优先级语义。
- `com.fs.starfarer.api.SettingsAPI.getInstanceOfScript(...)`：按完整类名创建脚本对象。
- `com.fs.starfarer.api.SettingsAPI.getScriptClassLoader()`：提供脚本类加载器。
- `mod_info.json`：通过 `modPlugin` 字段声明模组入口类。

## 边界

- Codex 生成阶段由 `onAboutToStartGeneratingCodex()`、`onAboutToLinkCodexEntries()` 和 `onCodexDataGenerated()` 表达。
- `BaseModPlugin` 负责为新增 `ModPlugin` 方法提供默认实现。
- `Global` 保存当前应用注入的 settings、sector、factory、sound player 和 combat engine 引用。
- `Global.getCombatEngine()` 的有效语义归属战斗运行期。
- `Global.getSector()` 的有效语义归属生涯运行期。
- `Global.getSettings()` 的有效语义覆盖应用加载后所有可读取资源。
- `ModPlugin` 实例由游戏创建，入口类路径来自 `mod_info.json`。
- `PluginPick` 只表达本次 picker 调用的选择结果。
- `SettingsAPI` 在本模块只作为 `Global.getSettings()` 返回的应用级服务对象。
- `configureXStream()` 归属存档序列化类型配置。
- `onAboutToStartGeneratingCodex()`、`onAboutToLinkCodexEntries()` 和 `onCodexDataGenerated()` 归属 Codex 数据生成阶段。
- `onApplicationLoad()` 归属应用级资源、外部数据和静态初始化。
- `onEnabled(boolean wasEnabledBefore)` 归属启用状态迁移。
- `onGameLoad(boolean newGame)` 归属存档载入后的生涯状态接入。
- `onNewGame()`、`onNewGameAfterProcGen()`、`onNewGameAfterEconomyLoad()` 和 `onNewGameAfterTimePass()` 归属新游戏生成阶段。
- `pickShipAI()`、`pickWeaponAutofireAI()`、`pickDroneAI()` 和 `pickMissileAI()` 归属运行时 AI 替换。

## 链路

### 应用加载链路

1. 游戏读取 `mod_info.json`。
2. 游戏按 `modPlugin` 创建 `ModPlugin` 实例。
3. 核心资源与脚本编译完成。
4. 游戏调用 `onApplicationLoad()`。
5. 脚本可通过 `Global.getSettings()` 读取已加载的设置和资源。

### 新游戏链路

1. 游戏开始创建新 Sector。
2. 游戏调用 `onNewGame()`。
3. 过程生成阶段完成后调用 `onNewGameAfterProcGen()`。
4. 经济系统加载并初步推进后调用 `onNewGameAfterEconomyLoad()`。
5. 新游戏初始时间推进后调用 `onNewGameAfterTimePass()`。
6. 新游戏流程进入常规 `onGameLoad(true)` 后续状态。

### 存档载入链路

1. 存档载入后 `Global.setSector(...)` 提供当前 Sector。
2. 对启用状态需要处理的模组调用 `onEnabled(...)`。
3. 游戏调用 `onGameLoad(boolean newGame)`。
4. 模组在 Sector 上注册生涯脚本、监听器或插件。
5. transient 生涯插件和 transient 生涯脚本在载入后重新注册。
6. 已注册脚本由生涯引擎按 `EveryFrameScript` 语义推进。

### Picker 链路

1. 战斗或任务需要创建 ship、weapon、drone 或 missile AI。
2. 游戏调用对应 `pick...AI(...)` 方法。
3. 返回 `null` 时保留原版选择。
4. 返回 `PluginPick<T>` 时使用其中 `plugin` 和 `priority`。
5. picker 竞争按 `PickPriority` 选择优先级最高的结果。
6. `PickPriority.MOD_SET` 表示模组明确指定该插件。

### 存档链路

1. 存档前调用 `beforeGameSave()`。
2. 存档成功后调用 `afterGameSave()`。
3. 存档失败时调用 `onGameSaveFailed()`。
4. 需要 XStream 类型配置时调用 `configureXStream(XStream x)`。

### Codex 链路

1. Codex 数据生成开始前调用 `onAboutToStartGeneratingCodex()`。
2. Codex 条目链接前调用 `onAboutToLinkCodexEntries()`。
3. Codex 数据生成完成后调用 `onCodexDataGenerated()`。

## 规范

- `BaseModPlugin` 子类只覆写需要接入的生命周期方法。
- `CampaignPlugin.PickPriority.CORE_GENERAL` 是最低优先级。
- `CampaignPlugin.PickPriority.HIGHEST` 是最高优先级。
- `CampaignPlugin.PickPriority.MOD_GENERAL` 用于模组整体替换一类生涯功能。
- `CampaignPlugin.PickPriority.MOD_SET` 用于模组处理一组特定场景。
- `CampaignPlugin.PickPriority.MOD_SPECIFIC` 用于模组处理单个特定遭遇。
- `Global.getCombatEngine()` 的使用范围应限定在战斗上下文。
- `Global.getSector()` 的使用范围应限定在 Sector 已建立之后。
- `PluginPick.plugin` 必须是当前 picker 声明的插件类型。
- `PluginPick.priority` 必须匹配替换强度。
- `configureXStream()` 只登记序列化需要的类型配置。
- `mod_info.json` 的 `modPlugin` 必须是可由脚本类加载器实例化的完整类名。
- `onApplicationLoad()` 适合读取 settings、加载静态数据和初始化应用级库。
- `onDevModeF8Reload()` 适合重建静态缓存与调试状态。
- `onEnabled(boolean wasEnabledBefore)` 适合处理新启用模组的存档兼容状态。
- `onGameLoad(boolean newGame)` 适合恢复生涯脚本、监听器、插件和 persistent data 状态。
- `onNewGameAfterEconomyLoad()` 适合访问市场、经济实体和人物。
- picker 返回 `null` 表示本模组放弃本次选择权。

## 陷阱

- `Global.getSector()` 在 `onApplicationLoad()` 阶段可能为 `null`。
- `Global.getCombatEngine()` 的缓存跨战斗使用会污染战斗实例边界。
- `onNewGame()` 与 `onGameLoad(true)` 对同一状态重复写入会造成重复注册。
- `onEnabled(boolean wasEnabledBefore)` 在 `onGameLoad(...)` 之前运行，读取 Sector 状态时要以当前载入阶段为准。
- `PluginPick` 中返回低优先级会被后续 picker 或原版选择覆盖。
- `SettingsAPI.getPlugin(id)` 返回的插件会跨多个存档缓存，字段保存 campaign 对象会形成泄漏风险。
- picker 参数中的 fleet member 按 `ModPlugin` 注释可为 `null`。
- picker 中读取 weapon、missile 或 hull spec 时必须接受其它模组已修改 spec 的现状。
