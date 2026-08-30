# 生涯内存

来源：原版 Starsector。

## 定义

生涯内存 API 是规则系统、交互对象、实体 memory 和 persistent data 的状态读写接口。

## 参考

- `com.fs.starfarer.api.campaign.InteractionDialogAPI.getPlugin()`：提供当前对话插件入口，插件可暴露当前交互使用的 memoryMap。
- `com.fs.starfarer.api.campaign.SectorAPI.getMemoryWithoutUpdate()`：提供 Sector 全局 memory 的直接读取入口。
- `com.fs.starfarer.api.campaign.SectorAPI.getPersistentData()`：提供随存档保存和恢复的全局对象 Map。
- `com.fs.starfarer.api.campaign.rules.MemKeys`：定义 memoryMap 中原版约定的 memory 槽位名称。
- `com.fs.starfarer.api.campaign.rules.MemoryAPI`：提供 key、value、过期时间和 required 依赖关系的读写接口。
- `com.fs.starfarer.api.campaign.rules.RulesAPI`：以 currentRule、trigger、dialog 和 memoryMap 执行规则匹配、文本替换和脚本运行。
- `com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin.getEntityMemory(...)`：按 `MemKeys.ENTITY` 优先、`MemKeys.LOCAL` 兜底取得交互实体 memory。

## 边界

- `InteractionDialogAPI.getInteractionTarget()` 在本模块只作为当前交互实体 memory 的来源路径。
- `InteractionDialogAPI.getPlugin()` 在本模块只作为 dialog plugin 暴露 memoryMap 的读取路径。
- `MemKeys.ENTITY` 归属当前交互实体 memory。
- `MemKeys.FACTION` 归属当前规则上下文势力 memory。
- `MemKeys.GLOBAL` 归属 Sector 全局 memory。
- `MemKeys.LOCAL` 归属当前交互局部 memory。
- `MemKeys.MARKET` 归属当前市场 memory。
- `MemKeys.MISSION` 归属当前任务上下文 memory。
- `MemKeys.PERSON_FACTION` 归属当前人物所属势力 memory。
- `MemKeys.PLAYER` 归属玩家 memory。
- `MemKeys.SOURCE_MARKET` 归属来源市场 memory。
- `MemoryAPI` 归属生涯规则、实体、人物、舰队、市场和势力的键值状态。
- `MemoryAPI.required` 归属 memory key 之间的依赖清理关系。
- `RulesAPI` 归属以 memoryMap 为输入的规则匹配、token 替换和 rulecmd 执行上下文。
- `SectorAPI.getPersistentData()` 归属 `Map<String, Object>` 形式的跨存档持久对象状态。

## 链路

### Memory 读写链路

1. 调用方取得 `MemoryAPI`。
2. 调用 `set(key, value)` 写入永久 key。
3. 调用 `set(key, value, expire)` 写入带过期天数的 key。
4. 生涯推进调用 `advance(amount)`。
5. 到期 key 从 memory 中移除。
6. 调用 `contains(key)` 判断 key 是否存在。
7. 调用 `get...()`、`is(...)` 或 `between(...)` 读取值。
8. 调用 `unset(key)` 主动移除 key。

### Memory 依赖清理链路

1. 调用方写入主 key。
2. 调用 `addRequired(key, requiredKey)` 添加 required key。
3. 同一主 key 可多次添加 required key。
4. memory 推进或清理时检查 required key 集合。
5. required key 全部消失后主 key 被移除。
6. 调用 `removeRequired(key, requiredKey)` 移除单个依赖。
7. 调用 `removeAllRequired(key)` 移除主 key 的全部依赖。

### memoryMap 解析链路

1. 对话系统或脚本提供 currentRule、trigger、dialog 和 memoryMap。
2. `RulesAPI.getBestMatching(...)` 或 `getAllMatching(...)` 读取同一个 memoryMap。
3. 规则条件通过 `$memory.key` 语法解析 memory 槽位。
4. `RulesAPI.performTokenReplacement(...)` 使用同一个 memoryMap 替换文本 token。
5. `RuleAPI.runScript(dialog, memoryMap)` 将同一个 memoryMap 传入 rulecmd。
6. `BaseCommandPlugin.getEntityMemory(memoryMap)` 优先读取 `MemKeys.ENTITY`。
7. `BaseCommandPlugin.getEntityMemory(memoryMap)` 在 `MemKeys.ENTITY` 缺失时读取 `MemKeys.LOCAL`。

### PersistentData 存档链路

1. 调用方通过 `Global.getSector().getPersistentData()` 取得 Map。
2. 调用方以稳定字符串 key 写入对象。
3. 存档系统序列化 Map 内容。
4. 载入存档后 Map 内容恢复。
5. 生涯载入入口读取 Map 中对象。
6. 生涯载入入口恢复运行时脚本、监听器或注册状态。

## 规范

- `BaseCommandPlugin.getEntityMemory(memoryMap)` 在 `MemKeys.ENTITY` 存在时返回 entity memory。
- `BaseCommandPlugin.getEntityMemory(memoryMap)` 在 `MemKeys.ENTITY` 缺失时返回 local memory。
- `MemoryAPI.addRequired(key, requiredKey)` 可为同一个 key 记录多个 required key。
- `MemoryAPI.between(key, min, max)` 包含 `min` 和 `max` 两个端点。
- `MemoryAPI.contains(key)` 是区分 key 缺失和值语义的入口。
- `MemoryAPI.getBoolean(key)` 对缺失 key 与显式 false 都落入 false 读取语义。
- `MemoryAPI.getExpire(key)` 返回 key 的剩余过期时间。
- `MemoryAPI.removeAllRequired(key)` 清除主 key 的全部 required 依赖。
- `MemoryAPI.removeRequired(key, requiredKey)` 清除主 key 的单个 required 依赖。
- `MemoryAPI.set(key, value)` 写入永不过期 key。
- `MemoryAPI.set(key, value, expire)` 的 expire 单位是 campaign days。
- `RulesAPI.getTokenReplacements(ruleId, target, memoryMap)` 使用传入 memoryMap 生成替换表。
- `RulesAPI.performTokenReplacement(ruleId, text, entity, memoryMap)` 使用传入 memoryMap 替换文本。
- `SectorAPI.getMemoryWithoutUpdate()` 返回 Sector 全局 memory。
- `SectorAPI.getPersistentData()` 返回可随存档序列化的 `Map<String, Object>`。

## 陷阱

- `BaseCommandPlugin.getEntityMemory(memoryMap)` 的 entity/local 选择会改变同一个 key 的状态归属。
- `MemoryAPI.getBoolean(key)` 无法区分 key 缺失与显式 false。
- `MemoryAPI.set(key, value, expire)` 的 expire 是 campaign days。
- `PersistentData` 保存运行时实体引用会跨存档恢复边界污染状态。
- `RulesAPI` 的规则匹配、文本替换和脚本执行共享同一个 memoryMap，上下文错传会造成读写槽位错位。
- `memoryMap.get(MemKeys.LOCAL)` 与 `memoryMap.get(MemKeys.GLOBAL)` 指向不同 memory，复用 key 时状态不会合并。
