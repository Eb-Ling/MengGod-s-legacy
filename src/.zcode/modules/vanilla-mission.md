# 战役 Mission API

来源：原版 Starsector。

## 定义

战役 Mission API 是独立战斗场景的数据注册、描述资源和 `MissionDefinition` 脚本定义接口集合。

## 参考

- `com.fs.starfarer.api.combat.BattleCreationContext`：提供 mission 战斗创建上下文、命令点、部署距离、撤退许可和战斗目标开关。
- `com.fs.starfarer.api.combat.EveryFrameCombatPlugin`：可通过 mission API 加入该场战斗。
- `com.fs.starfarer.api.fleet.FleetGoal`：定义 mission 舰队目标，包括 attack 与 escape。
- `com.fs.starfarer.api.fleet.FleetMemberAPI`：提供 mission 舰队定义中返回或接收的成员对象。
- `com.fs.starfarer.api.fleet.FleetMemberType`：提供 mission 加入舰队时使用的成员类型参数。
- `com.fs.starfarer.api.mission.FleetSide`：定义 mission 中的 `PLAYER` 与 `ENEMY` 两侧。
- `com.fs.starfarer.api.mission.MissionDefinitionAPI`：提供舰队、briefing、地图、点位、星体、星云、小行星、背景和插件定义入口。
- `com.fs.starfarer.api.mission.MissionDefinitionPlugin`：由 `MissionDefinition.java` 实现，向引擎填充 mission 内容。
- `data/missions/<mission_id>/descriptor.json`：提供 mission 标题、难度、图标和背景资源。
- `data/missions/<mission_id>/icon.png`：提供 mission 列表图标。
- `data/missions/<mission_id>/mission_text.txt`：提供 mission 详情叙述文本。
- `data/missions/mission_list.csv`：注册可加载 mission id。
- `src/data/missions/<mission_id>/MissionDefinition.java`：提供对应 mission 的定义脚本。

## 边界

- `BattleCreationContext` 归属 mission 战斗创建参数读取与局部改写。
- `EveryFrameCombatPlugin` 归属 mission 战斗开始后的附加每帧逻辑。
- `FleetGoal` 归属 mission 两侧舰队目标。
- `FleetMemberAPI` 在本模块只作为 mission 舰队定义的返回对象和输入对象。
- `FleetMemberType` 在本模块只作为 mission 加入舰队时的成员类型参数。
- `FleetSide` 归属 mission 两侧身份。
- `MissionDefinitionAPI.addBriefingItem(...)` 归属战术目标列表文本。
- `MissionDefinitionAPI.addFleetMember(...)` 归属已构造 fleet member 直接加入 mission 舰队。
- `MissionDefinitionAPI.addObjective(...)` 归属 mission 地图点位生成。
- `MissionDefinitionAPI.addPlugin(...)` 归属 mission 战斗插件注册。
- `MissionDefinitionAPI.addToFleet(...)` 归属按 variant id 加入舰船或战机。
- `MissionDefinitionAPI.defeatOnShipLoss(...)` 归属指定命名舰船损失时的失败条件。
- `MissionDefinitionAPI.initFleet(...)` 归属 mission 单侧舰队初始化。
- `MissionDefinitionAPI.initMap(...)` 归属 mission 战斗地图范围。
- `MissionDefinitionPlugin.defineMission(...)` 归属 mission 内容填充入口。
- `descriptor.json` 归属 mission 列表和详情页静态显示资源。
- `mission_list.csv` 归属 mission id 注册顺序。
- `mission_text.txt` 归属 mission 详情叙述文本。

## 链路

### Mission 注册链路

1. 游戏读取 `data/missions/mission_list.csv`。
2. 每一行 mission id 映射到 `data/missions/<mission_id>/` 目录。
3. 游戏读取该目录下 `descriptor.json`。
4. 游戏读取 `icon.png` 和 `mission_text.txt`。
5. mission 列表使用 descriptor、图标和文本展示条目。

### Mission 定义链路

1. 玩家选择一个 mission。
2. 游戏创建该 mission 对应的 `MissionDefinition` 实例。
3. `MissionDefinition` 实现 `MissionDefinitionPlugin`。
4. 游戏调用 `defineMission(api)`。
5. 脚本通过 `MissionDefinitionAPI` 写入舰队、地图、briefing、背景和插件。
6. 游戏使用写入结果创建战斗。

