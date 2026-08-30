# 生涯货物容器

来源：原版 Starsector。

## 定义

生涯货物容器是 cargo 集合、货物栈、credits、人员、燃料、补给、武器、战机芯片、mothballed ships 和容量统计的原版接口集合。

## 参考

- `com.fs.starfarer.api.campaign.CargoAPI`：维护 cargo stack 集合、credits、人员、燃料、补给、武器、战机芯片、mothballed ships 和容量统计。
- `com.fs.starfarer.api.campaign.CargoAPI.CargoItemQuantity`：承载 cargo 内按 id 聚合的武器或战机芯片数量。
- `com.fs.starfarer.api.campaign.CargoAPI.CargoItemType`：定义 cargo stack 的资源、武器、战机芯片、`SPECIAL` 和空类型枚举。
- `com.fs.starfarer.api.campaign.CargoStackAPI`：维护单个 stack 的类型、data、数量、空间占用、spec 读取和 cargo 归属。
- `com.fs.starfarer.api.campaign.FleetDataAPI`：承载 `CargoAPI.getMothballedShips()` 返回的舰船存储集合。
- `com.fs.starfarer.api.fleet.FleetMemberType`：提供 `CargoAPI.addMothballedShip(...)` 的舰船或战机联队类型参数。
- `com.fs.starfarer.api.util.MutableValue`：承载 `CargoAPI.getCredits()` 返回的 credits 数值容器。

## 边界

- `CargoAPI` 归属 stack 集合、credits、人员、燃料、补给、武器、战机芯片和 mothballed ships。
- `CargoAPI` 归属货物容量、燃料容量、人员容量、已用空间、剩余空间和容量刷新入口。
- `CargoAPI` 归属 add、remove、clear、sort、copy、merge、remove empty stacks 和 unlimited stacks 状态。
- `CargoAPI.CargoItemQuantity` 归属按 id 聚合后的数量项。
- `CargoAPI.CargoItemType` 归属 cargo stack 类型枚举。
- `CargoAPI.getFleetData()` 归属 cargo 关联舰队数据入口。
- `CargoAPI.getOrigSource()` 与 `setOrigSource(...)` 归属 cargo 来源引用。
- `CargoStackAPI` 归属单个 stack 的 type、data、size、free、max size 和 full 状态。
- `CargoStackAPI` 归属 stack 所在 cargo、玩家货舱状态、picked up 状态和空 stack 判定。
- `CargoStackAPI` 归属 stack 空间占用、单件空间占用、显示名和单位基础值。
- `FleetDataAPI` 在本模块只作为 mothballed ships 集合返回类型，不展开成员对象语义。
- `MutableValue` 在本模块只作为 credits 数值容器，不展开通用数值容器语义。

## 链路

### Cargo 获取链路

1. 代码从玩家舰队、战利品、子市场、交互回调或临时对象取得 `CargoAPI`。
2. 调用 `getStacksCopy()`、`getCredits()`、`getFleetData()` 或容量读取入口取得容器状态。
3. 调用 add、remove、copy、merge、clear 或 sort 入口修改容器状态。
4. 需要容量结果时调用 `updateSpaceUsed()` 后读取 space、fuel 或 personnel 容量。

### Stack 操作链路

1. 代码从 `CargoAPI.getStacksCopy()` 取得 `CargoStackAPI`。
2. 调用 `getType()` 与 `getData()` 判断 stack 类型和数据。
3. 调用 `getSize()`、`getFree()`、`getMaxSize()` 或 `isFull()` 读取数量状态。
4. 调用 `setSize(...)`、`add(...)` 或 `subtract(...)` 写入数量状态。
5. 调用 `removeEmptyStacks()` 清理空 stack。

### 人员燃料补给链路

1. 调用 `addCrew(...)`、`removeCrew(...)`、`addMarines(...)` 或 `removeMarines(...)` 写入人员。
2. 调用 `getCrew()`、`getMarines()`、`getTotalCrew()` 或 `getTotalPersonnel()` 读取人员。
3. 调用 `addFuel(...)`、`removeFuel(...)`、`addSupplies(...)` 或 `removeSupplies(...)` 写入燃料与补给。
4. 调用 `getFuel()`、`getSupplies()`、`getFreeCrewSpace()` 或 `getFreeFuelSpace()` 读取资源与剩余空间。

### 武器与战机芯片链路

1. 调用 `addWeapons(id, count)`、`removeWeapons(id, count)` 或 `getNumWeapons(id)` 操作武器数量。
2. 调用 `getWeapons()` 读取武器聚合数量。
3. 调用 `addFighters(id, count)`、`removeFighters(id, count)` 或 `getNumFighters(id)` 操作战机芯片数量。
4. 调用 `getFighters()` 读取战机芯片聚合数量。
5. 对 stack 调用 `getWeaponSpecIfWeapon()` 或 `getFighterWingSpecIfWing()` 读取对应 spec。

### Mothballed Ships 链路

1. 调用 `initMothballedShips(factionId)` 初始化舰船存储集合。
2. 调用 `addMothballedShip(type, variantOrWingId, optionalName)` 写入 mothballed ship。
3. 调用 `getMothballedShips()` 取得 `FleetDataAPI`。
4. 调用 `addAll(other, true)` 合并 cargo 时包含 mothballed ships。

## 规范

- `CargoAPI.addAll(other)` 合并 cargo 时使用默认包含范围。
- `CargoAPI.addAll(other, includeMothballedShips)` 的布尔参数控制是否包含 mothballed ships。
- `CargoAPI.addItems(type, data, quantity)` 以 `CargoItemType` 和 data 写入 stack。
- `CargoAPI.createCopy()` 返回 cargo 副本。
- `CargoAPI.getCredits()` 返回 cargo 持有的 credits 数值容器。
- `CargoAPI.getMothballedShips()` 使用前需要先调用 `initMothballedShips(factionId)`。
- `CargoAPI.getOrigSource()` 与 `setOrigSource(...)` 维护 cargo 来源引用。
- `CargoAPI.removeAll(other)` 按另一个 cargo 的内容移除当前 cargo 内容。
- `CargoAPI.removeItems(type, data, quantity)` 返回是否完成移除。
- `CargoAPI.updateSpaceUsed()` 用于刷新 cargo 容量统计。
- `CargoItemType` 的原版值为 `RESOURCES`、`WEAPONS`、`FIGHTER_CHIP`、`SPECIAL` 和 `NULL`。
- `CargoStackAPI.getCargo()` 返回包含该 stack 的 cargo。
- `CargoStackAPI.getData()` 的实际含义由 `CargoItemType` 决定。
- `CargoStackAPI.getHullModSpecIfHullMod()` 读取 hullmod spec stack 的 spec。
- `CargoStackAPI.getWeaponSpecIfWeapon()` 读取 weapon stack 的 spec。
- `CargoStackAPI.isNull()` 表示用于间隔的空 cargo stack。

## 陷阱

- `CargoAPI.getMothballedShips()` 未初始化时不具备舰船存储使用语义。
- `CargoAPI.setFreeTransfer(...)` 与 `isFreeTransfer()` 已标记废弃，实体转移费用语义由实体 free transfer 入口表达。
- `CargoStackAPI.getData()` 不能脱离 `CargoItemType` 解释。
- `CargoStackAPI.getMaxSize()` 与 `CargoAPI` 容量统计不是同一个边界。
- `CargoStackAPI.isPickedUp()` 表示 stack 拾取状态，不等同于该 stack 已进入玩家货舱。
- `CargoStackAPI.isResourceStack()` 已标记废弃。
