# Console Commands 兼容

来源：可选兼容库 Console Commands。

## 定义

Console Commands 兼容是 Console Commands 模组通过合并命令注册表加载外部 `BaseCommand` 命令类的接口协议。

## 参考

- `data/console/commands.csv`：外部命令注册表。
- `data/console/command_listeners.csv`：命令监听器注册表。
- `org.lazywizard.console.BaseCommand`：命令接口，定义 `runCommand(String args, CommandContext context)`。
- `org.lazywizard.console.BaseCommand.CommandContext`：调用场景枚举，定义 campaign、market、combat、simulation 和 main menu 场景。
- `org.lazywizard.console.BaseCommand.CommandResult`：命令结果枚举，定义 `SUCCESS`、`BAD_SYNTAX`、`WRONG_CONTEXT`、`ERROR`。
- `org.lazywizard.console.BaseCommandWithSuggestion`：命令参数建议接口，向新版控制台 UI 提供参数建议值。
- `org.lazywizard.console.CommandListener`：命令前置拦截和后置观察接口。
- `org.lazywizard.console.CommandListenerWithSuggestion`：监听器参数建议接口。
- `org.lazywizard.console.CommandStore.reloadCommands()`：合并读取 `commands.csv` 并校验命令类实现 `BaseCommand`。
- `org.lazywizard.console.Console.parseInput(...)`：把原始输入拆成命令名和参数并进入执行链路。

## 边界

- `args` 归属命令输入尾部字符串，未输入参数时值为空字符串。
- `class` 列归属命令实现类的全限定名，加载后必须能赋值给 `BaseCommand`。
- `command` 列归属玩家输入的命令名，存储和检索时按小写键处理。
- `CommandContext.CAMPAIGN_MAP` 归属生涯地图调用。
- `CommandContext.CAMPAIGN_MARKET` 归属市场交互调用，`getMarket()` 只在该上下文返回市场。
- `CommandContext.COMBAT_CAMPAIGN`、`COMBAT_MISSION`、`COMBAT_SIMULATION` 归属 3 类战斗调用。
- `CommandContext.MAIN_MENU` 归属主菜单调用。
- `CommandListener` 归属命令执行前拦截、接管执行和执行后观察。
- `help` 列归属 `help <command>` 的详细说明，支持把 `\n` 转成换行。
- `priority` 列归属监听器拦截优先级，数值最高的监听器取得接管权。
- `syntax` 列归属 `BAD_SYNTAX` 后的自动提示文本。
- `tags` 列归属帮助过滤、上下文适用性和 cheat 过滤。
- `BaseCommand` 命令类归属 Console Commands 的脚本类加载器实例化流程。
- `BaseCommandWithSuggestion` 归属新版控制台参数补全。
- `Console.showMessage(...)` 归属命令面向玩家的控制台输出。

## 链路

### 命令注册链路

1. Console Commands 进入命令重载入口。
2. `CommandStore.reloadCommands()` 调用 Starsector 合并表读取 `data/console/commands.csv`。
3. 读取器以 `command` 列过滤空行。
4. 读取器读取 `class` 列并通过脚本类加载器加载类。
5. 读取器校验命令类实现 `BaseCommand`。
6. 读取器读取 `syntax`、`help` 和 `tags`。
7. `help` 中的 `\n` 转成实际换行。
8. `tags` 以逗号分隔、转小写并去掉首尾空白。
9. 命令以小写命令名写入命令表。
10. 加载失败的单条命令通过控制台异常输出报告，其余命令继续加载。

### 命令执行链路

