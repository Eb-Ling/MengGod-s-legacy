# 人物

来源：原版 Starsector。

## 定义

人物是身份、关系、记忆、角色数值、officer、captain 和 important people 状态的原版接口集合。

## 参考

- `com.fs.starfarer.api.FactoryAPI.createOfficerData(...)`：创建包裹 `PersonAPI` 的 `OfficerDataAPI`。
- `com.fs.starfarer.api.FactoryAPI.createPerson()`：创建空白 `PersonAPI`。
- `com.fs.starfarer.api.characters.ImportantPeopleAPI`：维护全局 important people、位置、checkout 和 return 状态。
- `com.fs.starfarer.api.characters.MutableCharacterStatsAPI`：维护人物等级、XP、story points、skill level、角色动态 stat 和刷新入口。
- `com.fs.starfarer.api.characters.OfficerDataAPI`：维护 officer 包装、XP、升级和 skill picks。
- `com.fs.starfarer.api.characters.PersonAPI`：维护人物身份、派系、职位、记忆、关系、AI core、fleet、market 和 stats。
- `data/characters/person_names.csv`：定义随机人物姓名池、gender、usage 和 category。
- `data/characters/personalities.csv`：定义 personality id、显示名、描述、bravery 和舰队行为权重。

## 边界

- `FactoryAPI.createOfficerData(person)` 归属 `PersonAPI` 到 `OfficerDataAPI` 的包装创建。
- `FactoryAPI.createPerson()` 归属空白人物对象创建。
- `ImportantPeopleAPI` 归属全局 important people 集合、位置、checkout reason 和 return 状态。
- `MutableCharacterStatsAPI` 归属人物等级、XP、story points、skill level、动态 stat、admin 数、officer 数和 outpost 数。
- `OfficerDataAPI` 归属 officer 的 XP、升级、skill picks 和 person 包装状态。
- `PersonAPI` 归属人物 id、name、gender、rank、post、portrait、faction、relationship、memory、tags、voice、importance、AI core、fleet、market 和 stats。
- 人物 market 字段只表达当前市场归属，市场 roster 和 comm entry 集合由市场对象维护。

## 链路

### 人物数据加载链路

1. 游戏读取 `data/characters/person_names.csv`。
2. `name`、`gender`、`usage` 和 `category` 写入随机姓名池。
3. 游戏读取 `data/characters/personalities.csv`。
4. `id`、`bravery` 和行为权重写入 personality 数据。

### captain 链路

1. 调用 `FleetMemberAPI.setCaptain(person)` 写入舰队成员 captain。
2. 调用 `FleetDataAPI.getMemberWithCaptain(person)` 从舰队成员集合查找 captain 所在成员。
3. 部署进入战斗后 `ShipAPI.getCaptain()` 读取战斗舰船 captain。
4. 调用 `ShipAPI.getOriginalCaptain()` 读取部署初始 captain。
5. 调用 `ShipAPI.setCaptain(person)` 覆盖战斗运行时 captain。
6. 调用 `ShipAPI.getFleetCommander()` 读取战斗舰船关联的 fleet commander。

### important people 链路

1. 调用 `SectorAPI.getImportantPeople()` 取得全局 important people 集合。
2. 调用 `ImportantPeopleAPI.addPerson(person)` 注册人物。
3. 调用 `ImportantPeopleAPI.getData(person)` 或 `getData(id)` 读取 `PersonDataAPI`。
4. 调用 `PersonDataAPI.setLocation(location)` 写入人物位置。
5. 调用 `ImportantPeopleAPI.checkOutPerson(person, reasonId)` 写入 checkout reason。
6. 调用 `ImportantPeopleAPI.returnPerson(person, reasonId)` 归还人物。
7. 调用 `ImportantPeopleAPI.removePerson(person)` 或 `removePerson(id)` 移除人物。

### market 交接链路

1. 调用 `PersonAPI.setMarket(market)` 写入人物当前市场归属。
2. 市场对象将同一个人物加入 roster 集合。
3. 市场对象为同一个人物加入 comm entry。
4. 调用 `PersonAPI.getMarket()` 读取人物当前市场归属。
5. 移除可见联系人时先移除 comm entry。
6. 移除可见联系人时再移除市场 roster 中的人物对象。

### officer 链路

