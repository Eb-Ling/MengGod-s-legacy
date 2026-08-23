# GraphicsLib API

来源：硬依赖外部库 GraphicsLib。

## 定义

GraphicsLib API 是 ShaderLib 图形能力初始化、TextureData 材质/法线贴图注册、LightData 自动光照数据和 LightShader 动态光源渲染组成的硬依赖接口集合。

## 参考

- `data/lights/`：使用方注册 light data 与 texture data CSV 的约定目录。
- `data/shaders/`：使用方提供自定义 shader 源文件的约定目录。
- `GRAPHICS_OPTIONS.ini`：ShaderLib、LightShader 和 TextureData 读取图形能力与贴图加载门禁的设置入口。
- `org.dark.shaders.light.LightData.readLightDataCSV(...)`：读取 projectile 和 beam 自动光照 CSV。
- `org.dark.shaders.light.LightShader.addLight(...)`：向启用的 LightShader 当前战斗 local data 添加动态光源。
- `org.dark.shaders.light.StandardLight`：提供 point、line、cone 和 directional light 的标准实现。
- `org.dark.shaders.util.ShaderLib.areBuffersAllowed()`：返回 framebuffer object 能力开关。
- `org.dark.shaders.util.ShaderLib.areShadersAllowed()`：返回 shader 能力开关。
- `org.dark.shaders.util.ShaderLib.beginDraw(...)`：进入 screen-space shader 绘制状态。
- `org.dark.shaders.util.ShaderLib.init()`：读取图形设置、检测 OpenGL 能力并初始化 shader 支持。
- `org.dark.shaders.util.TextureData.readTextureDataCSV(...)`：读取 material、normal 和 surface texture CSV。

## 边界

- `LightData` 归属 projectile 与 beam 的自动 standard、hit 和 flash light 数据表。
- `LightData.getLightData(...)` 归属按 id 与 `LightDataType` 查询 light entry。
- `LightData.readLightDataCSV(...)` 归属覆盖式 CSV 加载。
- `LightData.readLightDataCSVNoOverwrite(...)` 归属保留既有 key 的 CSV 加载。
- `LightShader` 归属战斗内光照 shader、light local data、自动 projectile/beam light 和动态 light 列表。
- `LightShader.addLight(...)` 归属运行时动态 light 注入。
- `LightShader.DO_NOT_RENDER` 归属舰船、导弹和小行星跳过 normal、surface 与 lighting 渲染的 customData key。
- `LightShader.initCombat()` 归属当前战斗的 `LocalData` 创建。
- `LightShader.removeLight(...)` 归属运行时动态 light 移除。
- `StandardLight` 归属点光、线光、锥光和方向光的数据与逐帧衰减。
- `TextureData` 归属 material、normal 和 surface texture entry 注册。
- `TextureData.getTextureData(...)` 归属按 id、map、object type 和 frame 查询 texture entry。
- `TextureData.readTextureDataCSV(...)` 归属覆盖式 texture CSV 加载。
- `TextureData.readTextureDataCSVNoOverwrite(...)` 归属保留既有 key 的 texture CSV 加载。
- `ShaderLib` 归属 OpenGL 能力检测、FBO 工具、shader 编译、screen/world 坐标转换和 texture override。
- `ShaderLib.beginDraw(...)` 与 `ShaderLib.exitDraw()` 归属 screen-space shader 绘制栈。
- `ShaderLib.areShadersAllowed()` 和 `ShaderLib.areBuffersAllowed()` 归属运行时能力门禁。
- `ShaderLib.addShaderAPI(...)` 归属 ShaderAPI 实例注册。

## 链路

### GraphicsLib 初始化链路

