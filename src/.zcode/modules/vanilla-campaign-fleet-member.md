# 生涯舰队成员

来源：原版 Starsector。

## 定义

生涯舰队成员是 `FleetMemberAPI` 承载的舰船或战机联队保存态、状态、装配、舰长、数值和地图视图接口集合。

## 参考

- `com.fs.starfarer.api.FactoryAPI.createFleetMember(...)`：提供按成员类型、variant id、wing id 或 `ShipVariantAPI` 创建舰队成员的工厂入口。
- `com.fs.starfarer.api.SettingsAPI.createFleetMember(...)`：提供 settings 侧舰队成员创建入口。
- `com.fs.starfarer.api.campaign.FleetDataAPI`：提供成员加入集合、移除集合、旗舰、快照、按 captain 查询和排序入口。
- `com.fs.starfarer.api.fleet.FleetMemberAPI`：提供舰队成员身份、类型、舰体、装配、舰长、数值、维修、部署和保存态入口。
- `com.fs.starfarer.api.fleet.FleetMemberStatusAPI`：提供舰队成员船体损伤、维修、瘫痪、分离部件和弹药状态入口。
- `com.fs.starfarer.api.fleet.FleetMemberType`：定义舰队成员类型枚举。
- `com.fs.starfarer.api.fleet.FleetMemberViewAPI`：提供 campaign view 中舰队成员视觉偏移、抖动、引擎、尾迹和 glow 状态入口。

## 边界

- `FactoryAPI.createFleetMember(...)` 归属舰队成员对象创建。
- `FleetDataAPI.addFleetMember(...)` 与 `removeFleetMember(...)` 归属成员进入或离开舰队成员集合。
- `FleetDataAPI.getMemberWithCaptain(...)` 归属按 captain 查询成员。
- `FleetDataAPI.getMembersListCopy()` 与 `getMembersInPriorityOrder()` 归属成员集合读取。
- `FleetDataAPI.getSnapshot()` 归属成员集合 transient 快照读取。
- `FleetDataAPI.setFlagship(...)` 归属成员集合中的旗舰指定。
- `FleetMemberAPI` 归属单个舰队成员保存态。
- `FleetMemberAPI.getCaptain()` 与 `setCaptain(...)` 归属成员侧舰长读写入口。
- `FleetMemberAPI.getFleetData()` 归属成员回到所属 fleet data 的桥接入口。
- `FleetMemberAPI.getStats()` 归属成员关联的 mutable ship stats 入口。
- `FleetMemberAPI.getVariant()` 与 `setVariant(...)` 归属成员保存态装配读写入口。
- `FleetMemberAPI.isAlly()` 与 `setAlly(...)` 归属战斗中玩家侧非玩家控制舰船的 transient 标记。
- `FleetMemberStatusAPI` 归属成员船体损伤、维修、瘫痪和分离部件状态。
- `FleetMemberType` 归属 `SHIP`、`FIGHTER_WING` 和 `NULL` 成员类型。
- `FleetMemberViewAPI` 归属 campaign view 中成员视觉状态。
- `SettingsAPI.createFleetMember(...)` 归属 settings 侧成员创建。

## 链路

### 成员创建链路

1. 调用方准备 `FleetMemberType` 与 variant id、wing id 或 `ShipVariantAPI`。
2. 调用 `FactoryAPI.createFleetMember(...)` 或 `SettingsAPI.createFleetMember(...)`。
3. 创建结果生成 `FleetMemberAPI`。
4. 成员保存 id、type、hull spec、variant、ship name、owner 和 source mod。
5. 调用方把成员交给 `FleetDataAPI.addFleetMember(...)` 或其它成员消费入口。

### 成员集合链路

1. 调用方取得 `FleetDataAPI`。
2. 调用 `addFleetMember(...)`、`removeFleetMember(...)` 或 `scuttle(...)` 修改成员集合。
3. 调用 `setFlagship(...)` 指定集合中的旗舰。
4. 调用 `takeSnapshot()` 保存 transient 成员快照。
5. 调用 `getMembersListCopy()`、`getMembersInPriorityOrder()` 或 `getSnapshot()` 读取成员列表。
6. 调用 `getMemberWithCaptain(captain)` 按人物查找成员。

