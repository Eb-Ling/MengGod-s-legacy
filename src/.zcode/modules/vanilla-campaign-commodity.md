# 生涯商品

来源：原版 Starsector。

## 定义

生涯商品是全局 commodity 规格、commodity id 常量、cargo stack 商品桥接和商品 UI 扩展的原版接口集合。

## 参考

- `com.fs.starfarer.api.campaign.CargoStackAPI.getCommodityId()`：提供 commodity stack 到 commodity id 的读取入口。
- `com.fs.starfarer.api.campaign.CargoStackAPI.getResourceIfResource()`：提供 commodity stack 到 `CommoditySpecAPI` 的读取入口。
- `com.fs.starfarer.api.campaign.econ.CommoditySpecAPI`：维护全局 commodity 规格、显示、价格、容量、tag、效用、经济单位、音效和危险度字段。
- `com.fs.starfarer.api.campaign.econ.EconomyAPI.getAllCommodityIds()`：提供全局 commodity id 列表入口。
- `com.fs.starfarer.api.campaign.econ.EconomyAPI.getCommoditySpec(...)`：提供按 commodity id 读取全局 commodity spec 的入口。
- `com.fs.starfarer.api.campaign.listeners.CommodityDescriptionProvider`：提供 commodity stack tooltip 标题与描述扩展。
- `com.fs.starfarer.api.campaign.listeners.CommodityIconProvider`：提供 commodity stack 图标与 rank icon 扩展。
- `com.fs.starfarer.api.campaign.listeners.CommodityTooltipModifier`：提供 commodity stack 价格后 tooltip section 扩展。
- `com.fs.starfarer.api.impl.campaign.ids.Commodities`：提供原版 commodity id 常量与 commodity tag 常量。

## 边界

- `CargoStackAPI.getCommodityId()` 只作为 commodity stack 到 commodity id 的桥接入口。
- `CargoStackAPI.getResourceIfResource()` 只作为 commodity stack 到 commodity spec 的桥接入口。
- `CommodityDescriptionProvider` 归属 commodity stack 的 tooltip 标题与描述扩展。
- `CommodityIconProvider` 归属 commodity stack 的图标与 rank icon 扩展。
- `CommoditySpecAPI` 归属全局 commodity id、名称、来源、图标、基础价格、容量、stack size 和显示顺序。
- `CommoditySpecAPI` 归属全局 commodity tag、demand class、utility、economy tier、econ unit、export value 和 base danger。
- `CommoditySpecAPI` 归属 personnel、fuel、supplies、primary、exotic、meta 和 non-econ 判定。
- `CommoditySpecAPI` 归属 drop sound、sound id、large icon 和 source mod 信息。
- `CommodityTooltipModifier` 归属 commodity stack 价格后 tooltip 内容扩展。
- `Commodities` 归属原版 commodity id 常量和 commodity tag 常量。
- `EconomyAPI.getAllCommodityIds()` 归属全局 commodity id 集合读取入口。
- `EconomyAPI.getCommoditySpec(...)` 归属全局 commodity spec 按 id 读取入口。
- 市场内供需、库存、交易修饰和价格结算归市场 commodity 对象。

## 链路

### 全局商品规格读取链路

1. 代码取得 `Global.getSector().getEconomy()`。
2. 调用 `EconomyAPI.getAllCommodityIds()` 读取全局 commodity id 列表。
3. 调用 `EconomyAPI.getCommoditySpec(commodityId)` 读取 `CommoditySpecAPI`。
4. 调用方读取名称、图标、基础价格、容量、tag、音效或危险度字段。

### Cargo Stack 商品桥接链路

1. 代码取得 `CargoStackAPI`。
2. 调用 `isCommodityStack()` 判断 commodity stack。
3. 调用 `getCommodityId()` 取得 commodity id。
4. 调用 `getResourceIfResource()` 取得 `CommoditySpecAPI`。
5. 调用方只读取 commodity 规格字段，容器数量和空间状态仍由 cargo stack 维护。

### 商品 UI 扩展链路

1. 代码实现 `CommodityDescriptionProvider`、`CommodityIconProvider` 或 `CommodityTooltipModifier`。
2. provider 接收 `CargoStackAPI`。
3. provider 按 stack 对应 commodity 返回标题、描述、图标或 tooltip section。
4. cargo UI 使用 provider 返回值渲染 commodity stack 显示内容。

## 规范

- `Commodities` 的原版基础 id 常量包含 `supplies`、`fuel`、`crew` 和 `marines`。
- `Commodities` 的原版 AI core id 常量包含 `alpha_core`、`beta_core`、`gamma_core` 和 `omega_core`。
- `Commodities` 的原版 tag 常量包含 `crew`、`marines`、`personnel`、`food`、`exotic`、`expensive`、`military`、`meta`、`luxury`、`medical`、`nonecon`、`nosell`、`ai_core` 和 `no_loss_from_combat`。
- `CommoditySpecAPI.getBasePrice()` 返回全局基础价格，市场买卖价格由市场价格接口计算。
- `CommoditySpecAPI.getLowerCaseName()` 对非 exotic commodity 返回小写名称。
- `CommoditySpecAPI.getTags()` 返回 commodity tag 集合，`hasTag(tag)` 判断单个 tag。
- `CommoditySpecAPI.getUtility()` 是全局 utility，市场内效用读取使用市场 commodity 的 utility 入口。
- `CommoditySpecAPI.isFuel()`、`isPersonnel()` 和 `isSupplies()` 用于 commodity 类型判定。
- `CommoditySpecAPI.setBasePrice(...)`、`setName(...)`、`setIconName(...)` 和 `setDemandClass(...)` 修改全局 spec 字段。
- `EconomyAPI.getCommoditySpec(commodityId)` 按 commodity id 返回全局 commodity spec。
- `CargoStackAPI.getCommodityId()` 对非 commodity stack 返回 null。
- `CargoStackAPI.getResourceIfResource()` 返回 commodity spec 的兼容命名保留原版 resource 术语。

## 陷阱

- `CargoStackAPI.getCommodityId()` 返回 null 时不能继续按 commodity spec 读取。
- `CommodityDescriptionProvider` 和 `CommodityIconProvider` 是 generic plugin，返回值需要按单个 stack 判定。
- `CommoditySpecAPI.getBasePrice()` 不能替代市场买入价或卖出价。
- `CommoditySpecAPI.getUtility()` 不能替代市场内 exotic goods 的实际效用。
- `CommoditySpecAPI.isNonEcon()` 只表达商品规格标记，不表示 cargo stack 不可出现。
- `EconomyAPI.getCommoditySpec(...)` 读取的是全局 spec，不表示该 commodity 在某个市场有库存、供给或需求。
