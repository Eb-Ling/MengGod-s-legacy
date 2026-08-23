# 生涯实体

来源：原版 Starsector。

## 定义

生涯实体是原版生涯地图中承载可定位对象、轨道、感知、交互桥接和实体子类型的接口集合。

## 参考

- `com.fs.starfarer.api.campaign.CampaignTerrainAPI`：提供 campaign terrain 实体、terrain plugin、terrain type 和半径入口。
- `com.fs.starfarer.api.campaign.CustomCampaignEntityAPI`：提供 custom entity 半径、显示舰队和渲染层入口。
- `com.fs.starfarer.api.campaign.CustomCampaignEntityPlugin`：提供 custom entity 运行时插件入口。
- `com.fs.starfarer.api.campaign.CustomEntitySpecAPI`：提供 custom entity 规格读取入口。
- `com.fs.starfarer.api.campaign.JumpPointAPI`：提供跳跃目的地、目的地视觉、自动超空间入口和跳跃点开合入口。
- `com.fs.starfarer.api.campaign.NascentGravityWellAPI`：提供 nascent gravity well 目标和颜色入口。
- `com.fs.starfarer.api.campaign.OrbitAPI`：提供轨道焦点、轨道实体、推进、复制和当前位置计算入口。
- `com.fs.starfarer.api.campaign.PlanetAPI`：提供行星、恒星、星球类型、星体视觉规格和光照入口。
- `com.fs.starfarer.api.campaign.PlanetSpecAPI`：提供单个星体视觉规格的读取与改写入口。
- `com.fs.starfarer.api.campaign.RingBandAPI`：提供环带视觉实体的贴图、焦点、半径、宽度、颜色和螺旋状态入口。
- `com.fs.starfarer.api.campaign.SectorEntityToken`：提供生涯实体身份、坐标、轨道、location、memory、tag、感知、交互和跨系统桥接入口。

## 边界

- `CampaignTerrainAPI` 归属地形实体的 plugin、type 和半径状态。
- `CustomCampaignEntityAPI` 归属 custom entity 的半径、显示舰队和 active layers。
- `CustomCampaignEntityPlugin` 归属 custom entity 的运行时行为插件入口。
- `CustomEntitySpecAPI` 归属 custom entity 的规格读取入口。
- `JumpPointAPI` 归属跳跃目的地集合、目的地视觉、相关星体、wormhole 状态和开合视觉状态。
- `JumpPointAPI.JumpDestination` 归属单个跳跃目的地、交互标签和目标距离范围。
- `NascentGravityWellAPI` 归属 nascent gravity well 的目标实体和颜色覆盖。
- `OrbitAPI` 归属轨道焦点、轨道实体、轨道推进和当前位置计算。
- `PlanetAPI` 归属星体类型、恒星判定、气态巨行星判定、卫星判定、半径和光照覆盖。
- `PlanetSpecAPI` 归属单个星体实例的视觉规格改写。
- `RingBandAPI` 归属环带贴图、焦点实体、半径、宽度、颜色、螺旋参数和轨道天数。
- `SectorEntityToken` 归属实体身份、名称、派系、坐标、速度、朝向、半径、location、alive、expired 和 custom data。
- `SectorEntityToken` 的 ability 集合是实体到 ability 系统的挂载、查询和清理桥接入口。
- `SectorEntityToken` 的 cargo、market、active person 和 interaction image 是实体到对应系统的桥接入口。
- `SectorEntityToken` 的 drop value、drop random、discovery XP 和 salvage XP 是实体保存态入口。
- `SectorEntityToken` 的 memory、tag、script、floating text 和 light source 归属实体级运行时状态。
- `SectorEntityToken` 的 sensor strength、sensor profile、visibility level 和 transponder 状态归属实体感知入口。

## 链路

### 实体创建与注册链路

1. 代码通过 location 容器入口创建或取得实体。
2. 实体获得 id、name、faction、containing location 和基础坐标状态。
3. 代码按需要写入 market、active person、custom data、memory 或 tag。
4. 代码把实体加入 location 容器或读取已有 containing location。
5. 生涯推进时调用实体 `advance(amount)`。
6. 实体可通过 `isAlive()`、`isExpired()` 和 `getContainingLocation()` 表达容器持有状态。

### 坐标与轨道链路

1. 代码取得 `SectorEntityToken`。
2. 调用 `setFixedLocation(...)` 写入固定坐标，或调用 `setOrbit(...)` 写入 `OrbitAPI`。
3. 圆形轨道入口写入 orbit focus、angle、radius 和 period。
4. `OrbitAPI.setEntity(entity)` 绑定正在轨道运动的实体。
5. `OrbitAPI.advance(amount)` 推进轨道状态。
6. `OrbitAPI.computeCurrentLocation()` 或 `updateLocation()` 计算并写入当前坐标。

### 感知与可见性链路