1. 调用 `Global.getFactory().createOfficerData(person)` 创建 `OfficerDataAPI`。
2. 调用 `FleetDataAPI.addOfficer(officer)` 或 `addOfficer(person)` 加入舰队 officer 集合。
3. 调用 `FleetDataAPI.getOfficersCopy()` 读取 officer 集合。
4. 调用 `OfficerDataAPI.addXP(...)` 增加 officer XP。
5. 调用 `OfficerDataAPI.makeSkillPicks(...)` 生成可选技能。
6. 调用 `OfficerDataAPI.levelUp(skillId, random)` 升级选择的技能。
7. 调用 `FleetDataAPI.removeOfficer(person)` 移除 officer。

### 人物创建链路

1. 调用 `Global.getFactory().createPerson()` 创建空白人物。
2. 调用 `PersonAPI.setId(id)` 写入人物 id。
3. 调用 `PersonAPI.setFaction(factionId)` 写入人物 faction。
4. 调用 `PersonAPI.setName(name)` 和 `setGender(gender)` 写入姓名与性别。
5. 调用 `PersonAPI.setRankId(rank)` 和 `setPostId(postId)` 写入 rank 与 post。
6. 调用 `PersonAPI.setPortraitSprite(portraitSprite)` 写入头像。
7. 调用 `PersonAPI.setPersonality(personality)` 写入 personality。
8. 调用 `PersonAPI.setImportanceAndVoice(importance, random)` 或 `setVoice(voice)` 写入 importance 与 voice。

### 角色 stats 链路

1. 调用 `PersonAPI.getStats()` 取得 `MutableCharacterStatsAPI`。
2. 调用 `MutableCharacterStatsAPI.setLevel(level)` 或 `addXP(...)` 写入等级与 XP。
3. 调用 `MutableCharacterStatsAPI.setSkillLevel(skillId, level)` 写入 skill level。
4. 调用 `MutableCharacterStatsAPI.getSkillLevel(skillId)` 或 `getSkillsCopy()` 读取 skill level。
5. 调用 `MutableCharacterStatsAPI.getDynamic()` 读取角色动态 stat。
6. 调用 `MutableCharacterStatsAPI.refreshCharacterStatsEffects(...)` 刷新角色 stats effects。
7. 调用 `PersonAPI.getFleetCommanderStats()` 读取 fleet commander stats。

## 规范

- `ImportantPeopleAPI.checkOutPerson(person, reasonId)` 写入 checkout reason。
- `ImportantPeopleAPI.getPerson(faction, market, checkoutReason, defaultRank, postIds...)` 可按 faction、market、post 和 checkout reason 选择人物。
- `MutableCharacterStatsAPI.getSkillLevel(id)` 只返回整数技能等级，类型为 float 仅用于调用便利。
- `MutableCharacterStatsAPI.isPlayerStats()` 区分玩家角色 stats。
- `MutableCharacterStatsAPI.setSkipRefresh(true)` 会影响角色 stats effect 刷新。
- `OfficerDataAPI.makeSkillPicks()` 会重掷 skill picks。
- `PersonAPI.getFleet()` 和 `setFleet(...)` 用于 officer 所在舰队。
- `PersonAPI.getMemory()` 会触发 memory fact 更新，纯读取使用 `getMemoryWithoutUpdate()`。
- `PersonAPI.isAICore()` 由 AI core id 状态表达。
- `PersonAPI.isDefault()` 表达默认人物。
- `PersonAPI.setImportanceAndVoice(importance, random)` 同时写入 importance 与基于 importance 的 voice。
- `data/characters/personalities.csv` 的原版 personality id 包含 `timid`、`cautious`、`steady`、`aggressive` 和 `reckless`。

## 陷阱

- `ImportantPeopleAPI.checkOutPerson(person, reasonId)` 未归还时会影响之后按条件选择人物。
- `MutableCharacterStatsAPI.setSkipRefresh(true)` 会让之后的角色 stats effect 刷新链路保持跳过状态。
- `OfficerDataAPI.makeSkillPicks()` 重复调用会重掷 picks，已展示给玩家的可选项需要维持同一对象状态。
- `PersonAPI.getMemory()` 会触发 memory fact 更新，读取持久状态时使用 `getMemoryWithoutUpdate()`。
- `PersonAPI.setMarket(market)` 只写人物字段，市场 roster 与 comm entry 需要由市场对象分别维护。
