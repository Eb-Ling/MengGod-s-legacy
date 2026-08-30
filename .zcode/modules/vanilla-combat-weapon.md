# 战斗武器 API

来源：原版 Starsector。

## 定义

战斗武器 API 是武器 CSV、`.wpn`、武器 spec、武器槽位、运行时武器实例和武器回调的接口集合。

## 参考

- `com.fs.starfarer.api.combat.AmmoTrackerAPI`：描述武器弹药、装填进度、每秒恢复和装填数量。
- `com.fs.starfarer.api.combat.AutofireAIPlugin`：定义武器组自动开火目标、开火许可和目标读取接口。
- `com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin`：定义武器本帧转向、开火等核心操作前的逐帧回调。
- `com.fs.starfarer.api.combat.EveryFrameWeaponEffectPluginWithAdvanceAfter`：定义武器本帧核心操作后的逐帧回调。
- `com.fs.starfarer.api.combat.OnFireEffectPlugin`：定义武器生成弹丸时的一次回调。
- `com.fs.starfarer.api.combat.WeaponAPI`：描述运行时武器实例、角度、射程、弹药、冷却、伤害、sprite、spec 和 slot。
- `com.fs.starfarer.api.loading.BeamWeaponSpecAPI`：描述 `.wpn` 加载后的光束武器 spec、能耗、伤害、颜色、纹理和碰撞状态。
- `com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI`：描述 `.wpn` 加载后的弹丸武器 spec、能耗、装填、散布、弹速和弹丸 spec。
- `com.fs.starfarer.api.loading.WeaponSlotAPI`：描述舰船武器槽位、挂点类型、槽位尺寸、弧度、角度和装配适配。
- `com.fs.starfarer.api.loading.WeaponSpecAPI`：描述 `weapon_data.csv` 与 `.wpn` 加载后的武器 spec、数值、标签、tooltip 和图形字段。
- `data/weapons/*.wpn`：提供单个武器的 sprite、barrel、projectile、beam、sound 和 effect class 数据。
- `data/weapons/weapon_data.csv`：提供武器 id、tier、价格、射程、伤害、弹药、类型、OP、hint、tag 和 tooltip 数据。

## 边界

- `.wpn` 归属武器 sprite、barrel、projectile、beam、sound 和 weapon effect class 数据。
- `AmmoTrackerAPI` 归属运行时武器弹药数量和装填进度。
- `AutofireAIPlugin` 归属武器组自动开火目标选择与开火许可。
- `BeamWeaponSpecAPI` 归属光束武器的 spec 读取和运行时 spec 写入。
- `EveryFrameWeaponEffectPlugin` 归属单个武器实例核心操作前的每帧效果。
- `EveryFrameWeaponEffectPluginWithAdvanceAfter` 归属单个武器实例核心操作后的每帧效果。
- `OnFireEffectPlugin` 归属武器生成弹丸时的一次回调。
- `ProjectileWeaponSpecAPI` 归属弹丸武器的 spec 读取和运行时 spec 写入。
- `WeaponAPI` 归属当前战斗中的单个武器实例状态。
- `WeaponAPI.getEffectPlugin()` 归属当前武器实例的每帧武器效果对象。
- `WeaponAPI.getOriginalSpec()` 归属武器加载时的原始 spec 关联。
- `WeaponAPI.getSpec()` 返回当前武器实例的 spec。
- `WeaponSlotAPI` 归属舰船 hull spec 中的武器槽位约束和坐标。
- `WeaponSpecAPI` 归属加载后的武器基础数值、标签、tooltip 字段和资源字段。
- `weapon_data.csv` 归属武器表格数值注册。

## 链路

### 武器数据加载链路

1. 游戏读取 `data/weapons/weapon_data.csv`。
2. 表头列名映射武器 id、数值、类型、hint、tag 和 tooltip 字段。
3. 每一行按 id 创建或覆盖武器注册数据。
4. 游戏读取对应 `.wpn` 文件。
5. `.wpn` 绑定图形、声音、弹丸、光束和 effect class 数据。
6. 加载结果暴露为 `WeaponSpecAPI`、`ProjectileWeaponSpecAPI` 或 `BeamWeaponSpecAPI`。