### 成员保存态链路

1. 代码取得 `FleetMemberAPI`。
2. 调用 `getId()`、`getType()`、`getHullId()`、`getSpecId()` 或 `getHullSpec()` 读取成员身份与舰体。
3. 调用 `getVariant()` 或 `setVariant(...)` 读取或写入成员装配。
4. 调用 `getCaptain()` 或 `setCaptain(...)` 读取或写入成员舰长。
5. 调用 `getStats()`、`setStatUpdateNeeded(...)` 或 `updateStats()` 处理成员数值入口。
6. 调用 `getFleetData()`、`getOwner()`、`setOwner(...)` 或 `isFlagship()` 读取成员所属与标记。

### 成员状态链路

1. 代码调用 `FleetMemberAPI.getStatus()`。
2. 调用 `getHullFraction()`、`getHullDamageTaken()` 或 `needsRepairs()` 读取状态。
3. 调用 `applyDamage(...)`、`applyHullFractionDamage(...)` 或 `disable()` 写入损伤状态。
4. 调用 `repairFully()`、`repairFullyNoNewFighters()`、`repairFraction(...)` 或 `repairDisabledABit()` 执行维修。
5. 对 fighter 或模块状态调用 indexed hull fraction、detached 和 perma detached 入口。
6. 调用 `resetDamageTaken()`、`resetAmmoState()` 或 `updateNumStatusesFromMember()` 刷新状态。

### 成员视图链路

1. campaign 视图取得 `FleetMemberViewAPI`。
2. 调用 `getMember()` 读取对应 `FleetMemberAPI`。
3. 调用 engine、contrail、glow 或 wind shifter 读取或写入视觉修饰。
4. 调用 `setJitter(...)`、`endJitter()`、`overrideOffset(...)` 或 jitter 参数入口写入临时视觉状态。

## 规范

- `FleetDataAPI.getSnapshot()` 返回 `takeSnapshot()` 时的 transient 成员快照。
- `FleetDataAPI.scuttle(member)` 会从成员集合移除成员并处理拆解收益与已装武器。
- `FleetDataAPI.setFlagship(flagship)` 写入成员集合的旗舰，并会处理其它成员 captain。
- `FleetMemberAPI.getBaseBuyValue()` 与 `getBaseSellValue()` 不包含 tariff。
- `FleetMemberAPI.getBaseValue()` 包含 hull、已装非 built-in 武器和 fighter LPC 的基础价值。
- `FleetMemberAPI.getDeployCost()` 返回 CR fraction，fighter wing 会乘以 fighter 数量。
- `FleetMemberAPI.getId()` 是成员唯一 id。
- `FleetMemberAPI.getMemberStrength()` 基于 fleet points、CR 和 variant 已用 OP，不按 hull status 或 captain quality 修正。
- `FleetMemberAPI.isAlly()` 是 transient 状态，不保存。
- `FleetMemberAPI.setFleetCommanderForStats(...)` 写入成员 stats 计算用的替代 commander 与 fleet data。
- `FleetMemberStatusAPI.getNumStatuses()` 返回 `1`、fighter 数量或模块数。
- `FleetMemberStatusAPI.resetDamageTaken()` 会清零后续 `getHullDamageTaken()` 的累计基线。
- `FleetMemberType` 的原版值为 `SHIP`、`FIGHTER_WING` 和 `NULL`。

## 陷阱

- `FleetDataAPI.getSnapshot()` 是 transient 快照，不能当作存档持久成员集合。
- `FleetDataAPI.setFlagship(...)` 会影响其它成员 captain，不能只当作单个成员布尔标记写入。
- `FleetMemberAPI.isAlly()` 只表示战斗中玩家侧非玩家控制舰船的 transient 标记。
- `FleetMemberAPI.setVariant(...)` 改写的是成员保存态，已部署战斗实体仍按战斗实体状态读取。
- `FleetMemberStatusAPI.applyHullFractionDamage(fraction, index)` 的 index 语义用于 fighter 或模块状态。
- `FleetMemberStatusAPI.getHullDamageTaken()` 依赖 `resetDamageTaken()` 的累计基线。
- `FleetMemberViewAPI` 写入的是 campaign view 视觉状态，不等同于成员保存态字段。
