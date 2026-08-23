# 技能

来源：原版 Starsector。

## 定义

技能是原版 character skill 数据、spec、effect 脚本、scope、描述和生效范围的接口集合。

## 参考

- `com.fs.starfarer.api.SettingsAPI.getSkillSpec(...)`：按 skill id 读取 `SkillSpecAPI`。
- `com.fs.starfarer.api.characters.AfterShipCreationSkillEffect`：定义舰船实体创建后的 skill effect 回调。
- `com.fs.starfarer.api.characters.CharacterStatsSkillEffect`：定义角色 stats skill effect。
- `com.fs.starfarer.api.characters.CustomSkillDescription`：定义自定义 skill 描述 UI。
- `com.fs.starfarer.api.characters.DescriptionSkillEffect`：定义描述型 skill effect 的文本、高亮和颜色。
- `com.fs.starfarer.api.characters.FleetStatsSkillEffect`：定义 fleet stats skill effect。
- `com.fs.starfarer.api.characters.FleetTotalSource`：定义 skill tooltip 中的 fleet total 数据源。
- `com.fs.starfarer.api.characters.LevelBasedEffect`：定义按等级描述和 `ScopeDescription`。
- `com.fs.starfarer.api.characters.MarketSkillEffect`：定义市场或 outpost skill effect。
- `com.fs.starfarer.api.characters.ShipSkillEffect`：定义写入 `MutableShipStatsAPI` 的舰船 skill effect。
- `com.fs.starfarer.api.characters.SkillEffectType`：定义原版 skill effect 类型枚举。
- `com.fs.starfarer.api.characters.SkillSpecAPI`：定义 skill spec、effect spec、scope、tags、flags 和 unlock 集合。
- `com.fs.starfarer.api.impl.campaign.ids.Skills`：定义原版 skill id、aptitude id 和 skill tag 常量。
- `com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription`：提供 fleet total、阈值说明和自定义描述基础实现。
- `data/characters/skills/*.skill`：定义 skill 的 effectGroups、requiredSkillLevel、effectBasedOnLevel、script、scope、scopeStr 和 elite。
- `data/characters/skills/aptitude_data.csv`：定义 aptitude id、名称、颜色、描述、排序、音效和 elite overlay。
- `data/characters/skills/skill_data.csv`：定义 skill id、名称、排序、tier、点数需求、角色类型 flags、tags 和 icon。

## 边界

- `AfterShipCreationSkillEffect` 归属舰船实体创建后读写 `ShipAPI` 的 skill effect。
- `CharacterStatsSkillEffect` 归属写入 `MutableCharacterStatsAPI` 的角色数值 effect。
- `CustomSkillDescription` 归属 skill tooltip 的自定义描述 UI。
- `DescriptionSkillEffect` 归属纯描述行、highlight 和 highlight color。
- `FleetStatsSkillEffect` 归属写入 `MutableFleetStatsAPI` 的舰队数值 effect。
- `FleetTotalSource` 归属 skill 面板 fleet total 条目生成。
- `LevelBasedEffect` 归属等级描述、per-level 描述和 scope 描述。
- `MarketSkillEffect` 归属 market、governed outpost 和 all outposts 的数值 effect。
- `MutableCharacterStatsAPI` 归属角色持有的 skill level 与刷新入口。
- `ShipSkillEffect` 归属写入 `MutableShipStatsAPI` 的舰船数值 effect。
- `SkillEffectType` 归属 effect spec 到 Java effect 接口的类型分派。
- `SkillSpecAPI` 归属 skill id、aptitude、tier、scope、tags、flags、unlock 和 effect spec 集合。
- `Skills` 归属原版 skill id、aptitude id 和 tag 常量。
- `data/characters/skills/*.skill` 归属 effect group、脚本类名、required level、scope 和 elite 数据。
- `data/characters/skills/aptitude_data.csv` 归属 aptitude 分组、颜色、排序、音效和 elite overlay 数据。
- `data/characters/skills/skill_data.csv` 归属 skill 列表、排序、点数门槛、角色类型 flags、tags 和 icon 数据。

## 链路

### CSV / spec 加载链路

