# LazyLib API

## 定义

LazyLib API 是硬依赖外部库提供的公共工具层，用于数学与几何计算、战斗和战役查询、数据读取、mod 检测和轻量实体封装。

## 参考

- `data/console/commands.csv`：注册 LazyLib 自带的 Console Commands 测试命令入口。
- `jars/LazyLib.jar!org.lazywizard.lazylib.CollectionUtils`：提供集合合并、过滤、拼接以及按距离排序的 comparator。
- `jars/LazyLib.jar!org.lazywizard.lazylib.CollisionUtils`：提供线段、实体 bounds、collision circle 与 segment 的碰撞查询。
- `jars/LazyLib.jar!org.lazywizard.lazylib.EllipseUtils`：提供椭圆取点、随机点和点包含判定。
- `jars/LazyLib.jar!org.lazywizard.lazylib.FastTrig`：提供以 radians 为输入的快速三角函数近似。
- `jars/LazyLib.jar!org.lazywizard.lazylib.IOUtils`：提供按路径读取完整 byte array 的文件工具。
- `jars/LazyLib.jar!org.lazywizard.lazylib.JSONUtils`：提供 `JSONObject` 清理、`JSONArray` 到 `Color` 转换和 common JSON 读取封装。
- `jars/LazyLib.jar!org.lazywizard.lazylib.LazyLib`：提供库 mod id、版本、设置读取、缓存开关、日志级别和弃用 API 记录入口。
- `jars/LazyLib.jar!org.lazywizard.lazylib.MathUtils`：提供距离、角度归一、最短旋转、圆形取点、随机数和范围判定。
- `jars/LazyLib.jar!org.lazywizard.lazylib.ModUtils`：提供 class presence、mod enabled、enabled mod id 和 override 列表查询。
- `jars/LazyLib.jar!org.lazywizard.lazylib.ShapeUtils`：提供 circle、ellipse、arc 的顶点数组构造。
- `jars/LazyLib.jar!org.lazywizard.lazylib.StringUtils`：提供固定行宽换行和字符串缩进。
- `jars/LazyLib.jar!org.lazywizard.lazylib.VectorUtils`：提供 facing、angle、directional vector、resize、clamp length、rotate 和数组转换。
- `jars/LazyLib.jar!org.lazywizard.lazylib.campaign.CampaignUtils`：提供战役舰队、势力关系、星系实体、tag 和 faction 查询。
- `jars/LazyLib.jar!org.lazywizard.lazylib.campaign.CargoUtils`：提供 cargo stack、mothballed ship、commodity、crew、fuel、supply 和 weapon 空间统计与移动。
- `jars/LazyLib.jar!org.lazywizard.lazylib.campaign.MessageUtils`：提供 campaign UI message 输出封装。
- `jars/LazyLib.jar!org.lazywizard.lazylib.campaign.orbits.EllipticalOrbit`：提供基于 focus、宽高、角度和周期的椭圆 OrbitAPI 实现。
- `jars/LazyLib.jar!org.lazywizard.lazylib.combat.AIUtils`：提供最近敌我舰船、导弹、目标点、地图实体列表和拦截点查询。
- `jars/LazyLib.jar!org.lazywizard.lazylib.combat.CombatUtils`：提供战斗实体范围查询、可见性查询、舰船直接生成和屏幕坐标转换。
- `jars/LazyLib.jar!org.lazywizard.lazylib.combat.DefenseUtils`：提供装甲格、装甲伤害、船体伤害和点位防御类型查询。
- `jars/LazyLib.jar!org.lazywizard.lazylib.combat.WeaponUtils`：提供武器弧内目标查询、瞄准时间估算和转向控制。
- `jars/LazyLib.jar!org.lazywizard.lazylib.combat.entities.AnchoredEntity`：提供跟随 anchor 的 CombatEntityAPI wrapper。
- `jars/LazyLib.jar!org.lazywizard.lazylib.combat.entities.SimpleEntity`：提供基于 location、weapon 或 engine 的 CombatEntityAPI wrapper。
- `jars/LazyLib.jar!org.lazywizard.lazylib.opengl.ColorUtils`：提供 `Color` 到 OpenGL color state 的写入和随机颜色生成。
- `jars/LazyLib.jar!org.lazywizard.lazylib.opengl.DrawUtils`：提供 immediate-mode circle、ellipse 和 arc 绘制。
- `jars/LazyLib.jar!org.lazywizard.lazylib.ui.LazyFont`：提供 bitmap font 加载、文本测量、换行、`DrawableString` 创建、绘制和释放。
- `lazylib_settings.json`：提供 `enableCaching`、`logLevel`、`logDeprecated` 和 `crashOnDeprecated` 的库级设置来源。
- `https://github.com/LazyWizard/lazylib`：Github 源码仓库。

## 边界

