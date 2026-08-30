# 生涯特殊物品

来源：原版 Starsector。

## 定义

生涯特殊物品是 special item 的保存数据、规格、cargo stack 桥接、UI plugin、tooltip、render、右键动作和 drop params 解析接口集合。

## 参考

- `com.fs.starfarer.api.campaign.CargoAPI.addSpecial(...)`：提供向 cargo 写入 special item stack 的入口。
- `com.fs.starfarer.api.campaign.CargoStackAPI.getPlugin()`：提供 special item stack 到 plugin 实例的入口。
- `com.fs.starfarer.api.campaign.CargoStackAPI.getSpecialDataIfSpecial()`：提供 special item stack 到保存数据的入口。
- `com.fs.starfarer.api.campaign.CargoStackAPI.getSpecialItemSpecIfSpecial()`：提供 special item stack 到 spec 的入口。
- `com.fs.starfarer.api.campaign.CargoStackAPI.isSpecialStack()`：提供 special item stack 判定入口。
- `com.fs.starfarer.api.campaign.SpecialItemData`：承载 special item 保存态 id 与 data。
- `com.fs.starfarer.api.campaign.SpecialItemPlugin`：提供 special item 名称、价格、tooltip、render、右键动作、drop params 和 spec 入口。
- `com.fs.starfarer.api.campaign.SpecialItemPlugin.RightClickActionHelper`：提供右键动作中移除、添加和数量读取入口。
- `com.fs.starfarer.api.campaign.SpecialItemPlugin.SpecialItemRendererAPI`：提供 special item 渲染辅助入口。
- `com.fs.starfarer.api.campaign.SpecialItemSpecAPI`：维护 special item 规格、显示、价格、容量、tag、描述、参数、稀有度、音效、危险度和 plugin 实例入口。

## 边界

- `CargoAPI.addSpecial(...)` 只作为 special item 写入 cargo 的桥接入口。
- `CargoStackAPI.getPlugin()` 归属 special item stack 到 transient plugin 实例的桥接入口。
- `CargoStackAPI.getSpecialDataIfSpecial()` 归属 special item stack 保存数据读取入口。
- `CargoStackAPI.getSpecialItemSpecIfSpecial()` 归属 special item stack 规格读取入口。
- `CargoStackAPI.isSpecialStack()` 归属 special item stack 判定入口。
- `RightClickActionHelper` 归属右键动作中的 clicked stack 移除、任意 stack 移除、物品添加和数量读取。
- `SpecialItemData` 归属 special item 保存态 id、data、data 写入、equals 和 hashCode。
- `SpecialItemPlugin` 归属 special item cargo UI 名称、价格、tooltip、render、右键动作、drop params、design type 和 spec。
- `SpecialItemRendererAPI` 归属 schematic、scanlines、ship、weapon 和 background 渲染辅助。
- `SpecialItemSpecAPI` 归属 special item 规格 id、名称、图标、价格、容量、stack size、order、tag 和 source mod。
- `SpecialItemSpecAPI` 归属 special item 描述、首段描述、params、rarity、manufacturer、sound、drop sound、base danger 和 plugin 实例创建。
- 产业已安装特殊物品、产业 apply 和 installable item effect 由产业语义维护。

## 链路

### Cargo Stack 写入链路

1. 代码创建 `SpecialItemData(id, data)`。
2. 调用 `CargoAPI.addSpecial(data, quantity)` 写入 cargo。
3. cargo 创建 special item stack。
4. 调用 `CargoStackAPI.isSpecialStack()` 判定 stack 类型。
5. 调用 `getSpecialDataIfSpecial()` 或 `getSpecialItemSpecIfSpecial()` 读取保存数据或规格。

### Plugin 初始化链路

1. cargo UI 或 spec 调用 `SpecialItemSpecAPI.getNewPluginInstance(stack)`。
2. special item plugin 先接收 `setId(id)`。
3. 存在 stack 时调用 `init(stack)`。
4. 调用 `getName()`、`getPrice(market, submarket)`、`getSpec()` 或 `getDesignType()` 读取显示与规格。

### Tooltip 与渲染链路

1. cargo UI 调用 `isTooltipExpandable()` 和 `getTooltipWidth()`。
2. cargo UI 调用 `createTooltip(tooltip, expanded, transferHandler, stackSource)`。
3. cargo UI 调用 `render(x, y, w, h, alphaMult, glowMult, renderer)`。
4. plugin 通过 `SpecialItemRendererAPI` 渲染 schematic、scanlines、ship、weapon 或 background。

### 右键动作链路

1. cargo UI 调用 `hasRightClickAction()` 判断是否存在右键动作。
2. cargo UI 调用 `performRightClickAction()` 或 `performRightClickAction(helper)`。
3. plugin 通过 `RightClickActionHelper` 移除 clicked stack、移除任意 stack、添加物品或读取数量。
4. cargo UI 调用 `shouldRemoveOnRightClickAction()` 判断动作后是否移除当前物品。

### Drop Params 解析链路

1. 掉落逻辑取得 special item drop params 字符串。
2. 调用 `resolveDropParamsToSpecificItemData(params, random)`。
3. plugin 返回具体 item data 字符串。
4. 返回 null 表示掉落解析为空物品。
5. 返回空字符串表示该物品没有参数。

## 规范

- `CargoAPI.addSpecial(...)` 使用 `SpecialItemData` 写入 `CargoItemType.SPECIAL` stack。
- `CargoStackAPI.getPlugin()` 返回新的 special item plugin 实例。
- `CargoStackAPI.getSpecialDataIfSpecial()` 只在 special item stack 语义下读取保存数据。
- `CargoStackAPI.getSpecialItemSpecIfSpecial()` 只在 special item stack 语义下读取规格。
- `SpecialItemData.equals(...)` 同时比较 id 与 data。
- `SpecialItemData.hashCode()` 同时使用 id 与 data。
- `SpecialItemPlugin.setId(id)` 会先于 `init(stack)` 调用。
- `SpecialItemPlugin.resolveDropParamsToSpecificItemData(...)` 返回 null 表示掉落解析为空物品。
- `SpecialItemPlugin.resolveDropParamsToSpecificItemData(...)` 返回空字符串表示无参数物品。
- `SpecialItemSpecAPI.getNewPluginInstance(null)` 不会调用 plugin 的 `init(stack)`。
- `SpecialItemSpecAPI.getTags()` 返回 special item tag 集合，`hasTag(tag)` 判断单个 tag。
- `SpecialItemSpecAPI.setBaseDanger(...)` 与 `getBaseDanger()` 维护 special item 的 raid danger。

## 陷阱

- `CargoStackAPI.getPlugin()` 返回 transient plugin 实例，不能把实例字段当作同一 stack 的持久状态。
- `CargoStackAPI.getSpecialDataIfSpecial()` 在非 special item stack 上不具备读取语义。
- `RightClickActionHelper.removeFromClickedStackFirst(...)` 与 `removeFromAnyStack(...)` 的移除范围不同。
- `SpecialItemData` 的 equals 与 hashCode 包含 data，同 id 不同 data 不是同一个保存态。
- `SpecialItemPlugin.init(stack)` 在没有 stack 的路径下可能不被调用。
- `SpecialItemSpecAPI.getNewPluginInstance(null)` 不提供 stack 上下文。