1. 实体读取或写入 sensor strength、sensor profile 和 detected range modifier。
2. 调用 `getBaseSensorRangeToDetect(...)` 或 `getMaxSensorRangeToDetect(other)` 计算探测范围。
3. 调用 `isVisibleToSensorsOf(other)` 查询对指定实体的可见性。
4. 调用 `getVisibilityLevelToPlayerFleet()` 或 `getVisibilityLevelTo(other)` 读取可见等级。
5. 渲染或交互逻辑读取 sensor fader、sensor contact fader 和 indicator 状态。

### 桥接与脚本链路

1. 调用 `SectorEntityToken.addAbility(id)` 把 ability id 交给实体挂载入口。
2. 实体保存 ability id 到 plugin 的映射。
3. 调用 `getAbility(id)`、`hasAbility(id)` 或 `getAbilities()` 读取已挂载 ability plugin。
4. 调用 `removeAbility(id)` 移除 ability 挂载并触发 plugin 清理。
5. 调用 `addScript(script)` 注册实体脚本。
6. 实体推进时调用脚本，`removeScript(...)` 或 `removeScriptsOfClass(...)` 移除脚本。

### 星体视觉链路

1. 代码取得 `PlanetAPI`。
2. 调用 `getTypeId()`、`isStar()`、`isGasGiant()` 或 `isMoon()` 读取星体类型。
3. 调用 `getSpec()` 取得当前星体实例的视觉规格。
4. 代码修改 `PlanetSpecAPI` 字段。
5. 调用 `applySpecChanges()` 让视觉规格生效。
6. 调用 `changeType(type, random)` 改写星体类型。

### 跳跃点链路

1. 代码取得 `JumpPointAPI`。
2. 调用 `addDestination(new JumpDestination(target, label))` 写入目的地。
3. 调用 `setDestinationVisual(...)` 或标准 wormhole visual 方法设置目的地视觉。
4. 调用 `setRelatedPlanet(...)` 设置自动生成入口使用的关联星体。
5. 调用 `autoUpdateHyperJumpPointLocationBasedOnInSystemEntityAtRadius(...)` 更新超空间入口位置。
6. 调用 `getDestinations()` 读取目的地集合。
7. 调用 `open()`、`close()`、`forceOpen()` 或 `forceClose()` 改变跳跃点视觉状态。

### 子类型视觉链路

1. `CustomCampaignEntityAPI` 读取或写入 radius、fleet visual 和 active layers。
2. `CampaignTerrainAPI` 读取 terrain plugin、terrain type 和 radius。
3. `RingBandAPI` 读取或写入 focus、sprite、band width、middle radius、color 和 spiral 参数。
4. `NascentGravityWellAPI` 读取 target，并按需要写入 color override。
5. 视觉渲染读取各实体子类型暴露的当前状态。

## 规范

- `CampaignTerrainAPI.getPlugin()` 返回该地形实体的 terrain plugin。
- `CustomCampaignEntityAPI.setActiveLayers(...)` 写入 custom entity 的渲染层集合。
- `JumpPointAPI.addDestination(...)` 写入 `JumpDestination`，目的地对象包含 destination、label、min distance 和 max distance。
- `JumpPointAPI.clearDestinations()` 清空当前跳跃点目的地集合。
- `JumpPointAPI.forceOpen()` 和 `forceClose()` 跳过跳跃点动画。
- `JumpPointAPI.open()` 和 `close()` 只处理视觉状态。
- `OrbitAPI.makeCopy()` 返回轨道状态副本。
- `PlanetAPI.getSpec()` 返回当前星体实例可改写的视觉规格对象。
- `PlanetAPI.applySpecChanges()` 让 `getSpec()` 上的视觉改写生效。
- `RingBandAPI.getFocus()` 返回环带围绕的焦点实体。
- `SectorEntityToken.getContainingLocation()` 返回实体所在 location。
- `SectorEntityToken.getCustomPlugin()` 只在带插件的 custom campaign entity 上返回非空对象。
- `SectorEntityToken.isAlive()` 检查 containing location 及该 location 对实体的持有关系。
- `SectorEntityToken.setFixedLocation(...)` 写入实体固定坐标。
- `SectorEntityToken.setOrbit(...)` 写入实体轨道对象。

## 陷阱

- `JumpPointAPI.autoUpdateHyperJumpPointLocationBasedOnInSystemEntityAtRadius(...)` 要求跳跃点已加入星系且星系已存在超空间锚点。
- `JumpPointAPI.open()` 和 `close()` 只改变视觉表现。
- `LocationAPI.createToken(...)` 创建的 token 初始状态不在 location 实体集合中。
- `PlanetAPI.getSpec()` 改写后需要调用 `applySpecChanges()` 才会应用到视觉。
- `SectorEntityToken.isAlive()` 同时依赖 containing location 和 location 内实体集合。
- `SectorEntityToken.getCustomPlugin()` 在普通实体或无插件 custom entity 上返回 null。
- `SectorEntityToken.getMemory()` 会触发 fact 更新；只读当前实体 memory 使用 `getMemoryWithoutUpdate()`。
