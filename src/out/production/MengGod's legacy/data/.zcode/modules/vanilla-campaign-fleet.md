# 生涯舰队

来源：原版 Starsector。

## 定义

生涯舰队是原版生涯地图中承载舰队实体、成员集合、任务队列、AI 决策、生成和膨胀状态的接口集合。

## 参考

- `com.fs.starfarer.api.campaign.CampaignFleetAPI`：提供生涯舰队实体、位置、AI、任务、统计、同步和膨胀入口。
- `com.fs.starfarer.api.campaign.FleetAssignment`：提供舰队任务类型、任务显示文本和目标名拼接标记。
- `com.fs.starfarer.api.campaign.FleetDataAPI`：提供舰队成员、军官、旗舰、快照、同步和容量更新入口。
- `com.fs.starfarer.api.campaign.FleetInflater`：提供舰队装配膨胀、质量参数和 S-mod 数量入口。
- `com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI`：提供舰队按帧 AI、遭遇选择、追击选择、登舰选择和任务队列入口。
- `com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI`：提供单个舰队任务的目标、时长、文本、完成脚本和自定义状态入口。
- `com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3`：提供原版通用舰队生成入口。
- `com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3`：提供原版通用舰队生成参数。

## 边界

- `CampaignFleetAIAPI` 归属舰队行动选择、遭遇选择、追击选择、登舰选择和 assignment 队列控制。
- `CampaignFleetAPI` 归属生涯地图舰队实体身份、位置、速度、派系、名称、战斗绑定和传感器状态。
- `CampaignFleetAPI.forceSync()` 归属舰队组成、cargo 容量、船员需求、fleet stats 和舰队效果重算边界。
- `CampaignFleetAPI.getBattle()` 和 `setBattle(...)` 归属舰队与当前 campaign battle 的绑定入口。
- `CampaignFleetAPI.getInflater()` 归属舰队成员从保存态到完整装配态的膨胀入口。
- `CampaignFleetAPI.getLocation()` 归属坐标读取，舰队坐标写入归属 `setLocation(...)`。
- `CampaignFleetAPI.isHostileTo(...)` 归属舰队对目标的生涯敌对判定。
- `CampaignFleetAPI.setAIMode(...)` 归属 AI 舰队后勤消耗和 crew requirement 开关。
- `FleetAssignmentDataAPI` 归属单个 assignment 的目标、时长、文本、脚本和 custom 状态。
- `FleetAssignment` 归属 assignment 类型枚举和默认显示文本。
- `FleetDataAPI` 归属舰队成员集合、军官集合、旗舰、成员排序、成员同步和成员快照入口。
- `FleetDataAPI.getCacheClearedOnSync()` 归属随舰队同步清理的计算缓存。
- `FleetFactoryV3` 归属按 `FleetParamsV3`、派系 doctrine、市场质量和舰船 role 生成舰队。
- `FleetInflater` 归属成员装配质量、S-mod 数量、膨胀后移除标记和 params 暴露。
- `LocationAPI` 归属 location 对舰队集合的容器关系。

## 链路

### 舰队生成链路

1. 代码创建 `FleetParamsV3`。
2. `FleetParamsV3` 写入 source、locInHyper、factionId、fleetType、各类 fleet point 目标和质量参数。
3. 调用 `FleetFactoryV3.createFleet(params)`。
4. `FleetFactoryV3` 选择 create fleet plugin。
5. 未取得插件结果时，`FleetFactoryV3` 按市场、派系 doctrine、ship pick mode 和 fleet point 目标添加成员。
6. `FleetFactoryV3` 添加 commander 和 officers。
7. `CampaignFleetAPI.forceSync()` 同步舰队成员、容量、stats 和舰队效果。
8. `FleetFactoryV3` 创建 `FleetInflater` 并写入舰队。
9. `FleetDataAPI` 关闭 only sync member lists 并排序成员。
10. 生成结果以 `CampaignFleetAPI` 返回。

### 舰队任务链路

1. 代码取得 `CampaignFleetAPI`。
2. 调用 `addAssignment(...)` 或 `getAI().addAssignment(...)` 写入 `FleetAssignment`、target、duration、action text 和脚本。
3. `CampaignFleetAIAPI` 将任务保存为 `FleetAssignmentDataAPI`。
4. 生涯推进调用 `CampaignFleetAIAPI.advance(amount)`。
5. AI 读取当前 assignment、目标和舰队状态。
6. assignment 过期、完成或被移除后，AI 推进下一个 assignment。
7. `clearAssignments()` 清空队列。

### FleetData 集合链路

1. 代码取得 `CampaignFleetAPI.getFleetData()`。
2. `FleetDataAPI.addFleetMember(...)`、`removeFleetMember(...)` 或 `scuttle(...)` 修改成员集合。
3. `FleetDataAPI.addOfficer(...)` 或 `removeOfficer(...)` 修改 officer 集合。
4. `FleetDataAPI.setFlagship(...)` 设置旗舰。
5. `FleetDataAPI.setSyncNeeded()` 标记同步需求。
6. `FleetDataAPI.syncIfNeeded()` 或 `CampaignFleetAPI.forceSync()` 执行同步。
7. `FleetDataAPI.updateCargoCapacities()` 更新 cargo 容量。
8. `FleetDataAPI.getMembersListCopy()` 向调用方返回成员副本。

