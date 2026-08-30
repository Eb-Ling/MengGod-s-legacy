# 生涯战斗与遭遇

来源：原版 Starsector。

## 定义

生涯战斗与遭遇是 campaign 层连接舰队遭遇、战斗创建、自动结算、战斗结果、战后上报和战利品回调的接口集合。

## 参考

- `com.fs.starfarer.api.campaign.BattleAPI`：提供 campaign battle 双方、参战舰队、combined fleet、快照、空间站参与、完成和胜负判定入口。
- `com.fs.starfarer.api.campaign.BattleAutoresolverPlugin`：提供 campaign battle 自动结算入口和 encounter context 读取入口。
- `com.fs.starfarer.api.campaign.BattleCreationPlugin`：提供 combat battle 创建和战斗定义加载后回调入口。
- `com.fs.starfarer.api.campaign.CampaignEventListener`：提供 battle occurred、battle finished、player engagement 和 encounter loot 事件回调入口。
- `com.fs.starfarer.api.campaign.FleetEncounterContextPlugin`：提供舰队遭遇双方数据、战损状态、追击骚扰可用性、战后 recovery、声望调整和玩家贡献入口。
- `com.fs.starfarer.api.campaign.EngagementResultForFleetAPI`：提供单侧交战结果、目标、胜负、部署、损毁、瘫痪、撤退和 reserve 入口。
- `com.fs.starfarer.api.combat.EngagementResultAPI`：提供 winner、loser、玩家胜负、玩家提前离场、battle 和 combat damage data 入口。
- `com.fs.starfarer.api.campaign.CampaignPlugin.pickBattleAutoresolverPlugin(...)`：提供自动结算 plugin picker 入口。
- `com.fs.starfarer.api.campaign.CampaignPlugin.pickBattleCreationPlugin(...)`：提供战斗创建 plugin picker 入口。
- `com.fs.starfarer.api.campaign.SectorAPI.reportBattleOccurred(...)`：提供 battle occurred 上报入口。

## 边界

- `BattleAPI` 归属 campaign battle 双方、参战舰队集合、side 判定和完成状态。
- `BattleAPI.BattleSide` 归属 battle side 枚举，原版值为 `ONE`、`TWO` 和 `NO_JOIN`。
- `BattleAPI.finish(...)` 归属 campaign battle 完成与 winner side 写入。
- `BattleAPI.genCombined(...)` 归属把同侧参战舰队合并为 combined fleet 的入口。
- `BattleAPI.getMemberSourceMap()` 归属从参战成员到来源舰队的映射。
- `BattleAPI.takeSnapshots()` 归属 battle 前参战舰队快照。
- `BattleAutoresolverPlugin` 归属 campaign battle 的自动结算。
- `BattleCreationPlugin` 归属 combat battle 创建与战斗定义加载后处理。
- `CampaignEventListener.reportBattleOccurred(...)` 归属每轮自动结算或玩家战斗后的 battle occurred 回调。
- `CampaignEventListener.reportBattleFinished(...)` 归属完成 battle 的 battle finished 回调。
- `CampaignEventListener.reportEncounterLootGenerated(...)` 归属 encounter loot 生成后的回调。
- `CampaignEventListener.reportPlayerEngagement(...)` 归属玩家交战结果回调。
- `CampaignPlugin.pickBattleAutoresolverPlugin(...)` 归属 autoresolve plugin 选择。
- `CampaignPlugin.pickBattleCreationPlugin(...)` 归属 battle creation plugin 选择。
- `EngagementResultAPI` 归属一次 combat engagement 的 winner、loser、battle、玩家结果和 damage data。
- `EngagementResultForFleetAPI` 归属单侧舰队的 goal、部署、损毁、瘫痪、撤退和 reserve 结果。
- `FleetEncounterContextPlugin` 归属玩家相关舰队遭遇的双方状态、战后 recovery、追击骚扰可用性、声望调整和贡献计算。
- `FleetEncounterContextPlugin.DataForEncounterSide` 归属单侧遭遇数据、伤亡、部署记录、crew loss 和撤离状态。
- `FleetEncounterContextPlugin.EngagementOutcome` 归属玩家相关遭遇的最后交战结果。
- `FleetEncounterContextPlugin.FleetMemberData` 归属单个参战成员在遭遇中的战损结果记录。
- `SectorAPI.reportBattleOccurred(...)` 和 `reportBattleFinished(...)` 归属 Sector battle 事件广播。
- `SectorAPI.reportEncounterLootGenerated(...)` 归属 encounter loot 事件广播。
- `SectorAPI.reportPlayerEngagement(...)` 归属玩家 engagement 事件广播。

## 链路

### 战斗创建链路

1. 生涯交互或 UI 创建 `BattleCreationContext`。
2. 代码调用 dialog 或 campaign UI 的 battle 启动入口。
3. Campaign plugin picker 调用 `pickBattleCreationPlugin(opponent)`。
4. `BattleCreationPlugin.initBattle(context, api)` 写入 mission definition。
5. combat engine 加载战斗定义。
6. `BattleCreationPlugin.afterDefinitionLoad(engine)` 接收加载后的 combat engine。

### 自动结算链路

1. 生涯系统取得 `BattleAPI`。
2. Campaign plugin picker 调用 `pickBattleAutoresolverPlugin(battle)`。
3. picker 返回 `BattleAutoresolverPlugin`。
4. 调用 `resolve()` 自动结算 battle。
5. autoresolver 通过 `getContext()` 暴露 `FleetEncounterContextPlugin`。
6. Sector 上报 battle occurred 或 battle finished。

### Battle 参战链路

