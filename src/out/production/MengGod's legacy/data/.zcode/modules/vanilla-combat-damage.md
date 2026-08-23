# 战斗伤害 API

来源：原版 Starsector。

## 定义

战斗伤害 API 是战斗内直接伤害、伤害数据、伤害结果、伤害监听器和 EMP arc 的接口集合。

## 参考

- `com.fs.starfarer.api.campaign.CombatDamageData`：记录战斗中 fleet member 对 fleet member 的船体伤害统计。
- `com.fs.starfarer.api.combat.CombatEngineAPI.applyDamage(...)`：对战斗实体执行直接伤害结算。
- `com.fs.starfarer.api.combat.CombatEngineAPI.applyDamageModifiersToSpawnedProjectileWithNullWeapon(...)`：对无 weapon 的生成弹丸应用伤害修正。
- `com.fs.starfarer.api.combat.CombatEngineAPI.getDamageData()`：读取当前战斗伤害统计对象。
- `com.fs.starfarer.api.combat.CombatEngineAPI.spawnEmpArc(...)`：生成带伤害与 EMP 的 EMP arc 实体。
- `com.fs.starfarer.api.combat.CombatEngineAPI.spawnEmpArcPierceShields(...)`：生成穿透护盾选择目标的 EMP arc 实体。
- `com.fs.starfarer.api.combat.DamageAPI`：描述单次或持续伤害的数值、类型、EMP、DPS、软硬幅能和 modifier。
- `com.fs.starfarer.api.combat.DamageType`：定义 kinetic、high explosive、fragmentation、energy 和 other 的护盾、装甲、船体倍率。
- `com.fs.starfarer.api.combat.EmpArcEntityAPI`：描述生成后的 EMP arc 实体、目标位置、渲染状态和命中护盾状态。
- `com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams`：描述 EMP arc 的折线、淡入、闪烁、glow 和 missile flameout 参数。
- `com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI`：描述伤害结算后写入的船体、装甲、护盾、EMP 和类型结果。
- `com.fs.starfarer.api.combat.listeners.DamageDealtModifier`：在造成伤害一侧修改 `DamageAPI`。
- `com.fs.starfarer.api.combat.listeners.DamageListener`：在伤害应用后接收 source、target 和结果。
- `com.fs.starfarer.api.combat.listeners.DamageTakenModifier`：在承受伤害一侧修改 `DamageAPI`。
- `com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener`：在舰船即将承受船体伤害时决定是否抵消该次船体伤害。

## 边界

- `ApplyDamageResultAPI` 归属一次伤害应用后的结果数据。
- `CombatDamageData` 归属战斗结束和统计侧的船体伤害累计。
- `CombatEngineAPI.applyDamage(...)` 归属脚本主动触发的直接伤害入口。
- `CombatEngineAPI.applyDamageModifiersToSpawnedProjectileWithNullWeapon(...)` 归属无 weapon 生成弹丸的伤害修正入口。
- `CombatEngineAPI.getDamageData()` 归属当前战斗伤害统计读取。
- `CombatEngineAPI.spawnEmpArc(...)` 归属带伤害与 EMP 的 arc 生成。
- `CombatEngineAPI.spawnEmpArcPierceShields(...)` 归属穿透护盾选取目标的 arc 生成。
- `DamageAPI` 归属结算中的伤害数值、伤害类型、DPS、EMP、幅能和 modifier。
- `DamageDealtModifier` 归属造成伤害一侧的伤害修改。
- `DamageListener` 归属伤害应用完成后的结果通知。
- `DamageTakenModifier` 归属承受伤害一侧的伤害修改。
- `DamageType` 归属伤害对护盾、装甲和船体的倍率分类。
- `EmpArcEntityAPI` 归属生成后的 EMP arc 战斗实体状态。
- `EmpArcParams` 归属 EMP arc 生成时的视觉和行为参数。
- `HullDamageAboutToBeTakenListener` 归属船体伤害写入前的抵消判断。

## 链路

### 直接伤害链路

