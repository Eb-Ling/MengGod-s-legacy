# 船体插件

来源：原版 Starsector。

## 定义

船体插件 API 是舰船装配限制、stats 修改、战斗推进、tooltip、S-mod、生涯舰队效果和监听器接入的接口集合。

## 参考

- `com.fs.starfarer.api.combat.BaseHullMod`：提供 `HullModEffect` 的默认实现和常用 tooltip/S-mod 方法。
- `com.fs.starfarer.api.combat.HullModEffect`：定义船体插件效果接口。
- `com.fs.starfarer.api.combat.HullModFleetEffect`：定义船体插件舰队级生涯效果接口。
- `com.fs.starfarer.api.combat.HullModEffect.applyEffectsAfterShipAddedToCombatEngine(...)`：定义舰船加入战斗引擎后的单次回调。
- `com.fs.starfarer.api.combat.MutableShipStatsAPI`：承载插件在生成前修改的舰船 stats。
- `com.fs.starfarer.api.combat.ShipAPI`：承载生成后实体操作与监听器注册。
- `com.fs.starfarer.api.loading.HullModSpecAPI`：承载船体插件静态配置。
- `data/hullmods/hull_mods.csv`：注册船体插件表。

## 边界

- `BaseHullMod` 实例归属应用会话级插件对象。
- `HullModEffect.applyEffectsBeforeShipCreation()` 归属实体生成前 stats 修改。
- `HullModEffect.applyEffectsAfterShipCreation()` 归属实体生成后 ship、slot、weapon group 和监听器操作。
- `HullModEffect.applyEffectsAfterShipAddedToCombatEngine()` 归属舰船第一次加入 combat engine 后的实体操作。
- `HullModEffect.advanceInCampaign()` 归属舰队成员生涯推进。
- `HullModEffect.advanceInCombat()` 归属战斗中舰船每帧推进。
- `HullModEffect.applyEffectsToFighterSpawnedByShip()` 归属航母生成战机后的效果传播。
- `HullModEffect.canBeAddedOrRemovedNow()` 归属装配界面安装和移除时机限制。
- `HullModEffect.isApplicableToShip()` 归属安装对象适用性。
- `HullModEffect.getRequiredItem()` 归属装配所需特殊物品。
- `HullModFleetEffect` 归属所有舰队遍历的舰队层效果。
- `HullModFleetEffect.advanceInCampaign()` 归属生涯层舰队推进。
- `HullModFleetEffect.onFleetSync()` 归属生涯层舰队组成同步。
- `HullModSpecAPI` 归属加载后的 hullmod 静态配置。
- `MutableShipStatsAPI` 归属生成前可影响 campaign 与 combat 的数值。

## 链路

### CSV 加载链路

1. 游戏读取 `data/hullmods/hull_mods.csv`。
2. 每行按 `script` 创建 `HullModEffect`。
3. 其它参数写入 `HullModSpecAPI` 对应字段。
4. 游戏创建 `HullModSpecAPI`。
5. 插件被装配、内置或作为生涯舰队效果引用。

### 生成前链路

1. 游戏准备舰船 variant 与 hull spec。
2. 游戏收集舰船安装和内置 hullmod。
3. 对每个插件调用 `applyEffectsBeforeShipCreation(hullSize, stats, id)`。
4. 插件通过 `MutableShipStatsAPI` 写入 stat、dynamic stat 或 stats listener。
5. 游戏基于修改后的 stats 创建或展示舰船数据。

### 生成后链路

1. 舰船实体创建完成。
2. 游戏调用 `applyEffectsAfterShipCreation(ship, id)`。
3. 插件可读取 `ShipAPI`、武器、槽位、系统和 variant。
4. 插件可向 ship 注册监听器。
5. 舰船第一次加入 combat engine 后调用 `applyEffectsAfterShipAddedToCombatEngine(ship, id)`。
6. 战斗中调用 `advanceInCombat(ship, amount)`。
7. 舰船移除或战斗结束后实体状态由引擎生命周期接管。

### Tooltip 链路

