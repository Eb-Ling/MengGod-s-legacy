# HeiHeiFleet → MengGod's legacy 迁移记录

## 概述

将 HeiHeiFleet mod（外包）的所有功能并入 MengGod's legacy mod，包含 `heihei`（主舰系统）和 `krg`（武器系统）两个包共33个Java文件。

---

## 已迁移模块

### 1. ModPlugin 合并 → Mengplugin.java
- **原文件**: `HeiHeiFleetModPlugin.java`（88行）
- **原包**: `data.scripts.heihei`
- **处理方式**: 合并入已有的 `Mengplugin.java`，不单独迁移

### 2. Luna改装按钮 → Meng_ModuleRefitButton.java
- **原文件**: `HeiHeiModuleRefitButton.java`（223行）
- **原包**: `data.scripts.heihei.refit`
- **新位置**: `data.methods`
- **类名**: `HeiHeiModuleRefitButton` → `Meng_ModuleRefitButton`

### 3. 浮动模块选择器 → Meng_ModuleSelectorScript.java
- **原文件**: `HeiHeiModuleSelectorScript.java`（676行）
- **原包**: `data.scripts.heihei.campaign`
- **新位置**: `data.methods`
- **类名**: `HeiHeiModuleSelectorScript` → `Meng_ModuleSelectorScript`

---

## 迁移变更详情

### 1. 相位纹理预加载方式变更
| 项目 | 变更前 | 变更后 |
|------|--------|--------|
| 方式 | `preloadPhaseTextures()` 手动调用 `Global.getSettings().loadTexture()` | 在 `settings.json` 的 `HeiHei_phase` 分类中声明 |
| 数量 | 21组纹理基础路径，代码中循环加载 glow1+glow2 | 42条纹理记录，由游戏引擎启动时自动预加载 |
| 位置 | `HeiHeiFleetModPlugin` 内部方法 | `data/config/settings.json` → `graphics.Meng_OldEmpire_phase` |

### 2. Luna按钮注册
- 注册方式：`LunaRefitManager.addRefitButton()` × 动态数量
- 分布：N种模块模式 × 3按钮（轻型A组 + 重型A组 + B组），N由CSV条目数决定
- 注册位置：`Mengplugin.onApplicationLoad()` + `onGameLoad()` 双处调用
- 防重复：`oldEmpireLunaButtonsRegistered` 静态标记
- 动态化：循环上界从硬编码`4`改为`Math.max(getModeCount(true), getModeCount(false))`

### 3. VariantId驱动逻辑优化（CSV数据驱动）
| 项目 | 变更前（硬编码） | 变更后（CSV驱动） |
|------|------------------|-------------------|
| 模块ID数组 | `LIGHT_MODULES = {"02","04","05","08"}` / `HEAVY_MODULES = {"01","03","07","09"}` | `data/config/Meng_OldEmpire_floating_modules.csv` |
| VariantId构造 | `"heihei_float_" + moduleId + "_" + side + "_Standard"` 字符串拼接 | CSV直接存储完整variantId（如 `Meng_OldEmpire_float_02_l_Standard`），代码直接查表使用 |
| 显示名称 | `LIGHT_NAMES` / `HEAVY_NAMES` 字符串数组 | CSV 的 `name` 列 |
| 轻/重型区分 | 两个独立数组 + boolean参数选择 | CSV 的 `isLight` 布尔列 |
| 特殊舰船固定模块 | `syncVariant()` 中 `heihei_002`/`heihei_004` 的 if/else | CSV 的 `fixedHull` + `fixedSide` 列 |
| 默认模块 | `ensureDefaultModules()` 硬编码 "02"/"01" | 从CSV中读取 mode=0 的模块数据 |
| 模式反推 | `readModeFromModule()` 通过 `contains("heihei_float_" + moduleId)` 匹配 | 直接 `equals(leftVariant)` / `equals(rightVariant)` 精确匹配 |
| 加载时机 | 无（运行时直接访问常量） | `Mengplugin.onApplicationLoad()` 时调用 `loadModuleData()` |
| CSV解析方式 | `BufferedReader` + `split(",")` 手动解析 | `getMergedSpreadsheetDataForMod("leftVariant", path, "Meng")` 返回JSONArray，按列名访问 |

