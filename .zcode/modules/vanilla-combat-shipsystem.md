# 战斗战术系统

来源：原版 Starsector。

## 定义

战术系统 API 是 `.system` 数据、系统 spec、stats 脚本、AI 脚本和运行时系统状态机的接口集合。

## 参考

- `com.fs.starfarer.api.combat.ShipSystemAIScript`：定义系统 AI 初始化和每帧决策入口。
- `com.fs.starfarer.api.combat.ShipSystemAPI`：提供舰船运行时系统状态、冷却、弹药、效果强度和目标坐标。
- `com.fs.starfarer.api.combat.ShipSystemSpecAPI`：提供系统数据 spec、脚本类名、UI、耗费、状态限制和 tags。
- `com.fs.starfarer.api.combat.ShipwideAIFlags`：提供系统 AI 与舰船 AI 之间的短期 flag 容器。
- `com.fs.starfarer.api.impl.combat.BaseShipSystemScript`：提供 stats 脚本基类和高级接口空实现。
- `com.fs.starfarer.api.plugins.ShipSystemStatsScript`：定义 `State`、`StatusData`、`apply()` 和 `unapply()`。
- `com.fs.starfarer.api.plugins.ShipSystemStatsScriptAdvanced`：定义 `isUsable()` 和 `getInfoText()`。
- `data/shipsystems/*.system`：定义单个系统的 stats script、AI script、持续时间、冷却和 UI 参数。
- `data/shipsystems/ship_systems.csv`：定义系统注册表列。

## 边界

- `.system` 文件归属单个系统 spec 的 JSON 参数。
- `BaseShipSystemScript` 归属系统效果、UI 状态和可用性判定。
- `ShipSystemAIScript` 归属 AI 使用时机和目标输入。
- `ShipSystemAPI.canBeActivated()` 归属运行时激活许可汇总。
- `ShipSystemAPI.forceState(...)` 归属脚本或调试逻辑对运行时状态机的直接改写。
- `ShipSystemAPI` 归属舰船运行时系统状态。
- `ShipSystemSpecAPI` 归属系统加载后的静态配置。
- `ShipSystemSpecAPI.getSpecJson()` 归属 `.system` 原始 JSON 参数读取。
- `ShipSystemStatsScript.State.ACTIVE` 归属系统 active 段。
- `ShipSystemStatsScript.State.COOLDOWN` 归属冷却段。
- `ShipSystemStatsScript.State.IDLE` 归属待机段。
- `ShipSystemStatsScript.State.IN` 归属进入段。
- `ShipSystemStatsScript.State.OUT` 归属退出段。
- `ship_systems.csv` 归属系统列表注册和核心时序列。
- AI flags 归属舰船 AI 与 stats 脚本之间的运行时通信。

## 链路

### CSV 加载链路

1. 游戏读取 `data/shipsystems/ship_systems.csv`。
2. 游戏按 id 读取对应 `.system` 文件。
3. `.system` 指定 `statsScript` 类名。
4. `.system` 可指定 `aiScript` 类名。
5. 其它参数写入 `ShipSystemSpecAPI` 对应字段。
6. 游戏创建 `ShipSystemSpecAPI`。
7. 舰船 hull spec 或 variant 引用系统 id。
8. 战斗创建舰船时生成 `ShipSystemAPI`。

### Stats 脚本链路

1. 系统状态机进入 `IN`、`ACTIVE` 或 `OUT`。
2. 引擎调用 `apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)`。
3. 脚本按 id 写入 stats、engine、ship 或视觉状态。
4. 系统刷新、结束或创建边界调用 `unapply(stats, id)`。
5. UI 查询 `getStatusData(index, state, effectLevel)`。
6. UI 查询 `getInfoText(system, ship)`。

### AI 链路

1. 引擎创建 `ShipSystemAIScript`。
2. 引擎调用 `init(ship, system, flags, engine)`。
3. 每帧传入 `amount`、导弹危险方向、碰撞危险方向和当前目标。
4. AI 读取系统、舰船、flags 和目标状态。
5. AI 调用 `ship.useSystem()` 或写入 `ShipwideAIFlags`。
6. Stats 脚本通过 ship、system 或 flags 消费目标状态。

### 可用性链路

1. 玩家或 AI 试图激活系统。
2. 原版检查弹药、冷却、flux、禁用状态和系统配置。
3. `ShipSystemStatsScriptAdvanced.isUsable(system, ship)` 返回额外许可。
4. `ShipSystemAPI.canBeActivated()` 汇总运行时可激活状态。
5. 激活成功后状态机进入 `IN`。

### 时长覆写链路

1. 战斗创建舰船时加载 `ShipSystemSpecAPI` 的 in、active、out、cooldown、regen 和 max uses。
2. 引擎读取 stats 脚本的 `getInOverride(ship)`。
3. 引擎读取 stats 脚本的 `getActiveOverride(ship)`。
4. 引擎读取 stats 脚本的 `getOutOverride(ship)`。
5. 引擎读取 stats 脚本的 `getRegenOverride(ship)`。
6. 引擎读取 stats 脚本的 `getUsesOverride(ship)`。
7. 返回 `-1` 的覆写项使用 spec 原始值。

## 规范

- `apply()` 中使用的 stat 修改 id 必须能被 `unapply()` 使用同一 id 清除。
- `ShipSystemAPI.forceState(state, progress)` 会直接改写运行时系统状态。
- `ShipSystemAPI.getEffectLevel()` 或 `effectLevel` 表示当前运行时强度。
- `ShipSystemAPI.getScript()` 返回 `ShipSystemStatsScript` 接口。
- `ShipSystemAPI.getSpecAPI()` 返回当前系统 spec。
- `ShipSystemAIScript.advance()` 的 amount 是上一帧秒数。
- `ShipSystemAIScript.advance()` 的 target 参数可由舰船 AI 当前目标提供。
- `ShipSystemSpecAPI.getAIScript()` 返回 spec 关联的 AI 脚本实例。
- `ShipSystemSpecAPI.isRunScriptWhileIdle()` 控制 idle 状态脚本运行。
- `ShipSystemSpecAPI.isRunScriptWhilePaused()` 控制暂停状态脚本运行。
- `ShipwideAIFlags` 默认 flag 持续时间为 `0.5` 秒。
- `.system` 中 stats 和 AI 类名必须能由脚本类加载器实例化。

## 陷阱

- `BaseShipSystemScript.unapply()` 在舰船实体创建后会先于首次 `apply()` 运行 1 次。
- `active` 为空或 `0` 的系统中 `effectLevel == 1` 只存在 1 帧。
- `apply()` 与 `ShipSystemAIScript.advance()` 属于两条生命周期。
- `effectLevel == 1f` 作为多帧 once 条件会误判瞬时 active 系统。
- `ShipSystemAPI.getScript()` 返回 stats 脚本接口，直接转型前应确认实现类型。
- `ShipSystemSpecAPI.getStatsScript()` 与 `ShipSystemAPI.getScript()` 分别来自 spec 与运行时系统。
- `ShipwideAIFlags` 未持续刷新时会在默认 `0.5` 秒后过期。
- `ShipwideAIFlags` 写入的目标状态需要由同一系统语义消费。
