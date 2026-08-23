# 装配 Variant API

来源：原版 Starsector。

## 定义

装配 Variant API 是 `.variant` 数据与 `ShipVariantAPI` 在数据加载、舰队成员、战斗舰船和数值计算之间交接舰船配置的接口集合。

## 参考

- `com.fs.starfarer.api.FactoryAPI.createFleetMember(...)`：提供装配 id 或 `ShipVariantAPI` 被舰队成员创建消费的入口。
- `com.fs.starfarer.api.SettingsAPI.createEmptyVariant(...)`：按 hull spec 创建空装配。
- `com.fs.starfarer.api.SettingsAPI.doesVariantExist(...)`：查询装配 id 是否已加载。
- `com.fs.starfarer.api.SettingsAPI.getAllVariantIds()`：返回已加载装配 id 集合。
- `com.fs.starfarer.api.SettingsAPI.getHullIdToVariantListMap()`：返回 hull id 到装配列表的映射。
- `com.fs.starfarer.api.SettingsAPI.getVariant(...)`：按装配 id 读取加载后的 `ShipVariantAPI`。
- `com.fs.starfarer.api.combat.MutableShipStatsAPI.getVariant()`：从 stats 持有者读取当前装配。
- `com.fs.starfarer.api.combat.ShipAPI.getVariant()`：从战斗舰船实体读取当前装配。
- `com.fs.starfarer.api.combat.ShipAPI.cloneVariant()`：让战斗舰船持有克隆后的装配对象。
- `com.fs.starfarer.api.combat.ShipAPI.setVariantForHullmodCheckOnly(...)`：为船体插件检查临时指定装配。
- `com.fs.starfarer.api.combat.ShipVariantAPI`：描述 hull、武器组、插件、机库、模块、OP、source、tag 和序列化状态。
- `com.fs.starfarer.api.fleet.FleetMemberAPI.getVariant()`：提供成员侧读取当前装配的桥接入口。
- `com.fs.starfarer.api.fleet.FleetMemberAPI.setVariant(...)`：提供成员侧写回当前装配的桥接入口。
- `com.fs.starfarer.api.loading.VariantSource`：描述装配来源类型。
- `data/variants/**/*.variant`：注册舰船和战机使用的装配 JSON 文件。

## 边界

- `.variant` 文件归属装配数据入口，承载 hull id、武器组、机库、插件、幅容、幅散、模块和目标装配标记。
- `FactoryAPI.createFleetMember(...)` 是装配 id 或 `ShipVariantAPI` 被舰队成员创建消费的入口。
- `FleetMemberAPI.getVariant()` 和 `setVariant(...)` 是成员保存态到当前 `ShipVariantAPI` 的读写桥接入口。
- `MutableShipStatsAPI.getVariant()` 归属 stats 持有者读取当前装配，不拥有装配生命周期。
- `SettingsAPI.getAllVariantIds()` 归属全局已加载装配枚举。
- `SettingsAPI.getHullIdToVariantListMap()` 归属 hull 到已加载装配的索引读取。
- `SettingsAPI.getVariant(...)` 归属全局已加载装配读取。
- `ShipAPI.getVariant()` 归属战斗中已部署舰船实体的当前装配读取。
- `ShipVariantAPI` 拥有当前装配的 hull spec、武器、武器组、hullmod、perma mod、S-mod、suppressed mod、fighter wing、module、tag 和 source 状态。
- `ShipVariantAPI.computeOPCost(...)`、`computeWeaponOPCost(...)` 和 `computeHullModOPCost(...)` 归属装配 OP 成本计算。
- `ShipVariantAPI.getOriginalVariant()` 和 `setOriginalVariant(...)` 归属 autofit 与 fleet inflater deflate 需要的目标装配记录。
- `ShipVariantAPI.getSource()` 和 `setSource(...)` 归属装配来源语义。
- `ShipVariantAPI.getStatsForOpCosts()` 归属装配 OP 成本计算使用的 stats 读取。
- `ShipVariantAPI.getVariantFilePath()` 归属已加载装配文件路径读取。
- `ShipVariantAPI.toJSONObject()` 归属装配当前状态序列化。
- `VariantSource` 归属装配从 stock、mission、refit 或 hull 空装配来的来源分类。

## 链路

### 装配资产加载链路

1. 游戏读取 `data/variants/**/*.variant`。
2. JSON 字段写入装配 id、hull id、显示名、目标装配标记、幅容、幅散、武器组、机库、插件和模块状态。
3. 加载结果注册为 `ShipVariantAPI`。
4. `SettingsAPI.getVariant(...)` 按装配 id 返回加载后的装配对象。
5. `SettingsAPI.getAllVariantIds()` 和 `getHullIdToVariantListMap()` 提供装配枚举与 hull 索引。

### 舰队成员装配链路

