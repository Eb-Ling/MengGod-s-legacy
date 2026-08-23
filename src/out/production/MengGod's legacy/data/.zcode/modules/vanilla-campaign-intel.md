# 生涯 Intel

来源：原版 Starsector。

## 定义

生涯 Intel API 是情报条目、通信消息、可见性、地图定位、排序和结束移除的原版接口集合。

## 参考

- `com.fs.starfarer.api.campaign.CampaignUIAPI.addMessage(...)`：提供 Intel 更新消息进入生涯 UI 消息区的入口。
- `com.fs.starfarer.api.campaign.SectorAPI.getIntelManager()`：提供全局 Intel manager 入口。
- `com.fs.starfarer.api.campaign.comm.IntelInfoPlugin`：定义 Intel UI、可见性、按钮、地图、排序和结束状态接口。
- `com.fs.starfarer.api.campaign.comm.IntelManagerAPI`：提供 Intel 加入、排队、移除、查询和排序接口。
- `com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin`：提供 `IntelInfoPlugin`、`EveryFrameScript`、消息更新和结束状态的默认实现。
- `com.fs.starfarer.api.ui.IntelUIAPI`：提供 Intel 页面刷新、选择、地图显示和对话打开入口。

## 边界

- `BaseIntelPlugin` 归属 Intel 默认状态、推进、可见性、消息更新、地图定位和结束流程。
- `BaseIntelPlugin.endingTimeRemaining` 归属结束延迟倒计时。
- `BaseIntelPlugin.listInfoParam` 归属单次消息或列表渲染的更新参数。
- `BaseIntelPlugin.postingLocation` 归属通信范围与地图定位来源。
- `BaseIntelPlugin.timestamp` 归属玩家可见时间戳。
- `CampaignUIAPI.addMessage(IntelInfoPlugin, ...)` 归属生涯左侧消息区的 Intel 更新显示。
- `IntelInfoPlugin` 归属 Intel 列表、描述、按钮、地图、排序、重要标记和可见状态接口。
- `IntelInfoPlugin.ArrowData` 归属地图箭头显示数据。
- `IntelInfoPlugin.ListInfoMode` 归属消息、Intel 列表、地图 tooltip 和描述内渲染模式。
- `IntelManagerAPI` 归属 Intel 集合、通信队列、加入、移除、查询和排序。
- `IntelUIAPI` 归属 Intel 页面刷新、当前条目选择、自定义子集和地图跳转。
- `SectorAPI.getIntelManager()` 归属全局 Intel manager 读取入口。

## 链路

### Intel 创建与加入链路

1. 代码创建 `BaseIntelPlugin` 子类或其它 `IntelInfoPlugin` 实例。
2. 代码取得 `Global.getSector().getIntelManager()`。
3. 调用 `IntelManagerAPI.addIntel(plugin)`。
4. manager 将 Intel 加入已知 Intel 集合。
5. UI 按 `IntelInfoPlugin` 接口读取列表信息、图标和描述。
6. `autoAddCampaignMessage()` 决定加入时的默认消息显示语义。

### Intel 队列可见性链路

1. 代码创建 `IntelInfoPlugin` 实例。
2. 调用 `IntelManagerAPI.queueIntel(plugin)` 或 `queueIntel(plugin, maxCommQueueDays)`。
3. manager 将 Intel 放入通信队列。
4. manager 检查玩家是否在通信中继范围内。
5. 调用 `canMakeVisibleToPlayer(playerInRelayRange)` 判断当前可见性。
6. Intel 可见时调用 `reportMadeVisibleToPlayer()`。
7. Intel 设置玩家可见 timestamp。
8. manager 将 Intel 从通信队列转入已知 Intel 集合。

### Intel 更新消息链路

1. 调用 `BaseIntelPlugin.sendUpdate(listInfoParam, textPanel)`。
2. `BaseIntelPlugin` 写入 `listInfoParam`。
3. `IntelManagerAPI.addIntelToTextPanel(this, textPanel)` 将更新写入文本面板。
4. `BaseIntelPlugin` 清空 `listInfoParam`。
5. 调用 `sendUpdateIfPlayerHasIntel(...)` 时先检查玩家可见 timestamp。
6. 通过 hidden 和 important 条件后调用 `CampaignUIAPI.addMessage(...)`。
7. UI 使用 `MessageClickAction.INTEL_TAB` 打开 Intel 页面。

### Intel UI 渲染链路