- `AIUtils` 和 `CombatUtils` 拥有战斗引擎当前帧查询语义，返回对象仍归 Starsector combat engine 管理。
- `AnchoredEntity` 和 `SimpleEntity` 拥有轻量 `CombatEntityAPI` wrapper 语义，anchor、weapon、engine 和 location 来源由构造参数决定。
- `CampaignUtils` 拥有战役 token、fleet、tag、faction 和 reputation 查询语义，sector、location 和 faction state 仍归 Starsector campaign engine 管理。
- `CargoUtils` 拥有 cargo 内容移动和空间统计语义，stack、mothballed ship 和 commodity 的最终容器状态由传入 `CargoAPI` 或 `SectorEntityToken` 决定。
- `CollisionUtils` 拥有 bounds、collision radius、segment 和 line intersection 查询语义，damage application 与 hit resolution 仍归 Starsector combat engine 管理。
- `DefenseUtils` 拥有装甲格、装甲伤害、船体伤害和点位 defense type 读取语义，防御数值写入仍归 Starsector ship state 管理。
- `IOUtils` 和 `JSONUtils` 拥有文件与 JSON 读取封装语义，配置 schema、默认值含义和业务解释由调用方所属模块定义。
- `LazyFont` 拥有 bitmap font、text layout、drawable rebuild 和 OpenGL 资源释放语义，文本内容和 UI 生命周期由调用方管理。
- `LazyLib` 拥有库级 mod id、版本、缓存、日志和弃用 API 策略语义，设置文件键名是该库的外部配置协议。
- `MathUtils`、`VectorUtils`、`EllipseUtils`、`ShapeUtils` 和 `FastTrig` 拥有无持久状态的数学与几何工具语义，坐标系和单位由方法签名约定。
- `ModUtils` 拥有 classpath、enabled mod id 和 override 查询语义，加载顺序和 mod 列表来源归 Starsector runtime。
- `OpenGL` 相关工具拥有 immediate drawing 与 color state 写入语义，调用上下文、matrix、blend 和 render layer 由调用方保证。
- `WeaponUtils` 拥有武器 arc、aiming time 和 turn-to-point 工具语义，开火权限、弹药、目标选择策略和 weapon AI 状态仍归 Starsector weapon state 管理。
- `data/console/commands.csv` 中的 LazyLib 命令属于 LazyLib 自带测试命令数据入口，命令执行协议归 Console Commands 外部 mod。
- `org.lazywizard.lazylib.campaign.MarketUtils` 在源码中是包内 class，正式消费边界以 public class 和 public method 为准。

## 链路

### Cargo 移动

- 调用方取得 `CargoStackAPI`、源 `CargoAPI`、目标 `CargoAPI` 或目标 `SectorEntityToken`。
- `CargoUtils.moveStack(...)`、`moveCargo(...)` 或 `moveMothballedShips(...)` 读取源容器内容。
- LazyLib 将匹配内容加入目标容器。
- LazyLib 从源容器移除已迁移内容。

### JSON 与 common data 读取

- 调用方提供 JSON 文件名和可选默认 JSON 路径。
- `JSONUtils.loadCommonJSON(...)` 打开 common data JSON 源。
- LazyLib 返回实现 `AutoCloseable` 的 `CommonDataJSONObject`。
- 调用方读取字段后关闭 `CommonDataJSONObject`。

### mod 检测

- `ModUtils.isClassPresent(classCanonicalName)` 查询 classpath。
- `ModUtils.isModEnabled(modId)` 查询当前启用 mod id。
- `ModUtils.getEnabledModIds()` 返回启用 mod id 列表。
- `ModUtils.getOverrides()` 返回当前 override 列表。

### 插件与设置读取

- LazyLib 读取 `lazylib_settings.json`。
- `LazyLib.getVersion()`、`getSupportedGameVersion()` 和 `getInfo()` 暴露库元信息。
- `LazyLib.isCachingEnabled()` 暴露 `enableCaching`。
- `LazyLib.getLogLevel()`、`setLogLevel(...)` 和 `onDeprecatedMethodUsage()` 处理日志与弃用 API 策略。

### 数学与几何计算

- 调用方传入 `Vector2f`、`CombatEntityAPI`、`SectorEntityToken`、角度、半径、长度或范围。
- `MathUtils` 计算距离、范围、角度归一、最短旋转、圆形取点、线段取点和随机数。
- `VectorUtils` 计算 facing、angle、directional vector、resize、clamp length 和 rotate。
- `EllipseUtils` 计算椭圆取点、随机点和点包含判定。
- `FastTrig` 以 radians 调用 `sin(...)`、`cos(...)`、`atan(...)` 或 `atan2(...)`。

### 战斗查询

- 调用方传入 `CombatEntityAPI`、`ShipAPI`、`WeaponAPI`、location、range 或 side。
- `AIUtils` 读取 battle objectives、ships、missiles 和 ship system 可用状态。
- `CombatUtils` 读取 projectiles、missiles、ships、asteroids、objectives、entities 和 visibility。
- `DefenseUtils` 读取 armor grid、hull level、armor damage 和 defense type。
- `WeaponUtils` 读取 weapon arc 内 ships 或 missiles，并可调用 `aimTowardsPoint(...)` 推进武器朝向。

