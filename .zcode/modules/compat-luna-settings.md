# LunaLib 设置兼容

来源：可选兼容库 LunaLib。

## 定义

LunaLib 设置兼容是 LunaLib 读取 `LunaSettings.csv`、保存公共设置并通过 `LunaSettings` API 返回运行时设置值的接口协议。

## 参考

- `data/config/LunaSettings.csv`：设置项定义表。
- `data/config/LunaSettingsConfig.json`：设置页面配置，按 mod ID 提供 `iconPath`。
- `lunalib.backend.ui.settings.LunaSettingsConfigLoader.getIconPath(modId)`：读取设置页面图标路径。
- `lunalib.backend.ui.settings.LunaSettingsLoader.loadDefault()`：按启用 mod 读取 `data/config/LunaSettings.csv`。
- `lunalib.backend.ui.settings.LunaSettingsLoader.loadSettings(...)`：从公共设置文件加载保存值。
- `lunalib.backend.ui.settings.LunaSettingsLoader.saveDefaultsToFile(...)`：把缺失设置写入公共设置默认文件。
- `lunalib.lunaSettings.LunaSettings.getBoolean(modId, fieldId)`：读取布尔设置。
- `lunalib.lunaSettings.LunaSettings.getColor(modId, fieldId)`：读取颜色设置。
- `lunalib.lunaSettings.LunaSettings.getDouble(modId, fieldId)`：读取双精度设置。
- `lunalib.lunaSettings.LunaSettings.getFloat(modId, fieldId)`：读取浮点设置。
- `lunalib.lunaSettings.LunaSettings.getInt(modId, fieldId)`：读取整数设置。
- `lunalib.lunaSettings.LunaSettings.getString(modId, fieldId)`：读取字符串设置。
- `lunalib.lunaSettings.LunaSettingsListener.settingsChanged(modId)`：设置变更通知接口。

## 边界

- `defaultValue` 归属设置项默认值，并按 `fieldType` 转成目标类型。
- `fieldDescription` 归属设置 UI 的说明文本。
- `fieldID` 归属运行时读取 key。
- `fieldName` 归属设置 UI 的显示名。
- `fieldType` 归属设置类型，支持 `Header`、`Text`、`Int`、`Boolean`、`Double`、`String`、`Enum`、`Keycode`、`Color`、`Radio`。
- `iconPath` 归属设置页面中的 mod 图标。
- `maxValue` 归属 `Int` 和 `Double` 的最大值约束。
- `minValue` 归属 `Int` 和 `Double` 的最小值约束。
- `modId` 归属 Starsector 启用模组 ID。
- `secondaryValue` 归属枚举、单选和其它复合设置的附加值。
- `tab` 归属设置 UI 分页。
- `LunaSettings.csv` 归属默认设置项定义。
- `LunaSettingsConfig.json` 归属设置页面展示配置。
- `LunaSettings/<modId>.json` 归属 LunaLib 公共保存值。
- `LunaSettings` 静态读取 API 归属运行时消费。
- `LunaSettingsListener` 归属设置变更后的通知。

## 链路

### 设置定义加载链路

1. LunaLib 进入设置加载入口。
2. `LunaSettingsLoader.loadDefault()` 遍历启用 mod。
3. 加载器读取每个 mod 的 `data/config/LunaSettings.csv`。
4. 加载器跳过空 `fieldID` 行。
5. 加载器读取 `fieldName`、`fieldType`、`defaultValue`。
6. 加载器优先读取 `fieldTooltip`，再读取 `fieldDescription`。
7. 加载器读取 `secondaryValue` 和 `tab`。
8. `Int`、`Double`、`Boolean`、`Keycode` 和 `Enum` 按类型转换默认值。
9. `Int` 和 `Double` 读取 `minValue`、`maxValue`。
10. 加载器以 mod ID 和 `fieldID` 写入设置定义集合。

### 设置保存加载链路

1. `LunaSettingsLoader.saveDefaultsToFile(...)` 读取公共设置 JSON。
2. 公共设置 JSON 缺少某个 `fieldID` 时写入默认值。
3. `Header` 和 `Text` 类型跳过保存值写入。
4. 公共设置 JSON 有内容时保存。
5. `LunaSettingsLoader.loadSettings(...)` 读取公共设置 JSON。
6. 读取到的 JSON 按 mod ID 写入运行时设置表。

### 运行时读取链路

1. 调用方传入 `modId` 和 `fieldID`。
2. `LunaSettings.getBoolean(...)` 等静态方法检查设置是否已加载。
3. 未加载时触发 `LunaSettingsLoader.load()`。
4. 读取器按 `modId` 查询运行时设置表。
5. 读取器按 `fieldID` 查询目标值。
6. 类型匹配时返回 boxed 值。
7. mod 或字段缺失时记录错误并返回 `null`。
8. 消费方把 `null` 转成自己的默认语义。

### 变更通知链路

1. 消费方注册 `LunaSettingsListener`。
2. LunaLib 设置保存后调用 `reportSettingsChanged(modId)`。
3. LunaLib 遍历监听器。
4. 监听器收到 `settingsChanged(modId)`。
5. 监听器按目标 mod ID 刷新运行时缓存。

## 规范

- `Boolean` 设置应通过 `getBoolean(modId, fieldID)` 读取。
- `Color` 设置保存为十六进制字符串，读取时可缺省 `#` 前缀。
- `Double` 设置应提供可解析小数默认值。
- `Enum` 的 `defaultValue` 会按逗号拆成列表。
- `Header` 与 `Text` 只参与 UI 展示。
- `Int` 设置应提供可解析整数默认值。
- `Keycode` 保存的是 LWJGL/Starsector 输入键码，`0` 表示未绑定或未知键。
- `Radio` 的选项列表来自 `secondaryValue`。
- `String` 设置按原始字符串读取。
- `fieldID` 应与运行时读取 key 完全一致。
- `fieldType` 应使用 LunaLib 已支持类型名。
- `getBoolean(...)`、`getInt(...)`、`getFloat(...)`、`getDouble(...)`、`getString(...)`、`getColor(...)` 都可能返回 `null`。
- `minValue` 和 `maxValue` 只约束数值型设置。
- `modId` 应使用启用模组的实际 ID，而不是显示名。
- `settingsChanged(modId)` 应按 mod ID 过滤目标设置刷新。

## 陷阱

- `defaultValue` 与 `fieldType` 类型不匹配时，默认设置加载会在该字段转换时失败。
- `fieldID` 改名会让已有公共设置文件保留旧 key，新 key 使用默认值。
- `getBoolean(...)` 返回 `Boolean` 对象，直接拆箱 `null` 会触发运行时异常。
- `iconPath` 指向非正方形图像时，设置列表图标会出现缩放差异。
- `Keycode` 的 `0` 当作有效快捷键时，会把未绑定状态误判成按键绑定。
- `LunaSettingsConfig.json` 顶层键与 mod ID 不一致时，设置页面取不到对应图标配置。
- `Radio` 的 `defaultValue` 缺席于 `secondaryValue` 列表时，UI 默认选择和运行时读取会错位。
- `settingsChanged(modId)` 未按 mod ID 过滤时，一个 mod 的保存动作会刷新无关设置缓存。