### 舰队定义链路

1. 脚本对 `FleetSide.PLAYER` 调用 `initFleet(...)`。
2. 脚本对 `FleetSide.ENEMY` 调用 `initFleet(...)`。
3. 脚本调用 `setFleetTagline(...)` 写入两侧说明。
4. 脚本调用 `addToFleet(...)` 或 `addFleetMember(...)` 加入成员。
5. `addToFleet(...)` 返回 `FleetMemberAPI`。
6. 脚本可把返回的成员交给成员侧入口写入 captain、CR 或其它运行时成员状态。
7. 脚本可调用 `defeatOnShipLoss(...)` 绑定命名舰船失败条件。

### 地图定义链路

1. 脚本调用 `initMap(minX, maxX, minY, maxY)` 设置地图范围。
2. 脚本调用 `setBackgroundSpriteName(...)` 或 descriptor 背景决定背景资源。
3. 脚本调用 `addNebula(...)`、`addAsteroidField(...)` 或 `addRingAsteroids(...)` 写入环境。
4. 脚本调用 `addPlanet(...)` 写入星体或背景星体。
5. 脚本调用 `addObjective(...)` 写入 comm relay、nav buoy 或 sensor array。
6. 脚本通过 `getContext()` 调整该场战斗创建参数。

## 规范

- `MissionDefinitionAPI.addBriefingItem(...)` 写入的文本显示在 mission 描述的战术目标列表中。
- `MissionDefinitionAPI.addFleetMember(...)` 接收已构造的 `FleetMemberAPI`。
- `MissionDefinitionAPI.addObjective(...)` 的 type 使用 `comm_relay`、`nav_buoy` 或 `sensor_array`。
- `MissionDefinitionAPI.addPlugin(...)` 注册的 `EveryFrameCombatPlugin` 只作用于该场 mission 战斗。
- `MissionDefinitionAPI.addToFleet(...)` 的 variant id 指向 `data/variants` 或 `data/variants/fighters` 中的 variant。
- `MissionDefinitionAPI.defeatOnShipLoss(...)` 使用显式加入舰队时指定的完整舰船名。
- `MissionDefinitionAPI.getContext()` 返回当前 mission 的 `BattleCreationContext`。
- `MissionDefinitionAPI.getFleetPointCost(id)` 返回舰船 variant 或战机 wing 的舰队点数。
- `MissionDefinitionAPI.initFleet(...)` 每个 `FleetSide` 应调用 1 次。
- `MissionDefinitionAPI.initFleet(...)` 应先于同侧任何 `addToFleet(...)` 或 `addFleetMember(...)`。
- `MissionDefinitionAPI.initMap(...)` 使用像素坐标，惯例上地图中心为 0,0。
- `MissionDefinitionPlugin.defineMission(api)` 在玩家点击 mission 列表条目时调用。
- `descriptor.json` 的 `background` 指向 mission 详情使用的背景图资源。
- `mission_list.csv` 的 `mission` 列提供 mission id。

## 陷阱

- `MissionDefinitionPlugin.defineMission(...)` 会在玩家点击 mission 条目时调用，静态状态会跨同一 JVM 内的多次调用保留。
- `MissionDefinitionAPI.defeatOnShipLoss(...)` 依赖完整舰船名，使用随机命名成员会失去明确绑定目标。
- `MissionDefinitionAPI.initFleet(...)` 晚于 `addToFleet(...)` 会破坏该侧舰队初始化顺序。
- `MissionDefinitionAPI.addToFleet(...)` 使用 hull id 会找不到 variant，输入应是 variant id 或 fighter wing id。
- `MissionDefinitionAPI.addObjective(...)` 的 type 拼写错误会导致点位类型无法按预期生成。
- `MissionDefinitionAPI.getContext()` 写入的是当前 mission 战斗创建上下文，作用域限于该场 mission 战斗。
- `descriptor.json`、`mission_text.txt` 和 `MissionDefinition.java` 的目录名必须与 `mission_list.csv` 中的 mission id 对应。