1. 代码创建或取得 `BattleAPI`。
2. 调用 `canJoin(fleet)` 判断舰队能否加入。
3. 调用 `pickSide(fleet)` 或 `pickSide(fleet, considerPlayerTransponderStatus)` 选择 side。
4. 调用 `join(fleet)` 或 `join(fleet, side)` 加入 battle。
5. 调用 `genCombined(...)` 生成 combined fleet。
6. 调用 `takeSnapshots()` 保存 battle 前快照。
7. 调用 `leave(fleet, engagedInHostilities)` 移出参战舰队。
8. 调用 `finish(winner, engagedInHostilities)` 完成 battle。

### Encounter 结果链路

1. `FleetEncounterContextPlugin` 持有 battle 和双方 encounter data。
2. 战斗结束后生成 `EngagementResultAPI`。
3. 调用 `getWinnerResult()` 和 `getLoserResult()` 取得双方结果。
4. 单侧结果通过 `EngagementResultForFleetAPI` 暴露 goal、部署、损毁、瘫痪、撤退和 reserve。
5. encounter context 更新 winner、loser、last engagement outcome 和双方 casualty 数据。
6. 调用 `performPostVictoryRecovery(result)` 处理战后 recovery。
7. 调用 `adjustPlayerReputation(dialog, ffText)` 处理玩家声望调整。
8. 调用 `computePlayerContribFraction()` 计算玩家贡献。

### Sector 上报链路

1. battle 或 engagement 结算产生上报对象。
2. Sector 调用 `reportBattleOccurred(...)`、`reportBattleFinished(...)` 或 `reportPlayerEngagement(...)`。
3. 已注册 `CampaignEventListener` 接收对应回调。
4. encounter loot 生成后 Sector 调用 `reportEncounterLootGenerated(plugin, loot)`。
5. 已注册 listener 读取 encounter context 和 cargo loot。

## 规范

- `BattleAPI.canJoin(fleet)` 用于判断舰队能否加入当前 battle。
- `BattleAPI.finish(winner)` 使用 `BattleSide` 表达 winner side。
- `BattleAPI.genCombinedDoNotRemoveEmpty()` 生成 combined fleet 时保留空舰队。
- `BattleAPI.getBothSides()` 返回当前 battle 双方舰队集合。
- `BattleAPI.getCombined(side)` 返回指定 side 的 combined fleet。
- `BattleAPI.getPlayerSideSnapshot()` 返回 battle 前玩家侧快照。
- `BattleAPI.getSnapshotBothSides()` 返回双方 battle 前快照。
- `BattleAPI.getSourceFleet(member)` 返回成员来源舰队。
- `BattleAPI.isPlayerInvolvedAtStart()` 表达玩家是否在 battle 开始时已参与。
- `BattleAPI.wasFleetDefeated(fleet, primaryWinner)` 使用 primary winner 判定舰队是否被击败。
- `BattleAPI.wasFleetVictorious(fleet, primaryWinner)` 使用 primary winner 判定舰队是否胜利。
- `BattleAutoresolverPlugin.getContext()` 返回本次自动结算使用的 encounter context。
- `BattleCreationPlugin.afterDefinitionLoad(engine)` 在 combat battle definition 加载后调用。
- `CampaignEventListener.reportBattleOccurred(...)` 可在每轮 autoresolve 后和玩家战斗后调用。
- `CampaignEventListener.reportBattleFinished(...)` 在结束 battle 的 autoresolve round 后或玩家战斗后调用。
- `EngagementResultAPI.getBattle()` 返回本次 engagement 所属 campaign battle。
- `EngagementResultAPI.getLastCombatDamageData()` 返回最近 combat damage data。
- `EngagementResultForFleetAPI.enemyCanCleanDisengage()` 表达敌方能否干净脱离。
- `EngagementResultForFleetAPI.getAllEverDeployedCopy()` 返回曾经部署成员副本。
- `FleetEncounterContextPlugin.getLastEngagementOutcome()` 返回最后一次玩家相关 engagement outcome。
- `FleetEncounterContextPlugin.performPostVictoryRecovery(result)` 返回每船平均 recovery，范围为 `0` 到 `1`。
- `FleetEncounterContextPlugin.DataForEncounterSide.getMemberToDeployedMap()` 只对非 autoresolve engagement 有意义。
- `SectorAPI.reportEncounterLootGenerated(plugin, loot)` 在 encounter loot 生成后广播。

## 陷阱

- `BattleAPI.getPlayerSideSnapshot()` 和 `getNonPlayerSideSnapshot()` 是 battle 前快照，可能包含 battle 中已消灭的舰队。
- `BattleAPI.genCombined(...)` 会改变 combined fleet 状态，读取来源舰队应使用 source 或 snapshot 入口。
- `BattleAPI.pickSide(fleet, considerPlayerTransponderStatus)` 的结果受玩家 transponder 可见性语义影响。
- `CampaignEventListener.reportBattleOccurred(...)` 会在 autoresolve round 和玩家战斗后触发，不能等同于 battle finished。
- `CampaignEventListener.reportBattleFinished(...)` 发生在 finishing autoresolve round 后且晚于 battle occurred。
- `EngagementResultAPI.isPlayerOutBeforeEnd()` 表达玩家提前离场，不能只用 didPlayerWin 判断 engagement 叙事结果。
- `FleetEncounterContextPlugin.EngagementOutcome` 注释说明只用于玩家参与的 battle。
- `FleetEncounterContextPlugin.DataForEncounterSide.getMemberToDeployedMap()` 对 autoresolve engagement 不具备部署映射语义。
- `SectorAPI.reportEncounterLootGenerated(...)` 接收的是已生成 loot，listener 修改会影响同一 cargo 对象内容。
