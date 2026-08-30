# 数据资产 API

来源：原版 Starsector。

## 定义

数据资产 API 是 Starsector 从 CSV、JSON、专用扩展名文件和资源路径加载 spec、文本、图形、音效与脚本类名的入口集合。

## 参考

- `com.fs.starfarer.api.SettingsAPI`：提供 CSV、JSON、文本、sprite、spec 和脚本实例读取入口。
- `com.fs.starfarer.api.SettingsAPI.getMergedJSON(...)`：按 mod 合并语义读取 JSON。
- `com.fs.starfarer.api.SettingsAPI.getMergedSpreadsheetData(...)`：按 id 列合并 CSV。
- `com.fs.starfarer.api.combat.ShipHullSpecAPI`：提供 `.ship` 加载后的 hull spec 接口。
- `com.fs.starfarer.api.combat.ShipSystemSpecAPI`：提供 `.system` 加载后的系统 spec 接口。
- `com.fs.starfarer.api.combat.ShipVariantAPI`：提供 `.variant` 加载后的装配接口。
- `com.fs.starfarer.api.loading.Description`：提供 `descriptions.csv` 加载后的分段说明文本。
- `com.fs.starfarer.api.loading.HullModSpecAPI`：提供 `hull_mods.csv` 加载后的船体插件 spec。
- `com.fs.starfarer.api.loading.MissileSpecAPI`：提供导弹 projectile spec 加载后的制导、爆炸和碰撞接口。
- `com.fs.starfarer.api.loading.ProjectileSpecAPI`：提供 `.proj` 加载后的弹丸 spec。
- `com.fs.starfarer.api.loading.WeaponSpecAPI`：提供 `.wpn` 与 `weapon_data.csv` 加载后的武器 spec。
- `data/config/settings.json`：注册 settings、graphics、颜色、字体和部分全局资源。
- `data/hulls/ship_data.csv`：注册舰船 hull 数据。
- `data/weapons/weapon_data.csv`：注册武器数据。

## 边界

- `.proj` 在本模块只作为 projectile 或 missile spec 的专用文件加载入口。
- `.ship` 在本模块只作为 hull spec 的专用文件加载入口。
- `.system` 在本模块只作为 ship system spec 的专用文件加载入口。
- `.variant` 在本模块只作为 ship variant 的专用文件加载入口。
- `.wpn` 在本模块只作为 weapon spec 的专用文件加载入口。
- `Description.Type` 归属 `descriptions.csv` 的文本分类。
- `SettingsAPI` 归属加载后 spec、资源和数据文件读取。
- `SettingsAPI.getPlugin(id)` 归属 settings 中 `plugins` 注册表实例读取。
- `custom_entities.json` 归属自定义生涯实体 spec。
- `descriptions.csv` 归属按 id 与 type 读取的长说明文本。
- `hull_styles.json` 归属舰船引擎样式、武器覆盖和装饰规则。
- `hull_mods.csv` 归属船体插件注册。
- `settings.json` 归属全局 settings 与 graphics 类资源注册。
- `ship_data.csv` 归属舰船基础数值注册。
- `ship_systems.csv` 归属战术系统注册。
- `strings.json` 归属按 category 与 id 读取的字符串。
- `weapon_data.csv` 归属武器表格数值注册。

## 链路

### CSV 链路

1. 游戏按固定路径读取核心 CSV。
2. 表头列名确定字段映射。
3. id 列确定 spec 或规则 key。
4. 行数据写入对应 spec 或规则对象。
5. 多模组合并入口可按 id 列替换同 id 行。
6. Java 通过 `SettingsAPI.get...Spec(id)` 读取加载结果。

### JSON 链路

1. 游戏读取 JSON 文件。
2. JSON key 映射到 settings、spec、资源表或专用配置。
3. 路径字段以 `/` 表达资源路径。
4. 脚本字段以完整 Java 类名表达。
5. settings 中 `graphics` 分类注册 sprite category 与 key。
6. settings 中 `plugins` 注册可缓存读取的插件实例。
7. Java 可通过 `SettingsAPI.loadJSON(...)` 读取 JSON。

### 专用文件链路

1. hull 或 weapon 注册表声明 id。
2. spec 引用 `.ship`、`.wpn`、`.proj`、`.system` 或 `.variant`。
3. 游戏解析专用文件结构。
4. 文件中的资源路径绑定 graphics、sounds 或脚本类。
5. 文件中的内联 JSON 参数暴露给对应 spec。
6. spec 接口暴露加载后的运行时读写入口。

### 文本链路

1. `descriptions.csv` 按 id 和 type 注册说明。
2. UI 或脚本调用 `SettingsAPI.getDescription(id, type)`。
3. `strings.json` 按 category 和 id 注册字符串。
4. 脚本调用 `SettingsAPI.getString(category, id)`。
5. 字符串结果由 UI、tooltip 或脚本消费。

## 规范

- `SettingsAPI.getAllHullModSpecs()` 返回已加载 hullmod spec 集合。
- `SettingsAPI.getAllShipHullSpecs()` 返回已加载 hull spec 集合。
- `SettingsAPI.getAllShipSystemSpecs()` 返回已加载系统 spec 集合。
- `SettingsAPI.getAllWeaponSpecs()` 返回常规武器 spec 集合。
- `SettingsAPI.getActuallyAllWeaponSpecs()` 包含 SYSTEM weapon。
- `SettingsAPI.getDescription(id, type)` 返回 `Description` 或 `null`。
- `SettingsAPI.getInstanceOfScript(className)` 使用脚本类加载器创建对象。
- `SettingsAPI.getMergedSpreadsheetData(idColumn, path)` 使用 id 列合并 CSV。
- `SettingsAPI.getMergedJSON(path)` 使用 mod 合并语义读取 JSON。
- `SettingsAPI.getPlugin(id)` 返回缓存插件实例。
- `SettingsAPI.getSprite(category, key)` 使用 `settings.json` 的 graphics 注册。
- `SettingsAPI.getSpriteName(category, id)` 返回已注册 sprite 路径。
- `SettingsAPI.getVariant(variantId)` 返回装配 spec。
- `SettingsAPI.getWeaponSpec(weaponId)` 返回武器 spec。
- `SettingsAPI.loadCSV(filename)` 返回以列名为 key 的 JSON object 数组。
- `SettingsAPI.loadJSON(filename)` 返回 JSON object。
- `SettingsAPI.openStream(filename)` 和 `loadText(filename)` 要使用 `/` 路径。
- 脚本类名字段必须是脚本类加载器可实例化的完整类名。
- 资源路径字段必须与实际 graphics 或 sounds 文件路径一致。

## 陷阱

- CSV 列顺序和列名属于解析契约。
- `#` 行、空行和注释在不同数据表中的解析语义由对应 loader 决定。
- `SettingsAPI.getPlugin(id)` 返回的实例被缓存，字段保存存档对象会污染跨存档状态。
- `SettingsAPI.getAllWeaponSpecs()` 与 `getActuallyAllWeaponSpecs()` 覆盖范围不同。
- `.proj` 的 on-hit 与 on-fire 类名由 projectile spec 消费。
- `.ship` 中 `SYSTEM` 槽位和 `builtInWeapons` 会参与舰船生成。
- `.system` 的 `isRunScriptWhilePaused` 和 `isRunScriptWhileIdle` 会改变 stats 脚本调用范围。
- `.variant` 同时被数据加载、舰队成员和战斗舰船消费，读取方要区分资产入口与运行态装配对象。
- `strings.json` 与 `descriptions.csv` 是两套文本入口。
- Windows 反斜杠路径会破坏原版注释要求的跨平台路径语义。
