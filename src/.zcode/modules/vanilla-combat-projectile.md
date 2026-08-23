# 战斗弹丸 API

来源：原版 Starsector。

## 定义

战斗弹丸 API 是 `.proj` 数据、弹丸 spec、导弹 spec、运行时弹丸、导弹、爆炸弹丸和导弹 AI 的接口集合。

## 参考

- `com.fs.starfarer.api.combat.CombatEngineAPI.getMissiles()`：读取当前战斗导弹集合。
- `com.fs.starfarer.api.combat.CombatEngineAPI.getProjectiles()`：读取当前战斗弹丸集合，集合包含导弹。
- `com.fs.starfarer.api.combat.CombatEngineAPI.spawnDamagingExplosion(...)`：按 `DamagingExplosionSpec` 生成爆炸弹丸。
- `com.fs.starfarer.api.combat.CombatEngineAPI.spawnProjectile(...)`：按 weapon id 或 projectile spec id 生成弹丸实体。
- `com.fs.starfarer.api.combat.DamagingProjectileAPI`：描述运行时伤害弹丸的伤害、命中、source、weapon、spec 和生成状态。
- `com.fs.starfarer.api.combat.GuidedMissileAI`：定义导弹目标读取与 flare 改写目标入口。
- `com.fs.starfarer.api.combat.MissileAIPlugin`：定义导弹每帧导航回调。
- `com.fs.starfarer.api.combat.MissileAPI`：扩展伤害弹丸并描述运行时导弹、引擎、制导、装填、雷区和视觉状态。
- `com.fs.starfarer.api.combat.OnHitEffectPlugin`：定义弹丸命中结算时的一次回调。
- `com.fs.starfarer.api.loading.DamagingExplosionSpec`：描述爆炸弹丸的半径、伤害、碰撞、粒子、声音和视觉参数。
- `com.fs.starfarer.api.loading.MissileSpecAPI`：描述 `.proj` 加载后的导弹 spec、引擎、装填、爆炸、行为和回调字段。
- `com.fs.starfarer.api.loading.ProjectileSpawnType`：定义弹丸生成类型枚举。
- `com.fs.starfarer.api.loading.ProjectileSpecAPI`：描述 `.proj` 加载后的普通弹丸 spec、视觉、碰撞、伤害、行为和回调字段。
- `data/weapons/proj/*.proj`：提供 projectile 与 missile spec 数据入口。

## 边界

- `.proj` 归属 projectile 与 missile 的 spec 数据。
- `CombatEngineAPI.getMissiles()` 归属当前战斗导弹集合读取。
- `CombatEngineAPI.getProjectiles()` 归属当前战斗伤害弹丸集合读取。
- `CombatEngineAPI.spawnDamagingExplosion(...)` 归属爆炸弹丸生成。
- `CombatEngineAPI.spawnProjectile(...)` 归属普通弹丸和导弹生成。
- `DamagingExplosionSpec` 归属爆炸弹丸的伤害、半径、碰撞、粒子和声音参数。
- `DamagingProjectileAPI` 归属运行时伤害弹丸状态。
- `GuidedMissileAI` 归属导弹目标暴露与 flare 目标改写。
- `MissileAIPlugin` 归属导弹每帧导航。
- `MissileAPI` 归属运行时导弹实体状态。
- `MissileSpecAPI` 归属 `specClass:"missile"` 加载后的导弹 spec。
- `OnHitEffectPlugin` 归属弹丸命中结算时的一次回调。
- `ProjectileSpawnType` 归属弹丸生成类型。
- `ProjectileSpecAPI` 归属 `specClass:"projectile"` 加载后的普通弹丸 spec。

## 链路

### 弹丸数据加载链路

1. 武器 `.wpn` 或其它数据入口引用 projectile id。
2. 游戏读取 `data/weapons/proj/*.proj` 中匹配 id 的文件。
3. `specClass:"projectile"` 加载为 `ProjectileSpecAPI`。
4. `specClass:"missile"` 加载为 `MissileSpecAPI`。
5. `spawnType` 或 `missileType` 决定生成后的实体类型和行为族。
6. `onFireEffect`、`onHitEffect`、`behaviorSpec` 和 `explosionSpec` 绑定后续运行时回调和爆炸数据。

### 弹丸生成链路

