package data.example;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * 专长系统的战役能力菜单。
 *
 * 维护说明：
 * 1. 该插件由战役能力 [`PCOpenSpecializationAbility`](src/data/scripts/specialization/ability/PCOpenSpecializationAbility.java)
 *    通过 [`CampaignUIAPI.showInteractionDialog()`](starfarer.api/com/fs/starfarer/api/campaign/CampaignUIAPI.java:42)
 *    打开；
 * 2. 实测从能力按钮路径调用 [`InteractionDialogAPI.showCustomDialog()`](starfarer.api/com/fs/starfarer/api/campaign/InteractionDialogAPI.java:60)
 *    会在原版 UI 层触发 “May only anchor on siblings” 崩溃；
 * 3. 因此能力入口使用稳定的标准 [`InteractionDialogAPI`](starfarer.api/com/fs/starfarer/api/campaign/InteractionDialogAPI.java:14)
 *    文本面板 + 选项面板实现，先保证功能可用；
 * 4. 自定义 UI 相关类暂时保留，后续如果找到可稳定承载 custom dialog 的上下文，再重新接入。
 */
public class PCSpecializationDialogPlugin implements InteractionDialogPlugin {

    protected static final String OPT_CLOSE = "pc_spec_close";
    protected static final String OPT_REFRESH = "pc_spec_refresh";
    protected static final String OPT_BRANCH_PREFIX = "pc_spec_branch:";
    protected static final String OPT_LEARN_PREFIX = "pc_spec_learn:";
    protected static final String OPT_PAUSE_PREFIX = "pc_spec_pause:";
    protected static final String OPT_CANCEL_PREFIX = "pc_spec_cancel:";

    protected final PCSpecializationIntel intel;
    protected InteractionDialogAPI dialog;
    protected TextPanelAPI text;
    protected OptionPanelAPI options;
    protected Branch currentBranch = Branch.LOGISTICS;
    protected boolean overviewPage = true;
    protected boolean branchListExpanded = false;

    public PCSpecializationDialogPlugin(PCSpecializationIntel intel) {
        this(intel, Branch.LOGISTICS, true, false);
    }

    public PCSpecializationDialogPlugin(PCSpecializationIntel intel, Branch currentBranch) {
        this(intel, currentBranch, false, false);
    }

    public PCSpecializationDialogPlugin(PCSpecializationIntel intel, Branch currentBranch, boolean overviewPage) {
        this(intel, currentBranch, overviewPage, false);
    }

    public PCSpecializationDialogPlugin(PCSpecializationIntel intel, Branch currentBranch, boolean overviewPage, boolean branchListExpanded) {
        this.intel = intel;
        this.overviewPage = overviewPage;
        this.branchListExpanded = branchListExpanded;
        if (currentBranch != null) {
            this.currentBranch = currentBranch;
        }
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.text = dialog.getTextPanel();
        this.options = dialog.getOptionPanel();

        dialog.setPromptText("专长研修系统");
        dialog.setTextWidth(0f);
        dialog.setTextHeight(0f);
        dialog.hideTextPanel();
        dialog.hideVisualPanel();
        dialog.getOptionPanel().clearOptions();
        dialog.setOptionOnEscape("关闭", OPT_CLOSE);

        /*
         * Starsector 的 CustomVisualDialog 实际会额外叠加原版外框、滚动条与安全边距。
         * 之前使用 1600 宽度时，在 1920 分辨率下看似还能放下，但右侧滚动条和自绘边框会持续擦边。
         * 这里主动收窄根面板，让后续 TooltipMakerAPI 子区域有真实的横向安全空间。
         */
        dialog.showCustomVisualDialog(1460f, 900f, new PCSpecializationVisualDelegate(intel, dialog, currentBranch, overviewPage, branchListExpanded));
    }

