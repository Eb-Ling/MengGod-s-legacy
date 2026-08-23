# 战斗舰队 API

来源：原版 Starsector。

## 定义

战斗舰队 API 是战斗内单侧舰队管理器、部署成员、预备队、舰队状态、指挥官和部署点的接口集合。

## 参考

- `com.fs.starfarer.api.combat.CombatEngineAPI.getFleetManager(...)`：取得指定 side 或 owner 的战斗舰队管理器。
- `com.fs.starfarer.api.combat.CombatEngineAPI.setMaxFleetPoints(...)`：写入指定 `FleetSide` 的最大舰队点数。
- `com.fs.starfarer.api.combat.CombatFleetManagerAPI`：提供战斗内单侧舰队部署、成员集合、指挥官和舰队状态入口。
- `com.fs.starfarer.api.combat.CombatFleetManagerAPI.addToReserves(...)`：把 fleet member 加入战斗预备队。
- `com.fs.starfarer.api.combat.CombatFleetManagerAPI.getDeployedFleetMember(...)`：按舰船取得已部署成员。
- `com.fs.starfarer.api.combat.CombatFleetManagerAPI.getDeployedFleetMemberEvenIfDisabled(...)`：按舰船取得已部署或 disabled 成员。
- `com.fs.starfarer.api.combat.CombatFleetManagerAPI.spawnFleetMember(...)`：部署指定 fleet member。
- `com.fs.starfarer.api.combat.CombatFleetManagerAPI.spawnShipOrWing(...)`：按 spec、variant 或 wing id 部署舰船或战机联队。
- `com.fs.starfarer.api.combat.DeployedFleetMemberAPI`：关联战斗内已部署舰船或战机联队与 campaign fleet member。

## 边界

- `CombatEngineAPI.getFleetManager(FleetSide)` 归属按战斗 side 查询舰队管理器。
- `CombatEngineAPI.getFleetManager(int)` 归属按 owner 查询舰队管理器。
- `CombatEngineAPI.setMaxFleetPoints(...)` 归属战斗层写入指定 side 的最大舰队点数。
- `CombatFleetManagerAPI` 归属单侧舰队的战斗内状态。
- `CombatFleetManagerAPI.addToReserves(...)` 与 `removeFromReserves(...)` 归属预备队成员写入。
- `CombatFleetManagerAPI.getAllEverDeployedCopy()` 归属曾部署成员集合读取。
- `CombatFleetManagerAPI.getAllFleetCommanders()` 归属舰队所有指挥官读取。
- `CombatFleetManagerAPI.getBiggestStationDeployedOrNot()` 归属最大空间站 fleet member 查询。
- `CombatFleetManagerAPI.getCurrStrength()` 与 `getMaxStrength()` 归属当前部署点和最大部署点读取。
- `CombatFleetManagerAPI.getDeployedCopy()` 与 `getDeployedCopyDFM()` 归属当前已部署成员读取。
- `CombatFleetManagerAPI.getDisabledCopy()`、`getDestroyedCopy()` 与 `getRetreatedCopy()` 归属战斗结果成员集合读取。
- `CombatFleetManagerAPI.getFleetCommander()` 与 `getFleetCommanderPreferPlayer()` 归属战斗指挥官读取。
- `CombatFleetManagerAPI.getGoal()` 归属该侧战斗目标读取。
- `CombatFleetManagerAPI.getStations()` 归属已部署空间站成员集合读取。
- `CombatFleetManagerAPI.getTaskManager(...)`、`createWaypoint(...)` 和 `AssignmentInfo` 归属舰队管理器对战斗任务 API 的桥接入口。
- `CombatFleetManagerAPI.removeDeployed(...)` 归属从已部署集合移除舰船或战机联队。
- `CombatFleetManagerAPI.spawnFleetMember(...)` 归属按 fleet member 部署。
- `CombatFleetManagerAPI.spawnShipOrWing(...)` 归属按 id 部署舰船或战机联队。
- `DeployedFleetMemberAPI` 归属已部署成员与舰船、战机联队或 fleet member 的运行时关联。

## 链路

### 舰队管理器查询链路

1. 战斗引擎持有双方舰队管理器。
2. 代码按 `FleetSide` 调用 `CombatEngineAPI.getFleetManager(side)`。
3. 代码按 owner 调用 `CombatEngineAPI.getFleetManager(owner)`。
4. 调用方读取 `CombatFleetManagerAPI.getOwner()`、`getGoal()` 或成员集合。

