# 生涯能力

来源：原版 Starsector。

## 定义

生涯能力是舰队能力数据、spec、AI、运行时挂载、人物已解锁集合、ability slot、tooltip 渲染、tag 互斥和 rulecmd 接口的原版接口集合。

## 参考

- `com.fs.starfarer.api.campaign.ai.AbilityAIPlugin`：定义 ability AI 插件接口。
- `com.fs.starfarer.api.campaign.CharacterDataAPI`：维护玩家人物已解锁 ability 集合。
- `com.fs.starfarer.api.campaign.PersistentUIDataAPI`：承载 ability slot 槽位集合与当前排选择。
- `com.fs.starfarer.api.campaign.SectorAPI`：承载玩家舰队、玩家人物、UI 数据与玩家 ability 激活/失效事件分发。
- `com.fs.starfarer.api.campaign.SectorEntityToken`：承载实体 ability 运行时挂载与查询。
- `com.fs.starfarer.api.characters.AbilityPlugin`：定义 ability 生命周期接口。
- `com.fs.starfarer.api.impl.campaign.abilities.BaseAbilityPlugin`：提供 spec 解析、互斥、tooltip 与公共默认实现。
- `com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility`：定义 DURATION 类型 ability 生命周期。
- `com.fs.starfarer.api.impl.campaign.abilities.BaseToggleAbility`：定义 TOGGLE 类型 ability 生命周期。
- `com.fs.starfarer.api.impl.campaign.rulecmd`：承载 ActivateAbility、AddAbility、DeactivateAbility 三个 ability 侧 rulecmd。
- `com.fs.starfarer.api.loading.AbilitySpecAPI`：承载 ability 静态配置。
- `data/campaign/abilities.csv`：注册 ability 表。

## 边界

- `AbilityAIPlugin` 归属 ability AI 插件接口。
- `AbilityPlugin` 归属 ability 生命周期接口。
- `AbilitySpecAPI` 归属 ability 静态配置。
- `AbilitySpecAPI.getNewAIPluginInstance()` 归属 AI 实例化入口。
- `ActivateAbility` / `DeactivateAbility` 归属 ability 寻址式激活与失效入口。
- `AddAbility` 归属玩家 ability 持久化单点入口。
- `BaseAbilityPlugin` 归属 spec 解析、互斥、tooltip 默认实现。
- `BaseAbilityPlugin.forceDisable()` 归属 disableFrames=2 副作用。
- `BaseAbilityPlugin.getFleet()` 归属舰队入口判断。
- `BaseAbilityPlugin.interruptIncompatible()` 归属互斥链主动失效入口。
- `BaseAbilityPlugin.isUsable()` 归属激活守卫。
- `BaseDurationAbility` 归属 DURATION 激活、失效、冷却、渐变推进。
- `BaseDurationAbility.activate()` 归属 DURATION 激活主入口。
- `BaseDurationAbility.cleanup()` 归属强制清理入口。
- `BaseDurationAbility.deactivate()` 归属 DURATION 失效主入口。
- `BaseToggleAbility` 归属 TOGGLE 开关、双冷却、渐变推进。
- `BaseToggleAbility.activate()` 归属 TOGGLE 开启主入口。
- `BaseToggleAbility.deactivate()` 归属 TOGGLE 关闭主入口。
- `BaseToggleAbility.getCooldownFraction()` 归属双冷却区分入口。
- `CharacterDataAPI` 归属玩家人物已解锁 ability 集合。
- `CharacterDataAPI.addAbility()` 归属玩家已解锁集合写入入口。
- `PersistentUIDataAPI.AbilitySlotAPI` 归属单槽 ability id 与 hyper ability id 存储。
- `PersistentUIDataAPI.AbilitySlotsAPI` 归属 ability slot 集合与排选择状态。
- `SectorAPI.reportPlayerActivatedAbility()` / `reportPlayerDeactivatedAbility()` 归属玩家事件分发入口。
- `SectorEntityToken` 归属舰队或自定义实体的 ability 运行时挂载与查询入口。
- `SectorEntityToken.addAbility()` 归属运行时挂载入口。
- `data/campaign/abilities.csv` 归属 ability 注册表入口。

## 链路

### CSV / spec 加载链路