### 战役查询

- 调用方传入 `SectorEntityToken`、`CampaignFleetAPI`、faction id、tag、range 或 reputation level。
- `CampaignUtils` 读取 token relation、reputation、same faction、fleet membership 和 nearby fleets。
- `CampaignUtils` 按 tag、faction 或 reputation 查询当前 location 内实体。
- `MessageUtils.showMessage(...)` 将字符串输出到 campaign UI。
- `EllipticalOrbit.advance(amount)` 推进角度。
- `EllipticalOrbit.computeCurrentLocation()` 按 focus、宽高、orbit angle 和当前 angle 计算位置。

### 轻量战斗实体

- `new SimpleEntity(location)` 创建固定位置实体 wrapper。
- `new SimpleEntity(weapon)` 创建绑定武器位置的实体 wrapper。
- `new SimpleEntity(engine)` 创建绑定舰船引擎位置的实体 wrapper。
- `new AnchoredEntity(anchor, location)` 创建绑定 anchor 的实体 wrapper。
- `AnchoredEntity.reanchor(newAnchor, newLocation)` 替换 anchor 与相对位置来源。

## 规范

- `AIUtils` 和 `CombatUtils` 的 range 查询结果必须按当前帧快照处理，跨帧状态应重新查询或由调用方显式持有业务状态。
- `CampaignUtils` 的 faction、tag 和 reputation 查询只能表达当前 sector/location 状态，任务推进、奖励发放和持久变量归调用方保存。
- `CargoUtils.moveStack(...)`、`moveCargo(...)` 和 `moveMothballedShips(...)` 完成迁移后，后续读取应以目标 `CargoAPI` 和源 `CargoAPI` 的最新内容为准。
- `CollisionUtils.getCollisionPoint(...)` 返回碰撞几何点或 `null`，伤害结算、命中事件和 shield/hull 处理仍应通过 Starsector combat API 完成。
- `CommonDataJSONObject` 实现 `AutoCloseable`，读取 common JSON 后应关闭对象以释放 LazyLib 持有的读取资源。
- `EllipseUtils`、`MathUtils` 和 `ShapeUtils` 允许 nullable center 的方法以原点或方法内部默认中心语义构造点，调用方应按方法签名区分 nullable 与 required 参数。
- `FastTrig.sin(...)`、`cos(...)`、`atan(...)` 和 `atan2(...)` 使用 radians；`MathUtils` 与 `VectorUtils` 的 facing、angle、rotate 和 clamp angle 使用 degrees。
- `LazyFont.loadFont(...)` 抛出 `FontException`，字体路径、字体资源存在性和异常处理由调用方所属 UI 生命周期承担。
- `LazyLib.isCachingEnabled()` 读取 `enableCaching`，依赖 LazyLib 缓存的查询不得把缓存结果升级为业务持久状态。
- `ModUtils.isClassPresent(...)` 表达 classpath presence，`ModUtils.isModEnabled(...)` 表达 enabled mod id，两者语义应分别用于 API 可用性和 mod 启用状态判断。
- `SimpleEntity` 与 `AnchoredEntity` 只提供 `CombatEntityAPI` 形状与位置接口，owner、collision class、custom data 和 removed 状态的含义应按 wrapper 源码实现读取。
- `VectorUtils` 带 `dest` 的重载会向目标 vector 写入结果，调用方应避免把同一 mutable vector 同时当作仍需保留的旧值。
- `WeaponUtils.aimTowardsPoint(...)` 推进武器朝向，开火决策、ammo、cooldown、disabled state 和 target ownership 仍由 Starsector weapon API 控制。

## 陷阱

- 把 `AIUtils.getNearestEnemy(...)` 或 `CombatUtils.getShipsWithinRange(...)` 的返回对象当作 LazyLib 拥有的目标状态，会在实体死亡、脱离范围或 owner 变化后造成状态归属错误。
- 把 `CargoUtils.moveCargo(...)` 后的源容器旧 stack 继续作为可交易内容，会造成 cargo 输出和 UI 展示错位。
- 把 `CollisionUtils.isPointWithinBounds(...)` 当作命中结算，会绕过 shield、collision class、damage type 和 combat engine 的正式命中流程。
- 把 `FastTrig` 的 radians 输入与 `MathUtils`、`VectorUtils` 的 degrees 输入混用，会导致角度、转向和随机点方向错误。
- 把 `JSONUtils.loadCommonJSON(filename, defaultJSONPath)` 的默认 JSON 路径读取结果当作用户显式配置，会污染配置来源判断。
- 把 `ModUtils.isClassPresent(...)` 当作 mod enabled 判断，会把存在于 classpath 的未启用或间接携带 class 误判为运行时兼容状态。
- 把 `VectorUtils.resize(vector, length)` 和 `rotate(vector, angle)` 的 mutable 返回语义误当作纯值对象，会覆盖后续仍需使用的 vector 状态。
- 把包内 `MarketUtils` 当作 LazyLib 稳定公共入口，会把源码内部实现误归类为可消费 API。