1. 游戏读取 `data/characters/skills/aptitude_data.csv`。
2. `id`、`name`、`color`、`description`、`order`、`effect_skill_id`、音效列和 overlay 列写入 aptitude 数据。
3. 游戏读取 `data/characters/skills/skill_data.csv`。
4. `id`、`name`、`order`、`tier`、`reqPoints`、`reqPointsPerExtraSkill`、`description`、`author`、角色类型 flags、`tags` 和 `icon` 写入 `SkillSpecAPI`。
5. 游戏读取 `data/characters/skills/*.skill`。
6. `id` 和 `governingAptitude` 绑定到对应 `SkillSpecAPI`。
7. `scope`、`scopeStr`、`scope2`、`scopeStr2`、`elite` 和 `compressHullmods` 写入 `SkillSpecAPI`。
8. `effectGroups` 内的 `requiredSkillLevel`、`effectBasedOnLevel`、`name` 和 `effects` 写入 `SkillEffectSpecAPI`。
9. effect 内的 `type` 解析为 `SkillEffectType`。
10. effect 内的 `script` 实例化为对应 skill effect 接口。

### campaign 生效链路

1. 角色对象持有 `MutableCharacterStatsAPI` 与 skill level。
2. 调用 `MutableCharacterStatsAPI.refreshCharacterStatsEffects(...)` 刷新 `CHARACTER_STATS` effect。
3. `SkillEffectSpecAPI.getAsStatsEffect()` 返回 `CharacterStatsSkillEffect`。
4. `CharacterStatsSkillEffect.apply(stats, id, level)` 写入角色 stats。
5. 调用 `MutableCharacterStatsAPI.refreshGovernedOutpostEffects(market)` 刷新 `GOVERNED_OUTPOST` effect。
6. `SkillEffectSpecAPI.getAsMarketEffect()` 返回 `MarketSkillEffect`。
7. `MarketSkillEffect.apply(market, id, level)` 写入 market 或 outpost 状态。
8. 调用 `MutableCharacterStatsAPI.refreshAllOutpostsEffects(...)` 刷新 `ALL_OUTPOSTS` effect。
9. 舰队读取 commander stats 后刷新 `FLEET` effect。
10. `FleetStatsSkillEffect.apply(fleetStats, id, level)` 写入 fleet stats。

### combat 生效链路

1. 舰队成员读取 captain 或 commander-for-stats 的 `MutableCharacterStatsAPI`。
2. 战斗或舰船 stats 创建流程读取 `SkillSpecAPI.getEffectsAPI()`。
3. `SHIP` effect 通过 `SkillEffectSpecAPI.getAsShipEffect()` 返回 `ShipSkillEffect`。
4. `ShipSkillEffect.apply(stats, hullSize, id, level)` 写入被驾驶舰船的 `MutableShipStatsAPI`。
5. `ALL_SHIPS_IN_FLEET` effect 遍历舰队成员并写入每艘舰船的 `MutableShipStatsAPI`。
6. `SHIP_FIGHTERS` effect 写入被驾驶舰船关联 fighter 的 `MutableShipStatsAPI`。
7. `ALL_FIGHTERS_IN_FLEET` effect 遍历舰队 fighter 并写入对应 `MutableShipStatsAPI`。
8. 实体创建后 `SkillEffectSpecAPI.getAsAfterShipCreationEffect()` 返回 `AfterShipCreationSkillEffect`。
9. `AfterShipCreationSkillEffect.applyEffectsAfterShipCreation(ship, id)` 写入战斗 `ShipAPI`。

### skill 读取链路

1. 调用 `Global.getSettings().getSkillSpec(id)` 读取 `SkillSpecAPI`。
2. 调用 `SkillSpecAPI.getEffectsAPI()` 读取 effect spec 集合。
3. 调用 `SkillEffectSpecAPI.getType()` 读取 `SkillEffectType`。
4. 调用 `SkillEffectSpecAPI.getRequiredSkillLevel()` 读取 effect 生效等级。
5. 调用 `SkillSpecAPI.getScope()`、`getScope2()`、`getScopeStr()` 和 `getScopeStr2()` 读取 UI 与生效范围描述。
6. 调用 `SkillSpecAPI.getTags()`、`hasTag(...)`、`isCombatOfficerSkill()`、`isAdmiralSkill()` 和 `isAdminSkill()` 读取限制语义。

### UI / 描述链路

1. UI 读取 `SkillSpecAPI` 的 `name`、`description`、`author`、`spriteName`、`tier`、`order` 和 aptitude 信息。
2. `DESCRIPTION` effect 通过 `SkillEffectSpecAPI.getAsDescriptionEffect()` 返回 `DescriptionSkillEffect`。
3. UI 读取 `DescriptionSkillEffect.getString()`、`getHighlights()`、`getHighlightColors()` 和 `getTextColor()`。
4. 实现 `CustomSkillDescription.hasCustomDescription()` 的 effect 进入自定义描述链路。
5. UI 调用 `CustomSkillDescription.createCustomDescription(stats, skill, info, width)` 写入 tooltip。
6. 实现 `FleetTotalSource.getFleetTotalItem()` 的 effect 提供 fleet total。
7. `BaseSkillEffectDescription` 读取 fleet data、commander stats 和舰队成员贡献并生成 fleet total tooltip。

