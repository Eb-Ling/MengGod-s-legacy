# BoxUtil API

## 定义

BoxUtil API 是硬依赖外部库提供的 GPU 渲染接口，用于初始化图形能力、提交战斗渲染实体、驱动 control data、上传 instance/node/text 数据、执行 shader/direct draw 和管理常用视觉对象。

## 参考

- `org.boxutil.BoxUtilModPlugin`：提供 `initPre()`、`initLater()` 和初始化状态查询。
- `org.boxutil.base.BaseControlData`：提供 `ControlDataAPI` 的默认控制器基类。
- `org.boxutil.base.SimpleParticleControlData`：提供单实体粒子池、particle 添加、剩余时间和强制删除控制。
- `org.boxutil.base.api.InstanceDataAPI`：定义 dynamic/fixed instance timer、alpha 和 SSBO 数据拾取。
- `org.boxutil.base.api.RenderDataAPI`：定义 render entity 的 `delete()`、timer、matrix、layer、blend 和 control data。
- `org.boxutil.config.BoxConfigs`：提供 shader enable、GL 能力、instance clamp 和配置入口。
- `org.boxutil.define.BoxEnum`：提供 manager 返回码、timer state 和 boolean byte 常量。
- `org.boxutil.manager.CombatRenderingManager`：提供 combat render entity、background plugin、cleanup plugin 和 customData 队列入口。
- `org.boxutil.manager.ShaderCore`：提供 shader program、public FBO、screen size、matrix UBO 和能力查询。
- `org.boxutil.units.standard.ShaderProgram`：封装 GL program、uniform、texture binding、`active()`、`close()` 和 valid 状态。
- `org.boxutil.units.standard.attribute.Instance2Data`：提供 2D instance 的位置、速度、角度、缩放、颜色、emissive 和 timer。
- `org.boxutil.units.standard.attribute.NodeData`：提供 curve、trail 和 segment node 的位置、切线、宽度、颜色和 emissive。
- `org.boxutil.units.standard.entity.CommonEntity`：提供 3D model render entity。
- `org.boxutil.units.standard.entity.CurveEntity`：提供 curve node list、node refresh、`submitNodes()` 和 texture flow。
- `org.boxutil.units.standard.entity.DistortionEntity`：提供局部 distortion 的尺寸、power、inner ratio、arc 和 direct draw 类型。
- `org.boxutil.units.standard.entity.FlareEntity`：提供 flare size、style、flick、core/fringe color、glow 和 instance 渲染。
- `org.boxutil.units.standard.entity.SegmentEntity`：提供 segment node list、interpolation、texture flow 和 `submitNodes()`。
- `org.boxutil.units.standard.entity.SpriteEntity`：提供 sprite、tile、UV、base size、random tile、material 和 instance 渲染。
- `org.boxutil.units.standard.entity.TextFieldEntity`：提供 bitmap font、text data、alignment、field size、`submitText()` 和 `directDraw()`。
- `org.boxutil.units.standard.entity.TrailEntity`：提供 trail node、width、fill alpha、texture flow 和 `submitNodes()`。
- `org.boxutil.units.standard.misc.ArcObject` / `org.boxutil.units.standard.misc.PublicFBO`：提供 immediate arc/ring object 与公共 framebuffer object。
- `org.boxutil.util.CommonUtil`、`CurveUtil`、`ShaderUtil`、`TransformUtil`、`TrigUtil`：提供 buffer、曲线、shader、矩阵和三角工具。
- `https://github.com/ShioZakanaPtr/BoxUtil`：Github 源码仓库。

## 边界

