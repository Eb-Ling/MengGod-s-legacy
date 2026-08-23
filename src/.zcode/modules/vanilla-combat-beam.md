# 战斗光束 API

来源：原版 Starsector。

## 定义

战斗光束 API 是生成后的光束实体、光束集合、光束逐帧效果和光束运行态读写接口集合。

## 参考

- `com.fs.starfarer.api.combat.BeamAPI`：描述单根已生成光束的端点、来源、武器关联、命中、伤害和视觉状态。
- `com.fs.starfarer.api.combat.BeamAPI.didDamageThisFrame()`：暴露当前帧光束是否完成伤害结算。
- `com.fs.starfarer.api.combat.BeamAPI.getDamageTarget()`：暴露当前帧或当前状态关联的承伤实体。
- `com.fs.starfarer.api.combat.BeamAPI.getFrom()`：暴露当前光束起点坐标。
- `com.fs.starfarer.api.combat.BeamAPI.getRayEndPrevFrame()`：暴露上一帧光束射线终点。
- `com.fs.starfarer.api.combat.BeamAPI.getTo()`：暴露当前光束终点坐标。
- `com.fs.starfarer.api.combat.BeamEffectPlugin`：定义单根光束存在期间的逐帧效果回调。
- `com.fs.starfarer.api.combat.BeamEffectPluginWithReset`：定义带复位入口的光束逐帧效果回调。
- `com.fs.starfarer.api.combat.CombatEngineAPI.getBeams()`：读取当前战斗引擎拥有的光束实体集合。

## 边界

- `BeamAPI` 归属单根生成后的光束实体状态。
- `BeamAPI.getCoreColor()` 与 `BeamAPI.setCoreColor(...)` 归属光束核心颜色读写。
- `BeamAPI.getDamage()` 归属光束当前伤害对象。
- `BeamAPI.getDamageTarget()` 归属光束命中状态中的当前目标关联。
- `BeamAPI.getFringeColor()` 与 `BeamAPI.setFringeColor(...)` 归属光束边缘颜色读写。
- `BeamAPI.getFrom()` 与 `BeamAPI.getTo()` 归属光束当前帧端点坐标。
- `BeamAPI.getHitGlow()` 与 `BeamAPI.setHitGlow(...)` 归属命中 glow sprite 状态。
- `BeamAPI.getRayEndPrevFrame()` 与 `BeamAPI.getLengthPrevFrame()` 归属上一帧射线终点和长度。
- `BeamAPI.getSource()` 归属发射舰船关联。
- `BeamAPI.getWeapon()` 归属发射武器实例关联。
- `BeamAPI.getWidth()` 与 `BeamAPI.setWidth(...)` 归属光束显示宽度。
- `BeamEffectPlugin` 归属单根光束存在期间的逐帧效果。
- `BeamEffectPluginWithReset` 归属光束效果对象的复位入口。
- `CombatEngineAPI.getBeams()` 归属当前战斗引擎的活动光束集合读取。

## 链路

### 光束集合链路

1. 武器或战斗脚本触发光束生成。
2. 引擎创建 `BeamAPI` 实例。
3. 光束实例保存 from、to、source、weapon 和 damage 状态。
4. 引擎把光束实例纳入当前战斗光束集合。
5. 代码通过 `CombatEngineAPI.getBeams()` 读取当前集合。
6. 光束生命周期结束后退出当前战斗光束集合。

### 光束命中链路

1. 引擎推进光束射线。
2. 光束记录当前帧 from、to、length 和上一帧 ray end。
3. 引擎执行命中与伤害结算。
4. 光束通过 `didDamageThisFrame()` 暴露当前帧伤害结果。
5. 光束通过 `getDamageTarget()` 暴露命中目标关联。
6. 命中视觉通过 hit glow、hit glow brightness 和 hit glow radius 暴露。

### 光束效果链路

1. 光束实例绑定光束效果对象。
2. 引擎在光束存在期间调用 `BeamEffectPlugin.advance(amount, engine, beam)`。
3. 效果对象读取光束端点、source、weapon、damage 和命中状态。
4. 效果对象写入光束颜色、宽度、纹理、hit glow 或其它战斗实体状态。
5. 实现 `BeamEffectPluginWithReset` 的效果对象通过 `reset()` 接收复位调用。

## 规范

- `BeamAPI.didDamageThisFrame()` 只表达当前帧伤害结算结果。
- `BeamAPI.getBrightness()` 读取当前光束亮度。
- `BeamAPI.getDamage()` 返回当前光束伤害对象。
- `BeamAPI.getDamageTarget()` 返回当前光束命中目标关联。
- `BeamAPI.getFrom()` 与 `BeamAPI.getTo()` 返回当前帧世界坐标端点。
- `BeamAPI.getHitGlowBrightness()` 读取当前命中 glow 亮度。
- `BeamAPI.getHitGlowRadius()` 读取当前命中 glow 半径。
- `BeamAPI.getLength()` 返回当前帧光束长度。
- `BeamAPI.getLengthPrevFrame()` 返回上一帧光束长度。
- `BeamAPI.getPixelsPerTexel()` 与 `BeamAPI.setPixelsPerTexel(...)` 读写纹理缩放状态。
- `BeamAPI.getRayEndPrevFrame()` 返回上一帧光束射线终点。
- `BeamAPI.getSource()` 返回发射舰船关联。
- `BeamAPI.getWeapon()` 返回发射武器实例关联。
- `BeamAPI.setCoreTexture(...)` 与 `BeamAPI.setFringeTexture(...)` 使用 sprite name 写入当前光束纹理。
- `BeamEffectPlugin` 在暂停时由原版停止推进。

## 陷阱

- `BeamAPI.getDamageTarget()` 表示光束命中状态关联，读取前应以当前帧结算语义决定消费时机。
- `BeamAPI.getRayEndPrevFrame()` 与 `BeamAPI.getTo()` 分属上一帧和当前帧端点，混用时应明确扫线区间。
- `BeamAPI.getWeapon()` 与 `BeamAPI.getSource()` 表示两个实体关联。
- `BeamEffectPlugin` 实例字段归属单根光束效果对象，跨光束共享状态应放入引擎级或显式 owner 结构。
- `setCoreTexture(...)`、`setFringeTexture(...)` 和 `setHitGlow(...)` 写入当前光束视觉状态，复用 sprite 或名称时应保持状态来源明确。