### 4. 动态模式数量支持
| 项目 | 变更前（硬编码） | 变更后（动态） |
|------|------------------|------------------|
| 模式数量 | 全局硬编码 `4`（`clampMode` 用 `Math.min(3,...)`) | `getModeCount(light)` 从CSV列表 `.size()` 获取 |
| 标签读写 | `readTagMode`/`writeTagMode` 循环 `i < 4` | 传入 `getMaxModeCount()` 作为循环上界 |
| 按钮注册 | `Mengplugin` 循环 `i < 4` | `Math.max(getModeCount(true), getModeCount(false))` |
| 模式范围限制 | `clampMode()` 统一用 `[0,3]` | 废弃 `clampMode`，各处内联 `Math.max(0, Math.min(mode, getModeCount(light)-1))` |
| 扩展方式 | 需修改代码才能增加模式 | 只需在CSV中添加新行即可自动增加按钮和标签支持 |

### 5. 模块配置序列化改为JSON
| 项目 | 变更前（手工文本格式） | 变更后（JSON序列化） |
|------|------------------|------------------|
| 序列化格式 | 手工拼接文本 `vents=4\nweapon=base64(slot):base64(wpn)...` | `JSONObject` 结构化存储 `{'vents':4,'weapons':{'WS 001':'autolaser'},...}` |
| 解析方式 | 逐行 `startsWith` + `split` + Base64解码 | `JSONObject.optXxx()` 按字段名直接读取 |
| tag格式 | `prefix.base64(key).base64(payload)` 双层Base64 | `prefix.key.base64(json)` key明文、json仅一层Base64 |
| 可扩展性 | 新增字段需手动加解析分支 | `optXxx` 有默认值，新增字段不破坏旧格式 |
| 删除的代码 | `encode()`/`decode()`/`parseGroup()`/`appendLine()`/`parseInt()` | 全部删除（约67行） |

---

## 批量迁移（Phase 1-5）

### 命名规则
| 类别 | 变更前 | 变更后 |
|------|--------|--------|
| Java 类名 | `HeiHeiXxx` / `KrgXxx` | `Meng_OldEmpireXxx` |
| 数据ID | `heihei_xxx` / `krg_xxx` | `Meng_OldEmpire_xxx` |
| 槽位ID | `HEIHEI_XXX` | `OLDEMPIRE_XXX` |
| 标签/缓存Key | `heihei_xxx` | `Meng_OldEmpire_xxx` |
| 包名(heihei) | `data.scripts.heihei.hullmods/weapons/console/campaign` | `hullmods` / `weapons` / `scripts.console` / `methods` |
| 包名(krg) | `data.scripts.krg` / `data.scripts.krg.hullmods` / `data.scripts.krg.weapons` | `methods` / `hullmods` / `weapons` |

