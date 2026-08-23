# 生涯交互对话

来源：原版 Starsector。

## 定义

生涯交互对话是原版生涯 UI 中承载 dialog、plugin、文本、选项、视觉、picker、core UI 和战斗启动的接口集合。

## 参考

- `com.fs.starfarer.api.campaign.CampaignEntityPickerListener`：提供地图实体 picker 的选择、取消、提示、箭头、标记和可选星系入口。
- `com.fs.starfarer.api.campaign.CampaignUIAPI`：提供交互对话显示、当前对话读取、战斗启动和 core UI 显示入口。
- `com.fs.starfarer.api.campaign.CargoPickerListener`：提供 cargo picker 的选择、取消和文本面板重建入口。
- `com.fs.starfarer.api.campaign.CoreInteractionListener`：提供 core UI 关闭回调入口。
- `com.fs.starfarer.api.campaign.CustomDialogDelegate`：提供自定义 dialog 面板创建、确认、取消和 custom panel plugin 入口。
- `com.fs.starfarer.api.campaign.CustomProductionPickerDelegate`：提供自定义生产 picker 的可选蓝图、价格、上限和选择回调入口。
- `com.fs.starfarer.api.campaign.CustomVisualDialogDelegate`：提供自定义 visual dialog 的初始化、淡入淡出、推进和关闭回调入口。
- `com.fs.starfarer.api.campaign.FleetMemberPickerListener`：提供舰队成员 picker 的选择和取消入口。
- `com.fs.starfarer.api.campaign.GroundRaidTargetPickerDelegate`：提供 ground raid 目标 picker 的选择、取消、数值展示和损失估算入口。
- `com.fs.starfarer.api.campaign.IndustryPickerListener`：提供 industry picker 的选择和取消入口。
- `com.fs.starfarer.api.campaign.InteractionDialogAPI`：提供交互对话尺寸、prompt、面板、目标、plugin、picker、core UI、战斗和关闭入口。
- `com.fs.starfarer.api.campaign.InteractionDialogPlugin`：提供交互对话 plugin 生命周期、选项回调、战斗返回、上下文和 memoryMap 入口。
- `com.fs.starfarer.api.campaign.OptionPanelAPI`：提供对话选项、快捷键、tooltip、selector、确认和 story option 入口。
- `com.fs.starfarer.api.campaign.TextPanelAPI`：提供对话文本、字体、段落、高亮、tooltip、cost panel 和图片入口。
- `com.fs.starfarer.api.campaign.VisualPanelAPI`：提供对话视觉、人物、舰队、星球、战利品、core UI、custom panel 和地图标记入口。

## 边界

- `CampaignEntityPickerListener` 归属 campaign entity picker 的选择、取消、说明文本、确认条件和地图标记。
- `CampaignUIAPI.getCurrentInteractionDialog()` 归属当前显示中的交互对话读取。
- `CampaignUIAPI.showInteractionDialog(...)` 归属交互对话创建和显示。
- `CargoPickerListener` 归属 cargo picker 的选择结果、取消结果和 picker 文本重建。
- `CoreInteractionListener` 归属 visual panel core UI 关闭回调。
- `CustomDialogDelegate` 归属 `showCustomDialog(...)` 的面板创建、按钮文本和确认取消回调。
- `CustomProductionPickerDelegate` 归属 custom production picker 的可选项目集合、价格、上限和生产选择结果。
- `CustomVisualDialogDelegate` 归属 `showCustomVisualDialog(...)` 的面板初始化、噪声透明度、推进和关闭回调。
- `FleetMemberPickerListener` 归属 fleet member picker 的选择结果与取消结果。
- `GroundRaidTargetPickerDelegate` 归属 ground raid target picker 的选择、取消、效果、损失、空间和数值展示。
- `IndustryPickerListener` 归属 industry picker 的选择结果与取消结果。
- `InteractionDialogAPI` 归属当前 dialog 的尺寸、prompt、opacity、背景遮罩、文本面板、选项面板、视觉面板、交互目标和 plugin。
- `InteractionDialogAPI.startBattle(...)` 归属从当前 dialog 启动战斗。
- `InteractionDialogPlugin` 归属 dialog plugin 初始化、选项选择、鼠标悬停、按帧推进、战斗返回和 memoryMap。
- `OptionPanelAPI` 归属 dialog option 集合、selector、tooltip、快捷键、确认态和 story option 参数。
- `TextPanelAPI` 归属 dialog text panel 的段落、字体、高亮、tooltip、cost panel 和图片。
- `VisualPanelAPI` 归属 dialog visual panel 的人物、舰队、星球、舰队成员、图片、战利品、core UI 和 custom panel。

## 链路

### Dialog 创建链路

1. 代码取得 `Global.getSector().getCampaignUI()`。
2. 调用 `showInteractionDialog(plugin, interactionTarget)` 或 `showInteractionDialog(interactionTarget)`。
3. `CampaignUIAPI` 检查当前 UI 是否可显示 dialog。
4. 创建成功时生成 `InteractionDialogAPI`。
5. 系统调用 `InteractionDialogPlugin.init(dialog)`。
6. plugin 写入 text、options 和 visual。
7. `getCurrentInteractionDialog()` 可返回当前 dialog。

### Dialog plugin 链路

1. `InteractionDialogPlugin.init(dialog)` 接收当前 dialog。
2. plugin 读取 `getTextPanel()`、`getOptionPanel()` 和 `getVisualPanel()`。
3. 玩家选择 option 后调用 `optionSelected(optionText, optionData)`。
4. 玩家悬停 option 后调用 `optionMousedOver(optionText, optionData)`。
5. dialog 推进时调用 `advance(amount)`。
6. 战斗结束回到 dialog 时调用 `backFromEngagement(result)`。
7. 需要规则或状态上下文时读取 `getContext()` 或 `getMemoryMap()`。