### 舰队膨胀链路

1. 代码取得 `CampaignFleetAPI`。
2. 调用 `getInflater()` 读取当前 `FleetInflater`。
3. 调用 `inflateIfNeeded()` 触发按需膨胀。
4. `FleetInflater.inflate(fleet)` 根据 params、quality 和 average S-mod 数量补全成员装配。
5. `removeAfterInflating()` 为 true 时，膨胀后 inflater 可从舰队移除。
6. 调用 `deflate()` 可回到保存态表达。

### 舰队位置链路

1. 代码取得 `CampaignFleetAPI`。
2. 调用 `getContainingLocation()` 读取舰队所在 location。
3. 调用 `getLocation()` 读取舰队当前坐标。
4. 调用 `setLocation(x, y)` 写入舰队当前坐标。
5. 调用 `setMoveDestination(...)` 写入普通移动目标。
6. 调用 `setMoveDestinationOverride(...)` 写入覆盖移动目标。
7. 调用 `setVelocity(...)` 写入当前速度。

### 舰队遭遇决策链路

1. 生涯遭遇取得两支 `CampaignFleetAPI`。
2. 调用 `CampaignFleetAPI.isHostileTo(...)` 或 `CampaignFleetAIAPI.isHostileTo(...)` 判断敌对意图。
3. 调用 `pickEncounterOption(...)` 选择接战、脱离或保持。
4. 战后调用 `pickPursuitOption(...)` 选择追击、骚扰或放行。
5. 需要登舰时调用 `pickBoardingResponse(...)`。
6. 调用 `pickBoardingTaskForce(...)` 和 `makeBoardingDecision(...)` 选择登舰成员与动作。
7. 逃离前可调用 `performCrashMothballingPriorToEscape(...)`。

## 规范

- `CampaignFleetAIAPI.getAssignmentsCopy()` 返回 assignment 队列副本。
- `CampaignFleetAPI.addAssignment(...)` 可携带 completion script、action text、start script 和 add-time-to-next 语义。
- `CampaignFleetAPI.deflate()` 将舰队成员回到保存态表达。
- `CampaignFleetAPI.despawn(reason, param)` 使用 `FleetDespawnReason` 和附加参数表达 despawn 原因。
- `CampaignFleetAPI.getCurrBurnLevel()` 返回当前 burn level。
- `CampaignFleetAPI.getBattle()` 返回舰队当前绑定的 campaign battle。
- `CampaignFleetAPI.getFleetData()` 返回舰队成员集合入口。
- `CampaignFleetAPI.getFleetPoints()` 返回当前舰队 fleet point 总量。
- `CampaignFleetAPI.getMembersWithFightersCopy()` 返回包含 fighter 的成员副本。
- `CampaignFleetAPI.getStats()` 返回 `MutableFleetStatsAPI`。
- `CampaignFleetAPI.inflateIfNeeded()` 使用当前 inflater 处理按需膨胀。
- `CampaignFleetAPI.isValidPlayerFleet()` 要求舰队有成员且成员集合含非 fighter wing。
- `CampaignFleetAPI.setLocation(x, y)` 是舰队坐标写入入口。
- `FleetDataAPI.getBurnLevel()` 返回包含深层超空间影响的有效 burn level。
- `FleetDataAPI.getCacheClearedOnSync()` 返回随 sync 清理的缓存 map。
- `FleetDataAPI.getMembersInPriorityOrder()` 按 logistical priority、非 mothballed、mothballed 顺序返回成员。
- `FleetDataAPI.getMinBurnLevel()` 包含 fleetwide max burn 修正。
- `FleetDataAPI.getSnapshot()` 返回 `takeSnapshot()` 时的 transient 成员快照。
- `FleetDataAPI.scuttle(member)` 移除成员并把拆解收益与已装武器加入 cargo。
- `FleetDataAPI.setFlagship(flagship)` 写入成员集合中的旗舰。
- `FleetInflater.getAverageNumSMods()` 返回膨胀使用的平均 S-mod 数量。
- `FleetInflater.getParams()` 返回 inflater 参数对象。
- `FleetParamsV3.getTotalPts()` 返回 combat、freighter、tanker、transport、liner 和 utility 点数之和。
- `FleetParamsV3.setSource(source, true)` 会更新基于 source market 的质量。
- `FleetParamsV3.updateQualityAndProducerFromSourceMarket()` 使用 source market 更新舰船质量。

## 陷阱

- `CampaignFleetAPI.getLocation()` 注释要求移动舰队时使用 `setLocation(...)`。
- `CampaignFleetAPI.setMoveDestination(...)` 同时会被 AI 每帧调用，直接用它长期控制舰队会被 AI 覆盖。
- `FleetDataAPI.getSnapshot()` 是 transient 快照，存档持久状态应来自成员集合。
- `FleetDataAPI.setFlagship(...)` 会改写成员集合中的旗舰状态。
- `FleetInflater.getParams()` 返回 `Object`，调用方需要确认实际参数类型。
- `FleetParamsV3.source` 直接赋值时不会自动使用市场质量，需调用 `setSource(...)` 或 `updateQualityAndProducerFromSourceMarket()`。
- `FleetParamsV3` 的 fleet point 字段是生成目标值。
