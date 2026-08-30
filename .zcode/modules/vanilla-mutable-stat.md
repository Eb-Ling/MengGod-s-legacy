# 数值运算

来源：原版 Starsector。

## 定义

数值运算 API 是原版数值修饰、动态 stat 和舰船 stats 的计算接口。

## 参考

- `com.fs.starfarer.api.combat.MutableShipStatsAPI`：提供舰船、战机和部分战斗实体的 stats 集合。
- `com.fs.starfarer.api.combat.MutableStat`：提供固定 base 的 flat、percent 和 mult 组合计算。
- `com.fs.starfarer.api.combat.MutableStat.StatMod`：保存单项修饰的 source、说明和值。
- `com.fs.starfarer.api.combat.StatBonus`：提供以调用时 baseValue 计算的 flat、percent 和 mult bonus。
- `com.fs.starfarer.api.util.DynamicStatsAPI`：提供按 id 创建的动态 `MutableStat` 与 `StatBonus` 容器。

## 边界

- `DynamicStatsAPI.getMod(id)` 归属 `StatBonus` 型动态修饰。
- `DynamicStatsAPI.getStat(id)` 归属 base 为 `1` 的 `MutableStat` 型动态 stat。
- `MutableShipStatsAPI` 归属舰船、战机和部分战斗实体的标准 stats 集合。
- `MutableShipStatsAPI.getDynamic()` 归属舰船 stats 的动态 id 容器。
- `MutableShipStatsAPI.getEntity()` 归属 combat 中关联实体读取入口。
- `MutableShipStatsAPI.getFleetMember()` 归属舰队成员或战斗中 fighter wing member 读取入口。
- `MutableShipStatsAPI.getVariant()` 归属 stats 持有者的 variant 读取入口。
- `MutableStat.StatMod.source` 归属单项修饰的覆盖和解除 key。
- `MutableStat` 归属固定 base 的即时数值。
- `StatBonus` 归属调用 `computeEffective(baseValue)` 时才传入 base 的 bonus 数值。

## 链路

### MutableStat 计算链路

1. `MutableStat` 保存 base。
2. `modifyFlat(source, value)` 写入 flatMods。
3. `modifyPercent(source, value)` 写入 percentMods。
4. `modifyMult(source, value)` 写入 multMods。
5. `getModifiedValue()` 触发 recompute。
6. recompute 汇总 percent、flat 和 mult。
7. 计算公式为 `(base + base * percent / 100 + flat) * mult`。
8. `unmodify(source)` 移除同 source 的 flat、percent 和 mult。

### StatBonus 计算链路

1. `StatBonus` 保存 flatBonus、percentMod 和 mult。
2. `modifyFlat(source, value)` 写入 flatBonuses。
3. `modifyPercent(source, value)` 写入 percentBonuses。
4. `modifyMult(source, value)` 写入 multBonuses。
5. `computeEffective(baseValue)` 触发 recompute。
6. recompute 汇总 percent、flat 和 mult。
7. 计算公式为 `(baseValue + baseValue * percent / 100 + flat) * mult`。
8. `unmodify(source)` 移除同 source 的 flat、percent 和 mult。

### DynamicStats 创建与读取链路

1. 调用方取得 `MutableShipStatsAPI.getDynamic()`。
2. 调用 `getStat(id)` 取得 base 为 `1` 的 `MutableStat`。
3. 调用 `getValue(id)` 计算 dynamic stat 的当前值。
4. 调用 `getMod(id)` 取得以调用参数为 base 的 `StatBonus`。
5. 调用 `getValue(id, base)` 以传入 base 计算 dynamic bonus 的当前值。
6. 调用 `removeUmodified()` 清理未修改条目。

### MutableShipStatsAPI 实体读取链路

1. 调用方取得 `MutableShipStatsAPI`。
2. 调用 `getEntity()` 读取 combat 中关联的 `CombatEntityAPI`。
3. 调用 `getFleetMember()` 读取关联的 `FleetMemberAPI`。
4. 调用 `getVariant()` 读取关联的 `ShipVariantAPI`。
5. 调用具体 getter 取得 `MutableStat` 或 `StatBonus`。
6. 调用 `getDynamic()` 读取动态 stat 容器。

## 规范

- `DynamicStatsAPI.getMod(id)` 返回 `StatBonus`，base 由 `computeEffective(baseValue)` 或 `getValue(id, base)` 的调用参数提供。
- `DynamicStatsAPI.getStat(id)` 返回 base 为 `1` 的 `MutableStat`。
- `DynamicStatsAPI.getValue(id)` 读取 `getStat(id)` 的当前值。
- `DynamicStatsAPI.getValue(id, base)` 读取 `getMod(id).computeEffective(base)` 的当前值。
- `MutableShipStatsAPI.getEntity()` 仅在 combat 中返回关联实体。
- `MutableShipStatsAPI.getFleetMember()` 可返回 `null` 或 combat 中伪造的 fighter wing member。
- `MutableStat.modifyFlat(source, 0)` 在无现有 mod 时不创建条目。
- `MutableStat.modifyFlatAlways(source, value, desc)` 总是写入 flat 条目。
- `MutableStat.modifyMult(source, 1)` 在无现有 mod 时不创建条目。
- `MutableStat.modifyMultAlways(source, value, desc)` 总是写入 mult 条目。
- `MutableStat.modifyPercent(source, 0)` 在无现有 mod 时不创建条目。
- `MutableStat.modifyPercentAlways(source, value, desc)` 总是写入 percent 条目。
- `MutableStat.setBaseValue(base)` 改变固定 base 并触发重算。
- `MutableStat.unmodify(source)` 同时处理 flat、percent 和 mult。
- `StatBonus.getBonusMult()` 返回 percent 与 mult 合并后的倍率。
- `StatBonus.modifyFlat(source, 0)` 在无现有 bonus 时不创建条目。
- `StatBonus.modifyMult(source, 1)` 在无现有 bonus 时不创建条目。
- `StatBonus.modifyPercent(source, 0)` 在无现有 bonus 时不创建条目。

## 陷阱

- `DynamicStatsAPI.getMod(id)` 与 `getStat(id)` 返回类型和 base 语义不同。
- `MutableShipStatsAPI.getEntity()` 是 combat-only 关联实体入口，生涯 stats 读取会得到空实体语义。
- `MutableShipStatsAPI.getFleetMember()` 在 combat 中可返回伪造的 fighter wing member。
- `MutableStat.modifyPercent(source, -100)` 只抵消 base，flat 仍参与计算。
- `MutableStat` 相同 source 的同类修饰会覆盖旧值。
- `StatBonus.computeEffective(baseValue)` 的 base 来自调用参数，存储对象自身没有固定 base。
