# 战斗舰船 API

来源：原版 Starsector。

## 定义

战斗舰船 API 是 `ShipAPI` 承载的舰船实体、战斗状态、舰船组件和控制接口集合。

## 参考

- `com.fs.starfarer.api.combat.ArmorGridAPI`：描述舰船装甲格、格点坐标、单格装甲值和组件伤害映射。
- `com.fs.starfarer.api.combat.FighterWingAPI`：描述战机联队成员、队长、母舰和整备关联。
- `com.fs.starfarer.api.combat.FluxTrackerAPI`：描述舰船当前幅能、硬幅能、过载、耗散和排幅状态。
- `com.fs.starfarer.api.combat.ShieldAPI`：描述护盾类型、朝向、半径、弧度、开关状态、颜色和视觉参数。
- `com.fs.starfarer.api.combat.ShipAIPlugin`：定义舰船 AI 的逐帧推进、威胁重算、目标覆盖和战机整备判断。
- `com.fs.starfarer.api.combat.ShipAPI`：描述舰船实体、船体规格、装配、武器、系统、AI、舰长、战机、模块和监听器。
- `com.fs.starfarer.api.combat.ShipSystemAPI`：提供舰船持有的普通系统、相位斗篷和旅行驱动查询入口。
- `com.fs.starfarer.api.combat.ShipwideAIFlags`：提供舰船 AI 读取的短期 flag 容器入口。
- `com.fs.starfarer.api.combat.WeaponAPI`：描述舰船挂载的单个武器实例、武器组、禁用状态和武器归属。
- `com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI`：承载注册在当前舰船实体上的战斗监听器集合。

## 边界

- `ArmorGridAPI` 归属舰船装甲格状态和格点坐标换算。
- `FighterWingAPI` 归属战机成员、联队队长和联队级整备状态。
- `FluxTrackerAPI` 归属舰船当前幅能、硬幅能、过载和排幅运行态。
- `ShieldAPI` 归属舰船护盾运行态和护盾视觉参数。
- `ShipAIPlugin` 归属舰船 AI 的移动、开火、威胁评估和控制输出。
- `ShipAPI` 拥有当前战斗舰船实体的 hull、variant、fleet member、captain、owner 和实体标签。
- `ShipAPI` 拥有舰船级监听器、mutable stats、武器、系统、护盾、装甲格、幅能和 AI 状态。
- `ShipAPI.getAIFlags()` 归属 AI 控制舰船的跨 AI 模块短期通信。
- `ShipAPI.getAllWeapons()` 和 `ShipAPI.getWeaponGroupsCopy()` 归属当前舰船挂载武器与武器组视图。
- `ShipAPI.getChildModulesCopy()` 归属空间站或模块舰船的子模块实体关系。
- `ShipAPI.getListenerManager()` 归属当前舰船实体监听器集合。
- `ShipAPI.getMutableStats()` 归属当前舰船实体关联的运行时数值集合。
- `ShipAPI.getSystem()`、`getPhaseCloak()` 和 `getTravelDrive()` 分别归属舰船到普通系统、相位斗篷和旅行驱动的查询入口。

## 链路

### 舰船创建链路

1. 战斗部署或脚本生成准备 `FleetMemberAPI`、hull spec 和 variant。
2. 引擎创建 `ShipAPI` 实体。
3. 引擎把 `MutableShipStatsAPI`、武器、装甲格、幅能状态和系统状态绑定到舰船。
4. 舰船技能、船体插件或引擎流程执行实体创建后的回调。
5. 舰船进入战斗实体集合并接受战斗推进、渲染、伤害和监听器回调。

### 舰船 AI 链路

1. `BaseModPlugin.pickShipAI(ship, config)` 或原版选择流程准备 `ShipAIPlugin`。
2. 引擎把 AI 安装到舰船。
3. 引擎每帧推进舰船 AI。
4. AI 读取舰船、目标、武器、系统、幅能、护盾和 `ShipwideAIFlags`。
5. AI 通过 `giveCommand(...)`、`useSystem()` 或目标覆盖输出控制。
6. 引擎在核心控制流程中消费舰船控制输入。

### 舰船防御状态链路

1. 调用方通过 `ShipAPI` 取得 `FluxTrackerAPI`、`ShieldAPI` 或 `ArmorGridAPI`。
2. 幅能接口读取或写入当前幅能、硬幅能、过载和排幅状态。
3. 护盾接口读取或写入护盾开关、朝向、弧度、半径和视觉状态。
4. 装甲格接口读取或写入单格装甲值和格点坐标。
5. 引擎伤害结算、AI 判断和 UI 显示消费这些舰船状态。

### 舰船监听器链路

1. 代码取得当前舰船的 `CombatListenerManagerAPI` 或调用 `ShipAPI.addListener(...)`。
2. 监听器对象注册到舰船实体。
3. 引擎在对应伤害、部署、射程或 advance 节点回调监听器。
4. 监听器通过回调参数和所属舰船读取或修改正式状态。
5. 代码调用 `removeListener(...)`、`removeListenerOfClass(...)` 或舰船生命周期结束。

## 规范

- `addListener(...)` 注册舰船级监听器，注册方负责唯一性。
- `getAIFlags()` 在舰船由 AI 控制时返回 AI flag 容器。
- `getArmorGrid().getCellAtLocation(loc)` 对装甲格外坐标返回 `null`。
- `getFleetMemberId()` 在舰船没有对应舰队成员时可返回 `null`。
- `getFluxTracker().increaseFlux(amount, true)` 可让舰船过载。
- `getListenerManager()` 在没有监听器时可返回 `null`。
- `getMouseTarget()` 返回引擎坐标，并适用于 AI 舰船。
- `getOriginalOwner()` 返回 0 或 1。
- `getShipAI()` 返回内部包装对象，动态保存和恢复 AI 时按包装对象处理。
- `getShield()` 对没有护盾的舰船可返回 `null`。
- `giveCommand(...)` 应由 `ShipAIPlugin` 或明确控制逻辑调用。
- `setShipAI(...)` 只用于运行时需要动态改变 AI 的舰船。
- `useSystem()` 只表达下一帧尝试使用系统，实际使用仍受弹药、幅能、过载和系统状态限制。
- 战机降落动画开始后，需要由 launch bay 流程完成实体移除。
- 舰船死亡、移除或成为残骸后，已有 Java 引用仍指向同一个实体对象。

## 陷阱

- `getAIFlags()` 对玩家直接控制且没有 AI 的舰船可能没有可用 flag 容器。
- `getArmorGrid().getArmorValue(...)` 读取的是单格实际装甲值。
- `getMouseTarget()` 不需要用 `null` 作为无目标语义。
- `getShipTarget()` 只表示舰船当前目标，不能替代鼠标目标坐标。
- `getShieldCenterEvenIfNoShield()` 和 `getShieldRadiusEvenIfNoShield()` 可在无护盾舰船上读取几何信息。
- `getSystem()`、`getPhaseCloak()` 和 `getTravelDrive()` 是不同系统入口。
- `getWingMembers()` 和 `getWingLeader()` 是弃用入口，联队成员应从 `getWing()` 读取。
- `setShield(...)` 改写的是舰船实体护盾状态，不等同于修改 hull spec 或 variant 数据。
