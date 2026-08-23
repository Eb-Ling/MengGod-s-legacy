# 生涯产业

来源：原版 Starsector。

## 定义

生涯产业是市场产业、供需、收入维护、建设升级、AI core、特殊物品、改良和 disruption 的原版接口集合。

## 参考

- `com.fs.starfarer.api.campaign.SpecialItemData`:承载已安装特殊物品 id 与 data。
- `com.fs.starfarer.api.campaign.econ.Industry`:定义市场产业生命周期接口。
- `com.fs.starfarer.api.campaign.econ.MarketAPI`:承载 industry 增删与重算入口。
- `com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity`:承载单个商品供需数量修饰。
- `com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry`:提供产业公共基类。
- `com.fs.starfarer.api.loading.IndustrySpecAPI`:承载产业静态配置。
- `data/campaign/industries.csv`:注册产业表。

## 边界

- `BaseIndustry` 归属产业默认状态、供需 map、收入维护、disruption memory key。
- `BaseIndustry.apply(...)` 归属供需、收入、AI core、特殊物品、改良的写入顺序。
- `BaseIndustry.reapply()` 归属产业状态重写顺序。
- `BaseIndustry.unapply()` 归属 AI core、改良、移民、特殊物品的撤销顺序。
- `BaseIndustry.updateIncomeAndUpkeep()` 归属收入 stat 与维护费 stat 写入。
- `BaseIndustry.updateSupplyAndDemandModifiers()` 归属 AI core、改良、管理员、外部供需修饰汇总。
- `Industry` 归属市场产业对外契约。
- `IndustrySpecAPI` 承载 plugin class、tags、cost、buildTime、income。
- `MutableCommodityQuantity` 归属供需数量的 `MutableStat` 修饰，商品 legality 与市场价格结算由商品和市场价格接口维护。
- `SpecialItemData` 归属已安装特殊物品 id 与 data，特殊物品效果由 installable item effect 链路维护。

## 链路

### CSV 加载链路

1. 游戏读取 `data/campaign/industries.csv`。
2. `id` 列注册产业 spec key。
3. `plugin` 列写入 `IndustrySpecAPI.getPluginClass()`。
4. 其它参数写入 `IndustrySpecAPI` 对应字段。
5. 游戏创建 `IndustrySpecAPI`。
6. `MarketAPI.instantiateIndustry(id)` 或 `MarketAPI.addIndustry(...)` 读取 `IndustrySpecAPI`。
7. `IndustrySpecAPI.getNewPluginInstance(market)` 创建 `Industry`。
8. 进入产业实例链路。

### 产业实例链路

1. 调用 `MarketAPI.addIndustry(id)` 或 `MarketAPI.addIndustry(id, params)`。
2. 市场读取 `IndustrySpecAPI`。
3. 市场通过 `instantiateIndustry(id)` 创建 `Industry`。
4. 产业调用 `init(id, market)`。
5. 带参数加载时调用 `initWithParams(params)`。
6. 产业调用 `apply()`。
7. `BaseIndustry.apply(...)` 调用 `updateSupplyAndDemandModifiers()`。
8. `BaseIndustry.apply(...)` 调用 `updateIncomeAndUpkeep()`。

### 供需链路

1. 产业在 `apply()` 中调用 `supply(...)` 或 `demand(...)`。
2. `BaseIndustry` 通过 `getSupply(commodityId)` 或 `getDemand(commodityId)` 取得 `MutableCommodityQuantity`。
3. `MutableCommodityQuantity.getQuantity()` 接收 flat 修饰。
4. `updateSupplyAndDemandModifiers()` 重置供给 bonus 与需求 reduction。
5. `updateSupplyAndDemandModifiers()` 汇总 AI core、改良、管理员 dynamic stats 和外部修饰。
6. `getMaxDeficit(...)`、`getAllDeficit(...)` 读取需求缺口。
7. `applyDeficitToProduction(...)` 按缺口写入负供给修饰。

### 建设升级链路