1. 调用方执行 `ShaderLib.init()`。
2. `ShaderLib.init()` 检查 `initialized` 并保证初始化只执行 1 次。
3. `ShaderLib.init()` 检测 OpenGL core、ARB 和 EXT framebuffer 能力。
4. `ShaderLib.init()` 检测 OpenGL 2.0 shader 能力。
5. `ShaderLib.init()` 读取 `GRAPHICS_OPTIONS.ini` 的 `enableShaders`、`toggleKey`、`reloadKey`、`use64BitBuffer`、`extraScreenClear` 和 `aaCompatMode`。
6. `enableShaders == false` 时 `shadersAllowed` 和 `buffersAllowed` 均置为 false。
7. `shadersAllowed == true` 时创建 screen texture。
8. `buffersAllowed == true && shadersAllowed == true` 时创建 foreground 与 auxiliary framebuffer。
9. framebuffer 创建失败时 `buffersAllowed` 置为 false。
10. `areShadersAllowed()` 和 `areBuffersAllowed()` 暴露检测结果。
11. 调用方在两个能力均允许后读取 light data 和 texture data CSV。

### LightData CSV 链路

1. 调用方传入 light data CSV local path。
2. `Global.getSettings().loadCSV(localPath)` 读取 CSV。
3. 每行要求 `id` 与 `type`。
4. `type == projectile` 写入 projectile light map。
5. `type == beam` 写入 beam light map。
6. standard light 读取 `size`、`intensity`、`color`、`fadeout` 和 `offset`。
7. hit light 读取 `hit size`、`hit intensity`、`hit color` 和 `hit fadeout`。
8. flash light 读取 `flash size`、`flash intensity`、`flash color`、`flash fadeout` 和 `flash offset`。
9. `chance` 与 `fighter dim` 写入 light entry。

### LightShader 动态光源链路

1. 调用方创建 `StandardLight` 或其它 `LightAPI`。
2. 调用方设置 location、size、intensity、color、lifetime、fade 或 attachment。
3. 调用方执行 `LightShader.addLight(light)`。
4. `addLight(...)` 通过 `ShaderLib.getShaderAPI(LightShader.class)` 取得 shader。
5. shader 为 `LightShader` 且 `isEnabled()` 为 true 时读取 combat customData local data。
6. local data 存在时 light 写入当前战斗 lights list。
7. `LightShader.advance(...)` 在战斗未暂停时调用 `light.advance(amount)`。
8. `light.advance(amount)` 返回 true 时从 lights list 移除该 light。
9. render 调用按 lights list 绘制 light。

### ShaderLib 绘制链路

1. 调用方编译或取得 shader program id。
2. 调用方执行 `ShaderLib.beginDraw(program)`。
3. `beginDraw(...)` 绑定 shader program、保存 GL attrib 和矩阵栈、设置 screen-space ortho。
4. 调用方可执行 `copyScreen(...)`、`drawScreenQuad(...)` 或自定义 GL 绘制。
5. 调用方执行 `ShaderLib.exitDraw()`。
6. `exitDraw()` 恢复 GL 矩阵栈、attrib、active texture 和 shader program。

### TextureData CSV 链路

1. 调用方传入 texture data CSV local path。
2. `Global.getSettings().loadCSV(localPath)` 读取 CSV。
3. 每行要求 `id`、`type`、`map` 和 `path`。
4. `type` 转换为 ship、turret、hardpoint、missile 或 asteroid object suffix。
5. `map == material` 且 material loading 启用时加载 texture 并写入 material map。
6. `map == normal` 且 normal loading 启用时加载 texture 并写入 normal map。
7. `map == surface` 且 surface loading 启用时加载 texture 并写入 surface map。
8. `frame` 缺省为 0 并参与 animated weapon texture key。
9. `magnitude` 缺省为 1.0 并写入 `TextureEntry`。

## 规范