### 已迁移 Java 文件（30个）
| 原文件 | 目标位置 | 新类名 |
|--------|----------|--------|
| **heihei hullmods** | | |
| HeiHeiBlackHoleCoreHullmod | hullmods/ | Meng_OldEmpireBlackHoleCoreHullmod |
| HeiHeiCorePdSystem | hullmods/ | Meng_OldEmpireCorePdSystem |
| HeiHeiFloatingBatteryController | hullmods/ | Meng_OldEmpireFloatingBatteryController |
| **heihei weapons** | | |
| HeiHeiAxialChargeEffect | weapons/ | Meng_OldEmpireAxialChargeEffect |
| HeiHeiAxialFx | weapons/ | Meng_OldEmpireAxialFx |
| HeiHeiAxialWeaponEffect | weapons/ | Meng_OldEmpireAxialWeaponEffect |
| HeiHeiHullGlowEffect | weapons/ | Meng_OldEmpireHullGlowEffect |
| **heihei console** | | |
| HeiHeiAddAxialWeapons | scripts/console/ | Meng_OldEmpireAddAxialWeapons |
| HeiHeiAddFleet | scripts/console/ | Meng_OldEmpireAddFleet |
| **krg core** | | |
| KrgEffects | methods/ | Meng_OldEmpireEffects |
| KrgIds | methods/ | Meng_OldEmpireIds |
| KrgMath | methods/ | Meng_OldEmpireMath |
| KrgRangeModifier | methods/ | Meng_OldEmpireRangeModifier |
| **krg hullmods** | | |
| KrgBaseHullmod | hullmods/ | Meng_OldEmpireBaseHullmod |
| KrgAlphaHeavyCaliber / Multishot / WarheadSplit | hullmods/ | Meng_OldEmpireAlphaXxx |
| KrgBetaBurstRifling / DeadlyRhythm / PrecisionKinetics | hullmods/ | Meng_OldEmpireBetaXxx |
| KrgGammaChargedBreech / CrimsonTaint / VolatileLoad | hullmods/ | Meng_OldEmpireGammaXxx |
| **krg weapons** | | |
| KrgContinuousBeamEffect | weapons/ | Meng_OldEmpireContinuousBeamEffect |
| KrgDustPayloadController | weapons/ | Meng_OldEmpireDustPayloadController |
| KrgOnHitEffect | weapons/ | Meng_OldEmpireOnHitEffect |
| KrgProjectileOnFire | weapons/ | Meng_OldEmpireProjectileOnFire |
| KrgPulseChainRateEffect / SpiritDustRateEffect / TracerRateEffect | weapons/ | Meng_OldEmpireXxxRateEffect |

### 已迁移数据文件
| 类型 | 数量 | 说明 |
|------|------|------|
| .ship | 24 | 舰船数据，hullId/spriteName/槽位ID全部重命名 |
| .variant | 24 | 变体配置，hullId/variantId/tags全部重命名 |
| .system | 1 | Meng_OldEmpire_phasecloak.system |
| .wpn | 23 | 武器定义（7 heihei + 16 krg），id/脚本路径全部更新 |
| .proj | 15 | 弹丸定义（2 heihei + 13 krg），id全部更新 |

### 已更新 CSV 文件
| 文件 | 操作 | 说明 |
|------|------|------|
| ship_data.csv | 追加26行 | 舰船数据，标签改为 oldempire |
| hull_mods.csv | 追加11行 | 船插数据，脚本路径/图形路径全部更新 |
| ship_systems.csv | 追加1行 | Meng_OldEmpire_phasecloak |
| weapon_data.csv | 追加22行 | 武器数据 |
| Meng_OldEmpire_floating_modules.csv | 重命名+全量替换 | 原 heihei_floating_modules.csv |
| commands.csv | 追加2行 | 控制台命令 |

### 已迁移图形资源
| 类型 | 数量 | 目标目录 |
|------|------|----------|
| 舰船精灵图 | 25 | graphics/ships/oldempire/ |
| 模块精灵图 | 54 | graphics/ships/oldempire/modules/ |
| 船插图标 | 3 | graphics/hullmods/oldempire/ |
| 武器精灵图 | 12 | graphics/weapons/oldempire/ |

### 已更新的已有文件
| 文件 | 变更内容 |
|------|----------|
| Meng_ModuleSelectorScript.java | 8个槽位ID值、4个标签前缀、方法名isHeiHeiParent->isOldEmpireParent、CSV路径、hullId判断 |
| Meng_ModuleRefitButton.java | isHeiHeiParent->isOldEmpireParent、注释 |
| Mengplugin.java | 变量名/方法名(registerOldEmpireLunaButtons)、注释 |
| settings.json | HeiHei_phase->Meng_OldEmpire_phase、所有key和精灵路径 |
