# rules.csv 规则系统

来源：原版 Starsector。

## 定义

`rules.csv` 规则系统是生涯交互对话中按触发名、memory 条件、脚本、文本和选项匹配规则的原版数据驱动机制。

## 参考

- `com.fs.starfarer.api.campaign.InteractionDialogAPI`：提供规则执行时的对话上下文。
- `com.fs.starfarer.api.campaign.rules.MemKeys`：定义 memoryMap 的标准 key。
- `com.fs.starfarer.api.campaign.rules.MemoryAPI`：提供条件和脚本读取的 memory。
- `com.fs.starfarer.api.campaign.rules.RuleAPI`：提供单条规则文本、选项和脚本执行入口。
- `com.fs.starfarer.api.campaign.rules.RulesAPI`：提供规则匹配和 token 替换入口。
- `com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin`：提供 Java rulecmd 基类。
- `com.fs.starfarer.api.util.Misc.Token.getStringWithTokenReplacement(...)`：读取 token 文本并执行规则 token 替换。
- `com.fs.starfarer.api.util.Misc.Token.getVarNameAndMemory(...)`：按 `$memory.key` 语法解析 memoryMap。
- `com.fs.starfarer.api.util.Misc.Token`：承载 rulecmd 参数 token。
- `data/campaign/rules.csv`：原版规则系统的规则表入口。

## 边界

- `conditions` 列归属规则匹配。
- `id` 列归属规则唯一标识。
- `notes` 列归属备注。
- `options` 列归属对话选项生成。
- `script` 列归属命中后脚本执行。
- `text` 列归属命中文本。
- `trigger` 列归属规则入口名称。
- `BaseCommandPlugin` 归属 Java 条件命令与脚本命令。
- `FireAll` 归属目标 trigger 的全部匹配规则执行。
- `FireBest` 归属目标 trigger 的最佳匹配规则执行。
- `RulesAPI.getAllMatching(...)` 归属候选规则集合查询。
- `RulesAPI.getBestMatching(...)` 归属单条最佳规则查询。
- `RulesAPI.performTokenReplacement(...)` 归属文本 token 替换。
- `RulesAPI.setRandomForNextRulePick(...)` 归属下一次规则选择随机源。
- `score:` 归属规则候选排序权重。
- `token` 参数归属 rulecmd 调用时的字符串、变量和操作符输入。

## 链路

### 规则匹配链路

1. 对话系统或脚本提供 currentRule、trigger、dialog 和 memoryMap。
2. `RulesAPI.getAllMatching(...)` 读取 trigger 对应规则。
3. 系统按 conditions 求值。
4. 条件中的 memory 表达式读取 `MemoryAPI`。
5. 条件中的 Java 命令调用 rulecmd。
6. `score:` 参与候选排序。
7. `setRandomForNextRulePick(...)` 提供的随机源参与下一次规则选择。
8. `getBestMatching(...)` 返回排序后的单条规则。

### 规则执行链路

1. 规则命中后取得 `RuleAPI`。
2. 文本通过 `RulesAPI.performTokenReplacement(...)` 替换 token。
3. 对话系统显示 `text`。
4. `RuleAPI.runScript(dialog, memoryMap)` 执行 `script`。
5. `script` 可写入 memory、调用内置命令、调用 rulecmd 或触发其它 trigger。
6. `options` 生成 option id 和显示文本。
7. 玩家选择 option 后以 `$option` 回流到 `DialogOptionSelected`。

### Rulecmd 链路

1. CSV 条件或脚本写入命令名和参数。
2. 规则系统定位 `com.fs.starfarer.api.impl.campaign.rulecmd` 下的类。
3. 系统实例化 `CommandPlugin`。
4. 调用 `execute(ruleId, dialog, params, memoryMap)`。
5. 返回值作为条件布尔结果或脚本继续状态。
6. `doesCommandAddOptions()` 和 `getOptionOrder(...)` 参与 option 命令语义。

### Token 链路

1. 规则系统把条件或脚本文本拆成 `Misc.Token` 列表。
2. token 以 `$` 开头时标记为变量。
3. `$memory.key` 形式解析出 memory key 与变量名。
4. memory key 命中 memoryMap 时读取对应 memory。
5. memory key 缺失时回落到 local memory。
6. 文本 token 可调用 `getStringWithTokenReplacement(...)` 执行规则 token 替换。

### 再入链路

1. 当前规则脚本执行 `FireBest` 或 `FireAll`。
2. 目标 trigger 使用同一 dialog 和 memoryMap。
3. 规则系统重新执行匹配。
4. 目标规则执行 text、script 和 options。
5. 再入链路可继续触发下一层规则。

## 规范

- `BaseCommandPlugin.getEntityMemory(memoryMap)` 优先返回 `MemKeys.ENTITY`，否则返回 `MemKeys.LOCAL`。
- `BaseCommandPlugin.getOptionOrder(...)` 默认返回 `0`。
- `BaseCommandPlugin.doesCommandAddOptions()` 默认返回 false。
- `MemKeys.ENTITY` 的字符串值为 `entity`。
- `MemKeys.FACTION` 的字符串值为 `faction`。
- `MemKeys.GLOBAL` 的字符串值为 `global`。
- `MemKeys.LOCAL` 的字符串值为 `local`。
- `MemKeys.MARKET` 的字符串值为 `market`。
- `MemKeys.MISSION` 的字符串值为 `mission`。
- `MemKeys.PLAYER` 的字符串值为 `player`。
- `MemKeys.PERSON_FACTION` 的字符串值为 `personFaction`。
- `MemKeys.SOURCE_MARKET` 的字符串值为 `sourceMarket`。
- `RuleAPI.getScriptCopy()` 返回规则脚本表达式副本。
- `RuleAPI.pickText()` 从规则文本集合中选择一条文本。
- `RuleAPI.runScript(...)` 的 dialog 参数可为 `null`。
- `RulesAPI.getTokenReplacements(...)` 使用 ruleId、target 和 memoryMap 生成替换表。
- `RulesAPI.setRandomForNextRulePick(random)` 只影响下一次规则选择。
- 条件命令应返回可用于规则匹配的布尔语义。
- 选项 id 应与后续 `$option` 条件保持稳定。

## 陷阱

- `dialog` 在非对话触发中可为 `null`。
- `execute()` 返回值在条件列和脚本列中的消费语义不同。
- `FireBest` 与 `FireAll` 会造成规则系统再入。
- `Misc.Token.getStringWithTokenReplacement(...)` 读取 dialog interaction target，dialog 为空时调用方需要提供有效上下文。
- `score:` 是候选规则排序权重，状态持久化应写入 memory。
- `setRandomForNextRulePick(...)` 只绑定下一次 pick，跨多次匹配复用随机源会改变文本选择语义。
- 多行 CSV 单元格中的逗号和引号会影响表格解析。
- 条件列调用带副作用 rulecmd 会在匹配阶段写状态。
- 选项文本生成后通常依赖 `$option` 回流到 `DialogOptionSelected`。