    /**
     * 在原版视觉面板中显示专属 UI。
     *
     * 关键区别：
     * 1. 不再调用 [`InteractionDialogAPI.showCustomDialog()`](starfarer.api/com/fs/starfarer/api/campaign/InteractionDialogAPI.java:60)，
     *    避免能力路径下已确认存在的 sibling anchor 崩溃；
     * 2. 改用 [`VisualPanelAPI.showCustomPanel()`](starfarer.api/com/fs/starfarer/api/campaign/VisualPanelAPI.java:37)
     *    把专属面板嵌入标准 InteractionDialog 的视觉区域；
     * 3. 标准选项区只保留关闭/备用刷新，主要操作在视觉面板内完成。
     */
    protected void showVisualUiPage() {
        if (dialog == null || text == null || options == null) {
            return;
        }

        text.clear();
        options.clearOptions();

        PCSpecializationPanelPlugin panelPlugin = new PCSpecializationPanelPlugin(intel);
        com.fs.starfarer.api.ui.CustomPanelAPI panel = dialog.getVisualPanel().showCustomPanel(980f, 560f, panelPlugin);
        panelPlugin.create(panel);

        Color h = Misc.getHighlightColor();
        PCSpecializationData data = PCSpecializationData.get();
        text.addPara("Procyon 专长研修系统", h);
        text.addPara("已打开专属研修界面。当前队列：%s/%s，已学等级：%s。",
                h,
                String.valueOf(data.getQueueSize()),
                String.valueOf(PCSpecializationConstants.QUEUE_SIZE),
                String.valueOf(data.getTotalLearnedLevels()));

        options.addOption("刷新界面", OPT_REFRESH);
        options.addOption("关闭", OPT_CLOSE);
        options.setShortcut(OPT_CLOSE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    /**
     * 备用文本页面。
     *
     * 如果后续发现视觉面板在某些上下文不稳定，可以临时回退到该方法。
     */
    protected void showMainPage() {
        if (dialog == null || text == null || options == null) {
            return;
        }

        PCSpecializationData data = PCSpecializationData.get();
        Color h = Misc.getHighlightColor();
        Color good = Misc.getPositiveHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();

        text.clear();
        options.clearOptions();

        String credits = "N/A";
        if (Global.getSector() != null && Global.getSector().getPlayerFleet() != null) {
            credits = Misc.getWithDGS(Global.getSector().getPlayerFleet().getCargo().getCredits().get());
        }

        text.addPara("Procyon 专长研修系统", h);
        text.addPara("在此界面可查看研修队列、分支进度和专长详情；也可直接使用右侧选项开始、暂停或取消研修。");
        text.addPara("星币：%s    已学等级：%s    队列：%s/%s    当前分支：%s",
                h,
                credits,
                String.valueOf(data.getTotalLearnedLevels()),
                String.valueOf(data.getQueueSize()),
                String.valueOf(PCSpecializationConstants.QUEUE_SIZE),
                currentBranch.getDisplayName());

        addQueueText(data, h, good, bad);
        addSpecText(data, h, good, bad);
        addOptions(data, h, bad);
    }

    protected void addQueueText(PCSpecializationData data, Color h, Color good, Color bad) {
        text.addPara("");
        text.addPara("研修队列", h);

        if (data.getQueue().isEmpty()) {
            text.addPara("当前没有正在研修的项目。");
            return;
        }

        List<QueueItem> queue = data.getQueue();
        for (int i = 0; i < queue.size(); i++) {
            QueueItem item = queue.get(i);
            PCSpecializationSpec spec = PCSpecializationConstants.getSpec(item.getSpecId());
            if (spec == null) {
                continue;
            }

            String state = item.isPaused() ? "已暂停" : "进行中";
            String progress = String.valueOf(Math.round(item.getProgressFraction() * 100f)) + "%";
            text.addPara("槽位 %s：%s Lv%s    %s    剩余 %s 天    进度 %s",
                    h,
                    String.valueOf(i + 1),
                    spec.getName(),
                    String.valueOf(item.getTargetLevel()),
                    state,
                    String.format("%.1f", item.getRemainingDays()),
                    progress);
        }
    }

    protected void addSpecText(PCSpecializationData data, Color h, Color good, Color bad) {
        text.addPara("");
        text.addPara(currentBranch.getDisplayName(), h);

        List<PCSpecializationSpec> specs = PCSpecializationConstants.getSpecsForBranch(currentBranch);
        for (PCSpecializationSpec spec : specs) {
            int level = data.getLevel(spec.getId());
            String block = data.getBlockReason(spec.getId());

            text.addPara("%s    Lv%s/%s", h,
                    spec.getName(),
                    String.valueOf(level),
                    String.valueOf(spec.getMaxLevel()));
            text.addPara(spec.getDescription());
            text.addPara("当前效果：%s", good, spec.getEffectDescription(level));

            if (level < spec.getMaxLevel()) {
                text.addPara("下一级费用：%s 星币    %s 补给    学习时间：%s 天    额外物资：%s",
                        h,
                        Misc.getWithDGS(spec.getCreditCostForTargetLevel(level + 1)),
                        Misc.getWithDGS(spec.getSupplyCostForTargetLevel(level + 1)),
                        String.format("%.1f", spec.getDaysPerLevel()),
                        getAdditionalCostText(spec, level + 1));
            } else {
                text.addPara("该专长已满级。", good);
            }

            if (!spec.getPrerequisites().isEmpty()) {
                StringBuilder builder = new StringBuilder();
                for (String preId : spec.getPrerequisites()) {
                    PCSpecializationSpec pre = PCSpecializationConstants.getSpec(preId);
                    if (pre != null) {
                        if (builder.length() > 0) {
                            builder.append(" / ");
                        }
                        builder.append(pre.getName());
                    }
                }
                text.addPara("前置条件：%s", h, builder.toString());
            }

            if (block != null && level < spec.getMaxLevel()) {
                text.addPara("当前不可学习：%s", bad, block);
            }
        }
    }

    protected String getAdditionalCostText(PCSpecializationSpec spec, int targetLevel) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : spec.getCommodityCostsForTargetLevel(targetLevel).entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().intValue() <= 0) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(getCommodityDisplayName(entry.getKey())).append(" ").append(Misc.getWithDGS(entry.getValue().intValue()));
        }