1. 调用方准备 target、point、damage amount、damage type、EMP、护盾绕过、软幅能、source 和声音参数。
2. 调用方调用 `CombatEngineAPI.applyDamage(...)`。
3. 引擎构造结算用 `DamageAPI`。
4. 造成伤害侧的 `DamageDealtModifier` 通过 `damage.getModifier()` 写入修正。
5. 承受伤害侧的 `DamageTakenModifier` 通过 `damage.getModifier()` 写入修正。
6. 引擎按目标、命中点、护盾状态、装甲和血量应用修正后的伤害。
7. 引擎把结算结果写入 `ApplyDamageResultAPI`。
8. 已注册的 `DamageListener` 接收 source、target 和结果。
9. 战斗伤害统计写入 `CombatDamageData`。

### EMP Arc 链路

1. 调用方准备 damage source、起点、起点锚点、EMP 目标实体、damage type、伤害、EMP、最大距离、音效、宽度、颜色和可选参数。
2. 调用方调用 `spawnEmpArc(...)` 或 `spawnEmpArcPierceShields(...)`。
3. 引擎选取 EMP arc 的目标位置。
4. 引擎创建 `EmpArcEntityAPI`。
5. EMP arc 参与伤害或 EMP 结算。
6. 调用方可设置 target、flicker、glow、layer、warping 和 faded 状态。
7. 后续伤害修改监听器以 `EmpArcEntityAPI` 或 EMP 系统 param 识别来源。

## 规范

- `ApplyDamageResultAPI.getDamageToHull()` 返回本次结算写入船体的伤害。
- `ApplyDamageResultAPI.getDamageToShields()` 返回本次结算写入护盾的伤害。
- `ApplyDamageResultAPI.getEmpDamage()` 返回本次结算写入的 EMP 伤害。
- `CombatEngineAPI.applyDamage(...)` 的 source 参数可为 `null`。
- `DamageAPI.computeDamageDealt(amount)` 按 DPS、倍率和基础伤害计算实际伤害。
- `DamageAPI.computeFluxDealt(amount)` 按 EMP 与 DPS 参数计算 EMP 伤害。
- `DamageAPI.getModifier()` 是 `DamageTakenModifier` 与 `DamageDealtModifier` 写入伤害修正的入口。
- `DamageAPI.isForceHardFlux()` 用于强制光束伤害产生硬幅能。
- `DamageAPI.isSoftFlux()` 用于非光束伤害的软幅能语义。
- `DamageDealtModifier.EMP_SHIP_SYSTEM_PARAM` 表示 EMP ship system 造成伤害时传入的 param。
- `DamageDealtModifier` 应注册在造成伤害的舰船或 engine 上。
- `DamageListener.reportDamageApplied(...)` 在伤害应用完成后接收结果。
- `DamageTakenModifier` 应注册在承受伤害的舰船或 engine 上。
- `EmpArcEntityAPI.isShieldHit()` 表示该 EMP arc 是否命中护盾。
- `EmpArcEntityAPI.setTargetToShipCenter(...)` 把 EMP arc 目标设为舰船中心。
- `HullDamageAboutToBeTakenListener.notifyAboutToTakeHullDamage(...)` 返回 true 时抵消该次船体伤害。

## 陷阱

- `ApplyDamageResultAPI` 是结果对象，读取时机应在伤害结算完成后。
- `CombatDamageData` 记录 fleet member 之间的船体伤害统计，实体实时血量以战斗实体状态为准。
- `DamageDealtModifier` 注册在承伤舰船上会导致造成伤害侧修改缺失。
- `DamageAPI.computeFluxDealt(amount)` 的返回值对应 EMP 伤害，不对应护盾幅能伤害。
- `DamageTakenModifier` 注册在造成伤害舰船上会导致承伤侧修改缺失。
- `EmpArcEntityAPI.setFadedOutAtStart(...)` 注释说明会显著增加渲染成本。
- 伤害监听器修改数值时绕过 `DamageAPI.getModifier()` 会丢失正式 modifier 归属。
