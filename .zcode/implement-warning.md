# Implement Warning

本文件维护容易造成误判的运行期事实。Java 通用风格由 `.zcode/java-guidelines.md` 维护。

## Projectile source

- `DamagingProjectileAPI` 及 `MissileAPI` 的 `source` 仅在被直接解引用时必须判空。
- 接收可空 source 的 Starsector API 必须照常调用，禁止因 source 为空提前取消效果。
- source 允许保存为方法局部变量，严禁进入字段或跨帧状态。

## 确定返回值

- `ShipAPI.getMouseTarget()` 保证返回非 `null` 对象。
- `CombatEngineAPI.getPlayerShip()` 保证返回非 `null` 对象。
- `WeaponAPI.getSpec()` 取得当前武器实例的独立 spec，而非同类武器共享 spec。

## Ship system 时序

- 舰船实体创建后，`BaseShipSystemScript.unapply()` 必定先执行 1 次，首次 `apply()` 与系统 AI 均发生在它之后。
- `ship_systems.csv` 中 `active` 为空或为 `0` 时，`effectLevel == 1` 精确持续 1 帧。
- 判断 once 逻辑必须同时使用 CSV 配置、系统状态机和回调时序，禁止只观察局部分支。

## Hullmod 时序

- `applyEffectsAfterShipCreation()` 同时用于战斗实体创建与装配界面完成修改。
- `applyEffectsAfterShipCreation()` 执行时，舰船尚未进入 Combat Engine，且 `ShipAPI.isAlive()` 为 `false`。
- `applyEffectsAfterShipAddedToCombatEngine()` 执行时，`ShipAPI.isAlive()` 为 `false`。

## 战斗回调

- 一个 `BeamEffectPlugin` 实例只服务一根光束，实例字段不得按跨光束共享状态分析。
- 游戏暂停期间，`HullModEffect`、`BeamEffectPlugin`、`EveryFrameWeaponEffectPlugin` 与 `MissileAIPlugin` 均不推进。

## Controller 所有权

- 对于每实体、每 projectile、每目标或每 engine 唯一实例的 controller 类，必须自行管理注册、复用、customData 与注销。
- 唯一实例入口固定命名为 `getInstance(...)`，创建与注册必须在该入口内完成；严禁改名为 `getOrCreate(...)` 或另拆 `create(...)` 包装。
- 一次性持续效果允许使用触发型静态入口，但插件注册、实体写入和结束清理仍由所属类负责。
- 正式生命周期入口必须标记 `@LifecycleEntry`，静态检查严禁用方法名白名单替代注解。
- 含静态 `@LifecycleEntry` 的类不得公开构造函数；无需继承时必须使用 `private` 构造函数。
- `isExpired()` 只能查询状态；customData 解绑、实体恢复和资源释放必须放入 `cleanup()` 或明确结束流程。

## 可达性判定

- 对目标、customData、互斥标记、跨帧缓存或全局状态提出问题前，必须从 `isUsable()`、AI guard、CSV 时序、active/toggle 语义或注册入口证明异常可达。
- 必须写清 owner、写入顺序、消费位置和可观察后果。
- 正式入口已保证 owner、非空、单次触发或互斥时，严禁因缺少额外校验、once 标记或 fallback 报告问题。
- 启动前校验与启动后持续状态必须分开分析；跨边界问题必须指出语义失效的确切位置。

## 实体引用

- Java 引用不会因实体死亡或移出 engine 变成悬空引用。
- 读取已死亡、已移除或不在 play 的对象本身不构成错误。
- 只有旧对象状态被当作当前战斗事实继续结算并产生可观察错误时，才能确认问题。
