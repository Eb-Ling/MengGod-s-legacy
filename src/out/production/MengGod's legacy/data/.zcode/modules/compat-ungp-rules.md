# UNGP 兼容

来源：可选兼容库 UNGP。

## 定义

UNGP 兼容是 UNGP 通过规则注册表加载规则效果类，并按战斗标签或战役标签回调规则效果的接口协议。

## 参考

- `data/campaign/UNGP_rules.csv`：规则注册表。
- `data/campaign/UNGP_rules_ENG.csv`：英文规则文本表，使用同一列结构和同一规则 ID。
- `ungp.api.rules.UNGP_BaseRuleEffect`：规则效果基类，提供难度值、描述参数、campaign 数据槽、engine 数据槽和清理入口。
- `ungp.api.rules.UNGP_BaseRuleEffect.getDescriptionParams(...)`：按难度生成描述参数。
- `ungp.api.rules.UNGP_BaseRuleEffect.getValueByDifficulty(...)`：按难度生成数值。
- `ungp.api.rules.UNGP_BaseRuleEffect.updateDifficultyCache(...)`：刷新规则难度缓存。
- `ungp.api.rules.tags.UNGP_CampaignTag.advanceInCampaign(...)`：战役规则推进回调。
- `ungp.api.rules.tags.UNGP_CombatTag.advanceInCombat(...)`：战斗规则全局推进回调。
- `ungp.api.rules.tags.UNGP_CombatTag.applyEnemyShipInCombat(...)`：敌方舰船战斗回调。
- `ungp.api.rules.tags.UNGP_CombatTag.applyPlayerShipInCombat(...)`：玩家舰船战斗回调。
- `ungp.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty`：难度枚举，定义 `GAMMA`、`BETA`、`ALPHA`、`OMEGA` 和线性数值方法。
- `ungp.scripts.utils.UNGP_BaseBuff`：战役舰队成员 buff 基类。

## 边界

- `cost` 归属规则点数成本。
- `desc` 归属规则详细描述，`%s` 参数由规则效果提供。
- `effectPlugin` 归属规则效果类全限定名。
- `extra1` 和 `extra2` 归属规则自定义文本或额外配置。
- `id` 归属规则唯一 ID，并同步本地化表。
- `isBonus` 归属正面规则标记。
- `isGolden` 归属金色规则标记。
- `name` 归属规则显示名。
- `short` 归属规则短说明。
- `source` 归属规则来源显示文本。
- `spriteAuthor` 归属图标作者显示文本。
- `spritePath` 归属规则图标路径。
- `tags` 归属 UNGP 规则过滤和分类。
- `UNGP_CampaignTag` 归属战役层逐帧或周期效果。
- `UNGP_CombatTag` 归属战斗层全局、玩家舰船和敌方舰船效果。
- `UNGP_BaseBuff` 归属舰队成员 buff 生命周期。
- 难度缓存归属 `updateDifficultyCache(...)`。
- 数值参数归属 `getValueByDifficulty(...)`。
- 描述参数归属 `getDescriptionParams(...)`。

## 链路

### 规则注册链路

1. UNGP 读取 `data/campaign/UNGP_rules.csv`。
2. 读取器解析非空 `id` 行。
3. 读取器读取 `name`、`short`、`desc`、`cost` 和标记列。
4. 读取器读取 `spritePath` 和作者列。
5. 读取器读取 `effectPlugin`。
6. 读取器加载规则效果类。
7. 读取器把规则 ID、显示文本、图标和效果类绑定成规则。
8. UNGP 读取 `data/campaign/UNGP_rules_ENG.csv`。
9. 英文文本表按同一 `id` 覆盖或补充显示文本。

### 难度数值链路

