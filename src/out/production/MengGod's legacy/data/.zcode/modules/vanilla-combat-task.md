# 战斗任务 API

来源：原版 Starsector。

## 定义

战斗任务 API 是战斗内任务管理器、指派状态、指派目标、命令点和战术命令状态的接口集合。

## 参考

- `com.fs.starfarer.api.combat.AssignmentTargetAPI`：描述指派目标的坐标、速度和 owner 形状。
- `com.fs.starfarer.api.combat.CombatAssignmentType`：定义战斗指派类型枚举。
- `com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo`：描述单个指派的类型、目标和被指派成员。
- `com.fs.starfarer.api.combat.CombatFleetManagerAPI.createWaypoint(...)`：创建舰队管理器侧 waypoint 目标。
- `com.fs.starfarer.api.combat.CombatFleetManagerAPI.getTaskManager(...)`：取得单侧舰队的任务管理器。
- `com.fs.starfarer.api.combat.CombatTaskManagerAPI`：提供战斗任务查询、创建、下达、撤销和命令点入口。
- `com.fs.starfarer.api.combat.CombatTaskManagerAPI.createWaypoint2(...)`：创建任务管理器侧 waypoint 目标。
- `com.fs.starfarer.api.combat.DeployedFleetMemberAPI`：提供已部署成员作为指派输入和指派目标的接口形状。

## 边界

- `AssignmentTargetAPI` 归属指派目标的 location、velocity 和 owner 读取形状。
- `CombatAssignmentType` 归属战斗指派类型枚举。
- `CombatFleetManagerAPI.AssignmentInfo` 归属单个指派的类型、目标和成员集合。
- `CombatFleetManagerAPI.createWaypoint(...)` 归属舰队管理器创建的 waypoint 目标。
- `CombatFleetManagerAPI.getTaskManager(...)` 归属单侧舰队任务管理器查询。
- `CombatTaskManagerAPI.clearEmptyWaypoints()` 归属清理空 waypoint。
- `CombatTaskManagerAPI.clearTasks()` 归属清空当前任务状态。
- `CombatTaskManagerAPI.createAssignment(...)` 归属创建指派。
- `CombatTaskManagerAPI.createWaypoint2(...)` 归属任务管理器创建的 waypoint 目标。
- `CombatTaskManagerAPI.getAssignmentFor(...)` 归属舰船当前指派查询。
- `CombatTaskManagerAPI.getCommandPointsStat()` 与 `getCommandPointsLeft()` 归属命令点状态。
- `CombatTaskManagerAPI.giveAssignment(...)` 归属把已有指派下达给已部署成员。
- `CombatTaskManagerAPI.orderFullRetreat()` 归属全撤退命令状态。
- `CombatTaskManagerAPI.orderRetreat(...)` 归属单成员撤退命令。
- `CombatTaskManagerAPI.orderSearchAndDestroy(...)` 归属搜索歼灭命令。
- `DeployedFleetMemberAPI` 在任务模块中只作为指派下达输入和 `AssignmentTargetAPI` 目标形状。

## 链路

### 指派创建链路

1. 代码通过 `CombatEngineAPI.getFleetManager(...)` 取得舰队管理器。
2. 代码通过 `CombatFleetManagerAPI.getTaskManager(ally)` 取得任务管理器。
3. 代码准备 `AssignmentTargetAPI` 目标。
4. 代码调用 `CombatTaskManagerAPI.createAssignment(type, target, useCommandPoint)`。
5. 任务管理器返回 `AssignmentInfo`。
6. 代码读取 `AssignmentInfo.getType()`、`getTarget()` 和 `getAssignedMembers()`。

### 指派下达链路

1. 代码取得 `DeployedFleetMemberAPI` 成员。
2. 代码创建或取得 `AssignmentInfo`。
3. 代码调用 `CombatTaskManagerAPI.giveAssignment(member, assignment, useCommandPointIfNeeded)`。
4. 任务管理器把成员加入指派成员集合。
5. 代码通过 `getAssignmentFor(ship)` 或 `getAssignmentTargetFor(ship)` 查询当前结果。
6. 需要刷新分配时调用 `reassign()`。

### 战术命令链路

1. 单成员搜索歼灭调用 `orderSearchAndDestroy(member, useCommandPointIfNeeded)`。
2. 全队搜索歼灭调用 `orderSearchAndDestroy()`。
3. 单成员撤退调用 `orderRetreat(member, useCommandPointIfNeeded, direct)`。
4. 全队撤退调用 `orderFullRetreat()`。
5. 全突击状态通过 `setFullAssault(...)` 写入。
6. 阻止全撤退状态通过 `setPreventFullRetreat(...)` 写入。

### 目标链路

1. 点位、已部署成员和 waypoint 均可作为 `AssignmentTargetAPI`。
2. waypoint 由 `CombatFleetManagerAPI.createWaypoint(...)` 或 `CombatTaskManagerAPI.createWaypoint2(...)` 创建。
3. 指派通过 `AssignmentInfo.getTarget()` 保存目标。
4. 任务管理器通过 `getAssignmentInfoForTarget(target)` 反查目标上的指派。
5. 空 waypoint 通过 `clearEmptyWaypoints()` 清理。

## 规范

- `AssignmentInfo.getAssignedMembers()` 返回执行该指派的已部署成员列表。
- `AssignmentInfo.getTarget()` 返回该指派绑定的目标。
- `AssignmentInfo.getType()` 返回该指派的 `CombatAssignmentType`。
- `AssignmentTargetAPI.getOwner()` 使用 0 表示玩家侧、1 表示敌方侧、100 表示中立。
- `CombatAssignmentType` 包含侦察、夺取、控制、突击、交战、规避、防守、集结、打击、拦截、骚扰、护航、撤退、维修整备和搜索歼灭类型。
- `CombatFleetManagerAPI.getTaskManager(ally)` 返回该舰队管理器下指定 ally 模式的任务管理器。
- `CombatTaskManagerAPI.createAssignment(...)` 的 target 可使用点位、已部署成员或 waypoint。
- `CombatTaskManagerAPI.getAssignmentFor(ship)` 在舰船没有显式指派时返回 `null`。
- `CombatTaskManagerAPI.getAssignmentFor(ship)` 对战机联队可传入同联队任一战机查询。
- `CombatTaskManagerAPI.getCommandPointsStat()` 返回可被修改的命令点 stat。
- `CombatTaskManagerAPI.getCPRateModifier()` 返回命令点恢复倍率 modifier。
- `CombatTaskManagerAPI.orderFullRetreat()` 会取消全部指派并命令全部舰船撤退。
- `CombatTaskManagerAPI.orderSearchAndDestroy()` 会取消全部指派，之后仍可创建新指派。
- `CombatTaskManagerAPI.removeAssignment(info)` 移除指定指派。
- `CombatTaskManagerAPI.setAssignmentWeight(info, weight)` 写入指定指派权重。

## 陷阱

- `CombatTaskManagerAPI.getAssignmentFor(ship)` 返回 `null` 表示默认搜索歼灭状态。
- `CombatTaskManagerAPI.orderFullRetreat()` 注释说明全撤退无法中止。
- `CombatTaskManagerAPI.orderSearchAndDestroy()` 会取消全部当前指派。
- `CombatTaskManagerAPI.giveAssignment(...)` 需要 `DeployedFleetMemberAPI`，不是裸 `ShipAPI`。
- `AssignmentInfo.getTarget()` 返回的是目标接口，读取具体舰船前需要确认目标是 `DeployedFleetMemberAPI`。
- waypoint 是 `AssignmentTargetAPI`，空 waypoint 需要由任务管理器清理。
