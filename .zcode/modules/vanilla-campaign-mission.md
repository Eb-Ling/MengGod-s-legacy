# 生涯任务

来源：原版 Starsector。

## 定义

生涯任务是 mission board、hub mission、任务 intel、阶段、搜索和触发器的原版接口集合。

## 参考

- `com.fs.starfarer.api.campaign.CampaignMissionPlugin`：定义任务板任务的 id、名称、接受、推进、重要人物和 cleanup 入口。
- `com.fs.starfarer.api.campaign.MissionBoardAPI`：提供任务板任务可用地点、移除和数量查询入口。
- `com.fs.starfarer.api.impl.campaign.intel.BaseMissionIntel`：提供任务 intel 状态、限时、接受、放弃和结果入口。
- `com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission`：提供 hub mission 阶段、创建、接受、中止、结果、重要目标和清理入口。
- `com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub`：提供人物 mission hub、mission spec 收集、creator 抽取和 offered mission 入口。
- `com.fs.starfarer.api.impl.campaign.missions.hub.HubMission`：定义 hub mission 的 id、random、创建、接受、中止、hub、creator 和 person 绑定入口。
- `com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionCreator`：定义 hub mission creator 的创建、计数、权重、超时、声望匹配和 spec 入口。
- `com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent`：提供 mission 被 bar wrapper 显示前的 market 判定入口。
- `com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch`：提供 mission 搜索系统、市场、实体、地形和商品目标的条件入口。
- `com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers`：提供 mission 内 trigger action、舰队生成、实体生成、记忆写入和位置选择入口。
- `com.fs.starfarer.api.impl.campaign.missions.hub.MissionHub`：定义人物 mission hub 的 person 和打开选项入口。
- `com.fs.starfarer.api.impl.campaign.missions.hub.MissionTrigger`：保存 trigger id、条件、阶段集合、动作和 action context。
- `com.fs.starfarer.api.loading.PersonMissionSpec`：承载 person mission 静态配置、权重、超时、tag 和 plugin class。

## 边界

- `BaseHubMission` 归属 hub mission 的阶段、结果、限时、重要对象、任务实体和任务清理。
- `BaseHubMission.accept(...)` 归属 hub mission 从展示态进入已接受态。
- `BaseHubMission.abort()` 归属未完成 mission 对已注册变更的回滚。
- `BaseMissionHub` 归属人物可提供 mission 集合、creator 抽取和 open option。
- `BaseMissionIntel` 归属任务 intel 的 posted、accepted、cancelled、failed、abandoned 和 completed 状态。
- `CampaignMissionPlugin` 归属任务板任务的接受、推进、重要人物、faction、tooltip 和 cleanup。
- `HubMission` 归属 mission id、generation random、hub、creator、person 和 person override 绑定。
- `HubMissionCreator` 归属 mission creator 的完成失败计数、抽取权重、超时、active 状态和声望匹配。
- `HubMissionWithBarEvent.shouldShowAtMarket(market)` 归属 mission 在指定 market 的 bar 显示判定。
- `HubMissionWithSearch` 归属任务目标搜索的 require/prefer 条件与 pick 方法。
- `HubMissionWithTriggers` 归属 mission trigger 动作构造与 trigger 列表写入。
- `MissionBoardAPI` 归属任务板中 mission 到可用地点的注册、取消注册和移除。
- `MissionHub` 归属人物到 mission hub 的绑定和打开 mission 列表 option。
- `MissionTrigger` 归属 trigger 条件、阶段集合、动作集合和动作上下文。
- `PersonMissionSpec` 归属 person mission 的静态规格、plugin class、权重、超时和 tag。

## 链路

### Mission Board 链路

1. 代码创建 `CampaignMissionPlugin`。
2. 调用 `MissionBoardAPI.makeAvailableAt(mission, loc)`。
3. mission 可用地点进入 `MissionAvailabilityAPI.getAvailableAt()` 集合。
4. 玩家在可用地点接受任务时调用 `CampaignMissionPlugin.playerAccept(entity)`。
5. 生涯推进调用 `CampaignMissionPlugin.advance(amount)`。
6. 移除任务时调用 `MissionBoardAPI.removeMission(mission, withCleanup)`。
7. `withCleanup` 为 true 时调用 `CampaignMissionPlugin.cleanup()`。

### Mission Hub 抽取链路

1. 人物关联 `MissionHub`。
2. `BaseMissionHub` 读取人物可用 `PersonMissionSpec`。
3. 每个 spec 创建或更新 `HubMissionCreator`。
4. creator 按 active、声望、优先级、权重和超时进入 picker。
5. picker 选中 creator 后调用 `createHubMission(hub)`。
6. mission 写入 creator、hub、mission id 和 generation random。
7. mission 进入当前 offered mission 集合。

