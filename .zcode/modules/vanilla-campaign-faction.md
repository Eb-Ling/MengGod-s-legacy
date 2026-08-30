# 生涯势力

来源：原版 Starsector。

## 定义

生涯势力是 faction 身份、声望关系、doctrine、production、已知资源、随机人物和势力规格的原版接口集合。

## 参考

- `com.fs.starfarer.api.campaign.FactionAPI`：维护势力身份、显示、关系、资源、ship role picker、随机人物、doctrine、production 和 spec。
- `com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode`：定义舰船选择模式。
- `com.fs.starfarer.api.campaign.FactionAPI.ShipPickParams`：定义舰船选择的目标 FP、模式、时间戳、fleet 过滤和 fallback 控制。
- `com.fs.starfarer.api.campaign.FactionDoctrineAPI`：维护舰队组成倾向、质量、officer、侵略性、严格组成和技能 shuffle。
- `com.fs.starfarer.api.campaign.FactionProductionAPI`：维护势力月生产能力、生产条目、current/interrupted 集合和集结点。
- `com.fs.starfarer.api.campaign.FactionSpecAPI`：维护 `.faction` 加载后的显示、颜色、头像池、名称池、资源池、sell frequency、variant override 和 doctrine。
- `com.fs.starfarer.api.campaign.RepLevel`：定义声望等级、阈值、正负/中立判断和 float 到等级映射。
- `com.fs.starfarer.api.campaign.ReputationActionResponsePlugin`：处理玩家对 faction 或 person 的声望 action 与实际 delta。
- `com.fs.starfarer.api.campaign.SectorAPI.getFaction(...)`：提供 Sector 到单个势力对象的入口。

## 边界

- `FactionAPI` 归属势力 id、显示名、基础 UI 颜色、logo、crest、音乐和 bar 音效。
- `FactionAPI` 归属势力 memory、custom 数据、NPC 是否在 intel 显示和 player faction 标记。
- `FactionAPI` 归属非法商品、tariff、toll、fine、internal comms 和势力经济性字段。
- `FactionAPI` 归属与其它势力、玩家 faction、人物对象之间的关系读写。
- `FactionAPI` 归属已知 ship、weapon、fighter、hullmod、industry 与 priority 资源集合。
- `FactionAPI` 归属 ship role picker、随机舰名、随机人物、rank/post、personality 和 voice 选择。
- `FactionAPI` 归属 live doctrine、production、faction spec 和每 fleet 近似最大 FP。
- `FactionDoctrineAPI` 归属 fleet size、ship size、quality、officer quality、aggression、composition 和 skill shuffle 数据。
- `FactionProductionAPI` 归属生产能力、生产条目、current/interrupted 状态、gathering point、cost mult 和 faction 归属。
- `FactionSpecAPI` 归属 `.faction` 文件加载后的静态显示、资源池、头像池、名称池、variant override 和 doctrine 定义。
- `RepLevel` 归属声望 float 与离散等级之间的映射、阈值和等级方向判断。
- `ReputationActionResponsePlugin` 归属玩家声望 action 到实际声望变化结果的响应。
- `SectorAPI.getAllFactions()` 归属 Sector 内全部势力集合入口。
- `SectorAPI.getFaction(id)` 归属按 id 取得 live faction 对象。
- `SectorAPI.getPlayerFaction()` 归属玩家 faction 对象入口。

## 链路

### Sector 势力入口链路

1. 代码取得 `Global.getSector()`。
2. 调用 `SectorAPI.getFaction(id)`、`getAllFactions()` 或 `getPlayerFaction()`。
3. Sector 返回 live `FactionAPI` 对象或集合。
4. 调用方在 `FactionAPI` 上读取显示、关系、资源、doctrine、production 或 spec 数据。

### 声望调整链路

1. 代码调用 `SectorAPI.adjustPlayerReputation(action, factionId)` 或 `adjustPlayerReputation(action, person)`。
2. Sector 通过 campaign plugin picker 选择 `ReputationActionResponsePlugin`。
3. response plugin 按 action 和目标对象计算实际声望变化。
4. response plugin 返回 `ReputationAdjustmentResult`。
5. 调用方读取 `ReputationAdjustmentResult.delta` 判断实际变化量。

### 势力关系链路

1. 代码取得两个势力的 `FactionAPI`。
2. 调用 `setRelationship(...)` 或 `adjustRelationship(...)` 写入关系数值。
3. 调用 `ensureAtBest(...)`、`ensureAtWorst(...)`、`isAtWorst(...)` 或 `isAtBest(...)` 应用等级边界。
4. 调用 `getRelationshipLevel(...)` 或 `RepLevel.getLevelFor(value)` 取得离散声望等级。
5. 调用 `isHostileTo(...)`、`isNeutralFaction(...)` 或 `isPlayerFaction()` 判断特殊关系状态。

### 随机人物链路