- `ArcObject` 与其它 misc object 归属 immediate draw，生命周期由调用方所在 render callback 管理。
- `BoxConfigs.isShaderEnable()` 与 `ShaderCore.isValid()` 共同决定 shader 渲染入口可用性。
- `BoxEnum` 归属 manager 返回码、timer state 和 byte boolean 语义。
- `BoxUtilModPlugin` 归属 BoxUtil 预初始化和 OpenGL context 后初始化。
- `CampaignRenderingManager` 归属 campaign 渲染队列，当前文档只记录其边界归属。
- `CombatRenderingManager` 归属 combat 渲染队列、combat customData、background plugin 和 cleanup plugin。
- `CommonEntity`、`SpriteEntity`、`FlareEntity`、`CurveEntity`、`TrailEntity`、`SegmentEntity`、`DistortionEntity` 和 `TextFieldEntity` 归属 BoxUtil render entity 消费面。
- `Instance2Data` 与 `InstanceDataAPI` 归属 instance 状态封装，上传和渲染范围由对应 entity 控制。
- `NodeData` 归属 curve、trail 和 segment 的节点状态，节点写入后的 GPU 提交由 entity 负责。
- `OpenCL` 与 shader packs 属于 BoxUtil 扩展能力，本模块正文只保留归属边界。
- `RenderDataAPI` 归属 entity 生命周期、timer、matrix、layer、blend、control data 和 `delete()` 状态。
- `ShaderProgram`、`PublicFBO` 和 `ShaderCore` 归属自定义 shader、FBO、screen size、matrix UBO 和 GL program 状态。
- `BaseControlData`、`SimpleParticleControlData` 和 `ControlDataAPI` 归属 entity 控制器回调、跨线程 advance 和 remove 语义。
- `CommonUtil`、`CurveUtil`、`ShaderUtil`、`TransformUtil` 和 `TrigUtil` 归属 BoxUtil 工具函数，不拥有游戏状态。

## 链路

### 初始化与开关

1. 调用 `BoxUtilModPlugin.initPre()`，在 OpenGL context 创建后调用 `initLater()`。
2. 读取 `BoxConfigs.isShaderEnable()`。
3. 读取 `ShaderCore.isValid()` 或具体 `is*Valid()`。

### control data 生命周期

1. 创建 `BaseControlData` 或 `SimpleParticleControlData`。
2. 调用 `RenderDataAPI.setControlData(...)`。
3. BoxUtil 调用 `controlInit(renderEntity)`。
4. 渲染前后调用 `controlBeforeRenderingAdvance(...)` 与 `controlAfterRenderingAdvance(...)`。
5. 逻辑线程调用 `controlAdvance(...)`。
6. entity 删除或 timer out 时调用 `controlRemove(...)`。

### 渲染实体入队

1. 创建实现 `RenderDataAPI` 的 entity。
2. 设置 layer、timer、matrix、material、instance、node、text 或 control data。
3. 调用 `CombatRenderingManager.addEntity(entity)`。
4. 读取 manager 返回码，结束时调用 `RenderDataAPI.delete()` 或等待 timer/remove 回调。

### instance data 提交

1. 创建 `Instance2Data`。
2. 写入 location、velocity、facing、turnRate、scale、scaleRate、color、emissive 和 timer。
3. 调用 entity 的 `setInstanceData(...)` 或 `addInstanceData(...)`。
4. 设置 refresh range 后调用 `submitInstance()` 或旧式 `submitInstanceData()`。
5. 调用 `setRenderingCount(...)`。

### SpriteEntity

1. 创建 `SpriteEntity(sprite)` 或 `SpriteEntity(path)`。
2. 设置 material、tile、UV、base size、blend、layer、timer、control data 或 instance list。
3. 调用 `CombatRenderingManager.addEntity(spriteEntity)`。

### FlareEntity

1. 创建 `FlareEntity`。
2. 设置 size、style、flick、core/fringe color、glow、noise、blend、layer 和 timer。
3. 可选写入 `Instance2Data` 并提交 instance。
4. 调用 `CombatRenderingManager.addEntity(flareEntity)`。

### Curve/Trail/Segment node

1. 创建 `CurveEntity`、`TrailEntity` 或 `SegmentEntity`。
2. 写入 `NodeData` 或 node list。
3. 设置 material、width、texture pixels、texture speed、fill alpha、interpolation、layer 和 blend。
4. 调用 `setNodeRefreshAllFromCurrentIndex()`。
5. 调用 `submitNodes()`。
6. 调用 `CombatRenderingManager.addEntity(entity)`。

### DistortionEntity

1. 创建 `DistortionEntity`。
2. 设置 location、size in/full/out、power in/full/out、inner ratio、ring hardness 和 timer。
3. 调用 `CombatRenderingManager.addEntity(distortionEntity)` 或在合法 render callback 中 direct draw。

