package data.example;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;


/**
 * 专长系统的自定义弹窗委托。
 *
 * 维护说明：
 * 1. 该类只负责“打开一整个专属界面弹窗”；
 * 2. 具体布局、按钮、绘制细节交给 [`PCSpecializationPanelPlugin`](src/data/scripts/specialization/ui/PCSpecializationPanelPlugin.java)；
 * 3. Intel 侧只需要调用 InteractionDialogAPI.showCustomDialog(...) 即可复用这里；
 * 4. 后续如果要把界面拆成多个分页，优先在面板插件里扩展，而不是修改弹窗生命周期。
 */
public class PCSpecializationDialogDelegate extends BaseCustomDialogDelegate {

    /**
     * 当前 Intel 引用。
     * 主要用于界面内操作完成后，通知 Intel 刷新列表摘要与更新提示。
     */
    protected final PCSpecializationIntel intel;

    /**
     * 自定义弹窗的回调对象。
     * 目前主要用于后续可能的主动关闭、跳转详情确认等操作。
     */
    protected CustomDialogCallback callback;

    /**
     * 专属面板插件。
     * 这里提前构造，确保 createCustomDialog() 与 getCustomPanelPlugin() 返回的是同一个实例。
     */
    protected final PCSpecializationPanelPlugin panelPlugin;

    /**
     * 外层交互对话引用。
     * 用于在自定义弹窗关闭后，顺带把承载它的空白对话壳一起关闭。
     */
    protected final InteractionDialogAPI dialog;

    public PCSpecializationDialogDelegate(PCSpecializationIntel intel, InteractionDialogAPI dialog) {
        this.intel = intel;
        this.dialog = dialog;
        this.panelPlugin = new PCSpecializationPanelPlugin(intel);
    }

    @Override
    public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
        this.callback = callback;
        panelPlugin.setCallback(callback);

        // 交由专属面板插件在整个 custom panel 中创建结构化 UI。
        panelPlugin.create(panel);
    }

    @Override
    public boolean hasCancelButton() {
        return true;
    }

    @Override
    public String getConfirmText() {
        return "关闭";
    }

    @Override
    public String getCancelText() {
        return "返回";
    }

    @Override
    public void customDialogConfirm() {
        // 当前界面内的按钮操作都是即时生效的，因此确认键只承担“关闭窗口”的职责。
        if (intel != null) {
            PCSpecializationIntel.notifyUpdated();
        }
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public void customDialogCancel() {
        // 与确认逻辑一致：取消也只关闭，不回滚已经执行的操作。
        if (intel != null) {
            PCSpecializationIntel.notifyUpdated();
        }
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return panelPlugin;
    }

    public CustomDialogCallback getCallback() {
        return callback;
    }

    public void closeFromCode() {
        if (callback != null) {
            callback.dismissCustomDialog(0);
        } else if (dialog != null) {
            dialog.dismiss();
        } else if (Global.getSector() != null && Global.getSector().getCampaignUI().getCurrentInteractionDialog() != null) {
            Global.getSector().getCampaignUI().getCurrentInteractionDialog().dismiss();
        }
    }
}