1. 控制台收到原始输入和 `CommandContext`。
2. `Console.parseInput(...)` 把输入交给内部命令执行入口。
3. 执行入口按第 1 个空格拆出命令名和 `args`。
4. 命令名转小写。
5. 别名表命中时先展开别名并拼接原参数。
6. `CommandStore.retrieveCommand(...)` 读取已注册命令。
7. 命令缺失时查询相近命令名并输出错误。
8. 监听器按优先级执行 `onPreExecute(...)`。
9. 存在拦截器时调用 `CommandListener.execute(...)`。
10. 无拦截器时实例化命令类并调用 `runCommand(args, context)`。
11. 结果为 `BAD_SYNTAX` 且注册表有 `syntax` 时输出 `Syntax: ...`。
12. 所有监听器执行 `onPostExecute(...)`。

### 上下文过滤链路

1. UI 或控制台入口提供当前 `CommandContext`。
2. `CommandStore.getApplicableCommands(context)` 遍历已加载命令。
3. `console` 标签命令被视为系统级命令。
4. market 上下文匹配 `market` 标签或无 opposing 标签命令。
5. campaign 上下文匹配 `campaign` 标签或无 opposing 标签命令。
6. combat 上下文匹配 `combat` 标签或无 opposing 标签命令。
7. main menu 只保留能在主菜单显示的命令。
8. 命令实现仍在 `runCommand(...)` 内按 `CommandContext` 返回正式结果。

### 监听器链路

1. `CommandStore.reloadListeners()` 合并读取 `data/console/command_listeners.csv`。
2. 读取器以 `listenerId` 过滤空行。
3. 读取器加载 `listenerClass`。
4. 读取器校验监听器类实现 `CommandListener`。
5. 监听器实例按 `priority` 从高到低排序。
6. 执行命令前逐个调用 `onPreExecute(...)`。
7. 第 1 个最高优先级拦截者接管执行。
8. 命令结束后逐个调用 `onPostExecute(...)`。

## 规范

- `BAD_SYNTAX` 表示参数格式错误，并触发注册表 `syntax` 自动输出。
- `CommandContext.getEntityInteractedWith()` 在非生涯上下文返回 `null`。
- `CommandContext.getMarket()` 在非 `CAMPAIGN_MARKET` 上下文返回 `null`。
- `CommandContext.isCampaignAccessible()` 包含 campaign map、market、campaign battle 和 campaign simulation。
- `CommandResult.ERROR` 表示语法正确但执行失败。
- `CommandResult.SUCCESS` 表示命令完成。
- `CommandResult.WRONG_CONTEXT` 表示当前场景错误。
- `command` 应保持玩家输入名稳定；大小写差异由 Console Commands 的小写检索吸收。
- `Console.showMessage(...)` 是命令向玩家报告成功、错误细节或补充说明的输出入口。
- `help` 可包含转义换行，加载后作为多行帮助文本显示。
- `listenerClass` 必须有可实例化构造路径并实现 `CommandListener`。
- `priority` 越大，监听器越先取得拦截执行权。
- `tags` 中的 `cheat` 会受 Console Commands 的作弊开关过滤。
- `tags` 中的 `campaign`、`market`、`combat` 参与适用命令列表过滤。
- `syntax` 应与 `runCommand(...)` 的参数解析规则一致。

## 陷阱

- `BAD_SYNTAX` 被误用于执行失败时，控制台会显示语法而不是失败原因。
- `CAMPAIGN_MARKET` 被当成普通 campaign map 时，市场命令会丢失当前市场语义。
- `class` 指向未实现 `BaseCommand` 的类时，该行注册失败并输出异常。
- `command` 重名时，小写键会让后加载项覆盖同名命令。
- `CommandListener` 实例会在整场游戏会话中保持，字段缓存会跨命令调用保留。
- `ERROR` 被误用于参数错误时，玩家看不到 `syntax` 自动提示。
- `MAIN_MENU` 上下文访问 sector、combat engine 或 market 时可能没有对应对象。
- `tags` 只过滤列表和帮助项，命令实现仍必须返回 `WRONG_CONTEXT`。
- `help` 内未转义的 CSV 逗号和引号会破坏表格解析。
- `syntax` 留空时，`BAD_SYNTAX` 后没有自动语法提示。
