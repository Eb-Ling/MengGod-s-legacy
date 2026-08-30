# 生涯市场

来源：原版 Starsector。

## 定义

生涯市场是市场对象、经济集合、市场实体、商品价格、人物名册、人口移民和市场状态的原版接口集合。

## 参考

- `com.fs.starfarer.api.FactoryAPI.createMarket(...)`：创建 `MarketAPI` 实例。
- `com.fs.starfarer.api.campaign.CommDirectoryAPI`：提供市场通信目录 entry 的添加、移除和查询入口。
- `com.fs.starfarer.api.campaign.SectorAPI.getEconomy()`：提供全局 economy 入口。
- `com.fs.starfarer.api.campaign.SectorEntityToken.getMarket()/setMarket(...)`：提供实体到市场的归属读写入口。
- `com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI`：定义市场内单个商品的库存、供需和交易修饰。
- `com.fs.starfarer.api.campaign.econ.EconomyAPI`：提供市场集合、市场查询和经济重算入口。
- `com.fs.starfarer.api.campaign.econ.MarketAPI`：定义市场对象接口。
- `com.fs.starfarer.api.campaign.econ.MarketDemandAPI`：定义市场需求数值、基础商品和库存效用。
- `com.fs.starfarer.api.impl.campaign.population.PopulationComposition`：承载人口 composition 与权重。

## 边界

- `CommDirectoryAPI` 归属市场通信目录 entry 集合，不等同于 `MarketAPI.getPeopleCopy()` 的人物列表。
- `CommodityOnMarketAPI` 归属市场内商品库存、供需和交易修饰。
- `EconomyAPI` 归属全局市场集合、市场查询和经济重算。
- `FactoryAPI.createMarket(...)` 归属市场对象创建，注册到经济集合由 `EconomyAPI.addMarket(...)` 完成。
- `MarketAPI` 归属市场身份、实体绑定、商品价格、人物人口和市场状态。
- `MarketAPI.getConditions()` 只作为市场持有 condition 集合的入口，condition token、plugin 生命周期和 suppression 由 condition 语义维护。
- `MarketAPI.getIndustries()` 只作为市场持有 industry 集合的入口，industry 生命周期、供需、建设和 disruption 由 industry 语义维护。
- `MarketDemandAPI` 归属 demand stat、基础商品和库存效用。
- `MarketImmigrationModifier` 集合归属市场人口迁入修饰，modifier 自身逻辑由实现类维护。
- `PopulationComposition` 归属人口 composition、权重和迁入人口状态。

## 链路

### 市场创建与注册链路

1. 调用 `Global.getFactory().createMarket(id, name, size)` 创建 `MarketAPI`。
2. 调用 `MarketAPI.setFactionId(factionId)` 设置市场势力。
3. 调用 `MarketAPI.setPrimaryEntity(entity)` 设置主实体。
4. 调用 `MarketAPI.setSurveyLevel(level)`、`setPlayerOwned(...)`、`setFreePort(...)` 等写入初始市场状态。
5. 调用 `EconomyAPI.addMarket(market, withJunkAndChatter)` 注册到经济集合。
6. 调用 `SectorEntityToken.setMarket(market)` 绑定实体到市场。

### 市场读取链路

1. 调用 `Global.getSector()`。
2. 调用 `SectorAPI.getEconomy()` 取得 `EconomyAPI`。
3. 按 id 调用 `EconomyAPI.getMarket(id)`。
4. 按全集调用 `EconomyAPI.getMarketsCopy()`。
5. 按 location 调用 `EconomyAPI.getMarkets(loc)`。
6. 按 group 调用 `EconomyAPI.getMarketsInGroup(group)` 或 `getMarketsWithSameGroup(market)`。
7. 返回值作为 `MarketAPI` 读取市场状态。

### 实体归属链路