1. Intel 页面调用 `notifyPlayerAboutToOpenIntelScreen()`。
2. 列表调用 `createIntelInfo(info, mode)`。
3. 小描述区域检查 `hasSmallDescription()`。
4. 小描述区域调用 `createSmallDescription(info, width, height)`。
5. 大描述区域检查 `hasLargeDescription()`。
6. 大描述区域调用 `createLargeDescription(panel, width, height)`。
7. 按钮需要确认时调用 `doesButtonHaveConfirmDialog(buttonId)`。
8. 确认后调用 `buttonPressConfirmed(buttonId, ui)` 或 `storyActionConfirmed(buttonId, ui)`。
9. UI 通过 `IntelUIAPI.updateUIForItem(plugin)` 或 `recreateIntelUI()` 刷新显示。

### Intel 地图与排序链路

1. Intel 页面或地图调用 `getIntelTags(map)`。
2. `getIntelTags(map)` 根据 important、new 和地图上下文返回 tag 集合。
3. 地图调用 `getMapLocation(map)` 获取定位实体。
4. 地图调用 `getArrowData(map)` 获取箭头显示数据。
5. 列表排序调用 `getSortTier()`。
6. 同 tier 内排序调用 `getSortString()`。
7. manager 可调用 `IntelManagerAPI.sortIntel(toSort)` 对 Intel 列表排序。

### Intel 结束与移除链路

1. 调用 `endAfterDelay()` 或 `endAfterDelay(days)`。
2. `BaseIntelPlugin` 设置 ending 状态和剩余天数。
3. 首次进入 ending 时调用 `notifyEnding()`。
4. 生涯推进调用 `advance(amount)`。
5. `advance(amount)` 将秒数转换为 campaign days。
6. ending 倒计时归零后设置 ended 状态。
7. ended 时调用 `notifyEnded()`。
8. manager 调用 `removeAllThatShouldBeRemoved()`。
9. `shouldRemoveIntel()` 返回 true 后 manager 移除 Intel。
10. 移除时调用 `reportRemovedIntel()`。

## 规范

- `BaseIntelPlugin.endAfterDelay()` 默认使用 `getBaseDaysAfterEnd()`。
- `BaseIntelPlugin.getBaseDaysAfterEnd()` 默认返回 `3` campaign days。
- `BaseIntelPlugin.isHidden()` 受教程状态和本地 hidden 字段影响。
- `BaseIntelPlugin.runWhilePaused()` 默认返回 false。
- `BaseIntelPlugin.sendUpdateIfPlayerHasIntel(...)` 要求 Intel 已有玩家可见 timestamp。
- `BaseIntelPlugin.shouldRemoveIntel()` 在 ended 后返回 true。
- `CampaignUIAPI.addMessage(IntelInfoPlugin, MessageClickAction, Object)` 可把消息点击目标绑定到 Intel tab。
- `IntelInfoPlugin.NEW_DAYS` 为 `5`。
- `IntelInfoPlugin.canMakeVisibleToPlayer(...)` 仅由 `IntelManagerAPI.queueIntel(...)` 可见性链路检查。
- `IntelInfoPlugin.getIntelTags(map)` 必须处理 `map == null`。
- `IntelInfoPlugin.getTimeRemainingFraction()` 在无时间概念时返回 `0`。
- `IntelManagerAPI.addIntel(...)` 直接加入 Intel 集合。
- `IntelManagerAPI.queueIntel(plugin)` 通过通信中继范围进入玩家可见链路。
- `IntelManagerAPI.queueIntel(plugin, maxCommQueueDays)` 可设置通信队列最长等待天数。
- `IntelManagerAPI.removeIntel(plugin)` 同时移除与出队。
- `IntelManagerAPI.removeAllThatShouldBeRemoved()` 按 `shouldRemoveIntel()` 清理。

## 陷阱

- `BaseIntelPlugin` 同时实现 `EveryFrameScript`，Sector script 注册和 Intel manager 可见性属于两条链。
- `BaseIntelPlugin.isHidden()` 会让已有玩家可见 timestamp 的 Intel 在 UI 和消息中隐藏。
- `BaseIntelPlugin.listInfoParam` 只在发送更新或渲染更新信息期间有效。
- `BaseIntelPlugin.shouldRemoveIntel()` 对 ending 且未可见的 Intel 可直接返回 true。
- `IntelInfoPlugin.getIntelTags(map)` 接收到 `null` map 时仍需返回有效 tag 集合。
- `IntelManagerAPI.addIntel(...)` 绕过 `canMakeVisibleToPlayer(...)` 的通信中继可见性检查。
- `IntelManagerAPI.queueIntel(plugin)` 中的 Intel 会等待通信中继范围或移除条件。
