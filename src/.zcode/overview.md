# Overview

本文件维护模组运行内容的速查入口。API 与实现边界由 `.zcode/module-map.md` 和 `.zcode/modules/` 维护。

## 定位

- `mod_info.json` 定义模组标识、目标游戏版本、依赖、jar 和 plugin 入口。
- LazyLib、MagicLib、GraphicsLib 与 BoxUtil 属于硬依赖。
- UNGP、Nexerelin、Console Commands、SWP、LunaLib 及微型集成属于可选兼容。

## 顶层目录

- `data/` 保存游戏直接加载的数据、配置、专用资产定义、文本和 shader 入口。
- `graphics/` 保存 PNG 图形与法线资源。
- `jars/` 保存 `mod_info.json` 声明的运行 jar。
- `sounds/` 保存由 `data/config/sounds.json` 注册的 OGG 文件。
- `src/` 保存加载入口、战斗、生涯、UI、视觉与兼容 Java 实现。

## 关键注册表

- `data/campaign/commodities.csv` 注册商品。
- `data/campaign/industries.csv` 注册产业。
- `data/campaign/rules.csv` 注册交互规则、条件、结果和文本入口。
- `data/campaign/special_items.csv` 注册特殊物品。
- `data/config/settings.json` 注册设置、图形和插件。
- `data/config/sounds.json` 注册 sound ID 与 OGG 路径。
- `data/hulls/ship_data.csv` 注册舰船 hull。
- `data/hulls/wing_data.csv` 注册战机 wing。
- `data/shipsystems/ship_systems.csv` 注册战术系统。
- `data/strings/descriptions.csv` 保存长描述。
- `data/strings/strings.json` 保存脚本与 UI 字符串。
- `data/weapons/weapon_data.csv` 注册武器。

## 追踪路径

- 舰船从 `data/hulls/ship_data.csv` 的 hull ID 追到 `.ship`、`.variant` 和图形资源。
- 武器从 `data/weapons/weapon_data.csv` 的 weapon ID 追到 `.wpn`、`.proj` 和图形资源。
- 战机从 `data/hulls/wing_data.csv` 的 wing ID 追到 fighter variant、hull 和图形资源。
- 战术系统从 `data/shipsystems/ship_systems.csv` 的 system ID 追到 `.system`、Stats、AI 与图标。
- 船体插件从 `data/hullmods/hull_mods.csv` 的 hullmod ID 追到 Java、图标和描述。
- 生涯交互从 `data/campaign/rules.csv` 的 rule、condition、command 或 option 追到脚本和文本。
- 文本从 `strings.json` 或 `descriptions.csv` 的 key 反查数据字段与代码消费者。
- 音效从 `sounds.json` 的 sound ID 追到 OGG 文件和引用方。
- 可选兼容从兼容数据入口追到外部协议、模块文档和内部接入实现。
