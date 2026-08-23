# 生涯市场条件

来源：原版 Starsector。

## 定义

生涯市场条件是市场 condition 实例、plugin 生命周期、tooltip、survey、suppression、重算和保存语义的原版接口集合。

## 参考

- `com.fs.starfarer.api.campaign.econ.MarketAPI`:承载市场 condition 增删与重算入口。
- `com.fs.starfarer.api.campaign.econ.MarketConditionAPI`:定义单实例 condition 接口。
- `com.fs.starfarer.api.campaign.econ.MarketConditionPlugin`:定义 condition plugin 生命周期接口。
- `com.fs.starfarer.api.characters.MarketConditionSpecAPI`:承载 condition 静态配置。
- `com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin`:提供 condition plugin 公共基类。
- `com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin2`:提供带 daysActive 的可存档 condition plugin 基类。
- `data/campaign/market_conditions.csv`:注册市场 condition 表。

## 边界

- `BaseMarketConditionPlugin` 归属 market 绑定、token 替换、tooltip 工具。
- `BaseMarketConditionPlugin2` 归属可存档 condition plugin 基类。
- `MarketAPI.addCondition(...)` 归属 condition 实例创建和 token 返回。
- `MarketAPI.getConditions()` 归属市场当前 condition 列表读取。
- `MarketAPI.getFirstCondition(id)` 归属同 spec id condition 的首个实例读取。
- `MarketAPI.getSpecificCondition(token)` 归属按唯一 token 读取具体 condition 实例。
- `MarketAPI.reapplyCondition(token)` 与 `reapplyConditions()` 归属 condition plugin 重算入口。
- `MarketAPI.removeCondition(id)` 归属同 spec id condition 实例集合移除。
- `MarketAPI.removeSpecificCondition(token)` 归属具体 condition 实例移除。
- `MarketAPI.suppressCondition(id)` 与 `unsuppressCondition(id)` 归属 suppressed condition id 集合维护。
- `MarketConditionAPI` 归属 spec id、唯一 token、survey 状态。
- `MarketConditionPlugin` 归属 condition plugin 生命周期接口。
- `MarketConditionSpecAPI` 承载 desc、icon、order、script class。

## 链路

### CSV 加载链路

1. 游戏读取 `data/campaign/market_conditions.csv`。
2. `id` 列注册 market condition spec key。
3. `script` 列写入 `MarketConditionSpecAPI.getScriptClass()` 对应的 plugin 类名。
4. 其它参数写入 `MarketConditionSpecAPI` 对应字段。
5. 游戏创建 `MarketConditionSpecAPI`。
6. `MarketAPI.addCondition(...)` 读取 `MarketConditionSpecAPI`。
7. 市场创建 `MarketConditionAPI` 并实例化 `MarketConditionPlugin`。
8. 进入 condition 添加与 token 链路。

### Condition 添加与 token 链路

1. 调用 `MarketAPI.addCondition(id)` 或 `MarketAPI.addCondition(id, param)`。
2. 市场创建 `MarketConditionAPI`。
3. 市场创建并绑定 `MarketConditionPlugin`。
4. 方法返回该实例的唯一 token。
5. 调用 `MarketAPI.getSpecificCondition(token)` 读取具体实例。
6. 调用 `MarketAPI.removeSpecificCondition(token)` 移除具体实例。
7. 调用 `MarketAPI.removeCondition(id)` 移除同 spec id 的全部实例。

### Condition plugin 生命周期链路

1. 市场创建 condition plugin。
2. 调用 `init(market, condition)` 绑定 market 与 condition。
3. 带参数添加时调用 `setParam(param)`。
4. 调用 `apply(id)` 写入市场状态。
5. 生涯推进时按 `runWhilePaused()` 判定暂停推进。
6. 调用 `advance(amount)` 推进 plugin。
7. 移除或重算时调用 `unapply(id)`。

### Tooltip 与 token 替换链路

1. UI 调用 `hasCustomTooltip()` 与 `isTooltipExpandable()`。
2. UI 读取 `getTooltipWidth()`。
3. UI 调用 `createTooltip(tooltip, expanded)`。
4. `BaseMarketConditionPlugin` 读取 `condition.getSpec().getDesc()`。
5. `getTokenReplacements()` 返回 player、market、faction、system 和 player fleet token。
6. tooltip 描述执行 token 替换。
7. `getHighlights()` 与 `getHighlightColors()` 写入高亮文本和颜色。
8. 调用 `createTooltipAfterDescription(tooltip, expanded)` 扩展正文。