1. 游戏读取 `data/campaign/abilities.csv`。
2. `id`、`name` 写入 `AbilitySpecAPI` 的 id 和 name。
3. `type` 写入 `AbilitySpecAPI` 对应 DURATION 或 TOGGLE 标记。
4. `tags` 写入 `AbilitySpecAPI` 的 tag 集合。
5. `activationDays`、`activationCooldown`、`durationDays`、`deactivationDays`、`deactivationCooldown` 写入 `AbilitySpecAPI` 时间字段。
6. `unlockedAtStart` 写入 `AbilitySpecAPI.isUnlockedAtStart()`。
7. `defaultForAIFleet` 写入 `AbilitySpecAPI.isAIDefault()`。
8. `musicSuppression` 写入 `AbilitySpecAPI` 音乐抑制字段。
9. `uiOn`、`uiOff`、`uiLoop`、`worldOn`、`worldOff`、`worldLoop` 写入 `AbilitySpecAPI` 音效字段。
10. `icon` 写入 `AbilitySpecAPI` 的 icon name。
11. `plugin` 实例化为 `BaseDurationAbility` 或 `BaseToggleAbility` 子类。
12. `ai` 写入 `AbilitySpecAPI.getAIPluginClass()`。
13. `desc` 写入 `AbilitySpecAPI` 描述。
14. `sortOrder` 写入 `AbilitySpecAPI.getSortOrder()`。
15. `SettingsAPI.getAbilitySpec(id)` 提供按 id 读取入口。
16. `SettingsAPI.getSortedAbilityIds()` 提供按 sortOrder 排序的 id 集合。

### 能力挂载链路

1. 调用 `SectorEntityToken.addAbility(id)` 写入实体能力集合。
2. `BaseAbilityPlugin.init(id, entity)` 写入 id 和 entity 字段。
3. `readResolve()` 通过 `SettingsAPI.getAbilitySpec(id)` 解析 spec。
4. `SectorEntityToken.getAbilities()` 返回 `Map<String, AbilityPlugin>`。
5. 调用 `SectorEntityToken.getAbility(id)` 读取单个 `AbilityPlugin`。
6. 调用 `SectorEntityToken.hasAbility(id)` 查询 ability 是否已挂载。
7. 调用 `SectorEntityToken.removeAbility(id)` 移除 ability 并 `cleanup()`。

### DURATION 能力生命周期链路

1. 调用 `AbilityPlugin.activate()` 触发玩家舰队或 AI 激活。
2. `BaseAbilityPlugin.isUsable()` 检查 `!isOnCooldown()` 和 `disableFrames <= 0`。
3. `BaseDurationAbility.activate()` 写 `activeDaysLeft` 为 `activationDays + durationDays + deactivationDays`。
4. `BaseDurationAbility.activateImpl()` 写入子类的激活副作用。
5. `BaseDurationAbility.applyEffect(amount, level)` 按当前 level 推进持续效果。
6. 引擎每帧调用 `BaseAbilityPlugin.advance(amount)`。
7. `BaseDurationAbility.advance()` 根据 `activeDaysLeft` 计算 level。
8. `BaseDurationAbility.deactivate()` 写 `cooldownLeft` 为 `deactivationCooldown`。
9. `BaseDurationAbility.deactivateImpl()` 清理持续效果。
10. `BaseDurationAbility.cleanup()` 调 `cleanupImpl()` 收尾。

### TOGGLE 能力生命周期链路

1. 调用 `AbilityPlugin.activate()` 进入开状态。
2. `BaseToggleAbility.activate()` 写 `turnedOn=true` 并写 `cooldownLeft` 为 `activationCooldown`。
3. `BaseToggleAbility.activateImpl()` 写入子类的开启副作用。
4. `BaseToggleAbility.applyEffect(amount, level)` 按 `level` 推进淡入效果。
5. 引擎每帧调用 `BaseToggleAbility.advance(amount)`。
6. `BaseToggleAbility.advance()` 根据 `activationDays` 递增 level，按 `deactivationDays` 递减 level。
7. `BaseToggleAbility.deactivate()` 写 `turnedOn=false` 并写 `cooldownLeft` 为 `deactivationCooldown`。
8. `BaseToggleAbility.deactivateImpl()` 写入关闭副作用。
9. `BaseToggleAbility.cleanup()` 调 `cleanupImpl()` 收尾。

### tag 互斥链路

1. `BaseAbilityPlugin.isCompatible(other)` 读取 spec 的 tag 集合。
2. `AbilitySpecAPI` 的 positive tag 与 other spec 命中返回不兼容。
3. `AbilitySpecAPI` 的 negative tag 命中 `hasOppositeTag` 返回不兼容。
4. `BaseAbilityPlugin.interruptIncompatible()` 遍历舰队 abilities 并调 `deactivate()`。
5. `BaseAbilityPlugin.disableIncompatible()` 调 `forceDisable()` 设置 `disableFrames=2`。
6. `BaseDurationAbility.activate()` 调 `interruptIncompatible()` 和 `disableIncompatible()`。
7. `BaseToggleAbility.activate()` 调 `interruptIncompatible()`。

### 能力 AI 链路

