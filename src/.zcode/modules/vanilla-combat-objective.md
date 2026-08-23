# 战斗点位 API

来源：原版 Starsector。

## 定义

战斗点位 API 是战斗内点位实体、点位效果、点位状态文本和点位集合读取入口的接口集合。

## 参考

- `com.fs.starfarer.api.combat.AssignmentTargetAPI`：标记点位可作为战斗指派目标。
- `com.fs.starfarer.api.combat.BattleObjectiveAPI`：描述生成后的战斗点位实体。
- `com.fs.starfarer.api.combat.BattleObjectiveAPI.Importance`：描述点位重要度枚举。
- `com.fs.starfarer.api.combat.BattleObjectiveEffect`：描述点位效果的初始化、推进、状态文本和部署点加成。
- `com.fs.starfarer.api.combat.BattleObjectiveEffect.ShipStatusItem`：描述点位效果提供给舰船状态栏的单项文本。
- `com.fs.starfarer.api.combat.CombatEngineAPI.getObjectives()`：读取当前战斗的点位集合。

## 边界

- `AssignmentTargetAPI` 在点位模块中归属点位可作为战斗指派目标的继承关系。
- `BattleObjectiveAPI` 归属生成后的点位实体状态。
- `BattleObjectiveAPI.getBattleSizeFractionBonus()` 归属点位提供的战场规模比例加成。
- `BattleObjectiveAPI.getDisplayName()` 归属点位显示名读取。
- `BattleObjectiveAPI.getImportance()` 归属点位重要度读取。
- `BattleObjectiveAPI.getOwner()` 归属点位当前控制方读取。
- `BattleObjectiveAPI.getSprite()` 与 `setSprite(...)` 归属点位显示 sprite 读写。
- `BattleObjectiveAPI.getType()` 归属点位类型读取。
- `BattleObjectiveEffect` 归属单个点位效果脚本生命周期。
- `BattleObjectiveEffect.ShipStatusItem` 归属点位效果对舰船状态栏的文本输出。
- `CombatEngineAPI.getObjectives()` 归属当前战斗实例持有的点位集合读取。
- 点位实体继承 `CombatEntityAPI`，实体级 location、customData、collision 和 owner 写入遵循通用战斗实体语义。

## 链路

### 点位集合链路

1. 战斗创建阶段生成点位实体。
2. 战斗引擎持有当前战斗的点位集合。
3. 代码调用 `CombatEngineAPI.getObjectives()`。
4. 调用方遍历 `BattleObjectiveAPI`。
5. 调用方读取点位 location、owner、type、display name、sprite 或 customData。

### 点位效果链路

1. 点位效果实例创建后绑定到一个 `BattleObjectiveAPI`。
2. 引擎调用 `BattleObjectiveEffect.init(engine, objective)`。
3. 引擎按战斗推进调用 `advance(amount)`。
4. UI 或状态消费方按舰船调用 `getStatusItemsFor(ship)`。
5. 状态消费方读取 `ShipStatusItem` 的 title、description、isDebuff 和 key。
6. 部署或说明消费方读取 `getBonusDeploymentPoints()` 与 `getLongDescription()`。

### 点位目标语义链路

1. `BattleObjectiveAPI` 实现 `AssignmentTargetAPI`。
2. 点位实例进入战斗目标消费方。
3. 目标消费方把点位作为战场中的可指派目标读取。
4. 点位的实体状态继续由 `BattleObjectiveAPI` 与 `CombatEntityAPI` 提供。

## 规范

- `AssignmentTargetAPI` 使点位具备战斗目标接口形状。
- `BattleObjectiveAPI.Importance.NORMAL` 是原版公开的重要度枚举值。
- `BattleObjectiveAPI.getBattleSizeFractionBonus()` 返回点位提供的战场规模比例加成。
- `BattleObjectiveAPI.getDisplayName()` 返回点位在战斗 UI 中使用的显示名。
- `BattleObjectiveAPI.getImportance()` 返回点位重要度。
- `BattleObjectiveAPI.getOwner()` 返回点位当前控制方。
- `BattleObjectiveAPI.getSprite()` 返回点位当前显示 sprite。
- `BattleObjectiveAPI.getType()` 返回点位类型 ID。
- `BattleObjectiveAPI.setSprite(...)` 写入点位当前显示 sprite。
- `BattleObjectiveEffect.advance(amount)` 承载点位效果的 apply 与 unapply 推进。
- `BattleObjectiveEffect.getBonusDeploymentPoints()` 返回点位效果提供的部署点加成。
- `BattleObjectiveEffect.getLongDescription()` 返回点位效果长说明。
- `BattleObjectiveEffect.getStatusItemsFor(ship)` 在该点位效果作用于传入舰船时返回状态文本列表。
- `BattleObjectiveEffect.init(engine, objective)` 接收当前战斗引擎和绑定点位。
- `CombatEngineAPI.getObjectives()` 返回当前战斗实例中的点位列表。
- `ShipStatusItem.getKey()` 返回状态文本去重或识别用 key。

## 陷阱

- `BattleObjectiveAPI.getOwner()` 表示当前控制方，读取结果会随战斗占领状态变化。
- `BattleObjectiveAPI.setSprite(...)` 只改变点位当前显示 sprite，点位类型和显示名由其它字段表达。
- `BattleObjectiveEffect.getStatusItemsFor(ship)` 返回 `null` 表示该点位效果对传入舰船没有状态文本。
- `BattleObjectiveEffect.ShipStatusItem.isDebuff()` 只表达状态文本正负面显示语义。
- `CombatEngineAPI.getObjectives()` 暴露的是当前战斗实例点位集合，跨战斗缓存会污染状态归属。
- 点位 customData 属于该点位实体，key 冲突会污染同一实体上的共享状态。