### Survey / planetary / suppression 链路

1. `MarketConditionAPI.isSurveyed()` 读取 condition survey 状态。
2. `MarketConditionAPI.setSurveyed(...)` 写入 condition survey 状态。
3. `MarketConditionAPI.requiresSurveying()` 判定该 condition 是否需要 survey。
4. `MarketConditionAPI.isPlanetary()` 调用 plugin 的 planetary 语义。
5. `BaseMarketConditionPlugin.isPlanetary()` 读取 condition spec 的 planetary 标记。
6. `MarketAPI.suppressCondition(id)` 写入 suppressed condition id 集合。
7. `MarketAPI.isConditionSuppressed(id)` 读取 suppression 状态。
8. `MarketAPI.unsuppressCondition(id)` 移除 suppression 状态。

### Condition 重算链路

1. 调用 `MarketAPI.reapplyCondition(token)` 重算单个具体实例。
2. 市场对该 token 对应 plugin 调用 `unapply(id)`。
3. 市场对该 token 对应 plugin 调用 `apply(id)`。
4. 调用 `MarketAPI.reapplyConditions()` 重算市场全部 condition。
5. 市场按 condition 集合逐个执行 unapply/apply 重算。

## 规范

- `BaseMarketConditionPlugin.getHighStabilityBonusMult(market)` 返回倍率，调用方负责把该倍率用于高稳定度 bonus。
- `BaseMarketConditionPlugin.getHighStabilityPenaltyMult(market)` 返回倍率，调用方负责把该倍率用于高稳定度 penalty。
- `BaseMarketConditionPlugin.getLowStabilityBonusMult(market)` 返回倍率，调用方负责把该倍率用于低稳定度 bonus。
- `BaseMarketConditionPlugin.getLowStabilityPenaltyMult(market)` 返回倍率，调用方负责把该倍率用于低稳定度 penalty。
- `BaseMarketConditionPlugin.getModId()` 读取 condition 的 plugin modification id，condition 写入市场修饰时用该 id 隔离单实例 modifier。
- `BaseMarketConditionPlugin.isTransient()` 使该基类的 condition plugin 状态按 transient plugin 处理。
- `BaseMarketConditionPlugin.runWhilePaused()` 使 condition plugin 的 `advance(amount)` 只按非暂停生涯推进调用。
- `BaseMarketConditionPlugin2.advance(amount)` 将秒数转换为 campaign days，计时一般以 campaign days 为单位。
- `BaseMarketConditionPlugin2.isTransient()` 使该基类的 condition plugin 实例进入存档。
- `MarketAPI.addCondition(id)` 以 spec id 创建 condition 实例并返回 token，后续具体实例操作应使用该 token 定位。
- `MarketAPI.addCondition(id, param)` 以 spec id 创建 condition 实例、向 plugin 传入 param 并返回具体实例 token。
- `MarketAPI.removeCondition(id)` 以 spec id 移除同 id 全部 condition，多实例场景会同时移除该 spec id 下的实例集合。
- `MarketAPI.removeSpecificCondition(token)` 只移除 token 对应的具体 condition 实例。
- `MarketConditionAPI.getIdForPluginModifications()` 返回该实例全局唯一 id，plugin modifier 写入应使用该 id 区分同 spec id 多实例。
- `MarketConditionAPI.isPlanetary()` 通过 plugin 判定 planetary 分类，condition 的 planetary 语义由 plugin 实现承担。

## 陷阱

- `BaseMarketConditionPlugin.createTooltip(...)` 会执行 token 替换，tooltip 文本里的 `$` token 需要匹配 `getTokenReplacements()`。
- `BaseMarketConditionPlugin2` 会进入存档，批量使用会增加存档状态。
- `MarketAPI.addCondition(id)` 返回的是具体实例 token，`removeCondition(id)` 移除同 id 的全部实例。
- `MarketAPI.getFirstCondition(id)` 读取同 spec id 的首个实例，同 id 多实例场景需要使用 token 定位。
- `MarketAPI.suppressCondition(id)` 维护的是 suppressed id 集合，恢复显示需要配对调用 `unsuppressCondition(id)`。
- `MarketConditionAPI.getIdForPluginModifications()` 与 spec id 语义不同，plugin modifier id 用于单实例修饰。
