# OldEmpire 迁移与修复报告

日期：2026-08-28  
范围：将 `HeiHeiFleet` 合并进 `MengGod's legacy`，并将运行时和资源命名统一为 `Meng_OldEmpire`。

## 1. 目标与命名

本次迁移将原 mod 的 `heihei` 主舰/模块系统和 `krg` 武器系统并入现有 Meng 模组。原始 `HeiHeiFleetModPlugin` 不再作为独立入口，初始化职责被并入现有 `Mengplugin`。

统一命名规则如下：

| 类别 | 迁移前 | 迁移后 |
| --- | --- | --- |
| Java 类 | `HeiHeiXxx`、`KrgXxx` | `Meng_OldEmpireXxx` |
| 数据 ID | `heihei_xxx`、`krg_xxx` | `Meng_OldEmpire_xxx` |
| 模块槽位 | `HEIHEI_*` | `OLDEMPIRE_*` |
| Java 包 | `data.scripts.heihei/krg.*` | `data.hullmods`、`data.weapons`、`data.methods`、`data.scripts.console` |
| 图形资源 | HeiHei 原路径 | `graphics/ships/oldempire/`、`graphics/weapons/oldempire/` 等 |

## 2. 已迁移内容

### 2.1 Java 与插件入口

- `HeiHeiFleetModPlugin` 的加载职责合入 `src/scripts/Mengplugin.java`。
- 原 Luna 改装按钮迁移为 `Meng_ModuleRefitButton`。
- 原浮游模块选择器迁移为 `Meng_ModuleSelectorScript`。
- 迁移了 OldEmpire 船插、武器特效、控制台命令，以及 Krg 的核心工具、船插和武器逻辑。
- 所有新旧类路径已统一到 `data.*` 运行时包名；`jars/Meng.jar` 已包含对应编译产物。

### 2.2 数据、资源与注册表

- 船体：5 个主舰、18 个左右模块船体及 1 个空模块已迁移为 `Meng_OldEmpire_*.ship`。
- variant：主舰测试 variant、各模块标准 variant 已迁移并重命名。
- 武器与弹丸：`.wpn`、`.proj`、武器 CSV 和脚本路径已迁移到 `Meng_OldEmpire_*`。
- 图形：舰船、模块、船插和武器资源迁移到 OldEmpire 图形目录。
- 注册表：`ship_data.csv`、`hull_mods.csv`、`weapon_data.csv`、`ship_systems.csv`、`commands.csv`、描述文本和 settings 已更新。
- 相位纹理由原插件手动 `loadTexture()` 改为 `settings.json` 中 `graphics.Meng_OldEmpire_phase` 声明式预加载。

## 3. 模块配置与改装逻辑

### 3.1 CSV 驱动的模块选择器

`data/config/Meng_OldEmpire_floating_modules.csv` 是模块模式的唯一配置来源。它提供完整 variant ID、显示名、轻/重类型、模式编号和特殊舰船固定模块信息。

相较原始硬编码数组，`Meng_ModuleSelectorScript` 现在：

1. 直接读取完整 variant ID，不再拼接 `heihei_float_*` 字符串。
2. 通过精确 variant ID 匹配反推模式，避免 `contains()` 错配。
3. 从 CSV 条目数动态获得轻型/重型模式数量；新增模式不再要求修改按钮循环或标签范围。
4. 通过 JSON 保存模块通风口和武器配置，替代原始手工文本与双层 Base64 格式。
5. 对空 CSV、缺失槽位、非法 station-module 映射和损坏保存数据增加了防御性处理。

### 3.2 Luna 改装按钮

`Meng_ModuleRefitButton` 和 `Mengplugin` 按 CSV 模式数量动态注册按钮。应用加载与读档均会尝试注册，静态标记避免重复注册。

## 4. 已修复的迁移一致性问题

以下问题已在迁移期间定位并修正：

- graphics 资源引用、船插图标和 settings 纹理分类仍指向旧路径。
- `hull_mods.csv` 和武器数据中的 Java 类路径未迁移到 `data.hullmods.*` / `data.weapons.*`。
- `ship_data.csv` 表头不符合原版 schema。
- `Meng_OldEmpire_phasecloak` 被错误注册为 system 而不是 defense ID。
- `.wpn`、`.proj`、`.ship`、`.variant` 内部的旧 ID、脚本路径和引用关系。
- 模块子舰的 BUILT_IN 武器被旧筛选条件错误排除，导致最长武器射程为 0、普通模块无法选中目标。

## 5. 浮游炮控制器重构

控制器文件：`src/hullmods/Meng_OldEmpireFloatingBatteryController.java`。

### 5.1 结构