## 规范

- `AfterShipCreationSkillEffect` 同时继承 `ShipSkillEffect`。
- `BaseSkillEffectDescription.AUTOMATED_POINTS_THRESHOLD` 默认 `200`，启用 recovery cost 语义后为 `120`。
- `BaseSkillEffectDescription.FIGHTER_BAYS_THRESHOLD` 默认 `8`。
- `BaseSkillEffectDescription.OP_THRESHOLD` 默认 `1000`，启用 recovery cost 语义后为 `240`。
- `BaseSkillEffectDescription.PHASE_OP_THRESHOLD` 默认 `150`，启用 recovery cost 语义后为 `40`。
- `BaseSkillEffectDescription.TOOLTIP_WIDTH` 默认 `450`。
- `CustomSkillDescription.createCustomDescription(...)` 接收当前角色 stats、skill spec、tooltip 和宽度。
- `LevelBasedEffect.ScopeDescription` 原版值为 `PILOTED_SHIP`、`ALL_SHIPS`、`ALL_COMBAT_SHIPS`、`ALL_CARRIERS`、`ALL_FIGHTERS`、`SHIP_FIGHTERS`、`GOVERNED_OUTPOST`、`ALL_OUTPOSTS`、`FLEET`、`CUSTOM` 和 `NONE`。
- `MutableCharacterStatsAPI.getSkillLevel(id)` 返回整数技能等级，方法类型为 float。
- `SkillEffectSpecAPI.getAbilityUnlocks()` 返回 ability unlock id 集合。
- `SkillEffectSpecAPI.getUnlockedHullMods(level)` 返回指定 level 解锁的 hullmod id 集合。
- `SkillEffectType` 原版值为 `SHIP`、`ALL_SHIPS_IN_FLEET`、`SHIP_FIGHTERS`、`ALL_FIGHTERS_IN_FLEET`、`CHARACTER_STATS`、`FLEET`、`HULLMOD_UNLOCK`、`ABILITY_UNLOCK`、`GOVERNED_OUTPOST`、`ALL_OUTPOSTS` 和 `DESCRIPTION`。
- `SkillSpecAPI.isAdminSkill()`、`isAdmiralSkill()` 和 `isCombatOfficerSkill()` 来自 `skill_data.csv` 的角色类型列。
- `SkillSpecAPI.isElite()` 来自 `.skill` 的 `elite` 字段。
- `Skills.TAG_AI_CORE`、`TAG_AI_CORE_ONLY`、`TAG_DEPRECATED`、`TAG_ELITE_PLAYER_ONLY`、`TAG_NO_AI_CORE`、`TAG_NPC_ONLY` 和 `TAG_PLAYER_ONLY` 表达 skill 可用对象限制。
- `data/characters/skills/*.skill` 的 `effectBasedOnLevel=false` 表达 effect 使用固定 required level 语义。
- `data/characters/skills/*.skill` 的 `requiredSkillLevel` 表达该 effect group 的生效等级门槛。

## 陷阱

- `ALL_SHIPS_IN_FLEET` 与 `SHIP` 的 effect 接口同为 `ShipSkillEffect`，作用对象集合由 effect type 决定。
- `BaseSkillEffectDescription.getCommanderStats(stats)` 在 campaign 中可退回玩家 stats，传入 null 时语义依赖当前 game state。
- `BaseSkillEffectDescription` 的 fleet total 统计会跳过 mothballed 舰船。
- `DescriptionSkillEffect` 只提供文本描述，数值写入由其它 effect type 承担。
- `HULLMOD_UNLOCK` 和 `ABILITY_UNLOCK` 通过 effect spec 的 unlock 集合表达，脚本接口读取会得到对应类型语义。
- `MarketSkillEffect` 的入参是 `MarketAPI`，governed outpost 与 all outposts 的刷新入口由角色 stats 触发。
- `SkillSpecAPI.getScopeStr()` 只表达 UI 文本范围，实际写入对象仍由 effect type 与 effect 脚本决定。
- `.skill` 中的 `scope=CUSTOM` 需要同时读取 `scopeStr`，只读 `ScopeDescription` 会丢失原版显示语义。