1. 代码取得目标势力的 `FactionAPI`。
2. 调用 `createRandomPerson()`、`createRandomPerson(gender)` 或 `createRandomPerson(random)`。
3. Faction 按 spec 的姓名池、头像池、personality、voice、rank/post 数据创建 `PersonAPI`。
4. 调用方在返回的 `PersonAPI` 上写入 id、market、fleet、important people 或其它人物字段。

### Ship Role 选择链路

1. 代码创建 `FactionAPI.ShipPickParams`。
2. 写入 `mode`、`maxFP`、`timestamp`、`filter` 或 `blockFallback`。
3. 调用 `FactionAPI.pickShip(role, params, random)` 或 `pickShipAndAddToFleet(role, params, fleet, random)`。
4. Faction 按 role、已知舰船、priority、variant override 和可用性选择 `ShipRolePick`。
5. `pickShipAndAddToFleet(...)` 将选择结果写入目标舰队。

### 已知资源链路

1. 代码调用 add/remove 入口写入 known 或 priority 资源集合。
2. 需要按角色重新选择舰船时调用 `clearShipRoleCache()`。
3. 调用 `knowsShip(...)`、`knowsWeapon(...)`、`knowsFighter(...)`、`knowsHullMod(...)` 或 `knowsIndustry(...)` 读取已知状态。
4. 调用 priority 读取入口取得优先使用资源集合。
5. 调用 sell frequency、variant override 或 role availability 入口读取市场与舰船选择辅助数据。

### Doctrine 与生产链路

1. 调用 `FactionAPI.getDoctrine()` 取得 live doctrine。
2. 调用 doctrine 的 ship、quality、officer、aggression 和 composition 入口读取或写入倾向。
3. 调用 `FactionAPI.getProduction()` 取得 production。
4. 调用 production 的 add/remove/current/interrupted 入口维护生产条目。
5. 调用 `FactionAPI.getFactionSpec()` 取得 `.faction` 加载后的 spec 数据。

## 规范

- `FactionAPI.createRandomPerson(...)` 按势力数据生成人物对象，返回对象字段归属仍在 `PersonAPI`。
- `FactionAPI.getFactionSpec()` 返回 `.faction` 加载后形成的 spec 对象。
- `FactionAPI.pickShipAndAddToFleet(...)` 在 fallback 路径下可能向 fleet 加入多个舰队成员。
- `FactionAPI.removeKnownShip(...)`、`removeKnownWeapon(...)` 和 `removeKnownFighter(...)` 的注释要求在载入后重新移除原 `.faction` 定义的蓝图。
- `FactionAPI.setShipTimestamp(hullId, timestamp)` 和 `isShipKnownAt(hullId, timestamp)` 用于指定时间戳下的舰船可用性判断。
- `FactionDoctrineAPI.getShipQualityContribution()` 以 `(shipQuality - 1) * doctrineFleetQualityPerPoint` 计算质量贡献。
- `FactionProductionAPI.ProductionItemType` 的原版枚举值为 `SHIP`、`FIGHTER` 和 `WEAPON`。
- `FactionProductionAPI.getMonthlyProductionCapacity()` 使用船舰与武器商品的生产能力换算规则返回月生产能力。
- `RepLevel.getLevelFor(value)` 将声望 float 映射到离散等级。
- `RepLevel.getRepInt(value)` 将声望 float 乘以 `100` 后四舍五入为整数。
- `RepLevel` 的原版等级从负到正为 `VENGEFUL`、`HOSTILE`、`INHOSPITABLE`、`SUSPICIOUS`、`NEUTRAL`、`FAVORABLE`、`WELCOMING`、`FRIENDLY` 和 `COOPERATIVE`。
- `RepLevel` 的原版阈值常量为 `0.09`、`0.24`、`0.49` 和 `0.74`。
- `ReputationActionResponsePlugin.handlePlayerReputationAction(...)` 返回的 `ReputationAdjustmentResult.delta` 表示实际声望变化。
- `SectorAPI.adjustPlayerReputation(...)` 通过最高优先级 reputation response plugin 处理 action。

## 陷阱

- `FactionAPI.getCustomBoolean(key)`、`getCustomFloat(key)` 和 `getCustomInteger(key)` 按 custom map 取值，缺失 key 会产生默认值语义而非业务层校验。
- `FactionAPI.removeKnown...(...)` 不会阻止 `.faction` 文件内容在载入后重新加入。
- `FactionAPI` 直接改动已知资源集合后未清理 ship role cache，会让再次进行的 role 选择继续使用旧缓存。
- `RepLevel.getMin()` 返回的下界不包含在等级区间内，`getMax()` 返回的上界包含在等级区间内。
- `ReputationActionResponsePlugin` 接收的是 action 对象，实际 delta 由被选中的 response plugin 决定。
- `ShipPickParams.timestamp` 参与可用性判断，当前已知资源集合不能替代指定时间戳下的可用性检查。