### 部署链路

1. 代码取得 `CombatFleetManagerAPI`。
2. 代码调用 `spawnShipOrWing(...)` 或 `spawnFleetMember(...)`。
3. 舰队管理器消费 reserves 中已有成员，或为缺省 reserve 创建临时 fleet member。
4. 引擎创建 `ShipAPI` 或战机联队实体。
5. 舰队管理器把实体关联到 `DeployedFleetMemberAPI`。
6. 调用方通过 `getDeployedFleetMember(ship)` 或 `getShipFor(member)` 查询关联。

### 成员状态链路

1. 舰队管理器维护 reserves、deployed、disabled、destroyed 和 retreated 集合。
2. 战斗推进和部署流程更新这些集合。
3. 调用方读取 `getReservesCopy()`、`getDeployedCopy()`、`getDisabledCopy()`、`getDestroyedCopy()` 或 `getRetreatedCopy()`。
4. 战后或统计逻辑读取 `getAllEverDeployedCopy()`。
5. 空间站逻辑读取 `isDeployedStation()`、`isDefendingStation()` 和 `getStations()`。

### 指挥官与部署点链路

1. 舰队管理器读取或写入默认指挥官。
2. 代码读取 `getFleetCommander()`、`getFleetCommanderPreferPlayer()` 或 `getAllFleetCommanders()`。
3. 引擎或管理器写入最大部署点。
4. 调用方读取 `getMaxStrength()` 与 `getCurrStrength()`。
5. 代码通过 `modifyFlatMax(...)`、`modifyPercentMax(...)` 或 `unmodifyMax()` 调整最大部署点。

## 规范

- `CombatEngineAPI.getFleetManager(FleetSide)` 返回指定战斗 side 的舰队管理器。
- `CombatEngineAPI.getFleetManager(int)` 返回指定 owner 的舰队管理器。
- `CombatFleetManagerAPI.getFleetCommander()` 在 reserves 和 deployed 均为空时可返回 `null`。
- `CombatFleetManagerAPI.getShipFor(fleetMember)` 对战机联队返回 wing leader。
- `CombatFleetManagerAPI.getShipFor(person)` 返回该 captain 对应的舰船。
- `CombatFleetManagerAPI.getShardToOriginalShipMap()` 返回 shard 到原始已部署成员的映射。
- `CombatFleetManagerAPI.isCanForceShipsToEngageWhenBattleClearlyLost()` 读取败势强制交战许可。
- `CombatFleetManagerAPI.setCanForceShipsToEngageWhenBattleClearlyLost(...)` 对敌方默认 true，对玩家侧默认 false。
- `CombatFleetManagerAPI.spawnFleetMember(...)` 的 member 不要求已经在 reserves 中。
- `CombatFleetManagerAPI.spawnShipOrWing(...)` 可为 reserves 中缺失的 id 创建临时 fleet member。
- `CombatFleetManagerAPI.spawnShipOrWing(...)` 不把临时 fleet member 写入底层 campaign fleet。
- `DeployedFleetMemberAPI.canBeGivenOrders()` 表示该已部署成员可接收普通命令。
- `DeployedFleetMemberAPI.canBeGivenRetreatOrders()` 表示该已部署成员可接收撤退命令。
- `DeployedFleetMemberAPI.getMember()` 返回关联的 campaign fleet member。
- `DeployedFleetMemberAPI.getShip()` 返回舰船或战机联队队长。
- `DeployedFleetMemberAPI.isDirectRetreat()` 与 `setDirectRetreat(...)` 读写直接撤退状态。

## 陷阱

- `CombatFleetManagerAPI.getFleetCommander()` 注释说明在 reserves 和 deployed 都为空时可返回 `null`。
- `CombatFleetManagerAPI.getDeployedFleetMember(ship)` 不覆盖 disabled 成员，disabled 查询使用 `getDeployedFleetMemberEvenIfDisabled(...)`。
- `CombatFleetManagerAPI.spawnShipOrWing(...)` 的临时 fleet member 只属于战斗内 reserves。
- `CombatFleetManagerAPI.spawnFleetMember(...)` 的传入 member 不必已经在 reserves 中。
- `DeployedFleetMemberAPI.getShip()` 对战机联队返回 wing leader。
- `removeDeployed(ship, retreated)` 和 `removeDeployed(wing, retreated)` 写入的是战斗内已部署集合状态。
- 指挥官、部署点和 clean disengage 属于战斗内舰队管理器状态，不等同于生涯舰队持久状态。