| 组件 | 职责 |
| --- | --- |
| `Controller` | 生命周期、初始化、环绕目标切换和每帧调用编排 |
| `FleetTargeting` | 每 0.16 秒统一扫描战场候选目标 |
| `FormationStrategy` | 只计算跟随、返航、08 环绕、09 编队的位置和期望朝向 |
| `ModuleRuntime` | 写入真实模块 `ShipAPI`，同步相位、通量、目标、移动、转向与停火 |
| `ModuleLayout` | 初始化后不变的槽位、锚点、模块类别和编队角色 |
| `ModuleMode` | `FOLLOW`、`ORBIT`、`RETURNING`、`DISABLED` |

### 5.2 目标与朝向行为

1. 模块最长射程取其所有非装饰武器的 `WeaponAPI.getRange()` 最大值。
2. 锁敌优先级为：母舰当前目标、射程内最近非舰载机/非无人机、射程内最近其他目标。
3. 普通模块只在目标位于母舰前方正负 60 度时瞄准目标，否则平滑回到母舰朝向。
4. 普通和返航模块最大转向速度为 120 度/秒。
5. 08/09 环绕模块在环绕模式中直接面向环绕目标。
6. 武器槽射界由船体数据限制为 30 度，火力可用性由武器槽自身处理；母舰 station-module 挂点射界为 360 度。

### 5.3 战斗结束与改装界面隔离

STATION_MODULE 的最终战斗朝向必须通过 station slot 参与结算；单独修改 `child.setFacing()` 会被引擎挂点结算覆盖。

为同时解决转向和退出模拟战后预览角度残留，当前实现：

1. 初始化每个模块时调用 `child.ensureClonedStationSlotSpec()`。
2. 战斗中只写入该子舰独有的 station-slot 规格副本和子舰实体。
3. 退出战斗、实体离场或母舰被摧毁时，通过 `SlotResetWatchdog` 复原子舰槽位、战斗实体 HullSpec、实体 variant HullSpec 和 FleetMember variant HullSpec 的锚点与初始角度。

此实现对应 Starsector 的 station-module API。编译已通过；仍需要游戏内按“进入模拟战 -> 模块转向 -> 退出 -> 切换其他船 -> 切回”流程复测一次。

## 6. 构建产物

浮游炮控制器每次修改后均以 Starsector API 编译，并同步更新 `jars/Meng.jar`。JAR 当前包含：

```text
Meng_OldEmpireFloatingBatteryController.class
Meng_OldEmpireFloatingBatteryController$Controller.class
Meng_OldEmpireFloatingBatteryController$Controller$SlotResetWatchdog.class
Meng_OldEmpireFloatingBatteryController$FleetTargeting.class
Meng_OldEmpireFloatingBatteryController$FormationStrategy.class
Meng_OldEmpireFloatingBatteryController$ModuleLayout.class
Meng_OldEmpireFloatingBatteryController$ModuleMode.class
Meng_OldEmpireFloatingBatteryController$ModulePose.class
Meng_OldEmpireFloatingBatteryController$ModuleRuntime.class
```

过期的 `ModuleState` 和 `ModuleTargeting` 内部 class 已从 JAR 移除。最后一次 Java 编译和 `git diff --check` 均通过。

## 7. 提交范围建议

本次提交应包含：

- OldEmpire 的 `.ship`、`.variant`、`.wpn`、`.proj`、`.system`、模块 CSV、图形和声音资源。
- 对各数据 CSV、`settings.json`、描述文件和控制台命令的迁移修改。
- 所有 `Meng_OldEmpire*.java`、`Meng_ModuleSelectorScript.java`、`Meng_ModuleRefitButton.java`、`Mengplugin.java` 的相关修改。
- `jars/Meng.jar`。
- 本报告和迁移记录。

建议不要混入本次迁移提交：

- `.idea/`、`src/.idea/`、`src/out/production/` 等 IDE 配置和编译输出。
- `build/codex-verify/`、`.zcode/` 等临时验证目录。
- `MengGods_legacy.zip`，除非该压缩包明确是计划发布的发行物。
- 与 OldEmpire 无关的现有修改，例如 MingGod、Dragonheart、时间系统等文件；应单独核对后另行提交。

## 8. 已知风险与提交前验证

- 旧存档如果仍引用 `heihei_float_*` 旧 variant/hull ID，可能出现找不到模块；目前未新增旧 ID alias，需决定是否提供兼容映射。
- 改装界面按钮依赖 LunaLib；缺少 LunaLib 时应确认插件入口的可选依赖处理仍符合当前模组策略。
- 请执行一次完整冷启动，避免 IDE 编译输出或内存中的旧 class 掩盖 JAR 问题。
- 建议最少验证：主舰生成、改装模块切换、模拟战进入/退出、普通模块锁敌转向、08/09 环绕、相位、母舰满通量停火、武器/弹丸发射。

## 9. 建议提交信息

```text
feat(oldempire): migrate HeiHeiFleet systems and refactor floating batteries

- migrate HeiHei/Krg hulls, variants, weapons, projectiles, assets and registrations
- move floating module configuration to CSV-driven selector/refit logic
- fix migrated graphics, class paths and cross-data references
- refactor floating battery targeting, formation and runtime control
- isolate station-module combat slot rotation from refit preview state
```