1. `MarketAPI.getPrimaryEntity()` 返回市场主实体。
2. `MarketAPI.getConnectedEntities()` 返回关联实体集合。
3. `MarketAPI.getPlanetEntity()` 返回行星实体。
4. `MarketAPI.getStarSystem()` 返回所在星系。
5. `MarketAPI.getContainingLocation()` 返回所在 location。
6. `MarketAPI.getLocationInHyperspace()` 返回 hyperspace 坐标。
7. `MarketAPI.getLocation()` 读取同一 hyperspace 坐标。
8. `MarketAPI.isInHyperspace()` 判断市场位置归属。

### 商品与价格链路

1. 调用 `MarketAPI.getAllCommodities()` 或 `getCommoditiesCopy()` 读取商品集合。
2. 调用 `MarketAPI.getCommodityData(commodityId)` 读取单个 `CommodityOnMarketAPI`。
3. 调用 `CommodityOnMarketAPI.getStockpile()`、`getAvailable()`、`getMaxSupply()`、`getMaxDemand()` 读取库存与供需。
4. 调用 `MarketAPI.getDemand(demandClass)` 或 `getDemandWithTag(tag)` 读取需求。
5. 调用 `MarketAPI.getTariff()` 读取市场 tariff stat。
6. 调用 `MarketAPI.getSupplyPrice(...)` 读取市场卖出价格。
7. 调用 `MarketAPI.getDemandPrice(...)` 读取市场买入价格。
8. 调用 `MarketAPI.updatePrices()` 更新商品价格。
9. 调用 `MarketAPI.updatePriceMult()` 按稳定度更新本地价格倍率。

### 人物与通信链路

1. 调用 `MarketAPI.addPerson(person)` 添加市场人物。
2. 调用 `PersonAPI.setMarket(market)` 写入人物市场归属。
3. 调用 `MarketAPI.getCommDirectory().addPerson(person)` 添加通信目录 entry。
4. 调用 `MarketAPI.getPeopleCopy()` 读取市场人物列表。
5. 调用 `MarketAPI.getAdmin()` 读取管理员。
6. 调用 `MarketAPI.setAdmin(person)` 设置管理员。
7. 调用 `CommDirectoryAPI.getEntryForPerson(person)` 或 `getEntryForPerson(personId)` 读取通信 entry。
8. 调用 `CommDirectoryAPI.removePerson(person)` 移除人物通信 entry。
9. 调用 `MarketAPI.removePerson(person)` 移除市场人物。

### 人口与移民链路

1. 调用 `MarketAPI.getPopulation()` 读取当前人口 composition。
2. 调用 `MarketAPI.setPopulation(population)` 写入当前人口 composition。
3. 调用 `MarketAPI.getIncoming()` 读取迁入人口 composition。
4. 调用 `MarketAPI.setIncoming(incoming)` 写入迁入人口 composition。
5. 调用 `PopulationComposition.updateWeight()` 更新 composition 权重。
6. 调用 `MarketAPI.addImmigrationModifier(mod)` 添加可存档移民修饰。
7. 调用 `MarketAPI.addTransientImmigrationModifier(mod)` 添加临时移民修饰。
8. 调用 `MarketAPI.getAllImmigrationModifiers()` 读取全部移民修饰。
9. 调用 `MarketAPI.setImmigrationIncentivesOn(...)` 写入移民激励状态。
10. 调用 `MarketAPI.setFreePort(...)` 和 `setImmigrationClosed(...)` 写入开放状态。

### 市场状态链路