### Hub Mission 接受链路

1. 显示入口创建 `HubMission`。
2. 调用 `createAndAbortIfFailed(market, barEvent)`。
3. 创建成功后调用 `updateInteractionData(dialog, memoryMap)`。
4. 玩家接受时调用 `accept(dialog, memoryMap)`。
5. mission 要求 starting stage 已设置。
6. mission 写入当前阶段、重要对象和相关 memory。
7. mission 进入已接受推进状态。

### Hub Mission 阶段链路

1. mission 调用 `setStartingStage(...)` 写入起始阶段。
2. mission 调用 `setSuccessStage(...)` 或 `setFailureStage(...)` 写入结束阶段。
3. mission 调用 `setStageOn...(...)` 注册阶段转换条件。
4. 生涯推进时检查阶段连接条件。
5. 条件满足后写入目标阶段。
6. 成功或失败阶段触发结果、声望、奖励、intel 更新和清理。

### Mission Trigger 链路

1. mission 调用 trigger begin 入口创建当前 `MissionTrigger`。
2. mission 写入 trigger 条件和适用阶段。
3. mission 调用 trigger action helper 追加动作。
4. mission 调用 `endTrigger()` 将 trigger 加入列表。
5. 条件满足时创建 `TriggerActionContext`。
6. trigger 依次执行 `TriggerAction.doAction(context)`。
7. 动作写入舰队、实体、人物、memory、位置或 drop 结果。

### Mission Intel 状态链路

1. `BaseMissionIntel` 初始状态为 posted。
2. posted 状态按随机撤回间隔推进。
3. 玩家确认接受按钮后状态变为 accepted。
4. accepted 状态按生涯天数推进 elapsed days。
5. 限时耗尽时状态变为 failed。
6. 玩家确认放弃按钮后状态变为 abandoned。
7. 任务结束时写入 `MissionResult` 并发送 intel update。

### Search 目标链路

1. mission 写入系统、市场、星球、实体、地形或商品的 require/prefer 条件。
2. `HubMissionWithSearch` 收集候选对象。
3. require 条件过滤候选集合。
4. prefer 条件提高候选排序权重。
5. pick 方法返回被选中的任务目标。
6. mission 可把目标写入阶段、地图、重要对象或 trigger context。

## 规范

- `BaseHubMission.createAndAbortIfFailed(...)` 在创建失败时会设置 mission creation aborted。
- `BaseHubMission.accept(...)` 要求 starting stage 已存在。
- `BaseHubMission.abort()` 遍历已记录变更并执行对应 abort 逻辑。
- `BaseHubMission.getReason()` 提供重要对象和 memory flag 的 reason。
- `BaseHubMission.setNoAbandon()` 控制任务是否显示放弃入口。
- `BaseHubMission.setTimeLimit(...)` 绑定限时、失败阶段和可暂停限时的系统条件。
- `BaseMissionIntel.MissionState` 保存 posted、cancelled、failed、accepted、abandoned 和 completed。
- `CampaignMissionPlugin.cleanup()` 只在 mission board 移除且 `withCleanup` 为 true 时调用。
- `HubMission.createAndAbortIfFailed(market, barEvent)` 接收 market 和是否来自 bar 展示的布尔值。
- `HubMission.setGenRandom(random)` 写入本次 mission 生成使用的随机源。
- `HubMissionCreator.isPriority()` 控制 creator 进入 priority picker。
- `HubMissionWithSearch` 的 require 条件用于过滤候选对象。
- `HubMissionWithSearch` 的 prefer 条件用于候选对象偏好排序。
- `MissionBoardAPI.getAvailabilityForMission(id)` 按 mission id 返回可用地点集合。
- `MissionTrigger.endTrigger()` 将当前 trigger 写入 mission trigger 列表。

## 陷阱

- `BaseHubMission.accept(...)` 在 starting stage 缺失时抛出运行时异常。
- `BaseHubMission.abort()` 会移除由 mission 注册的临时实体、人物、重要标记和 memory 变更。
- `BaseMissionIntel` 的 posted 状态会执行随机撤回检查，任务接受前的实例状态会继续推进。
- `HubMissionBarEventWrapper.shouldShowAtMarket(market)` 会在 market 检查前 abort 当前 mission。
- `HubMissionCreator.matchesRep(rep)` 参与 creator 抽取，声望边界会影响 mission 是否可被创建。
- `HubMissionWithTriggers.endTrigger()` 在缺少对应 begin 入口时抛出运行时异常。
- `MissionBoardAPI.removeMission(mission, false)` 移除 mission board 可用性时不会调用 mission cleanup。
