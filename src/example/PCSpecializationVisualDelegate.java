package data.example;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;


/**
 * 专长研修系统的准全屏视觉 UI 委托。
 *
 * 维护说明：
 * 1. 该类使用 [`CustomVisualDialogDelegate`](starfarer.api/com/fs/starfarer/api/campaign/CustomVisualDialogDelegate.java:6)
 *    路线，目标是做成类似殖民地/仓库界面的完整 UI，而不是事件对话右侧的一张小图；
 * 2. 原版 [`DuelDialogDelegate`](starfarer.api/com/fs/starfarer/api/impl/campaign/eventide/DuelDialogDelegate.java:13)
 *    也是通过同一路线承载全屏交互面板；
 * 3. 真正的布局、按钮、自绘边框都交给 [`PCSpecializationPanelPlugin`](src/data/scripts/specialization/ui/PCSpecializationPanelPlugin.java)；
 * 4. 关闭操作由面板内按钮调用 [`DialogCallbacks.dismissDialog()`](starfarer.api/com/fs/starfarer/api/campaign/CustomVisualDialogDelegate.java:8)，
 *    因此交互不会再散落到原版选项列表。
 */
public class PCSpecializationVisualDelegate implements CustomVisualDialogDelegate {

    protected final PCSpecializationIntel intel;
    protected final InteractionDialogAPI dialog;
    protected final Branch currentBranch;
    protected final boolean overviewPage;
    protected final boolean branchListExpanded;
    protected final PCSpecializationPanelPlugin panelPlugin;
    protected DialogCallbacks callbacks;

    public PCSpecializationVisualDelegate(PCSpecializationIntel intel, InteractionDialogAPI dialog, Branch currentBranch, boolean overviewPage, boolean branchListExpanded) {
        this.intel = intel;
        this.dialog = dialog;
        this.currentBranch = currentBranch;
        this.overviewPage = overviewPage;
        this.branchListExpanded = branchListExpanded;
        this.panelPlugin = new PCSpecializationPanelPlugin(intel, currentBranch, overviewPage, branchListExpanded);
    }

    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        this.callbacks = callbacks;
        panelPlugin.setVisualCallbacks(callbacks);
        panelPlugin.create(panel);
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return panelPlugin;
    }

    @Override
    public float getNoiseAlpha() {
        return 0f;
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void reportDismissed(int option) {
        if (intel != null) {
            PCSpecializationIntel.notifyUpdated();
        }
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    public DialogCallbacks getCallbacks() {
        return callbacks;
    }
}