1. 调用方准备装配 id 或 `ShipVariantAPI`。
2. `FactoryAPI.createFleetMember(...)` 或 `SettingsAPI.createFleetMember(...)` 创建 `FleetMemberAPI`。
3. 创建结果持有装配引用。
4. 调用方通过 `FleetMemberAPI.getVariant()` 读取成员装配。
5. 调用方通过 `FleetMemberAPI.setVariant(...)` 把克隆或改写后的装配交回成员保存态。
6. 舰队同步、膨胀和战斗创建继续消费成员装配。

### 战斗舰船装配链路

1. 战斗部署流程从 `FleetMemberAPI` 读取装配。
2. 引擎创建 `ShipAPI` 战斗舰船实体。
3. 引擎把 hull spec、装配、武器、机库、模块和 stats 绑定到舰船实体。
4. 战斗脚本通过 `ShipAPI.getVariant()` 读取当前实体装配。
5. 船体插件、技能和战术系统通过 `MutableShipStatsAPI.getVariant()` 读取 stats 持有者装配。
6. 需要实体级独立改写时，调用 `ShipAPI.cloneVariant()` 让舰船持有克隆装配。

### Refit 与 Autofit 链路

1. Refit、autofit 或 fleet inflater 准备目标装配。
2. 需要成员独立状态时先克隆现有 `ShipVariantAPI`。
3. 调用方改写武器、武器组、机库、hullmod、perma mod、S-mod、tag、幅容、幅散或 source。
4. `ShipVariantAPI.setOriginalVariant(...)` 记录 autofit 或 inflater 使用的目标装配。
5. `FleetMemberAPI.setVariant(...)` 把结果装配写回成员。
6. `FleetInflater.deflate()` 读取 original variant 还原保存态表达。

## 规范

- `addMod(id)` 写入可移除 hullmod，`addPermaMod(id)` 写入永久 hullmod。
- `addPermaMod(id, true)` 写入永久 hullmod 并标记为 S-mod。
- `addWeapon(slotId, weaponId)` 使用 hull spec 中的武器槽 id。
- `clear()` 移除非内置武器、战机和 hullmod，并把 vents 与 capacitors 设为 0。
- `clearHullMods()` 不清理内置 hullmod 和 perma mod。
- `computeHullModOPCost(stats)` 使用传入人物 stats 计算 hullmod OP 成本。
- `computeOPCost(stats)` 同时计算武器、hullmod、幅容、幅散和相关 stats 修正后的 OP 消耗。
- `getFittedWeaponSlots()` 返回当前装有武器的槽位集合。
- `getHullVariantId()` 返回装配 id，`getHullSpec()` 返回 hull spec。
- `getModuleVariant(slotId)` 和 `setModuleVariant(slotId, variant)` 读写模块槽位装配。
- `getNonBuiltInWeaponSlots()` 返回装有非内置武器的槽位集合。
- `getOriginalVariant()` 可为 autofit 或 fleet inflater deflate 保存目标装配 id。
- `getPermaMods()`、`getSMods()`、`getSModdedBuiltIns()` 和 `getSuppressedMods()` 表达不同 hullmod 状态集合。
- `getSource()` 返回 `VariantSource`，`setSource(...)` 改写装配来源。
- `getUnusedOP(stats)` 返回传入人物 stats 下的剩余 OP。
- `getWingId(index)` 和 `setWingId(index, wingId)` 按 launch bay 索引读写战机 wing。
- `isEmptyHullVariant()` 表示由 hull spec 创建的空装配。
- `isGoalVariant()` 表示该装配可作为 autofit 或生成目标。
- `isStockVariant()` 表示装配来源为 stock。
- `setNumFluxCapacitors(value)` 和 `setNumFluxVents(value)` 写入幅容与幅散数量。
- `toJSONObject()` 序列化当前装配状态。

## 陷阱

- `FleetMemberAPI.setVariant(...)` 改写的是成员保存态，已部署战斗实体仍按战斗实体自身装配读取。
- `SettingsAPI.getVariant(...)` 读取的是全局已加载装配，成员级或实体级改写应先克隆。
- `ShipAPI.setVariantForHullmodCheckOnly(...)` 只用于 hullmod 检查语义，不表达完整换装。
- `VariantSource.REFIT` 常用于 refit 后的成员装配，source 错置会影响保存态和 UI 语义。
- `clearHullMods()` 与 `getHullMods().clear()` 对内置 hullmod 和 perma mod 的处理语义不同。
- `getHullVariantId()` 是装配 id，`getHullSpec().getHullId()` 是 hull id。
- `getOriginalVariant()` 缺失会影响 fleet inflater deflate 还原目标装配。
- `getSMods()`、`getSModdedBuiltIns()` 和 `getPermaMods()` 覆盖的 hullmod 集合不同。
- `setWingId(index, wingId)` 使用 launch bay 索引，武器槽 id 只能用于 `addWeapon(...)`。
