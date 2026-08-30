# 微型兼容

来源：可选兼容微型入口。

## 定义

微型兼容是收纳规模不足以单独成模块、且主要由外部模组按约定文件消费的小型可选兼容入口。

## 参考

- `data/config/CommissionBonus/TechpriestCommission.csv`：CommissionBonus 委托船插映射表，列为 `faction_id`、`hullmod_id`。
- `data/config/ctbsettings/ctb_refinery_stations.csv`：CTB 炼厂站点数量表，列为 `FactionID`、`number`。
- `data/config/ctbsettings/ctb_system_armor_mult.csv`：CTB 战术系统装甲倍率表，列为 `system id`、`armor mult`、`by effectlevel`、`unique`。
- `data/config/gsounty/EndlessFaction.csv`：gsounty 无限赏金势力表，列为 `factionid`。
- `data/config/newbeginnings/allowed_factions.json`：New Beginnings 允许势力表，以势力 ID 为 JSON 键。
- `data/config/prism/prism_ships_blacklist.csv`：Prism 舰船黑名单表，列为 `id`。
- `Global.getSettings().getMergedSpreadsheetDataForMod(...)`：外部 CSV 读取方常用的跨模组合并入口。
- `Global.getSettings().getMergedJSONForMod(...)`：外部 JSON 读取方常用的跨模组合并入口。

## 边界

- `CommissionBonus/TechpriestCommission.csv` 归属委托势力到委托船插 ID 的数据映射。
- `ctb_refinery_stations.csv` 归属势力到炼厂站点数量的数值映射。
- `ctb_system_armor_mult.csv` 归属战术系统到装甲倍率的数值映射。
- `EndlessFaction.csv` 归属 gsounty 可进入无限赏金池的势力列表。
- `allowed_factions.json` 归属 New Beginnings 可选择势力集合。
- `prism_ships_blacklist.csv` 归属 Prism 舰船排除列表。
- CommissionBonus 入口只归属委托船插映射。
- CTB 入口只归属炼厂数量和战术系统装甲倍率。
- gsounty 入口只归属无限赏金势力列表。
- New Beginnings 入口只归属新开局允许势力集合。
- Prism 入口只归属舰船黑名单。
- 已有独立 API、注册表、运行时回调或生成分支的兼容归属对应大模块。
- CSV 表头归属外部读取方的列名匹配。
- JSON 顶层键归属外部读取方的 ID 匹配。
- 势力 ID、船插 ID、舰船 ID 和系统 ID 归属 Starsector 全局数据 ID 空间。
- 微型兼容目录只承载外部协议数据，状态保存归属外部读取方。
- 微型兼容目录的启用条件归属外部读取方和对应模组加载状态。

## 链路

### CommissionBonus 链路

1. CommissionBonus 读取 `data/config/CommissionBonus/TechpriestCommission.csv`。
2. 读取器解析 `faction_id`。
3. 读取器解析 `hullmod_id`。
4. 读取器把委托势力映射到委托奖励船插。

### CTB 炼厂链路

1. CTB 读取 `data/config/ctbsettings/ctb_refinery_stations.csv`。
2. 读取器解析 `FactionID`。
3. 读取器解析 `number`。
4. 读取器把数量并入该势力的炼厂站点规则。

### CTB 系统装甲链路

1. CTB 读取 `data/config/ctbsettings/ctb_system_armor_mult.csv`。
2. 读取器解析 `system id`。
3. 读取器解析 `armor mult`。
4. 读取器解析 `by effectlevel`。
5. 读取器解析 `unique`。
6. 读取器把倍率应用到对应战术系统装甲规则。

### New Beginnings 链路

1. New Beginnings 读取 `data/config/newbeginnings/allowed_factions.json`。
2. 读取器解析顶层势力 ID。
3. 读取器解析势力 ID 对应数组。
4. 读取器把势力写入新开局允许集合。

### Prism 链路

1. Prism 读取 `data/config/prism/prism_ships_blacklist.csv`。
2. 读取器解析 `id`。
3. 读取器把舰船 ID 写入舰船排除集合。

### gsounty 链路

1. gsounty 读取 `data/config/gsounty/EndlessFaction.csv`。
2. 读取器解析 `factionid`。
3. 读取器把势力 ID 写入无限赏金可选集合。

## 规范

- 新增兼容入口只有在没有独立 API、注册表、运行时回调或生成分支时才进入微型兼容。
- 新增兼容入口若形成 2 个以上运行时链路，应拆出独立可选兼容模块。
- 微型兼容不允许出现在 `mod_info.json` 中的 `dependencies` 中。
- 数据文件应保留在外部读取方约定目录下。
- 同一 ID 在同一外部协议中应保持单一语义。

## 陷阱

- 带生成分支和势力配置的兼容入口应归入对应大型兼容模块。
- 带命令类接口和监听器接口的兼容入口应归入对应大型兼容模块。
- 带运行时读取 API 的设置入口应归入对应大型兼容模块。
- 带规则效果和标签回调的兼容入口应归入对应大型兼容模块。
- CSV 引号、逗号和换行未按表格规则转义时，右侧列会整体偏移。
- 跨模组同路径文件会被合并读取，同一 ID 重复时结果取决于读取方覆盖策略。