1. UI 读取 `HullModSpecAPI.getDescriptionFormat()`。
2. UI 调用 `getDescriptionParam(index, hullSize, ship)` 填充参数。
3. UI 调用 `shouldAddDescriptionToTooltip(...)`。
4. UI 调用 `addPostDescriptionSection(...)` 添加说明后段。
5. S-mod 展示时调用 `hasSModEffectSection(...)`。
6. S-mod 展示时调用 `addSModSection(...)` 和 `addSModEffectSection(...)`。

### 生涯舰队效果链路

1. hullmod 效果类实现 `HullModFleetEffect`。
2. 原版按 `withAdvanceInCampaign()` 决定是否调用舰队推进。
3. 原版按 `withOnFleetSync()` 决定是否调用舰队同步。
4. `advanceInCampaign(CampaignFleetAPI fleet)` 会遍历所有舰队。
5. `onFleetSync(CampaignFleetAPI fleet)` 在舰队组成变化时触发。

### 安装限制链路

1. 装配界面、autofit 或市场 UI 准备 ship、market 和 trade mode。
2. UI 调用 `showInRefitScreenModPickerFor(ship)` 判断列表可见性。
3. UI 调用 `isApplicableToShip(ship)` 判断对象适用性。
4. UI 调用 `canBeAddedOrRemovedNow(ship, marketOrNull, mode)` 判断时机许可。
5. 失败时 UI 调用 `getUnapplicableReason(ship)` 或 `getCanNotBeInstalledNowReason(...)`。
6. 安装后 variant 保存 hullmod id、perma mod 或 S-mod 状态。

## 规范

- `affectsOPCosts()` 返回 true 的插件应作为内置插件使用。
- `applyEffectsAfterShipCreation()` 中的效果不应承担 campaign stats 修改。
- `applyEffectsAfterShipCreation()` 中注册监听器前应检查重复注册。
- `applyEffectsBeforeShipCreation()` 是影响 campaign 与 combat stats 的正式入口。
- `BaseHullMod.isBuiltIn(ship)` 依赖当前 spec id 与 hull spec built-in mods。
- `BaseHullMod.isSMod(ship)` 同时检查普通 S-mod 和 enhanced built-in。
- `canBeAddedOrRemovedNow(...)` 的 ship 参数可能来自装配或 autofit 场景。
- `getCanNotBeInstalledNowReason(...)` 应与 `canBeAddedOrRemovedNow(...)` 的失败语义一致。
- `getDescriptionParam()` 参数序号从 `0` 开始。
- `getRequiredItem()` 返回的 cargo stack 会被 required item tooltip 消费。
- `hasSModEffect()` 依赖 spec 中 S-mod 文本存在且未以 `#` 开头。
- `hasSModEffectSection(...)` 会结合 built-in、enhanced built-in 和 penalty 语义决定展示。
- `HullModFleetEffect` 生涯舰队效果处理 `CampaignFleetAPI`，战斗实体效果通过 stats、ship 回调和战斗推进链路表达。
- `isInPlayerFleet(stats)` 通过 FleetMember 的 fleet commander 判断玩家归属。
- `isSModEffectAPenalty()` 返回 true 时 S-mod tooltip 使用 penalty 样式。
- `showInRefitScreenModPickerFor(ship)` 控制装配列表可见性。
- `stat` 修改 id 必须稳定，解除或覆盖时使用同一 id。
- `shipHasOtherModInCategory(...)` 通过 hullmod tag 判断同类互斥。

## 陷阱

- `HullModEffect` 注释说明效果类每个应用会话实例化 1 次，字段保存 campaign 对象会形成泄漏风险。
- `applyEffectsAfterShipAddedToCombatEngine()` 同一舰船移除后再加入引擎时也只调用 1 次。
- `applyEffectsAfterShipAddedToCombatEngine()` 调用时，`ShipAPI` 的 `isAlive()` 为 false。
- `applyEffectsAfterShipCreation()` 在装配界面完成修改后也会运行。
- `applyEffectsBeforeShipCreation()` 中没有 `ShipAPI` 实体。
- `advanceInCampaign(FleetMemberAPI, amount)` 可在 fleet 数据同步时以 `amount = 0` 调用。
- `advanceInCombat(ship, amount)` 原版注释说明暂停时停止调用。
- `HullModFleetEffect.advanceInCampaign(fleet)` 会对所有舰队调用。
- `HullModFleetEffect` 的回调接收 `CampaignFleetAPI`，把战斗实体状态放入该链路会造成层级错位。