1. 引擎为舰队 ability 触发 AI 选择点。
2. 引擎调用 `CampaignPlugin.pickAbilityAI(ability, ai)`。
3. `AbilitySpecAPI.getAIPluginClass()` 返回 AI plugin 类名。
4. `AbilitySpecAPI.getNewAIPluginInstance(ability)` 实例化 AI plugin。
5. `AbilityAIPlugin.init(ability)` 写入 ability 引用。
6. 引擎每帧调用 `AbilityAIPlugin.advance(days)`。
7. `BaseAbilityAI` 提供默认实现。

### 玩家舰队能力使用链路

1. UI 调用 `PlayerFleet` 上 ability 的 `pressButton()`。
2. `BaseAbilityPlugin.pressButton()` 调 `activate()` 或 `deactivate()`。
3. `BaseAbilityPlugin.activate()` 检查玩家舰队分支。
4. `BaseAbilityPlugin.activate()` 调 `Global.getSector().reportPlayerActivatedAbility(this, null)`。
5. `BaseCampaignEventListener.reportPlayerActivatedAbility` 接收事件。
6. `BaseAbilityPlugin.deactivate()` 调 `Global.getSector().reportPlayerDeactivatedAbility(this, null)`。
7. `BaseCampaignEventListener.reportPlayerDeactivatedAbility` 接收事件。

### 能力持久化与玩家解锁链路

1. 玩家升级时 `MutableCharacterStatsAPI` 计算 `getGrantedAbilityIds()` 集合。
2. skill ABILITY_UNLOCK 写 `MutableCharacterStatsAPI` 的 granted ability id。
3. `CharacterDataAPI.addAbility(id)` 写入玩家人物已解锁集合。
4. `StandardRespawnDialogPluginImpl` 在重生时把 `CharacterDataAPI.getAbilities()` 同步到玩家舰队。
5. `SectorEntityToken.addAbility(id)` 重新挂载 ability。
6. `BaseAbilityPlugin.init(id, entity)` 重新解析 spec。
7. ABILITY_UNLOCK 路径与 `addAbility` 路径是两条独立入口。

### 能力 slot 链路

1. `Global.getSector().getUIData()` 返回 `PersistentUIDataAPI`。
2. `PersistentUIDataAPI.getAbilitySlotsAPI()` 返回 `AbilitySlotsAPI`。
3. `AbilitySlotsAPI.getCurrSlotsCopy()` 返回当前 10 槽。
4. `AbilitySlotsAPI.setCurrBarIndex(i)` 切换 5 排。
5. `AbilitySlotAPI.getAbilityId()` / `setAbilityId(id)` 读取和写入槽位。
6. `AbilitySlotAPI.getInHyperAbilityId()` / `setInHyperAbilityId(id)` 维护超空间槽位。

### 能力侧 rulecmd 链路

1. 规则系统调用 `AddAbility`。
2. `AddAbility` 检查 `SectorAPI.getPlayerFleet().hasAbility(abilityId)`。
3. `AddAbility` 扫描 5 排 slot 寻找已分配槽位。
4. `SectorAPI.getCharacterData().addAbility(abilityId)` 写入已解锁集合。
5. `AddAbility` 按 `slotIndex` 参数或自动分配写入 `AbilitySlotAPI.setAbilityId(abilityId)`。
6. `AddAbility` 写 `CharacterDataAPI.getMemoryWithoutUpdate().set("$ability:" + abilityId, true, 0)`。
7. 死亡重生时 `CharacterDataAPI` 集合与 `$ability:id` memory 决定重挂能力。
8. 规则系统调用 `RemoveAbility` 按 id 移除能力。
9. 规则系统调用 `ActivateAbility` 按 `(entityId, abilityId)` 调实体的 `activate()`。
10. 规则系统调用 `DeactivateAbility` 按 `(entityId, abilityId)` 调实体的 `deactivate()`。

## 规范

