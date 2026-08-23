# 生涯酒吧

来源：原版 Starsector。

## 定义

生涯酒吧是 portside bar 数据、事件生成、事件显示、mission 酒吧包装和临时 bar 选项的原版接口集合。

## 参考

- `com.fs.starfarer.api.impl.campaign.CoreLifecyclePluginImpl.addBarEvents()`：注册 `PortsideBarData`、原版手写 creator。
- `com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarData`：保存当前 active bar event 列表。
- `com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent`：定义 bar event 接口。
- `com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager`：管理 bar event creator 与抽取。
- `com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEvent`：提供 `PortsideBarEvent` 默认实现。
- `com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator`：提供非 CSV creator 基类。
- `com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson`：提供带人物 bar event 基类。
- `com.fs.starfarer.api.impl.campaign.intel.bar.events.SpecBarEventCreator`：把 `BarEventSpec` 转换为 runtime event。
- `com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionBarEventWrapper`：把 mission plugin 包装为 bar event。
- `com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent`：提供 mission 在指定 market 的 bar 显示判定。
- `com.fs.starfarer.api.impl.campaign.rulecmd.missions.BarCMD`：负责 bar UI 选项与事件流程。
- `com.fs.starfarer.api.impl.campaign.rulecmd.salvage.AddBarEvent`：通过 market memory 写入临时 bar 选项。
- `com.fs.starfarer.api.impl.campaign.rulecmd.salvage.RemoveBarEvent`：按 option id 移除临时 bar 选项。
- `com.fs.starfarer.api.loading.BarEventSpec`：承载 bar event 静态配置。
- `data/campaign/bar_events.csv`：注册 bar event 表。

## 边界

- `AddBarEvent` 归属临时 bar 选项写入。
- `BarCMD` 归属 bar UI 选项展示、事件流程。
- `BarEventManager` 归属 creator 集合、active 与 timeout tracker。
- `BarEventSpec` 归属 CSV 到 event 或 mission plugin 的实例化。
- `BaseBarEvent` 归属 event 对话状态、显示市场限制。
- `BaseBarEventCreator` 归属非 CSV creator 默认持续时间、超时、权重。
- `BaseBarEventWithPerson` 归属人物生成与按市场稳定随机。
- `HubMissionBarEventWrapper` 归属 mission plugin 到 `PortsideBarEvent` 的酒吧包装。
- `HubMissionWithBarEvent.shouldShowAtMarket(market)` 在本模块只作为 mission bar wrapper 的显示判定调用点。
- `PortsideBarData` 归属当前可显示 bar event 列表。
- `PortsideBarEvent` 归属 bar event 对话入口与选项响应。
- `RemoveBarEvent` 归属临时 bar 选项按 option id 删除。
- `SpecBarEventCreator` 归属 `BarEventSpec` 到 event 或 mission wrapper 的创建分派。

## 链路

### CSV 加载链路

1. 游戏读取 `data/campaign/bar_events.csv`。
2. `bar event id` 列注册 bar event spec key。
3. `plugin` 列写入 `BarEventSpec.getPluginClass()`。
4. 其它参数写入 `BarEventSpec`。
5. `SettingsAPI.getAllBarEventSpecs()` 暴露全部 bar event spec。
6. `BarEventManager.updateBarEventCreatorsFromSpecs()` 读取 spec 列表。
7. 每个缺失 creator 的 spec 创建一个 `SpecBarEventCreator`。
8. `SpecBarEventCreator` 持有 spec id 并按需从 settings 读取 `BarEventSpec`。

### Bar Manager 注册链路

1. `CoreLifecyclePluginImpl.addBarEvents()` 取得 `Global.getSector()`。
2. Sector 缺少 `PortsideBarData` 时调用 `addScript(new PortsideBarData())`。
3. `PortsideBarData` 构造时用 `PortsideBarData.KEY` 写入 sector memory。
4. Sector 缺少 `BarEventManager` 时调用 `addScript(new BarEventManager())`。
5. `BarEventManager` 构造时用 `BarEventManager.KEY` 写入 sector memory。
6. `BarEventManager.readResolve()` 调用 `updateBarEventCreatorsFromSpecs()`。
7. `CoreLifecyclePluginImpl.addBarEvents()` 将原版手写 creator 注册到 manager。

### Bar Event 生成链路

1. 生涯推进调用 `BarEventManager.advance(amount)`。
2. manager 将秒数转换为 campaign days。
3. active tracker 与 timeout tracker 按天数推进。
4. manager 清理不在 active tracker 中的 orphaned event。
5. manager 按 priority picker 先抽取 priority creator。
6. priority 未命中时按普通 picker 抽取 creator。
7. creator 调用 `createBarEvent()`。
8. manager 用 active duration 把 event 加入 active tracker。
9. manager 记录 event 到 creator 的映射。
10. `PortsideBarData.addEvent(event)` 将 event 加入 active 列表。

### Bar Event 显示链路

1. `BarCMD.showOptions(...)` 取得当前 market。
2. `BarCMD` 清空 market 上的临时 bar event memory。
3. `BarCMD` 触发 `AddBarEvents` 规则收集临时 option。
4. `BarCMD` 从 `PortsideBarData.getEvents()` 读取 active event。
5. event 先通过 `shouldRemoveEvent()` 和 `shouldShowAtMarket(market)`。
6. event 调用 `addPromptAndOption(dialog, memoryMap)` 写入 bar 提示和选项。
7. event 调用 `wasShownAtMarket(market)` 记录显示 market。
8. 玩家选择 `PortsideBarEvent` 选项时进入 `BarEventDialogPlugin`。
9. event 调用 `init(dialog, memoryMap)` 接管对话。
10. event 调用 `optionSelected(optionText, optionData)` 处理选择。
11. event 通过 `isDialogFinished()` 和 `endWithContinue()` 决定返回 bar 时是否显示 continue。