### Dialog 面板链路

1. 代码从 dialog 取得 `TextPanelAPI`、`OptionPanelAPI` 或 `VisualPanelAPI`。
2. `TextPanelAPI` 写入段落、字体、高亮、tooltip、cost panel 或图片。
3. `OptionPanelAPI` 添加 option、selector、tooltip、快捷键、确认或 story option。
4. `VisualPanelAPI` 显示人物、舰队、星球、舰队成员、图片、loot、core UI 或 custom panel。
5. `hideTextPanel()`、`showTextPanel()`、`hideVisualPanel()` 和 `showVisualPanel()` 控制 dialog 面板显示。
6. `setTextWidth(...)`、`setTextHeight(...)`、`setXOffset(...)` 和 `setYOffset(...)` 控制 dialog 布局参数。

### Picker 链路

1. 代码调用 dialog 的 picker 显示入口。
2. dialog 创建对应 picker UI。
3. listener 或 delegate 提供可显示数据、确认条件、tooltip 或数值。
4. 玩家确认后调用 picked 回调。
5. 玩家取消后调用 cancelled 回调。
6. picker 关闭后控制权返回当前 dialog。

### Core UI 与战斗入口链路

1. dialog 调用 `makeOptionOpenCore(...)` 或 visual panel 调用 `showCore(...)`。
2. 玩家选择对应 option 或 visual panel 打开 core UI。
3. core UI 关闭时调用 `CoreInteractionListener.coreUIDismissed()`。
4. dialog 调用 `startBattle(context)` 或 campaign UI 调用 `startBattle(context)` 进入生涯战斗流程。
5. 战斗结束后调用 dialog plugin 的 `backFromEngagement(result)`。

### 关闭链路

1. 代码调用 `setOptionOnEscape(text, optionId)` 设置 ESC 行为。
2. 代码调用 `setOptionOnConfirm(text, optionId)` 设置确认行为。
3. 代码调用 `dismiss()` 正常关闭 dialog。
4. 代码调用 `dismissAsCancel()` 以取消语义关闭 dialog。
5. `CustomDialogDelegate` 的 ESC 关闭会调用 `customDialogCancel()`。

## 规范

- `CampaignUIAPI.showInteractionDialog(...)` 返回 false 表示当前 UI 未显示新 dialog。
- `CampaignUIAPI.showInteractionDialog(plugin, interactionTarget)` 允许 interactionTarget 为 null。
- `CampaignUIAPI.showInteractionDialogFromCargo(...)` 使用 dismiss delegate 接收关闭回调。
- `CustomDialogDelegate.CustomDialogCallback.dismissCustomDialog(0)` 表示确认。
- `CustomDialogDelegate.CustomDialogCallback.dismissCustomDialog(1)` 表示取消。
- `CustomDialogDelegate.customDialogCancel()` 在 ESC 关闭自定义 dialog 时也会被调用。
- `InteractionDialogAPI.addOptionSelectedText(optionId)` 可写入当前 option 的选择文本。
- `InteractionDialogAPI.dismiss()` 关闭当前 dialog。
- `InteractionDialogAPI.dismissAsCancel()` 以取消语义关闭当前 dialog。
- `InteractionDialogAPI.getInteractionTarget()` 返回当前交互目标。
- `InteractionDialogAPI.makeOptionOpenCore(...)` 把 option 绑定到 core UI 打开行为。
- `InteractionDialogAPI.setOptionOnEscape(null, optionId)` 可让 ESC 不执行默认关闭行为。
- `InteractionDialogAPI.showCustomDialog(...)` 使用 `CustomDialogDelegate` 创建 custom panel。
- `InteractionDialogAPI.showCustomVisualDialog(...)` 使用 `CustomVisualDialogDelegate` 创建 visual custom panel。
- `InteractionDialogPlugin.getMemoryMap()` 返回当前 dialog plugin 暴露的 memoryMap。
- `OptionPanelAPI.addOptionConfirmation(...)` 为 option 增加确认步骤。
- `OptionPanelAPI.getSavedOptionList()` 和 `restoreSavedOptions(...)` 保存与恢复 option 列表。
- `OptionPanelAPI.setEnabled(...)` 只作用于 option。
- `TextPanelAPI.getDialog()` 返回所属 `InteractionDialogAPI`。
- `VisualPanelAPI.showCore(...)` 使用 `CoreInteractionListener` 接收 core UI 关闭。
- `VisualPanelAPI.showCore(CoreUITabId, SectorEntityToken, boolean, CoreInteractionListener)` 是废弃入口。

## 陷阱

- `CampaignUIAPI.showInteractionDialog(...)` 可因已有 dialog 正在显示而返回 false。
- `CustomDialogDelegate.customDialogCancel()` 会在 ESC 关闭时触发。
- `InteractionDialogAPI.getInteractionTarget()` 在部分自定义对话中需要调用方确认语义。
- `InteractionDialogAPI.setOptionOnEscape(null, optionId)` 会让 ESC 不执行关闭。
- `OptionPanelAPI.setEnabled(...)` 不作用于 selector。
- `TextPanelAPI.highlightInLastPara(...)` 要求高亮字符串按段落出现顺序传入。
- `VisualPanelAPI.isShowingPersonInfo(...)` 只检查 first person。
- `VisualPanelAPI.showCore(..., boolean noCost, ...)` 的 noCost 参数不被使用。