- `LightData.readLightDataCSV(...)` 对重复 key 采用后读覆盖前读。
- `LightData.readLightDataCSVNoOverwrite(...)` 对重复 key 保留先读 entry。
- `LightData` 的 `type` 只接受 `projectile` 和 `beam`。
- `LightData` 的 color 字段使用 `[r,g,b]` 字符串并 clamp 到 0-255。
- `LightData` 的 standard light 需要 `size > 0`、`intensity > 0` 和非空 `color`。
- `LightData` 的 hit light 需要 `hit size > 0`、`hit intensity > 0` 和非空 `hit color`。
- `LightData` 的 flash light 需要 `flash size > 0`、`flash intensity > 0` 和非空 `flash color`。
- `LightShader.addLight(...)` 在 light 为 null 时直接返回。
- `LightShader.addLight(...)` 只在 `LightShader.isEnabled()` 为 true 且 combat local data 存在时写入 lights list。
- `LightShader.initCombat()` 把 `LocalData` 写入 combat customData，key 为 `shaderlib_LightShader`。
- `LightShader.removeLight(...)` 在 light 为 null 时直接返回。
- `ShaderLib.addShaderAPI(...)` 对同 class shader 保持唯一实例。
- `ShaderLib.areBuffersAllowed()` 为 false 时 FBO 相关绘制入口应停止。
- `ShaderLib.areShadersAllowed()` 为 false 时 shader program 与 shader 绘制入口应停止。
- `ShaderLib.beginDraw(...)` 后必须执行 `ShaderLib.exitDraw()` 恢复 GL 状态。
- `ShaderLib.getAuxiliaryBufferId()` 和 `getAuxiliaryBufferTexture()` 在 `buffersAllowed == false` 时返回 0。
- `ShaderLib.init()` 在 `enableShaders == false` 时把 shader 与 FBO 能力同时关闭。
- `ShaderLib.loadShader(...)` 编译或链接失败时返回 0。
- `StandardLight.advance(...)` 在 intensity 或 size 到达结束条件时返回 true 表示移除。
- `StandardLight.fadeOut(seconds)` 的 seconds 小于等于 0 会把 intensity 置为 0。
- `StandardLight.getIntensity()` 对 beam endpoint link 会乘以 beam brightness。
- `StandardLight.makePermanent()` 把 lifetime 和 superLifetime 均设为 -1。
- `StandardLight.setArc(...)` 会把 start 与 end 规整到 0-360 度。
- `TextureData.readTextureDataCSV(...)` 对重复 key 采用后读覆盖前读。
- `TextureData.readTextureDataCSVNoOverwrite(...)` 对重复 key 保留先读 entry。
- `TextureData` 的 `map` 只接受 `material`、`normal` 和 `surface`。
- `TextureData` 的 `type` 只接受 ship、turret、turretbarrel、turretunder、turret cover、hardpoint、hardpoint barrel、hardpoint under、hardpoint cover、missile 和 asteroid。
- `TextureData` 的 material map 读取依赖 `enableShaders`、`enableLights` 和 `loadMaterial` 同时允许。
- `TextureData` 的 normal map 读取依赖 `enableShaders`、`enableLights` 和 `enableNormal` 同时允许。
- `TextureData` 的 surface map 读取依赖 enableShaders、enableLights、enableNormal 和 loadSurface 同时允许。

## 陷阱

- `LightData` 行缺少 `id` 或 `type` 时该行不会注册。
- `LightData` 的 `type` 拼写不匹配 projectile/beam 时该行不会注册。
- `LightShader.addLight(...)` 在 LightShader 未启用时静默跳过。
- `LightShader.addLight(...)` 在当前战斗 customData 缺少 local data 时静默跳过。
- `LightShader.advance(...)` 直接读取 combat customData 的 local data，缺少 `shaderlib_LightShader` 会使后续 lights list 读取失效。
- `LightShader.DO_NOT_RENDER` 放入实体 customData 后会跳过该实体的 normal、surface 或 lighting 相关绘制。
- `ShaderLib.beginDraw(...)` 与 `exitDraw()` 未成对调用会污染 GL 矩阵栈、attrib 或 shader program。
- `ShaderLib.loadShader(...)` 返回 0 表示 shader program 构建失败，继续绑定该 program 会得到空绘制或错误绘制。
- `ShaderLib.getForegroundTexture(...)` 注释说明该方法可能重置 framebuffer binding。
- `StandardLight` 默认 superLifetime 为 60 秒，未永久化的长寿命 light 会被 failsafe 移除。
- `StandardLight` attached 到 entity、beam 或 beam endpoint 后，手动设置 location 或 velocity 入口会失去直接效果。
- `StandardLight` 类型 3 的 directional light 结束条件使用 intensity 和 specularIntensity；其它类型使用 size 和 intensity。
- `TextureData` 行缺少 `id`、`type`、`map` 或 `path` 时该行不会注册。
- `TextureData` 的 `map` 受图形设置控制，material、normal 或 surface 关闭时对应 entry 不会写入。
- `TextureData` 的 `path` 贴图加载失败时该行不会写入 texture map。
