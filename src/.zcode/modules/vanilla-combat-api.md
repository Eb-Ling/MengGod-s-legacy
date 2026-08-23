# 战斗 API

来源：原版 Starsector。

## 定义

战斗 API 是战斗引擎、战斗实体、战斗插件、分层渲染插件和监听器组成的运行时接口集合。

## 参考

- `com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin`：提供分层渲染插件基类。
- `com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin`：提供战斗每帧插件基类。
- `com.fs.starfarer.api.combat.CombatEngineAPI`：提供战斗实体集合、粒子、插件和 customData 入口。
- `com.fs.starfarer.api.combat.CombatEngineAPI.addLayeredRenderingPlugin(...)`：注册分层渲染插件。
- `com.fs.starfarer.api.combat.CombatEngineAPI.addPlugin(...)`：注册战斗全局插件。
- `com.fs.starfarer.api.combat.CombatEngineAPI.getCustomData()`：提供当前战斗引擎级共享状态表。
- `com.fs.starfarer.api.combat.CombatEntityAPI`：描述通用战斗实体的位置、owner、碰撞、血量和 customData。
- `com.fs.starfarer.api.combat.CombatUIAPI`：提供战斗 UI 消息、命令界面、目标 reticle 和玩家控制入口。
- `com.fs.starfarer.api.combat.CollisionClass`：定义战斗实体碰撞类别。
- `com.fs.starfarer.api.combat.ViewportAPI`：描述战斗 viewport 的世界坐标、屏幕坐标、缩放和可见区域。
- `com.fs.starfarer.api.combat.listeners`：提供射程、部署、advance 和通用战斗监听器接口。

## 边界

- `BaseCombatLayeredRenderingPlugin` 归属 combat layer 渲染生命周期。
- `BaseEveryFrameCombatPlugin` 归属战斗全局每帧推进与输入处理。
- `CombatEngineAPI` 拥有战斗级实体集合、插件集合、时间倍率和 customData。
- `CombatEngineAPI.addLayeredRenderingPlugin(...)` 归属分层渲染插件注册。
- `CombatEngineAPI.addPlugin(...)` 归属战斗全局插件注册。
- `CombatEngineAPI.getCustomData()` 归属当前战斗实例共享状态。
- `CombatEngineAPI.getElapsedInLastFrame()` 归属战斗引擎帧时间读取。
- `CombatEngineAPI.getPlayerShip()` 归属玩家舰船查询入口。
- `CombatEntityAPI` 归属通用实体几何、owner、碰撞、血量和实体级 customData。
- `CombatUIAPI` 归属战斗内 UI 状态、消息、命令界面和目标 reticle。
- `CollisionClass` 归属实体碰撞和命中筛选类别。
- `ViewportAPI` 归属战斗渲染的世界/屏幕坐标换算和可见区域。
- 战斗监听器归属被注册的 engine、ship 或 stats 对象。
- 战斗层 customData 归属当前 `CombatEngineAPI` 实例。

## 链路

### 战斗插件链路

1. 代码创建 `EveryFrameCombatPlugin`。
2. 代码调用 `CombatEngineAPI.addPlugin(plugin)`。
3. 引擎在 add 调用内执行 `plugin.init(engine)`。
4. 核心控制处理前调用 `processInputPreCoreControls(amount, events)`。
5. 引擎每帧调用 `advance(amount, events)`。
6. UI 坐标渲染阶段调用 `renderInUICoords(viewport)`。
7. 世界坐标渲染阶段调用 `renderInWorldCoords(viewport)`。
8. 代码调用 `CombatEngineAPI.removePlugin(plugin)` 或战斗结束。

### 分层渲染链路

1. 代码创建 `CombatLayeredRenderingPlugin`。
2. 代码调用 `CombatEngineAPI.addLayeredRenderingPlugin(plugin)`。
3. 引擎调用 `init(entity)`。
4. 引擎按 `getActiveLayers()` 选择渲染层。
5. 引擎每帧调用 `advance(amount)`。
6. 对活动层调用 `render(layer, viewport)`。
7. `isExpired()` 返回结束状态。
8. 结束时调用 `cleanup()`。

### 通用实体与碰撞链路

1. 战斗引擎持有 `CombatEntityAPI` 实体集合。
2. 实体暴露 location、velocity、owner、collision radius 和 collision class。
3. 碰撞查询读取 exact bounds、collision radius 或 collision grid。
4. 命中流程按碰撞类别和实体状态选择后续结算路径。
5. 实体状态读取或写入 hitpoints、customData 和 removed 状态。

## 规范

- `CombatEngineAPI.getElapsedInLastFrame()` 在暂停时也返回当前帧值。
- `CombatEngineAPI.getPlayerShip()` 在项目警告约束内视为有效返回。
- `CombatEngineAPI.getCustomData()` 的 key 必须按模块或类名维持唯一。
- `BaseCombatLayeredRenderingPlugin.getActiveLayers()` 返回当前分层渲染插件要参与的 combat layers。
- `BaseCombatLayeredRenderingPlugin.isExpired()` 返回分层渲染插件结束状态。
- `BaseCombatLayeredRenderingPlugin.render(...)` 使用 combat layer 与 viewport 执行世界渲染。
- `BaseEveryFrameCombatPlugin.processInputPreCoreControls(...)` 在核心控制处理前运行。
- `BaseEveryFrameCombatPlugin.renderInUICoords(...)` 使用 UI 坐标渲染。
- `BaseEveryFrameCombatPlugin.renderInWorldCoords(...)` 使用世界坐标渲染。
- `CombatEntityAPI.getCustomData()` 读取返回值不应用 map put 写入，写入应使用 `setCustomData(...)`。
- `CombatEntityAPI.getExactBounds()` 可返回 `null`，此时使用 collision radius 作为碰撞范围。
- `ViewportAPI` 的 convert 方法用于世界坐标和屏幕坐标换算。

## 陷阱

- `CombatEngineAPI.getCustomData()` 的 key 冲突会污染同一场战斗的共享状态。
- `CombatEngineAPI.getElapsedInLastFrame()` 注释说明暂停时也返回当前帧值。
- `CollisionClass.NONE` 会影响碰撞、命中和目标筛选。
- `CombatEntityAPI.getCustomData()` 返回的 map 不应直接 put。
- `BaseEveryFrameCombatPlugin.renderInUICoords(...)` 与 `renderInWorldCoords(...)` 使用不同坐标空间。
- `BaseCombatLayeredRenderingPlugin.getActiveLayers()` 决定 render 调用层级，层级选择错误会改变视觉遮挡关系。
- 分层渲染插件的 `isExpired()` 应保持纯查询，资源释放放在 `cleanup()`。