1. 调用 `startBuilding()` 设置 building、buildProgress、upgradeId 和 buildTime。
2. `startBuilding()` 调用 `unapply()`。
3. 每帧 `advance(amount)` 将秒数转换为 campaign days。
4. buildProgress 达到 buildTime 后调用 `finishBuildingOrUpgrading()`。
5. 初次建设完成后调用 `buildingFinished()`。
6. 初次建设完成后调用 `reapply()`。
7. 调用 `startUpgrading()` 设置 upgradeId 与升级 buildTime。
8. 升级完成后调用 `MarketAPI.removeIndustry(id, null, true)`。
9. 升级完成后调用 `MarketAPI.addIndustry(upgradeId)`。
10. 升级完成后迁移 AI core 与改良状态并调用新产业 `reapply()`。
11. 调用 `downgrade()` 设置 downgrade id 并进入 `finishBuildingOrUpgrading()`。

### AI core / 特殊物品 / 改良链路

1. 调用 `setAICoreId(aiCoreId)` 写入产业 AI core id。
2. `apply()` 调用 `applyAICoreModifiers()`。
3. `updateSupplyAndDemandModifiers()` 调用 AI core 供需修饰。
4. `updateIncomeAndUpkeep()` 调用 AI core 收入维护修饰。
5. 调用 `setSpecialItem(special)` 写入特殊物品。
6. `setSpecialItem(...)` 先撤销旧特殊物品 effect。
7. `apply()` 查询特殊物品 effect 的 unmet requirements。
8. unmet requirements 为空时调用 effect `apply(this)`。
9. unmet requirements 存在时调用 effect `unapply(this)`。
10. 调用 `setImproved(improved)` 写入改良状态。
11. `apply()` 调用 `applyImproveModifiers()`。
12. `updateSupplyAndDemandModifiers()` 调用改良供需修饰。

### Disruption 链路

1. 调用 `setDisrupted(days, useMax)`。
2. `canBeDisrupted()` 返回 true 后继续。
3. `getDisruptedKey()` 生成 market memory key。
4. `useMax` 为 true 时读取 `market.getMemoryWithoutUpdate().getExpire(key)`。
5. 写入天数取现有剩余天数与传入 days 的较大值。
6. 写入天数小于等于 0 时调用 `memory.unset(key)`。
7. 写入天数大于 0 时调用 `memory.set(key, true, dur)`。
8. 从未 disrupted 变为 disrupted 时调用 `notifyDisrupted()`。
9. `isDisrupted()` 读取 market memory。
10. `getDisruptedDays()` 读取 market memory expire。
11. `advance(amount)` 发现 disruption 结束后调用 `disruptionFinished()`。

## 规范

- `BaseIndustry.getDisruptedKey()` 使用产业类名生成 market memory key。
- `BaseIndustry.isFunctional()` 在 disrupted 时返回 false。
- `BaseIndustry.setDisrupted(days, useMax)` 在 `useMax=true` 时保留现有剩余天数与新天数中的较大值。
- `BaseIndustry.startBuilding()` 设置 building 状态后调用 `unapply()`。
- `BaseIndustry.updateIncomeAndUpkeep()` 按产业 spec、市场规模、收入倍率和维护费倍率写入 stat。
- `BaseIndustry.updateSupplyAndDemandModifiers()` 重置并汇总 supply bonus 与 demand reduction。
- `Industry.reapply()` 语义为先 `unapply()` 再 `apply()`。
- `MarketAPI.addIndustry(id, params)` 支持加载 AI core 与特殊物品参数。
- `MarketAPI.getIndustry(id)` 返回 in-system industry。
- `MarketAPI.reapplyIndustries()` 归属市场产业重算入口。
- `MarketAPI.removeIndustry(id, mode, forUpgrade)` 的 `mode` 可为 null。
- `SpecialItemData` 安装后由产业 apply 链路应用对应 installable item effect。

## 陷阱

- `BaseIndustry.getDisruptedKey()` 使用产业类名，同一 Java 类的多个产业共享 disruption key 语义。
- `BaseIndustry.setSpecialItem(...)` 会撤销旧特殊物品 effect，直接替换字段会跳过撤销链路。
- `BaseIndustry.startBuilding()` 会调用 `unapply()`，建设中的初始产业功能状态由 `isFunctional()` 判定。
- `Industry.initWithParams(params)` 用于经济数据加载特殊物品和 AI core，参数顺序和可识别 id 会影响安装结果。
- `MarketAPI.getIndustry(id)` 返回 in-system industry，调用方需要按原版语义区分市场当前状态与外部短缺影响。
- `MarketAPI.removeIndustry(id, null, true)` 是升级完成链路的一部分，升级迁移 AI core 与改良状态后需要重算新产业。
