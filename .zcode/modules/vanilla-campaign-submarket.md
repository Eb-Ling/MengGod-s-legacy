# 生涯子市场

来源：原版 Starsector。

## 定义

生涯子市场是市场下属交易入口、cargo、交易合法性、UI/tooltip 和玩家交易经济影响的原版接口集合。

## 参考

- `com.fs.starfarer.api.campaign.CargoAPI`：提供子市场库存 cargo 读取、更新和交易内容入口。
- `com.fs.starfarer.api.campaign.CargoStackAPI`：承载子市场交易中的单个货物栈。
- `com.fs.starfarer.api.campaign.CoreUIAPI`：提供子市场 UI trade mode 和 tooltip 上下文。
- `com.fs.starfarer.api.campaign.PlayerMarketTransaction`：承载玩家交易记录。
- `com.fs.starfarer.api.campaign.SubmarketPlugin`：定义子市场 plugin 初始化、cargo、交易和 UI/tooltip 接口。
- `com.fs.starfarer.api.campaign.econ.MarketAPI.addSubmarket(...)`：提供子市场添加入口。
- `com.fs.starfarer.api.campaign.econ.MarketAPI.getSubmarket(...)`：提供按 spec id 读取子市场入口。
- `com.fs.starfarer.api.campaign.econ.MarketAPI.removeSubmarket(...)`：提供按 spec id 移除子市场入口。
- `com.fs.starfarer.api.campaign.econ.SubmarketAPI`：定义子市场运行时对象、spec、plugin 和 cargo 入口。
- `com.fs.starfarer.api.campaign.econ.SubmarketSpecAPI`：定义子市场静态 id、名称、描述、图标和默认 faction。
- `com.fs.starfarer.api.impl.campaign.ids.Submarkets`：提供原版 open market、black market、storage、military 和 local resources 子市场 id。
- `com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin`：提供子市场 plugin 默认 cargo、交易、经济影响、UI 和 tooltip 实现。
- `data/campaign/submarkets.csv`：注册子市场表。

## 边界

- `BaseSubmarketPlugin` 归属子市场库存 cargo 入口、tariff、交易和 UI/tooltip 默认实现。
- `CargoAPI` 在本模块只作为子市场库存和玩家交易内容的 cargo 入口。
- `CargoStackAPI` 在本模块只作为交易合法性与经济影响判定的 stack 输入。
- `CoreUIAPI` 归属子市场启用状态、点击行为、dialog 文本和 tooltip 渲染上下文。
- `MarketAPI.addSubmarket(...)` 与 `removeSubmarket(...)` 归属市场内子市场集合增删入口。
- `MarketAPI.getSubmarket(...)` 与 `getSubmarketsCopy()` 归属市场内子市场集合读取入口。
- `PlayerMarketTransaction` 归属玩家交易 bought、sold、market、submarket 和 trade mode 数据。
- `SubmarketAPI` 归属运行时对象、plugin、cargo 和 stack 合法性检查。
- `SubmarketPlugin` 归属 cargo 更新、交易上报、UI/tooltip 和经济影响。
- `SubmarketSpecAPI` 归属子市场静态 id、名称、描述、图标和默认 faction。
- `SubmarketPlugin.PlayerEconomyImpactMode` 归属玩家买卖对市场 commodity trade mod 的方向语义。
- `SubmarketPlugin.TransferAction` 归属玩家买入与卖出的交易方向。
- `Submarkets` 归属原版常用子市场 id 常量。

## 链路

### CSV 加载链路

1. 游戏读取 `data/campaign/submarkets.csv`。
2. `id` 列注册 submarket spec key。
3. `name`、`faction`、`desc`、`icon` 和 `order` 写入子市场静态 spec。
4. `script` 列写入子市场 plugin 类名。
5. 游戏创建 `SubmarketSpecAPI`。
6. `SettingsAPI.getAllSubmarketSpecs()` 暴露全部子市场 spec。
7. `MarketAPI.addSubmarket(specId)` 读取对应 spec。
8. 市场创建 `SubmarketAPI` 并进入子市场集合链路。

### 子市场集合链路

1. 调用 `MarketAPI.addSubmarket(specId)` 或 `addSubmarket(submarket)`。
2. 市场创建或接收 `SubmarketAPI`。
3. 调用 `SubmarketAPI.getSpec()` 绑定 `SubmarketSpecAPI`。
4. 调用 `SubmarketAPI.getPlugin()` 绑定 `SubmarketPlugin`。
5. 调用 `MarketAPI.hasSubmarket(specId)` 检查存在。
6. 调用 `MarketAPI.getSubmarket(specId)` 读取单个子市场。
7. 调用 `MarketAPI.getSubmarketsCopy()` 读取子市场列表。
8. 调用 `MarketAPI.removeSubmarket(specId)` 移除子市场。

### Plugin 初始化与 cargo 链路

1. 市场创建 `SubmarketAPI`。
2. 市场创建 `SubmarketPlugin`。
3. 调用 `SubmarketPlugin.init(submarket)`。
4. `BaseSubmarketPlugin.init(...)` 写入 submarket 与 market 字段。
5. UI 或代码调用 `SubmarketPlugin.updateCargoPrePlayerInteraction()`。
6. 调用 `SubmarketPlugin.getCargo()`。
7. `BaseSubmarketPlugin.getCargo()` 在 cargo 为空时创建 unlimited cargo。
8. `BaseSubmarketPlugin.getCargo()` 调用 `cargo.initMothballedShips(submarket.getFaction().getId())`。
9. 调用 `SubmarketPlugin.getCargoNullOk()` 读取当前 cargo 字段。