        int interceptorLpcCost = spec.getInterceptorLpcCostForTargetLevel(targetLevel);
        if (interceptorLpcCost > 0) {
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append("截击机 LPC ").append(Misc.getWithDGS(interceptorLpcCost));
        }

        if (builder.length() <= 0) {
            return "无";
        }
        return builder.toString();
    }

    protected String getCommodityDisplayName(String commodityId) {
        if (Global.getSettings() == null || commodityId == null) {
            return commodityId == null ? "未知物资" : commodityId;
        }

        CommoditySpecAPI commoditySpec = Global.getSettings().getCommoditySpec(commodityId);
        if (commoditySpec == null) {
            return commodityId;
        }
        return commoditySpec.getName();
    }

    protected void addOptions(PCSpecializationData data, Color h, Color bad) {
        options.addOption("刷新", OPT_REFRESH);

        for (Branch branch : Branch.values()) {
            String label = branch == currentBranch ? "当前分支：" + branch.getDisplayName() : "切换到：" + branch.getDisplayName();
            options.addOption(label, OPT_BRANCH_PREFIX + branch.name());
            if (branch == currentBranch) {
                options.setEnabled(OPT_BRANCH_PREFIX + branch.name(), false);
            }
        }

        List<PCSpecializationSpec> specs = PCSpecializationConstants.getSpecsForBranch(currentBranch);
        for (PCSpecializationSpec spec : specs) {
            String block = data.getBlockReason(spec.getId());
            String label = block == null ? "开始研修：" + spec.getName() : "不可研修：" + spec.getName();
            String id = OPT_LEARN_PREFIX + spec.getId();
            options.addOption(label, id, block);
            options.setEnabled(id, block == null);
        }

        List<QueueItem> queue = data.getQueue();
        for (int i = 0; i < queue.size(); i++) {
            QueueItem item = queue.get(i);
            PCSpecializationSpec spec = PCSpecializationConstants.getSpec(item.getSpecId());
            String name = spec == null ? item.getSpecId() : spec.getName();

            options.addOption((item.isPaused() ? "继续：" : "暂停：") + name, OPT_PAUSE_PREFIX + i);
            options.addOption("取消并返还 50%：" + name, OPT_CANCEL_PREFIX + i);
        }

        options.addOption("关闭", OPT_CLOSE);
        options.setShortcut(OPT_CLOSE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (!(optionData instanceof String)) {
            return;
        }

        String id = (String) optionData;
        PCSpecializationData data = PCSpecializationData.get();

        if (OPT_CLOSE.equals(id)) {
            if (dialog != null) {
                dialog.dismiss();
            }
            return;
        }

        if (OPT_REFRESH.equals(id)) {
            showMainPage();
            return;
        }

        if (id.startsWith(OPT_BRANCH_PREFIX)) {
            try {
                currentBranch = Branch.valueOf(id.substring(OPT_BRANCH_PREFIX.length()));
            } catch (IllegalArgumentException ex) {
                // 非法分支参数直接忽略，避免 UI 中断。
            }
            showMainPage();
            return;
        }

        if (id.startsWith(OPT_LEARN_PREFIX)) {
            String specId = id.substring(OPT_LEARN_PREFIX.length());
            if (data.enqueue(specId)) {
                PCSpecializationIntel.notifyUpdated();
            }
            showMainPage();
            return;
        }

        if (id.startsWith(OPT_PAUSE_PREFIX)) {
            try {
                int index = Integer.parseInt(id.substring(OPT_PAUSE_PREFIX.length()));
                if (index >= 0 && index < data.getQueue().size()) {
                    QueueItem item = data.getQueue().get(index);
                    item.setPaused(!item.isPaused());
                    PCSpecializationIntel.notifyUpdated();
                }
            } catch (NumberFormatException ex) {
                // 非法队列索引直接忽略。
            }
            showMainPage();
            return;
        }

        if (id.startsWith(OPT_CANCEL_PREFIX)) {
            try {
                int index = Integer.parseInt(id.substring(OPT_CANCEL_PREFIX.length()));
                QueueItem item = data.cancelQueueItem(index, 0.5f);
                if (item != null) {
                    PCSpecializationIntel.notifyUpdated();
                }
            } catch (NumberFormatException ex) {
                // 非法队列索引直接忽略。
            }
            showMainPage();
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }

    public InteractionDialogAPI getDialog() {
        return dialog;
    }
}