1. 调用 `MarketAPI.getStability()` 或 `getStabilityValue()` 读取稳定度。
2. 调用 `MarketAPI.getHazard()` 或 `getHazardValue()` 读取危险度。
3. 调用 `MarketAPI.getSurveyLevel()` 或 `setSurveyLevel(...)` 读取和写入 survey 状态。
4. 调用 `MarketAPI.isPlayerOwned()` 或 `setPlayerOwned(...)` 读取和写入玩家拥有状态。
5. 调用 `MarketAPI.isHidden()` 或 `setHidden(...)` 读取和写入隐藏状态。
6. 调用 `MarketAPI.getEconGroup()` 或 `setEconGroup(...)` 读取和写入经济 group。
7. 调用 `MarketAPI.isInvalidMissionTarget()` 或 `setInvalidMissionTarget(...)` 读取和写入任务目标状态。
8. 调用 `MarketAPI.getMemoryWithoutUpdate()` 读取市场 memory。
9. 调用 `MarketAPI.addTag(...)`、`removeTag(...)`、`getTags()` 维护市场 tag。
10. 生涯推进调用 `MarketAPI.advance(amount)` 推进市场状态。

## 规范

- `CommDirectoryAPI.removePerson(person)` 移除该人物关联的全部通信 entry。
- `CommodityOnMarketAPI.addTradeMod(...)` 可写入正负市场经济单位变化。
- `CommodityOnMarketAPI.addTradeModMinus(...)` 只写入负向市场经济单位变化。
- `CommodityOnMarketAPI.addTradeModPlus(...)` 只写入正向市场经济单位变化。
- `CommodityOnMarketAPI.getCombinedTradeModQuantity()` 合并 tradeMod、正向 mod 和负向 mod。
- `EconomyAPI.addUpdateListener(...)` 使用 economy update listener 集合。
- `EconomyAPI.doubleStep()` 等同调用 `nextStep()` 两次。
- `EconomyAPI.nextStep()` 只作为 UI 交互使用的经济重算入口。
- `EconomyAPI.tripleStep()` 等同调用 `nextStep()` 三次。
- `FactoryAPI.createMarket(id, name, size)` 只创建市场对象，经济集合注册由 `EconomyAPI.addMarket(...)` 完成。
- `MarketAPI.getHazardValue()` 中 `1f` 表示 `100%`。
- `MarketAPI.getIndustry(id)` 返回 in-system industry；市场文档只记录 market 持有 industry 集合入口。
- `MarketAPI.getLocation()` 等同 `getLocationInHyperspace()`。
- `MarketAPI.getStabilityValue()` 返回 `0` 到 `10` inclusive 的稳定度值。
- `MarketAPI.getTariff()` 返回市场 tariff 的 `MutableStat`。
- `MarketAPI.setCachedFaction(...)` 只用于 faked-up market 的 transient faction。
- `MarketAPI.setHidden(true)` 会使市场退出任务、事件、Intel 间接揭示链路，并退出经济参与。
- `MarketAPI.setPrimaryEntity(entity)` 写入市场主实体，实体到市场的绑定仍由 `SectorEntityToken.setMarket(market)` 表达。
- `MarketAPI.setAdmin(admin)` 会移除旧管理员的市场归属和通信目录 entry。

## 陷阱

- `CommDirectoryAPI` 和 `MarketAPI.getPeopleCopy()` 是两个集合边界，添加或移除可见联系人时需要分别维护人物列表与通信 entry。
- `EconomyAPI.nextStep()`、`doubleStep()` 和 `tripleStep()` 注释要求用于 UI 交互，后台脚本调用会承担单帧耗时风险。
- `MarketAPI.getLocation()` 是 `getLocationInHyperspace()` 的别名，不表示市场在当前星系内的本地坐标。
- `MarketAPI.getMemory()` 会触发 memory fact 更新，纯读取市场 memory 时使用 `getMemoryWithoutUpdate()`。
- `MarketAPI.setCachedFaction(...)` 是 faked-up market 的 transient 入口，常规市场 faction 应通过 `setFactionId(...)` 表达。
- `MarketAPI.setHidden(true)` 同时影响任务、事件、Intel 间接揭示链路和经济参与。
- `PersonAPI.setMarket(market)` 只表达人物归属市场，不会自动创建通信目录 entry。
- `SectorEntityToken.setMarket(market)` 只表达实体归属市场，经济集合注册仍由 `EconomyAPI.addMarket(...)` 表达。