### 交易合法性链路

1. 交易 UI 按 `TransferAction.PLAYER_BUY` 或 `PLAYER_SELL` 判定方向。
2. 对 commodity 调用 `isIllegalOnSubmarket(commodityId, action)`。
3. 对 cargo stack 调用 `isIllegalOnSubmarket(stack, action)`。
4. 对舰船调用 `isIllegalOnSubmarket(member, action)`。
5. 非法时读取 `getIllegalTransferText(...)`。
6. 非法时读取 `getIllegalTransferTextHighlights(...)`。
7. `SubmarketAPI.isIllegalOnSubmarket(stack, action)` 提供运行时子市场的 stack 合法性入口。

### 玩家交易经济影响链路

1. 玩家交易生成 `PlayerMarketTransaction`。
2. 子市场 plugin 调用 `reportPlayerMarketTransaction(transaction)`。
3. `BaseSubmarketPlugin.reportPlayerMarketTransaction(...)` 检查 `isParticipatesInEconomy()`。
4. 读取 `getPlayerEconomyImpactMode()`。
5. 读取 `getPlayerTradeImpactMult()`。
6. 交易记录写入 player activity tracker 的 submarket trade data。
7. 遍历 `transaction.getSold().getStacksCopy()`。
8. 遍历 `transaction.getBought().getStacksCopy()`。
9. commodity stack 按 mode 写入 `CommodityOnMarketAPI.addTradeMod(...)`、`addTradeModPlus(...)` 或 `addTradeModMinus(...)`。
10. trade mod 使用 `TRADE_IMPACT_DAYS` 作为持续天数。

### UI 与 tooltip 链路

1. UI 调用 `SubmarketPlugin.isEnabled(ui)` 判断子市场是否可用。
2. UI 调用 `getOnClickAction(ui)` 判断点击行为。
3. `SHOW_TEXT_DIALOG` 行为读取 `getDialogText(ui)`、`getDialogTextHighlights(ui)` 和 `getDialogOptions(ui)`。
4. UI 调用 `isHidden()`、`showInFleetScreen()` 和 `showInCargoScreen()` 判断显示位置。
5. tooltip 调用 `hasCustomTooltip()`。
6. tooltip 调用 `getTooltipWidth()`。
7. tooltip 调用 `createTooltip(ui, tooltip, expanded)`。
8. `BaseSubmarketPlugin.createTooltip(...)` 读取 `SubmarketSpecAPI.getDesc()`。
9. 描述文本执行 rules token replacement。
10. tooltip 追加 `getTooltipAppendix(ui)` 与 `getTooltipAppendixHighlights(ui)`。

## 规范

- `BaseSubmarketPlugin.TRADE_IMPACT_DAYS` 为 `120` campaign days。
- `BaseSubmarketPlugin.getCargo()` lazy 创建 unlimited cargo。
- `BaseSubmarketPlugin.getCargo()` 创建 cargo 后按 `submarket.getFaction().getId()` 初始化 mothballed ships。
- `BaseSubmarketPlugin.getPlayerEconomyImpactMode()` 默认返回 `NONE`。
- `BaseSubmarketPlugin.getTariff()` 返回 `market.getTariff().getModifiedValue()`。
- `BaseSubmarketPlugin.isIllegalOnSubmarket(commodityId, action)` 默认使用 `market.isIllegal(commodityId)`。
- `BaseSubmarketPlugin.isIllegalOnSubmarket(stack, action)` 对非 commodity stack 默认返回 false。
- `BaseSubmarketPlugin.isMilitaryMarket()` 默认返回 false。
- `BaseSubmarketPlugin.isOpenMarket()` 默认返回 false。
- `BaseSubmarketPlugin.isParticipatesInEconomy()` 默认返回 true。
- `BaseSubmarketPlugin.okToUpdateShipsAndWeapons()` 默认检查 `sinceSWUpdate >= minSWUpdateInterval`。
- `Submarkets.GENERIC_MILITARY` 为 `generic_military`。
- `Submarkets.LOCAL_RESOURCES` 为 `local_resources`。
- `Submarkets.SUBMARKET_BLACK` 为 `black_market`。
- `Submarkets.SUBMARKET_OPEN` 为 `open_market`。
- `Submarkets.SUBMARKET_STORAGE` 为 `storage`。

## 陷阱

- `BaseSubmarketPlugin.getCargoNullOk()` 可返回 null，读取 cargo 内容前需要区分 lazy 创建语义。
- `BaseSubmarketPlugin.getCargo()` 会初始化 mothballed ships，直接绕过该入口读取 cargo 字段会跳过初始化。
- `BaseSubmarketPlugin.reportPlayerMarketTransaction(...)` 只处理 commodity stack 对市场经济的 trade mod，非 commodity stack 没有该经济修饰路径。
- `BaseSubmarketPlugin.isBlackMarket()` 默认由 faction 敌对关系判断，具体 black market plugin 可覆写其它语义。
- `BaseSubmarketPlugin.isEnabled(ui)` 默认依赖 UI trade mode，不能只按子市场存在判定可交互。
- `BaseSubmarketPlugin.isIllegalOnSubmarket(stack, action)` 对非 commodity stack 默认合法，舰船合法性走 member 重载。
- `MarketAPI.addSubmarket(specId)` 使用 spec id，移除时需要配对使用同一 spec id。
- `SubmarketPlugin.createTooltip(...)` 使用 spec desc 并执行 rules token replacement，desc 中 token 的可用性受市场主实体上下文影响。
