# MagicLib API

来源：硬依赖外部库 MagicLib。

## 定义

MagicLib API 是 MagicTrail 战斗尾迹、MagicFakeBeam 瞬时伪光束、MagicCampaign 生涯辅助、MagicUI 战斗 HUD、MagicSettings 设置读取和 MagicBounty 数据赏金组成的硬依赖接口集合。

## 参考

- `data/config/modFiles/magicBounty_data.json`：MagicBounty 按启用模组合并读取的赏金定义入口。
- `org.magiclib.bounty.MagicBountyLoader.loadBountyData()`：遍历启用模组并按当前模式合并 MagicBounty JSON。
- `org.magiclib.bounty.MagicBountyLoader.loadBountiesFromJSON(...)`：读取、过滤、转换并校验 MagicBounty JSON。
- `org.magiclib.bounty.MagicBountySpec`：保存 trigger、job、target、fleet 和 location 字段的运行时结构。
- `org.magiclib.plugins.MagicTrailPlugin`：维护战斗尾迹插件、trail tracker、cutting map 和分层渲染插件。
- `org.magiclib.util.MagicCampaign.createDerelict(...)`：按 variant、condition、orbit 和 recoverable 参数创建 derelict 实体。
- `org.magiclib.util.MagicFakeBeam.getCollisionPointOnCircumference(...)`：计算线段进入圆周的第一个碰撞点。
- `org.magiclib.util.MagicFakeBeam.getShipCollisionPoint(...)`：同时处理舰体和护盾弧的线段碰撞点。
- `org.magiclib.util.MagicSettings.getBoolean(...)`：从合并后的 `data/config/modSettings.json` 读取布尔设置。
- `org.magiclib.util.MagicUI.drawHUDStatusBar(...)`：在战斗 HUD 区域绘制状态条。
- `org.magiclib.util.MagicVariables`：保存 MagicLib 常量、sector 尺寸缓存和 bounty 系统状态。

## 边界

- `MagicBountyLoader.BOUNTIES` 归属 MagicBounty 已加载赏金的全局注册表。
- `MagicBountyLoader.loadBountiesFromJSON(...)` 归属赏金 JSON 到 `MagicBountySpec` 的字段转换。
- `MagicBountySpec` 归属单个赏金的 trigger、job、target、fleet 和 location 数据结构。
- `MagicCampaign.createDerelict(...)` 归属 derelict token、salvage special、discoverable XP 和 circular orbit 创建。
- `MagicCampaign.findSuitableTarget(...)` 归属按 market、theme、entity tag、distance 和 explored 状态挑选生涯目标。
- `MagicFakeBeam.spawnFakeBeam(...)` 归属瞬时伤害、命中粒子和伪光束视觉生成。
- `MagicFakeBeam.getShipCollisionPoint(...)` 归属线段对舰体与护盾弧的命中点解析。
- `MagicSettings` 归属 `data/config/modSettings.json` 的合并读取、类型转换和缺省返回。
- `MagicTrailPlugin` 归属 tail piece、trail ID、linked entity cutting map 和 render layer 状态。
- `MagicTrailPlugin.getUniqueID()` 归属同一条 trail 的跨帧连接 ID 分配。
- `MagicUI` 归属战斗 HUD 坐标、UI scaling、LazyFont 文本和 OpenGL 栈切换。
- `MagicVariables` 归属 MagicLib 常量、bounty 开关缓存和 sector 尺寸缓存。
- MagicBounty JSON 的 `required_mods_id` 归属单个赏金的启用条件过滤。
- MagicBounty JSON 的 `trigger_*` 字段归属赏金出现条件。
- MagicBounty JSON 的 `fleet_*` 字段归属赏金目标舰队生成参数。

## 链路

### MagicBounty 读取链路

1. `MagicBountyLoader.loadBountiesFromJSON(appendOnly)` 调用 `loadBountyData()`。
2. `loadBountyData()` 依据 MagicLib test mode 选择 `magicBounty_data.json` 或 `magicBounty_data_testing.json`。
3. `Global.getSettings().getModManager().getEnabledModsCopy()` 提供启用模组列表。
4. `Global.getSettings().loadJSON(jsonPath, modSpec.getId())` 读取每个启用模组的赏金 JSON。
5. 每个 bounty object 按 bounty id 合并进 `bounty_data`。
6. `required_mods_id` 逐项检查启用状态。
7. `getString(...)`、`getInt(...)`、`getFloat(...)`、`getStringList(...)` 和 map 读取器解析字段。
8. 枚举字段转换为 `JobType`、`ShowFleet`、`ShowDistance`、`FleetAssignment` 和 `SkillPickPreference`。
9. 构造 `MagicBountySpec`。
10. `appendOnly` 控制已存在或已接取赏金的写入条件。
11. 符合写入条件的赏金写入 `MagicBountyLoader.BOUNTIES`。
12. `validateAndCullLoadedBounties()` 校验 faction、variant 和 preset ship 关联。
13. `bountyProviders` 从 `MagicSettings.getList(...)` 读取并重建 provider 注册表。