1. 调用方准备 source、weapon、weapon id、projectile spec id、生成点、角度和可选速度。
2. 调用 `CombatEngineAPI.spawnProjectile(...)`。
3. 引擎读取 weapon id 或 projectile spec id 对应的 projectile/missile spec。
4. 引擎创建 `DamagingProjectileAPI` 或 `MissileAPI` 实体。
5. 实体保存 source、weapon、spawn location、spawn type、damage 和 projectile spec 关联。
6. 引擎把实体纳入 projectile 集合，导弹实体同时纳入 missile 集合。

### 导弹 AI 链路

1. 游戏创建 `MissileAPI` 实例。
2. `BaseModPlugin.pickMissileAI(missile, launchingShip)` 可返回 `PluginPick<MissileAIPlugin>`。
3. 引擎把选择结果安装到导弹。
4. 引擎每帧调用 `MissileAIPlugin.advance(amount)`。
5. 导弹 AI 通过 `missile.giveCommand(...)` 输出移动指令。
6. 同时实现 `GuidedMissileAI` 时通过 `getTarget()` 暴露当前目标。
7. flare 或目标系统可通过 `setTarget(...)` 改写导弹目标。

### 命中与爆炸链路

1. 弹丸或导弹进入命中结算。
2. 引擎按 `DamageAPI`、damage type、EMP 和碰撞语义结算伤害。
3. 命中完成后 `didDamage()` 返回 true。
4. `getDamageTarget()` 暴露承伤实体。
5. 绑定的 `OnHitEffectPlugin.onHit(...)` 接收 projectile、target、命中点、护盾命中状态和伤害结果。
6. `spawnDamagingExplosion(...)` 以 `DamagingExplosionSpec` 创建爆炸弹丸。
7. 爆炸弹丸用 damaged-already 集合记录已经承伤的实体。

## 规范

- `.proj` 的 `specClass:"missile"` 加载为导弹 spec。
- `.proj` 的 `specClass:"projectile"` 加载为普通弹丸 spec。
- `CombatEngineAPI.getProjectiles()` 返回的集合包含导弹。
- `CombatEngineAPI.spawnProjectile(...)` 的 ship、weapon 和 shipVelocity 参数可为 `null`，weaponId 和 point 是必需输入。
- `DamagingProjectileAPI.didDamage()` 表达弹丸是否已经完成伤害结算。
- `DamagingProjectileAPI.getDamageTarget()` 在 `didDamage()` 为 true 后暴露承伤对象，返回值可为 `null`。
- `DamagingProjectileAPI.getSource()` 作为允许 `null` 的 API 参数传递时可直接传入。
- `DamagingProjectileAPI.getSource()` 只有直接调用 source 方法前需要判空。
- `DamagingProjectileAPI.getTailEnd()` 只对 moving ray 和 ballistic projectile 类型有非空语义。
- `DamagingProjectileAPI.getWeapon()` 可为 `null`。
- `DamagingProjectileAPI.setFromMissile(true)` 用于从导弹生成的 ballistic、ballistic-as-beam 和 plasma 弹丸。
- `MissileAPI.getMissileAI()` 返回内部包装对象，`getUnwrappedMissileAI()` 返回传入 `setMissileAI(...)` 的对象。
- `MissileAPI.giveCommand(...)` 只应由 `MissileAIPlugin` 使用，且只有移动类 `ShipCommand` 有效。
- `MissileAIPlugin` 在暂停时由原版停止推进。
- `OnHitEffectPlugin.onHit(...)` 在弹丸命中结算时接收当前命中上下文。
- `ProjectileSpecAPI.getMoveSpeed(shipStats, weapon)` 可用 `null` shipStats 读取基础值。

## 陷阱

- `CombatEngineAPI.getProjectiles()` 已包含导弹，同时遍历 `getMissiles()` 会重复处理导弹。
- `DamagingProjectileAPI.getSource()` 可为 `null`，直接调用 source 方法前需要判空。
- `DamagingProjectileAPI.getTailEnd()` 对导弹、plasma shot 等类型缺省。
- `MissileAPI.getMissileAI()` 与 `getUnwrappedMissileAI()` 表示不同 AI 对象。
- `MissileAIPlugin` 未实现 `GuidedMissileAI` 时 flare 影响语义会缺失。
- `OnHitEffectPlugin` 消费的是本次命中上下文，跨命中缓存 projectile、target 或 damageResult 会污染命中状态归属。
- source 可在方法内局部缓存，跨帧字段缓存会污染实体归属。