1. UNGP 选定规则难度。
2. 规则效果收到 `updateDifficultyCache(Difficulty)`。
3. 规则效果通过 `getValueByDifficulty(index, difficulty)` 计算数值。
4. `Difficulty.getLinearValue(...)` 按 `GAMMA`、`BETA`、`ALPHA`、`OMEGA` 生成线性值。
5. 规则效果缓存战斗或战役中要重复使用的数值。
6. UI 描述调用 `getDescriptionParams(index, difficulty)`。
7. 描述参数替换 `desc` 中的 `%s`。

### 战斗规则链路

1. UNGP 进入战斗并取得启用的 `UNGP_CombatTag` 规则。
2. UNGP 调用 `advanceInCombat(engine, amount)`。
3. UNGP 对敌方舰船调用 `applyEnemyShipInCombat(amount, enemy)`。
4. UNGP 对玩家侧舰船调用 `applyPlayerShipInCombat(amount, engine, ship)`。
5. 战斗规则使用 `buffID` 标识自己的 stat 修改或监听器修改。
6. 战斗结束后规则效果进入清理语义。

### 战役规则链路

1. UNGP 进入战役规则推进。
2. UNGP 对 `UNGP_CampaignTag` 调用 `advanceInCampaign(amount, tempCampaignParams)`。
3. 规则效果按 campaign 时间或 interval 计算周期。
4. 规则效果读取或写入 campaign 数据槽。
5. 规则效果向舰队成员 buff 管理器添加或刷新 `UNGP_BaseBuff`。
6. buff 的 `apply(member)` 写入舰队成员 stats。
7. buff 过期后由 buff 管理器移除。

### 数据保存链路

1. 规则效果调用 `saveDataInCampaign(key, value)` 或实例数据槽方法。
2. UNGP 基类把数据写入 campaign 持久空间。
3. 规则效果通过 `getDataInCampaign(...)` 读取历史状态。
4. 规则效果通过 `clearDataInCampaign(...)` 清理实例数据。
5. 战斗短期数据通过 engine 数据槽读写。

## 规范

- `cost` 应为 UNGP 可解析整数。
- `desc` 中 `%s` 的数量应与 `getDescriptionParams(...)` 可返回参数数量一致。
- `effectPlugin` 指向的类应继承 `UNGP_BaseRuleEffect`。
- `effectPlugin` 指向的类应按规则作用域实现 `UNGP_CombatTag` 或 `UNGP_CampaignTag`。
- `getDescriptionParams(...)` 应使用与 `getValueByDifficulty(...)` 相同的 index 语义。
- `getValueByDifficulty(...)` 应覆盖规则描述中需要的每个数值 index。
- `id` 应在中文表和英文表中保持一致。
- `isBonus` 和 `isGolden` 应为 UNGP 可解析布尔值。
- `spritePath` 应指向可加载图标资源。
- `UNGP_BaseBuff.getId()` 应返回稳定 buff ID。
- `UNGP_BaseBuff.refresh()` 应恢复 buff 持续时间。
- `updateDifficultyCache(...)` 应缓存战斗或战役中频繁使用的难度值。
- 战斗 stat 修改应使用 `buffID` 作为修改 ID。
- 战役周期状态应放入 campaign 数据槽或 buff 生命周期。
- 英文文本表应保留与主规则表相同的列顺序。

## 陷阱

- `desc` 的 `%s` 多于描述参数时，规则说明会缺参数。
- `effectPlugin` 类缺少对应 tag 接口时，UNGP 能注册规则但不会进入目标生命周期回调。
- `getDescriptionParams(...)` 与 `getValueByDifficulty(...)` 使用不同 index 时，UI 显示数值和实际效果会分离。
- `isBonus` 写错会把正面规则归入负面规则池。
- `spritePath` 指向缺失图标时，规则选择界面会显示缺图。
- `UNGP_BaseBuff` 的持续时间短于刷新周期时，舰队成员 stat 会周期性断开。
- 战斗监听器未用 `buffID` 返回修改 ID 时，同一规则的伤害修正难以移除或追踪。
- 战役数据槽 key 复用其它规则 key 时，规则状态会相互覆盖。