### 武器实例链路

1. 舰船 hull spec 或 variant 指定武器槽位和武器 id。
2. 战斗创建舰船时按武器 id 和槽位创建 `WeaponAPI` 实例。
3. 武器实例绑定 `WeaponSpecAPI`、`WeaponSlotAPI`、ammo tracker、damage、sprite 和 effect plugin。
4. 引擎每帧推进武器转向、充能、开火、冷却、弹药和禁用状态。
5. 脚本通过 `WeaponAPI` 读取或写入当前武器实例状态。

### 武器回调链路

1. 武器实例从 `.wpn` 或 projectile spec 绑定 effect class。
2. `EveryFrameWeaponEffectPlugin.advance(amount, engine, weapon)` 在武器本帧核心操作前执行。
3. 实现 `EveryFrameWeaponEffectPluginWithAdvanceAfter` 时，`advanceAfter(amount, engine, weapon)` 在本帧核心操作后执行。
4. `OnFireEffectPlugin.onFire(projectile, weapon, engine)` 在弹丸生成时执行。

### 自动开火链路

1. `BaseModPlugin.pickWeaponAutofireAI(weapon)` 或原版选择流程准备 `AutofireAIPlugin`。
2. 武器组开启自动开火时，引擎推进 `AutofireAIPlugin.advance(amount)`。
3. 自动开火 AI 读取目标点、舰船目标、导弹目标和当前武器。
4. 引擎读取 `shouldFire()` 判断当前武器是否开火。
5. 武器组开关变化时引擎调用 `forceOff()` 促使 AI 重新评估。

## 规范

- `AutofireAIPlugin.getTarget()` 可在武器目标缺省时返回 `null`。
- `AutofireAIPlugin.getTargetShip()` 只在当前目标是舰船时返回舰船。
- `EveryFrameWeaponEffectPlugin` 在暂停时不总是停止推进。
- `WeaponAPI.ensureClonedSpec()` 用于当前武器实例需要独立 spec 时的显式克隆。
- `WeaponAPI.getAnimation()` 对静态贴图武器可返回 `null`。
- `WeaponAPI.getBarrelSpriteAPI()` 对 barrel 图形缺省的武器可返回 `null`。
- `WeaponAPI.getSpec()` 读取当前武器实例 spec，修改前应确认 clone 语义。
- `WeaponAPI.getUnderSpriteAPI()` 对 under sprite 缺省的武器可返回 `null`。
- 弹丸、导弹和光束上的 weapon 关联由对应实体接口暴露。
- `WeaponAPI.hasAIHint(...)` 读取当前武器实例 spec 中的 AI hint。
- `WeaponSlotAPI.weaponFits(spec)` 按槽位类型、尺寸和武器类型判断装配适配。
- 武器 spec 集合由 settings 接口读取。
- `WeaponSpecAPI.getProjectileSpec()` 对不同武器 spec 类型返回对应 projectile spec 对象或空语义。
- `weapon_data.csv` 的 id 列是武器 spec 注册和合并的主键。
- `.wpn` 中 effect class 字段必须是脚本类加载器可实例化的完整类名。

## 陷阱

- `EveryFrameWeaponEffectPlugin.advance(...)` 发生在武器本帧核心操作前。
- `WeaponAPI.getOriginalSpec()` 和 `WeaponAPI.getSpec()` 表示不同 spec 关联。
- `WeaponAPI.getSpec()` 当作所有同类武器共享全局 spec 会污染单实例状态。
- `WeaponAPI.getSprite()` 返回的 sprite 在渲染前会被武器角度和 alpha 覆盖部分状态。
- `WeaponAPI.getUnderSpriteAPI()` 注释中的示例为原版武器文件示例。
- `WeaponAPI.isBeam()` 表示当前武器实例类型；`BeamAPI` 实体归属在光束实体接口中描述。
- `.wpn` 与 `weapon_data.csv` 共同形成武器 spec，单独读取其中一侧会丢失字段语义。
- `weapon_data.csv` 的 hints 和 tags 是不同字段，消费入口不同。