### Mission Bar 包装链路

1. `SpecBarEventCreator.createBarEvent()` 调用 `BarEventSpec.isMission()`。
2. spec plugin 是 `HubMissionWithBarEvent` 时创建 `HubMissionBarEventWrapper`。
3. wrapper 在 `shouldShowAtMarket(market)` 中按 `seed + market.getId().hashCode() * 181783497276652981` 创建 random。
4. wrapper 用 `BarEventSpec.getProb()` 判定本次市场显示概率。
5. wrapper 调用 `BarEventSpec.createMission()`。
6. wrapper 设置 mission id 与 generation random。
7. wrapper 调用 `HubMissionWithBarEvent.shouldShowAtMarket(market)`。
8. 显示时 wrapper 调用 `mission.createAndAbortIfFailed(market, true)`。
9. wrapper 调用 `mission.updateInteractionData(dialog, memoryMap)`。
10. wrapper 触发 `mission.getTriggerPrefix() + "_blurbBar true"`。
11. wrapper 触发 `mission.getTriggerPrefix() + "_optionBar true"`。

### 临时 Bar 选项链路

1. 规则系统执行 `AddBarEvent <option id> <option text> <blurb>`。
2. `AddBarEvent` 从 dialog interaction target 取得 market。
3. `AddBarEvent.getTempEvents(market)` 读取或创建 `$core_tempBarEvents`。
4. `AddBarEvent` 对 option text 与 blurb 执行 token replacement。
5. `AddBarEvent` 以 option id 写入 `TempBarEvents.events`。
6. `BarCMD.showOptions(...)` 读取 temp events 并写入文本与 option。
7. 规则系统执行 `RemoveBarEvent <option id>`。
8. `RemoveBarEvent` 取得 market 后调用 `AddBarEvent.removeTempEvent(market, optionId)`。

## 规范

- `AddBarEvent.KEY` 管理 market memory 中的临时 bar option 容器，`BarCMD.showOptions(...)` 读取前重建该容器。
- `BarEventManager.KEY` 管理 sector memory 中的全局 bar manager 实例，注册入口为 `CoreLifecyclePluginImpl.addBarEvents()`。
- `BarEventManager.runWhilePaused()` 控制 active tracker 与 timeout tracker 的推进时间边界。
- `BarEventSpec.createEvent()` 按 spec plugin class 创建普通 `PortsideBarEvent`。
- `BarEventSpec.createMission()` 按 spec plugin class 创建可被 wrapper 显示的 mission plugin。
- `BaseBarEvent.endWithContinue()` 控制 event 结束后返回 bar 时的 continue option。
- `BaseBarEvent.getBarEventId()` 提供 event active 与 timeout 状态归并 id。
- `BaseBarEvent.shouldShowAtMarket(market)` 按 `shownAt` 限制 event 的跨市场复用。
- `BaseBarEvent.wasShownAtMarket(market)` 写入 event 的显示 market 归属。
- `BaseBarEventCreator.getBarEventAcceptedTimeoutDuration()` 提供已接受 event 的 creator timeout 时长。
- `BaseBarEventCreator.getBarEventActiveDuration()` 提供 event 写入 active tracker 的时长。
- `BaseBarEventCreator.getBarEventFrequencyWeight()` 提供普通 picker 抽取权重。
- `BaseBarEventCreator.getBarEventId()` 提供 creator active 与 timeout 状态归并 id。
- `BaseBarEventCreator.getBarEventTimeoutDuration()` 提供 creator 抽取后的 timeout 时长。
- `BaseBarEventCreator.isPriority()` 控制 creator 是否进入 priority picker。
- `BaseBarEventWithPerson.regen(market)` 按 market id 派生随机源并生成 market 稳定人物。
- `HubMissionBarEventWrapper` 用 `BarEventSpec.getProb()` 判定指定 market 的 mission bar 显示。
- `PortsideBarData.KEY` 管理 sector memory 中的 active bar event 列表容器。
- `PortsideBarData.runWhilePaused()` 控制 active event 列表维护的推进时间边界。
- `SpecBarEventCreator.isPriority()` 用 `Tags.MISSION_PRIORITY` 将 spec creator 分流到 priority picker。

## 陷阱

- `BarCMD` 会在每次展示前清空 `$core_tempBarEvents` 并重新触发 `AddBarEvents`，临时 option 不能当作长期状态读取。
- `BaseBarEvent.shouldShowAtMarket(market)` 默认受 `shownAt` 限制，同一 event 显示过的 market 会影响后续市场可见性。
- `BaseBarEventWithPerson.regen(market)` 在 market 对象不变时不会重建人物和 random。
- `HubMissionBarEventWrapper.addPromptAndOption(...)` 可能因 mission 创建失败而停止写入 bar option。
- `HubMissionBarEventWrapper.shouldShowAtMarket(market)` 每次检查前会 abort 当前 mission，bar 显示检查不提供稳定 mission 实例。
- `RemoveBarEvent` 只删除 market memory 中对应 option id 的临时 bar option，不会移除 `PortsideBarData` 中的 active event。
- `SpecBarEventCreator` 根据 plugin 是否是 `HubMissionWithBarEvent` 决定创建 wrapper 或普通 event，CSV plugin 类型写错会导致创建分派语义错位。