### MagicCampaign Derelict 链路

1. 调用方传入 variant、condition、discoverable、discovery XP、recoverable、orbit center、orbit angle、orbit radius 和 orbit days。
2. `MagicCampaign.createDerelict(...)` 创建 `ShipRecoverySpecial.PerShipData`。
3. `DerelictShipEntityPlugin.DerelictShipData` 封装 derelict 参数。
4. `BaseThemeGenerator.addSalvageEntity(...)` 生成 wreck token。
5. wreck 设置 discoverable 与 discovery XP。
6. wreck 设置 circular orbit。
7. recoverable 为 true 时创建并写入 salvage special。
8. 带 `isRecoverable` 参数的重载可追加 unrecovarable tag。

### MagicFakeBeam 碰撞链路

1. 调用方传入 from、range、angle、damage、damage type、emp 和 source。
2. `MathUtils.getPoint(...)` 生成默认终点。
3. `CombatUtils.getEntitiesWithinRange(...)` 取候选实体。
4. `CollisionClass.NONE` 实体跳过。
5. 舰船候选进入 `getShipCollisionPoint(...)`。
6. 非舰船候选进入 `getCollisionPointOnCircumference(...)`。
7. 最近碰撞点替换 beam end。
8. 命中目标通过 `CombatEngineAPI.applyDamage(...)` 立即结算。
9. `MagicFakeBeamPlugin.addBeam(...)` 或 MagicTrail 方法生成视觉。

### MagicTrail 渲染链路

1. 战斗初始化创建 `MagicTrailPlugin`。
2. `MagicTrailPlugin.init(engine)` 创建并注册 `MagicTrailRenderer`。
3. `init(...)` 把插件与 renderer 写入 engine customData。
4. 调用方通过 `getUniqueID()` 保存 trail ID。
5. 调用方按帧调用 `addTrailMemberSimple(...)` 或 `addTrailMemberAdvanced(...)`。
6. `addOrGetTrailTracker(...)` 按 layer、texture 和 ID 取得 tracker。
7. linked entity 写入 cutting map。
8. `advance(...)` 推进 tracker timer 并清理过期 tracker。
9. `MagicTrailRenderer.render(...)` 按 layer 渲染 tracker。

### MagicUI HUD 链路

1. 调用方传入 player ship、fill、颜色、文本和偏移参数。
2. `shouldDrawHUD(...)` 或绘制方法内部检查 HUD、dialog、command UI 和 player ship。
3. `getUIElementOffset(...)` 计算 HUD 元素偏移。
4. `UI_SCALING` 缩放坐标和尺寸。
5. `openGL11ForText(...)` 或绘制方法内 OpenGL 栈设置 HUD 坐标。
6. bar、box 或 LazyFont 文本绘制。
7. `closeGL11ForText(...)` 或方法尾部恢复 OpenGL 栈。

### MagicSettings 读取链路

1. 调用方以 mod id 和 setting id 请求具体类型。
2. `modSettings == null` 时调用 `loadModSettings()`。
3. `loadModSettings()` 调用 `Global.getSettings().getMergedJSONForMod(...)`。
4. 类型读取方法进入对应 mod id 的 JSON object。
5. 字段存在时执行 boolean、string、float、integer、color、list 或 map 转换。
6. 字段缺失或解析失败时返回该类型的固定缺省值。

## 规范