- `AbilitySpecAPI.getNewAIPluginInstance(ability)` 在 AI picker 路径中实例化；`ai` 字段为空时返回 null 且引擎跳过该 ability 的 AI 推进。
- `ActivateAbility` 返回 `ability.isActiveOrInProgress()`；`DeactivateAbility` 恒返回 `true`。
- `ActivateAbility` / `DeactivateAbility` 在 entity 缺失或 ability 缺失时返回 false，不抛错。
- `AddAbility` 不会覆盖已被占用的 slot；不会分配到已分配同一 ability 的 slot。
- `AddAbility` 同时写 `CharacterDataAPI.addAbility`、`AbilitySlotsAPI.setAbilityId` 和 `$ability:id` memory；不可拆分为多次独立写入。
- `AddAbility` 在 `hadAbilityAlready` 时不分配 slot、不写 `$ability:id` memory，只保证 `CharacterDataAPI` 集合中保留。
- `AddAbility` 的 `slotIndex` 取值：`>= 0` 写到 bar 0 的对应 slot；`< 0` 表达「不分配 slot」；缺省则按 5 排 slot 顺序扫描第一个空位。
- `BaseAbilityPlugin.PLAY_UI_SOUNDS_IN_WORLD_SOURCES` 为 true 时玩家 UI 音效走 world sound sources；为 false 时走专用 UI sources。
- `BaseAbilityPlugin.getFleet()` 在 `entity` 不是 `CampaignFleetAPI` 时返回 null；`interruptIncompatible()` / `disableIncompatible()` 在该情况下是 no-op。
- `BaseAbilityPlugin.getInterruptedList()` 在「this 是 `BaseToggleAbility`、curr 是 `BaseDurationAbility`」时跳过，避免 TOGGLE 把 DURATION 列进互斥清单。
- `BaseAbilityPlugin.interruptIncompatible()` 只对 `isActive()` 为 true 的 ability 调 `deactivate()`；progress 中的 DURATION 不会被主动 deactivate。
- `BaseDurationAbility.activate()` 内部 `applyEffect(0f, level)` 在 `activateImpl()` 之后；`activateImpl` 不应依赖当前 effect 量。
- `BaseDurationAbility.activateImpl` / `deactivateImpl` / `cleanupImpl` 由基类负责调用，子类不应自行调用。
- `BaseDurationAbility.applyEffect` / `BaseToggleAbility.applyEffect` 的 `amount` 是日时间增量；`level` 是 0..1 渐变系数，不是布尔开关。
- `BaseDurationAbility.isUsable()` 与 `activate()` 内部共同依赖 `!turnedOn` 守卫；子类覆写任一方法都不应去掉该守卫，否则幂等失效。
- `BaseToggleAbility.activate()` 只调 `interruptIncompatible()`，不调 `disableIncompatible()`；TOGGLE 不阻止 2 帧内再次激活。
- `BaseToggleAbility.getCooldownFraction()` 通过 `isActivateCooldown` 区分激活冷却 / 失效冷却；子类覆写时不应破坏该双冷却机制。
- `BaseToggleAbility.isUsable()` 的 `getProgressFraction() > 0 && getProgressFraction() < 1 && getDeactivationDays() > 0` 守卫与 `applyEffect` 的 `level` 渐变是同一规则；子类若覆写 `isUsable` 应保留 progress 期禁用 deactivation 的守卫。
- `CharacterDataAPI.addAbility` 与 skill `ABILITY_UNLOCK` 是两条独立入口；`AbilitySpecAPI` 不关心 ability 来自哪条路径。
- `data/campaign/abilities.csv` 的 `type` 取值 `TOGGLE` 或 `DURATION`；与 `plugin` 字段的基类必须一致。
- `data/campaign/abilities.csv` 的 `unlockedAtStart=TRUE` 走 `CharacterDataAPI.addAbility` 路径；`defaultForAIFleet=TRUE` 走 `FleetFactoryV3` 的 `startingAbilities` 路径；两条路径互相不可见。
- 重生时 ability 重挂同时依赖 `CharacterDataAPI.getAbilities()` 与 `$ability:id` memory；任一缺失会让 ability 不挂载。

## 陷阱

- AI 舰队 ability 与玩家舰队共享 spec 与 plugin，但 AI 不触发 `SectorAPI.reportPlayerActivatedAbility`，UI 监听依赖玩家事件的代码不会在 AI 行为时收到通知。
- `AddAbility` 写 `$ability:id` memory 的 `expireDays` 是 0（永久）；只删 `CharacterDataAPI.addAbility` 不删 memory 仍会重挂。
- `BaseAbilityPlugin.forceDisable()` 仅设置 `disableFrames=2`，不会取消进行中的 DURATION 能力。
- `BaseDurationAbility.cleanup()` 不论 `turnedOn` 都会执行 `applyEffect(0f, 0f)` 与 `cleanupImpl()`；若 `cleanupImpl` 假设仅在 `turnedOn` 状态下被调用，会产生空清理或多次清理。
- `data/campaign/abilities.csv` 的 `defaultForAIFleet=TRUE` 走 `FleetFactoryV3.startingAbilities`，不进 `CharacterDataAPI`；AI 舰队 ability 在角色重生后不受 `AddAbility` 链路影响。
- `data/campaign/abilities.csv` 的 `plugin` 必须继承 `BaseDurationAbility` 或 `BaseToggleAbility`，否则 `pressButton()` 会因基类签名不匹配抛错。
- `data/campaign/abilities.csv` 的 `tags` `+`/`-` 修饰符必须紧跟 tag 名（`+tag` 而非 `tag+`）；CSV 解析只在 tag 名首字符位置识别修饰符。
- `SectorEntityToken.addAbility` 不触发 `CharacterDataAPI.addAbility`；`removeAbility` 同样不触发。
