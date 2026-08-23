# Nexerelin 兼容

来源：可选兼容库 Nexerelin。

## 定义

Nexerelin 兼容是 Nexerelin 启用检测、Corvus 生成分支、势力配置和特遣队命名接口组成的可选接入协议。

## 参考

- `data/config/exerelin/corvus_spawnpoints.csv`：Corvus 模式生成点表。
- `data/config/exerelin/groundBattleDefs.json`：地面战产业定义表，按 industry ID 配置标签、强度、兵种、插件和图标。
- `data/config/exerelinFactionConfig/<faction>.json`：单势力配置文件，定义动态开局、外交、市场生成、地面战和特遣队配置。
- `data/config/exerelinFactionConfig/mod_factions.csv`：Nexerelin 外部势力名单。
- `exerelin.campaign.SectorManager.getManager()`：取得 Nexerelin sector 管理器。
- `exerelin.campaign.SectorManager.isCorvusMode()`：判断当前是否为 Corvus 模式。
- `exerelin.campaign.colony.IndustrialColonyTargetValuator`：工业殖民目标估值器。
- `exerelin.campaign.intel.specialforces.namer.SpecialForcesNamer`：特遣队命名接口。
- `Global.getSettings().getModManager().isModEnabled("nexerelin")`：Nexerelin 启用检测方法。

## 边界

- `alignments` 归属 Nexerelin 外交倾向模型。
- `baseFleetCostMultiplier` 归属势力舰队成本倍率。
- `bonusSeeds` 归属市场奖励物品种子。
- `colonyExpeditionChance` 归属殖民远征概率。
- `colonyTargetValuator` 归属殖民目标估值器类名。
- `corvusCompatible` 归属 Corvus 模式可用性。
- `defenceStations` 归属防御空间站可选集合。
- `diplomacyNegativeChance` 和 `diplomacyPositiveChance` 归属外交事件权重。
- `diplomacyTraits` 归属 Nexerelin 外交特性。
- `directoryUseShortName` 归属势力目录短名规则。
- `freeMarket` 归属市场自由港默认倾向。
- `groundBattleSettings` 归属该势力地面战修正。
- `industrySeeds` 归属市场产业种子。
- `marketSpawnWeight` 归属随机市场生成权重。
- `mod_factions.csv` 归属 Nexerelin 外部势力发现入口。
- `playableFaction` 归属动态开局可选性。
- `rebelFleetSuffix` 归属叛军舰队后缀。
- `specialForcesNamerClass` 归属特遣队命名器类名。
- `startRelationships`、`maxRelationships`、`minRelationships` 归属势力关系边界。
- `startShips*` 归属动态开局舰队包。
- `vengeanceFleetSizeMult` 归属复仇舰队规模倍率。

## 链路

### 启用检测链路

1. 调用方进入新存档或读档生命周期。
2. 调用方查询 `Global.getSettings().getModManager().isModEnabled("nexerelin")`。
3. 返回 true 时允许访问 Nexerelin API。
4. 返回 false 时使用基础生成入口。

### Corvus 生成链路

1. 启用检测通过后进入 Nexerelin 生成分支。
2. 分支取得 `SectorManager.getManager()`。
3. 分支调用 `isCorvusMode()`。
4. Corvus 模式为 true 时执行 Corvus 兼容生成。
5. Corvus 模式为 false 时跳过 Corvus 专用生成。

### 势力配置链路

1. Nexerelin 读取 `data/config/exerelinFactionConfig/mod_factions.csv`。
2. 读取器解析 `faction` 列。
3. Nexerelin 按势力 ID 读取 `data/config/exerelinFactionConfig/<faction>.json`。
4. 读取器解析可玩性、Corvus 兼容性和市场生成权重。
5. 读取器解析关系、外交特性和倾向。
6. 读取器解析开局舰队包、特殊物品和产业种子。
7. 读取器解析地面战设置。
8. 读取器解析特遣队命名器类名。

### 生成点链路

1. Nexerelin 读取 `data/config/exerelin/corvus_spawnpoints.csv`。
2. 读取器解析 `faction`。
3. 读取器解析 `system`。
4. 读取器解析 `entityID`。
5. Corvus 模式生成时把势力内容绑定到目标实体。

### 地面战定义链路

1. Nexerelin 读取 `data/config/exerelin/groundBattleDefs.json`。
2. 读取器进入 `industries` 对象。
3. 读取器按 industry ID 解析条目。
4. 读取器解析 `tags`、`strengthMult`、`troopCounts` 和成本倍率。
5. 读取器把地面战修正并入对应产业。

### 特遣队命名链路

1. Nexerelin 从势力配置读取 `specialForcesNamerClass`。
2. Nexerelin 加载命名器类。
3. 命名器实现 `SpecialForcesNamer`。
4. Nexerelin 调用 `getFleetName(fleet, origin, commander)`。
5. 返回字符串作为特遣队舰队名。

## 规范

- `corvus_spawnpoints.csv` 的 `entityID` 应引用目标星系内已存在实体。
- `corvus_spawnpoints.csv` 的 `faction` 应引用已注册势力 ID。
- `corvus_spawnpoints.csv` 的 `system` 应使用目标星系名称。
- `corvusCompatible` 应与 Corvus 生成点和势力配置一致。
- `groundBattleDefs.json` 的 `industries` 键应保持对象结构。
- `groundBattleSettings` 应只写 Nexerelin 地面战可识别键。
- `isCorvusMode()` 应只在 Nexerelin 启用且 manager 已存在后调用。
- `marketSpawnWeight` 应为可解析数值。
- `mod_factions.csv` 的 `faction` 列大小写应与势力 ID 一致。
- `playableFaction` 应为可解析布尔值。
- `specialForcesNamerClass` 指向的类应实现 `SpecialForcesNamer`。
- `startRelationships`、`maxRelationships`、`minRelationships` 的值应使用 Nexerelin 关系数值区间。
- `startShips*` 应保持二维数组结构，每个内层数组表示一组开局舰队。
- `troopCounts` 应使用 Nexerelin 地面战兵种 key。
- 启用检测应先于任何 `exerelin.*` 类的运行期访问。

## 陷阱

- `SectorManager.getManager()` 在 manager 尚未创建时返回 `null`，紧接着调用 `isCorvusMode()` 会失败。
- `colonyTargetValuator` 指向不存在或类型错误的类时，殖民目标估值器加载会失败。
- `corvus_spawnpoints.csv` 的 `system` 名称写错时，生成点无法绑定到目标星系。
- `entityID` 写成市场 ID 或显示名时，Corvus 生成点会找不到实体。
- `groundBattleDefs.json` 中 industry ID 写错时，对应产业没有地面战修正。
- `mod_factions.csv` 遗漏势力时，单势力 JSON 不会进入 Nexerelin 外部势力发现链路。
- `specialForcesNamerClass` 实现方法签名不匹配时，特遣队命名会在接口调用时失败。
- `startShips*` 的数组层级减少 1 层时，动态开局舰队包会被解析成错误结构。