- `MagicBountyLoader` 读取 `required_mods_id` 时要求列表中的每个 mod 均已启用。
- `MagicBountyLoader` 对 `fleet_behavior` 接受 `PASSIVE`、`AGGRESSIVE` 和 `ROAMING` 并分别转换为 `ORBIT_PASSIVE`、`DEFEND_LOCATION` 和 `PATROL_SYSTEM`。
- `MagicBountyLoader` 对 `fleet_behavior` 的空值、`GUARDED` 和其它未匹配字符串使用初始值 `ORBIT_AGGRESSIVE`。
- `MagicBountyLoader` 对 `job_type` 接受 assassination、destruction、obliteration、neutralization 和 neutralisation。
- `MagicBountyLoader` 对 `job_show_distance` 接受 none、vague、distance、system、vanilla、vanillaDistance 和 exact。
- `MagicBountyLoader` 对 `job_show_fleet` 接受 none、text、flagship、flagshipText、preset、presetText、vanilla 和 all。
- `MagicBountyLoader` 把缺省 job memory key 设为 `$` 加 bounty id。
- `MagicBountyLoader` 在 appendOnly 为 true 时保留已存在或已接取赏金。
- `MagicBountySpec` 的 `existing_target_memkey` 会覆盖 target、fleet 和 location 后续生成语义。
- `MagicCampaign.createDerelict(...)` 的 recoverable 参数控制 salvage special 写入。
- `MagicCampaign.createDerelict(..., isRecoverable)` 的 `isRecoverable == false` 会添加 `Tags.UNRECOVERABLE`。
- `MagicFakeBeam.getCollisionPointOnCircumference(...)` 在线段起点位于圆内时返回起点。
- `MagicFakeBeam.getShipCollisionPoint(...)` 在 shield 开启且圆周碰撞点处于 shield arc 内时返回 shield 命中点。
- `MagicSettings.getBoolean(...)` 缺省返回 false。
- `MagicSettings.getColorRGB(...)` 和 `getColorRGBA(...)` 缺省返回 `Color.RED`。
- `MagicSettings.getColorRGB(...)` 和 `getColorRGBA(...)` 接受 `[r,g,b]` 或 `[r,g,b,a]` 字符串并把分量 clamp 到 0-255。
- `MagicSettings.getFloat(...)` 和 `getInteger(...)` 缺省返回 0。
- `MagicSettings.getList(...)`、`getFloatMap(...)`、`getStringMap(...)` 和 `getColorMap(...)` 缺省返回空集合。
- `MagicTrailPlugin.addTrailMemberAdvanced(...)` 的 `SIZE_PULSE_WIDTH`、`SIZE_PULSE_COUNT` 和 `FORWARD_PROPAGATION` advanced options 需要对应 Float、Integer 和 Boolean 类型。
- `MagicTrailPlugin.addTrailMemberSimple(...)` 默认渲染层为 `CombatEngineLayers.CONTRAILS_LAYER`。
- `MagicTrailPlugin.cutTrailsOnEntity(...)` 通过替换 tracker ID 断开 linked entity 的旧 trail。
- `MagicTrailPlugin.getUniqueID()` 返回的 ID 必须在同一条 trail 生命周期内复用。
- `MagicTrailPlugin` 的 library-free cut 入口要求 engine customData 的 key 包含 `MagicTrailPlugin_LIB_FREE_TRAIL_CUT`，value 为 `CombatEntityAPI`。
- `MagicUI` 文本绘制依赖 Victor 字体加载后的 `LazyFont.DrawableString`。
- `MagicUI` 的 HUD 绘制只在目标 ship 是玩家舰船且 HUD 可见时输出。

## 陷阱

- `MagicBountyLoader` 对 faction 和 variant 的校验会剔除无法解析的赏金。
- `MagicBountyLoader` 对 `fleet_behavior` 的 `GUARDED` 输入使用默认 `ORBIT_AGGRESSIVE`，该字符串本身没有独立 case。
- `MagicBountyLoader` 的 `required_mods_id` 过滤发生在字段转换之前，缺失依赖会使该赏金跳过。
- `MagicBountySpec` 的 `job_memKey` 是接取、完成和 appendOnly 保留逻辑的状态键，复用会污染赏金状态。
- `MagicCampaign.loadVariant(...)` 读取 `.variant` 时会把无法解析的 JSON 记录为 warn 并返回 null。
- `MagicFakeBeam.spawnFakeBeam(...)` 立即调用 `applyDamage(...)`，持续视觉时间不代表持续伤害时间。
- `MagicFakeBeam.spawnFakeBeam(...)` 使用 source owner 判断战机和导弹过滤，source 需要符合伤害归属语义。
- `MagicFakeBeam.getShipCollisionPoint(...)` 在 `CollisionUtils.getCollisionPoint(...)` 返回 null 后继续偏移会传递 null 风险。
- `MagicSettings` 缺失字段返回固定缺省值，同时写入 warn 日志；缺省值会参与后续运行语义。
- `MagicTrailPlugin` 的同一条 trail 使用变化的 sprite 会拆分成不同 texture tracker。
- `MagicTrailPlugin` 的同一条 trail 每帧生成新 ID 会表现为多条无法连接的短 trail。
- `MagicTrailPlugin` 清理 linked entity cutting map 依赖 engine entity in play 状态。
- `MagicUI` 绘制方法直接操作 OpenGL 栈，未成对恢复会污染后续 HUD 或世界渲染。