### TextFieldEntity

1. 创建 `TextFieldEntity(fontPath)`。
2. 可选调用 `setFontMap(...)`。
3. 调用 `addText(...)` 并设置 alignment、font space、field size、matrix 和 location。
4. 调用 text refresh 方法和 `submitText()`。
5. 调用 `CombatRenderingManager.addEntity(textEntity)` 或 `directDraw()`。

### CommonEntity

1. 创建 `CommonEntity(modelId, syncTextures)`。
2. 设置 base size、material、light、model matrix、layer 和 timer。
3. 可选写入 instance data。
4. 调用 `CombatRenderingManager.addEntity(commonEntity)`。

### ShaderProgram/PublicFBO/direct draw

1. 通过 `ShaderCore` 查询 program、screen size、matrix UBO 或 `tryPublicFBO()`。
2. 调用 `ShaderProgram.active()` 并写入 uniform、UBO 或 texture binding。
3. 执行 entity `directDraw()`、object `glDraw(...)` 或自定义 GL draw。
4. 调用 `ShaderProgram.close()`。

## 规范

- `BoxConfigs.isShaderEnable()` 为 false 时，BoxUtil shader entity 不能作为唯一视觉输出。
- `BoxEnum.STATE_SUCCESS`、`STATE_FAILED` 和 `STATE_FAILED_OTHER` 是 manager 与 submit 类入口的正式返回语义。
- `BoxEnum.TIMER_IN`、`TIMER_FULL`、`TIMER_OUT`、`TIMER_ONCE` 和 `TIMER_INVALID` 是 render entity 与 instance timer 的正式状态集。
- `BoxUtilModPlugin.initPre()` 先于配置、GL state 或 render entity 读取，`initLater()` 在 OpenGL context 创建后执行。
- `CombatRenderingManager.getCustomData()` 的数据随 combat manager 生命周期清理。
- `ControlDataAPI.controlAdvance(...)` 在 BoxUtil 逻辑线程执行，跨线程状态由 control data 自身同步。
- `Instance2Data.setTimer(...)` 只用于 dynamic instance；`setFixedInstanceAlpha(...)` 只用于 fixed instance。
- `RenderDataAPI.delete()` 后 `hasDelete()` 为 true 的 entity 退出 active render object 语义。
- `RenderDataAPI.setControlData(...)` 会立即执行 `controlInit(...)`。
- `SimpleParticleControlData` 的 `maxParticles` 和 `maxDur` 是粒子池容量与保留时间上限。
- `submitInstance()`、`submitNodes()` 和 `submitText()` 是对应数据写入 GPU 后的正式提交点。
- `TextFieldEntity.directDraw()` 由调用方负责后续 `delete()` 或 cleanup plugin。
- `ShaderProgram.active()` 后必须按 `ShaderProgram.close()` 关闭 program 状态。

## 陷阱

- 把 `BoxConfigs.isBaseGL43Supported()` 当作 `ShaderCore.isValid()`，会漏掉 program、FBO、VAO 或 matrix program 初始化失败。
- 把 `BoxConfigs.isShaderEnable()` 为 false 的环境继续提交 BoxUtil shader entity，会丢失视觉分支语义。
- 把 `BUtil_*` backend、测试任务或 package-internal helper 写成外部协议，会把实现细节固化进长期文档。
- 把 `CombatRenderingManager.addEntity(...)` 的 `STATE_FAILED` 忽略，会让本地对象继续按已入队状态推进。
- 把 direct draw 对象同时加入 manager，会造成双重绘制或 cleanup 边界错位。
- 把 `addInstanceData(...)` 成功当成 GPU 已上传，会漏掉 refresh、submit 和 rendering count。
- 把 node 或 text 数据修改后直接绘制，会漏掉 `submitNodes()` 或 `submitText()`。
- 把 `RenderDataAPI.delete()` 后的 entity 继续作为 active object，会绕过 `hasDelete()` 与 control remove 语义。
- 把 fixed instance alpha 写到 dynamic instance，会违反 `InstanceDataAPI` 的 fixed/dynamic 方法边界。
- 把 control data 或 background 回调当主线程回调，会把跨线程状态和 Starsector 主线程对象混写。
