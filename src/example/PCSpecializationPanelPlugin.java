package data.example;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomDialogDelegate.CustomDialogCallback;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate.DialogCallbacks;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.DelayedActionScript;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 专长系统的专属面板插件。
 *
 * 维护说明：
 * 1. 这是“自定义弹窗内部真正的界面构建器”；
 * 2. 当前版本先用 TooltipMakerAPI 做出更现代的分区布局；
 * 3. 未来如果要加入科技树连线、拖拽画布、悬浮详情，可在这里继续演进；
 * 4. 所有按钮 id 都采用统一前缀协议，便于和 Intel / 数据层复用。
 */
public class PCSpecializationPanelPlugin extends BaseCustomUIPanelPlugin {

    public static final String BUTTON_PREFIX_LEARN = "dialog_learn:";
    public static final String BUTTON_PREFIX_PAUSE = "dialog_pause:";
    public static final String BUTTON_PREFIX_CANCEL = "dialog_cancel:";
    public static final String BUTTON_PREFIX_BRANCH = "dialog_branch:";
    public static final String BUTTON_OVERVIEW = "dialog_overview";
    public static final String BUTTON_CLOSE = "dialog_close";

    protected static final float PROGRESS_SEGMENT_WIDTH = 44f;
    protected static final float PROGRESS_SEGMENT_HEIGHT = 18f;
    protected static final float PROGRESS_SEGMENT_GAP = 6f;
    protected static final int PERCENT_PROGRESS_SEGMENTS = 8;

    protected final PCSpecializationIntel intel;
    protected CustomDialogCallback callback;
    protected DialogCallbacks visualCallbacks;
    protected CustomPanelAPI rootPanel;
    protected boolean closeRequested = false;

    /**
     * 当前 UI 内所有需要轮询的按钮。
     *
     * 说明：
     * 1. 在 [`CustomVisualDialogDelegate`](starfarer.api/com/fs/starfarer/api/campaign/CustomVisualDialogDelegate.java:6)
     *    路线下，部分环境不会稳定触发 [`TooltipMakerAPI.ActionListenerDelegate`](starfarer.api/com/fs/starfarer/api/ui/TooltipMakerAPI.java:44)；
     * 2. 因此参考 [`ui设计.txt`](ui设计.txt:197) 的做法，在 [`advance()`](src/data/scripts/specialization/ui/PCSpecializationPanelPlugin.java)
     *    中主动轮询 [`ButtonAPI.isChecked()`](starfarer.api/com/fs/starfarer/api/ui/ButtonAPI.java:17)；
     * 3. 每次重建 UI 时会清空并重新登记，避免旧按钮引用继续生效。
     */
    protected final List<ButtonAPI> trackedButtons = new ArrayList<ButtonAPI>();

    /**
     * 当前根面板的绝对位置。
     *
     * 自绘 UI 使用 [`PositionAPI.getX()`](starfarer.api/com/fs/starfarer/api/ui/PositionAPI.java:6) /
     * [`PositionAPI.getY()`](starfarer.api/com/fs/starfarer/api/ui/PositionAPI.java:7) 取得屏幕坐标，
     * 再通过 [`renderBelow()`](src/data/scripts/specialization/ui/PCSpecializationPanelPlugin.java:98) 在所有
     * Tooltip 组件下方绘制背景、边框与强调线。
     */
    protected PositionAPI rootPosition;

    /**
     * UI 分区尺寸缓存。
     *
     * 这些数值既用于 [`create()`](src/data/scripts/specialization/ui/PCSpecializationPanelPlugin.java:90) 放置组件，
     * 也用于 [`renderBelow()`](src/data/scripts/specialization/ui/PCSpecializationPanelPlugin.java:98) 绘制对应底板。
     */
    protected float topHeight = 108f;
    protected float queueHeight = 186f;
    protected float leftWidth = 220f;
    protected float contentGap = 18f;

    /**
     * 未来科技树/节点化界面的扩展点。
     *
     * 当前卡片布局先保持完整可用；后续可以把该字段用于“选中节点详情面板”，
     * 而无需改动数据层、队列层或效果注册表。
     */
    protected String selectedSpecId;

    /**
     * 当前挂在根面板上的四个主要 UI 区块引用。
     * 之所以显式缓存，是因为 API 没有提供通用的“getChildrenCopy()”能力，
     * 因此重绘时需要手动移除这些已知组件。
     */
    protected TooltipMakerAPI topBarRef;
    protected TooltipMakerAPI topCloseRef;
    protected TooltipMakerAPI sideBarRef;
    protected TooltipMakerAPI contentRef;
    protected TooltipMakerAPI queueBarRef;

    /**
     * 当前选中的分支。
     * 后续如果要做分页切换、搜索定位、树状布局，都可以从这里继续扩展。
     */
    protected Branch currentBranch = Branch.LOGISTICS;

    /**
     * 当前是否显示总览主页。
     *
     * 总览页用于承载“当前掌握、队列、加成摘要”等宏观信息；
     * 分支技能页只在玩家点击分支后打开，避免未来专长数量增加时主界面过载。
     */
    protected boolean overviewPage = true;
    protected boolean branchListExpanded = false;

    public PCSpecializationPanelPlugin(PCSpecializationIntel intel) {
        this(intel, Branch.LOGISTICS, true, false);
    }

    public PCSpecializationPanelPlugin(PCSpecializationIntel intel, Branch currentBranch) {
        this(intel, currentBranch, false, false);
    }

    public PCSpecializationPanelPlugin(PCSpecializationIntel intel, Branch currentBranch, boolean overviewPage) {
        this(intel, currentBranch, overviewPage, false);
    }

    public PCSpecializationPanelPlugin(PCSpecializationIntel intel, Branch currentBranch, boolean overviewPage, boolean branchListExpanded) {
        this.intel = intel;
        this.overviewPage = overviewPage;
        this.branchListExpanded = branchListExpanded;
        if (currentBranch != null) {
            this.currentBranch = currentBranch;
        }
    }

    public void setCallback(CustomDialogCallback callback) {
        this.callback = callback;
    }

    /**
     * 设置准全屏视觉 UI 的关闭回调。
     *
     * [`DialogCallbacks`](starfarer.api/com/fs/starfarer/api/campaign/CustomVisualDialogDelegate.java:7)
     * 用于由 UI 内按钮主动关闭 [`showCustomVisualDialog()`](starfarer.api/com/fs/starfarer/api/campaign/InteractionDialogAPI.java:106)
     * 创建的准全屏界面。
     */
    public void setVisualCallbacks(DialogCallbacks visualCallbacks) {
        this.visualCallbacks = visualCallbacks;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.rootPosition = position;
    }

    /**
     * 在 custom dialog 提供的根面板上构建完整 UI。
     */
    public void create(CustomPanelAPI panel) {
        this.rootPanel = panel;
        this.rootPosition = panel.getPosition();
        trackedButtons.clear();

        float width = panel.getPosition().getWidth();
        float height = panel.getPosition().getHeight();

        float safeOuterPad = 24f;
        float contentWidth = width - leftWidth - contentGap - safeOuterPad * 2f;
        float contentHeight = height - topHeight - queueHeight - 24f;
        TooltipMakerAPI topBar = panel.createUIElement(width - safeOuterPad * 2f, topHeight, false);
        prepareInteractiveTooltip(topBar);
        topBarRef = topBar;
        buildTopBar(topBar, width - safeOuterPad * 2f, topHeight);
        panel.updateUIElementSizeAndMakeItProcessInput(topBar);
        panel.addUIElement(topBar).inTL(safeOuterPad, 0f);

        TooltipMakerAPI topClose = panel.createUIElement(58f, 48f, false);
        prepareInteractiveTooltip(topClose);
        topCloseRef = topClose;
        buildCloseButton(topClose, 58f, 48f);
        panel.updateUIElementSizeAndMakeItProcessInput(topClose);
        panel.addUIElement(topClose).inTL(width - safeOuterPad - 64f, 14f);

        TooltipMakerAPI sideBar = panel.createUIElement(leftWidth - safeOuterPad, contentHeight, true);
        prepareInteractiveTooltip(sideBar);
        sideBarRef = sideBar;
        buildSideBar(sideBar, leftWidth - safeOuterPad, contentHeight);
        panel.updateUIElementSizeAndMakeItProcessInput(sideBar);
        panel.addUIElement(sideBar).inTL(safeOuterPad, topHeight + 10f);

        TooltipMakerAPI content = panel.createUIElement(contentWidth, contentHeight, true);
        prepareInteractiveTooltip(content);
        contentRef = content;
        buildContent(content, contentWidth, contentHeight);
        panel.updateUIElementSizeAndMakeItProcessInput(content);
        panel.addUIElement(content).inTL(leftWidth + contentGap + safeOuterPad, topHeight + 10f);

        /**
         * 队列区是横向超框最敏感的区域：
         * 1. 底板仍由 renderBelow() 绘制到全宽，保持视觉完整；
         * 2. 真正承载 Tooltip 内容的 queueBar 主动左右内缩，避免滚动条、按钮和自绘条贴到外框；
         * 3. 高度下调，减少底部队列对主技能页的空间占用。
         */
        TooltipMakerAPI queueBar = panel.createUIElement(width - safeOuterPad * 2f, queueHeight - 18f, true);
        prepareInteractiveTooltip(queueBar);
        queueBarRef = queueBar;
        buildQueueBar(queueBar, width - safeOuterPad * 2f, queueHeight - 18f);
        panel.updateUIElementSizeAndMakeItProcessInput(queueBar);
        panel.addUIElement(queueBar).inTL(safeOuterPad, height - queueHeight + 9f);
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (rootPosition == null) {
            return;
        }

        float x = rootPosition.getX();
        float y = rootPosition.getY();
        float width = rootPosition.getWidth();
        float height = rootPosition.getHeight();

        float contentHeight = height - topHeight - queueHeight - 24f;
        float contentX = x + leftWidth + contentGap;
        float contentY = y + queueHeight + 10f;
        float contentWidth = width - leftWidth - contentGap;
        float sideY = y + queueHeight + 10f;
        float topY = y + height - topHeight;
        float queueY = y;

        Color bg = new Color(4, 8, 14, 236);
        Color panel = new Color(12, 18, 30, 218);
        Color panelAlt = new Color(18, 27, 43, 228);
        Color accentDim = new Color(42, 111, 168, 120);
        Color accent = getBranchAccent(190);
        Color accentSoft = getBranchAccent(55);
        Color shadow = new Color(0, 0, 0, 178);

        beginUiDraw();
        renderRect(x - 8f, y - 8f, width + 16f, height + 16f, shadow, alphaMult);
        renderRect(x, y, width, height, bg, alphaMult);
        renderRect(x + 8f, topY + 4f, width - 16f, topHeight - 8f, panelAlt, alphaMult);
        renderRect(x + 8f, sideY, leftWidth - 8f, contentHeight, panel, alphaMult);
        renderRect(contentX - 4f, contentY, contentWidth - 8f, contentHeight, panel, alphaMult);
        renderRect(x + 8f, queueY + 8f, width - 16f, queueHeight - 16f, panelAlt, alphaMult);

        renderRect(x + 12f, topY + 8f, width - 24f, 2f, accent, alphaMult);
        renderRect(x + 12f, topY + 13f, width - 24f, 1f, accentSoft, alphaMult);
        renderRect(x + 14f, sideY + contentHeight - 4f, leftWidth - 20f, 2f, accent, alphaMult);
        renderRect(contentX - 4f, contentY + contentHeight - 2f, contentWidth - 8f, 2f, accentDim, alphaMult);
        renderRect(x + 12f, queueY + queueHeight - 14f, width - 24f, 2f, accentDim, alphaMult);

        renderBorder(x, y, width, height, accent, alphaMult, 1.5f);
        renderBorder(x + 8f, topY + 4f, width - 16f, topHeight - 8f, accentDim, alphaMult, 1f);
        renderBorder(x + 8f, sideY, leftWidth - 8f, contentHeight, accentDim, alphaMult, 1f);
        renderBorder(contentX - 4f, contentY, contentWidth - 8f, contentHeight, accentDim, alphaMult, 1f);
        renderBorder(x + 8f, queueY + 8f, width - 16f, queueHeight - 16f, accentDim, alphaMult, 1f);

        renderCornerAccents(x, y, width, height, accent, alphaMult);
        endUiDraw();
    }

    /**
     * UI 自绘开始前设置 OpenGL 状态。
     *
     * 当前只绘制纯色矩形和线段，因此关闭纹理并启用普通 alpha 混合即可。
     */
    protected void beginUiDraw() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * UI 自绘结束后恢复常用状态，避免影响后续原版 Tooltip 元素渲染。
     */
    protected void endUiDraw() {
        GL11.glLineWidth(1f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    protected void renderRect(float x, float y, float width, float height, Color color, float alphaMult) {
        Misc.renderQuad(x, y, width, height, color, alphaMult);
    }

    protected void renderBorder(float x, float y, float width, float height, Color color, float alphaMult, float lineWidth) {
        Misc.setColor(color, alphaMult);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + height);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x + width, y);
        GL11.glEnd();
    }

    protected void renderCornerAccents(float x, float y, float width, float height, Color color, float alphaMult) {
        float len = 22f;
        float inset = 10f;
        Misc.setColor(color, alphaMult);
        GL11.glLineWidth(2f);
        GL11.glBegin(GL11.GL_LINES);

        GL11.glVertex2f(x + inset, y + inset);
        GL11.glVertex2f(x + inset + len, y + inset);
        GL11.glVertex2f(x + inset, y + inset);
        GL11.glVertex2f(x + inset, y + inset + len);

        GL11.glVertex2f(x + width - inset, y + inset);
        GL11.glVertex2f(x + width - inset - len, y + inset);
        GL11.glVertex2f(x + width - inset, y + inset);
        GL11.glVertex2f(x + width - inset, y + inset + len);

        GL11.glVertex2f(x + inset, y + height - inset);
        GL11.glVertex2f(x + inset + len, y + height - inset);
        GL11.glVertex2f(x + inset, y + height - inset);
        GL11.glVertex2f(x + inset, y + height - inset - len);

        GL11.glVertex2f(x + width - inset, y + height - inset);
        GL11.glVertex2f(x + width - inset - len, y + height - inset);
        GL11.glVertex2f(x + width - inset, y + height - inset);
        GL11.glVertex2f(x + width - inset, y + height - inset - len);

        GL11.glEnd();
    }

    /**
     * 为 Tooltip UI 元素绑定统一的按钮回调代理。
     *
     * 关键点：
     * 1. [`TooltipMakerAPI.setActionListenerDelegate()`](starfarer.api/com/fs/starfarer/api/ui/TooltipMakerAPI.java:413)
     *    需要在创建按钮前调用；
     * 2. 这样无论按钮位于主 tooltip 还是子 tooltip，最终都会回流到
     *    [`buttonPressed()`](src/data/scripts/specialization/ui/PCSpecializationPanelPlugin.java:258)；
     * 3. 这里只转发 data，因为我们的按钮协议本身就是以 id 字符串驱动。
     */
    protected void prepareInteractiveTooltip(TooltipMakerAPI ui) {
        ui.setForceProcessInput(true);
        ui.setActionListenerDelegate(new TooltipMakerAPI.ActionListenerDelegate() {
            @Override
            public void actionPerformed(Object data, Object source) {
                buttonPressed(data);
            }
        });
    }

    /**
     * 登记需要在 [`advance()`](src/data/scripts/specialization/ui/PCSpecializationPanelPlugin.java)
     * 中轮询的按钮。
     */
    protected void trackButton(ButtonAPI button, Object data) {
        if (button == null) {
            return;
        }
        button.setCustomData(data);
        trackedButtons.add(button);
    }

    @Override
    public void advance(float amount) {
        for (ButtonAPI button : new ArrayList<ButtonAPI>(trackedButtons)) {
            if (button == null || !button.isEnabled() || !button.isChecked()) {
                continue;
            }

            button.setChecked(false);
            buttonPressed(button.getCustomData());
            break;
        }

    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (events == null) {
            return;
        }

        for (InputEventAPI event : events) {
            if (event == null || event.isConsumed()) {
                continue;
            }

            if (event.isKeyboardEvent()
                    && event.isKeyDownEvent()
                    && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                event.consume();
                closeUi();
                return;
            }
        }
    }

    protected void buildTopBar(TooltipMakerAPI ui, float width, float height) {
        PCSpecializationData data = PCSpecializationData.get();

        /**
         * 星币文本单独格式化为字符串。
         *
         * 这里不能使用 float 接收 [`Misc.getWithDGS()`](starfarer.api/com/fs/starfarer/api/util/Misc.java) 的结果，
         * 因为该方法返回的是已经格式化完成的显示字符串。
         */
        String credits = "N/A";
        if (Global.getSector() != null
                && Global.getSector().getPlayerFleet() != null) {
            credits = Misc.getWithDGS(Global.getSector().getPlayerFleet().getCargo().getCredits().get());
        }

        Color title = new Color(236, 244, 255);
        Color text = new Color(150, 164, 184);
        Color accent = getBranchAccent(255);
        Color panel = new Color(18, 28, 44);

        ui.setBgAlpha(0.95f);
        ui.setTitleOrbitronVeryLarge();
        ui.addTitle("普罗希昂", title);
        ui.addSpacer(10f);
        ui.setParaInsigniaLarge();
        String pageName = overviewPage ? "总览主页" : currentBranch.getDisplayName();
        String pageFlavor = overviewPage ? "掌握进度、队列状态与当前生效加成的系统总览。" : getBranchFlavor(currentBranch);

        LabelAPI line = ui.addPara("当前页面：%s    星币：%s    队列：%s/%s    已掌握专长：%s/%s",
                4f,
                text,
                accent,
                pageName,
                credits,
                String.valueOf(data.getQueueSize()),
                String.valueOf(PCSpecializationConstants.QUEUE_SIZE),
                String.valueOf(getMasteredSpecsForAllBranches()),
                String.valueOf(getTotalSpecsForAllBranches()));
        line.setHighlightColors(accent, accent, accent, accent, accent, accent);
        addBoxHeading(ui, pageFlavor, title, panel, Math.min(820f, width - 120f), 10f);
        ui.setParaFontDefault();
    }

    protected void buildCloseButton(TooltipMakerAPI ui, float width, float height) {
        ui.setBgAlpha(0f);
        ui.setButtonFontOrbitron24Bold();

        ButtonAPI close = ui.addButton(
                "×",
                BUTTON_CLOSE,
                new Color(255, 232, 232),
                new Color(92, 18, 22),
                Alignment.MID,
                CutStyle.C2_MENU,
                Math.min(48f, width),
                38f,
                0f
        );
        close.setEnabled(true);
        close.setHighlightBrightness(1.35f);
        close.setGlowBrightness(0.55f);
        trackButton(close, BUTTON_CLOSE);

        ui.setButtonFontDefault();
        ui.setParaFontDefault();
    }

    protected void buildSideBar(TooltipMakerAPI ui, float width, float height) {
        Color base = getBranchAccent(255);
        Color bg = getActionButtonBg();
        Color activeBg = getActiveButtonBg();
        Color activeText = new Color(238, 248, 255);

        ui.setBgAlpha(0.75f);
        ui.setParaInsigniaLarge();
        addBoxHeading(ui, "导航", activeText, new Color(17, 27, 42), Math.min(150f, width - 28f), 8f);
        ui.setButtonFontDefault();
        ButtonAPI overview = ui.addButton(
                "总览主页",
                BUTTON_OVERVIEW,
                base,
                overviewPage ? activeBg : bg,
                Alignment.MID,
                CutStyle.C2_MENU,
                width - 14f,
                50f,
                6f
        );
        overview.setChecked(overviewPage);
        overview.setEnabled(!overviewPage);
        if (!overviewPage) {
            trackButton(overview, BUTTON_OVERVIEW);
        }

        ui.addSpacer(10f);
        addBoxHeading(ui, "技能分支", activeText, new Color(17, 27, 42), Math.min(150f, width - 28f), 8f);

        for (Branch branch : Branch.values()) {
            boolean active = !overviewPage && branch == currentBranch;
            int mastered = getMasteredSpecsForBranch(branch);
            int total = getTotalSpecsForBranch(branch);
            String label = branch.getDisplayName() + "  " + mastered + "/" + total;

            ButtonAPI button = ui.addButton(
                    label,
                    BUTTON_PREFIX_BRANCH + branch.name(),
                    active ? getBranchAccent(255) : base,
                    active ? activeBg : bg,
                    Alignment.MID,
                    CutStyle.C2_MENU,
                    width - 14f,
                    42f,
                    6f
            );

            button.setChecked(active);
            button.setEnabled(!active);
            if (!active) {
                trackButton(button, BUTTON_PREFIX_BRANCH + branch.name());
            }
        }

        ui.setButtonFontDefault();
        ui.addSpacer(8f);
        ui.setParaFontDefault();
    }

    protected void buildContent(TooltipMakerAPI ui, float width, float height) {
        PCSpecializationData data = PCSpecializationData.get();

        Color cardBg = new Color(18, 27, 43);
        Color title = new Color(235, 244, 255);
        Color sub = new Color(148, 160, 178);
        Color accent = getBranchAccent(255);
        Color green = new Color(84, 210, 124);
        Color red = new Color(248, 81, 73);
        Color dim = new Color(72, 88, 110);

        ui.setBgAlpha(0.72f);
        ui.setParaInsigniaLarge();
        if (overviewPage) {
            buildOverviewContent(ui, width, height, data, title, cardBg, sub, accent, green, dim);
            ui.setParaFontDefault();
            return;
        }

        addBoxHeading(ui, currentBranch.getDisplayName(), title, cardBg, Math.min(300f, width - 120f), 8f);
        ui.addPara("已掌握专长：%s/%s    队列：%s/%s",
                6f,
                sub,
                accent,
                String.valueOf(getMasteredSpecsForBranch(currentBranch)),
                String.valueOf(getTotalSpecsForBranch(currentBranch)),
                String.valueOf(data.getQueueSize()),
                String.valueOf(PCSpecializationConstants.QUEUE_SIZE));
        ui.addPara(getBranchFlavor(currentBranch), 4f, sub);

        List<PCSpecializationSpec> specs = PCSpecializationConstants.getSpecsForBranch(currentBranch);
        for (int index = 0; index < specs.size(); index++) {
            PCSpecializationSpec spec = specs.get(index);
            int level = data.getLevel(spec.getId());
            String block = data.getBlockReason(spec.getId());
            boolean canLearn = block == null;
            boolean maxed = level >= spec.getMaxLevel();

            addBoxHeading(ui, formatNodeTitle(index, spec, level), title, cardBg, Math.min(460f, width - 140f), 16f);
            ui.addPara("状态：%s    完成率：%s",
                    4f,
                    sub,
                    maxed ? green : (canLearn ? accent : red),
                    getNodeStateText(canLearn, maxed),
                    String.valueOf(Math.round((float) level / Math.max(1f, (float) spec.getMaxLevel()) * 100f)) + "%");
            addSegmentBar(ui, level, spec.getMaxLevel(), getCappedBarWidth(width, 118f, 720f), maxed ? green : accent, dim, 4f);

            ui.addPara("说明：%s", 5f, sub, spec.getDescription());
            ui.addPara("当前效果：%s", 4f, maxed ? green : accent, spec.getEffectDescription(level));

            if (level < spec.getMaxLevel()) {
                ui.addPara("下一级效果：%s", 3f, accent, spec.getEffectDescription(level + 1));
                addUpgradeCostBlock(ui, spec, level + 1, sub, accent, 4f);
            } else {
                ui.addPara("状态：%s", 4f, green, "已完全掌握");
            }

            if (!spec.getPrerequisites().isEmpty()) {
                ui.addPara("前置链路：%s", 4f, sub, getPrerequisiteText(spec));
            }

            if (selectedSpecId == null) {
                selectedSpecId = spec.getId();
            }

            String buttonText;
            if (maxed) {
                buttonText = "已完成";
            } else if (canLearn) {
                buttonText = "开始研修";
            } else {
                buttonText = "锁定";
            }

            ui.setButtonFontDefault();
            ButtonAPI button = ui.addButton(
                    buttonText,
                    BUTTON_PREFIX_LEARN + spec.getId(),
                    canLearn ? accent : dim,
                    canLearn ? getActionButtonBg() : getDisabledButtonBg(),
                    Alignment.MID,
                    CutStyle.C2_MENU,
                    232f,
                    40f,
                    6f
            );
            ui.setButtonFontDefault();
            button.setEnabled(canLearn && !maxed);
            trackButton(button, BUTTON_PREFIX_LEARN + spec.getId());

            if (!canLearn && !maxed) {
                ui.addPara("锁定原因：%s", 3f, red, block);
            }

            ui.addSpacer(5f);
        }
        ui.setParaFontDefault();
    }

    /**
     * 构建总览主页内容。
     *
     * 总览页不展开完整技能列表，只展示：
     * 1. 全局研修进度；
     * 2. 各分支掌握程度；
     * 3. 当前已经生效的加成摘要；
     * 4. 队列概览和下一步操作提示。
     *
     * 这样未来专长数量增加时，玩家进入系统后首先看到的是战略概览，
     * 再通过左侧分支按钮进入具体技能页。
     */
    protected void buildOverviewContent(TooltipMakerAPI ui,
                                        float width,
                                        float height,
                                        PCSpecializationData data,
                                        Color title,
                                        Color cardBg,
                                        Color sub,
                                        Color accent,
                                        Color green,
                                        Color dim) {
        int masteredTotal = getMasteredSpecsForAllBranches();
        int specTotal = getTotalSpecsForAllBranches();
        Color complete = new Color(238, 170, 78);

        addBoxHeading(ui, "研修总览", title, cardBg, Math.min(240f, width - 140f), 8f);
        ui.addPara("已掌握专长：%s/%s    队列：%s/%s",
                8f,
                sub,
                accent,
                String.valueOf(masteredTotal),
                String.valueOf(specTotal),
                String.valueOf(data.getQueueSize()),
                String.valueOf(PCSpecializationConstants.QUEUE_SIZE));
        addSpecStateBar(ui, PCSpecializationConstants.getAllSpecs(), getCappedBarWidth(width, 72f, 1100f), dim, green, complete, 5f);
        LabelAPI legend = ui.addPara("图例：灰色 未研修    绿色 已开始    橙色 已完全掌握", 3f, sub);
        legend.setHighlight("灰色", "绿色", "橙色");
        legend.setHighlightColors(dim, green, complete);

        addBoxHeading(ui, "分支掌握", title, cardBg, Math.min(240f, width - 140f), 18f);
        for (Branch branch : Branch.values()) {
            int mastered = getMasteredSpecsForBranch(branch);
            int total = getTotalSpecsForBranch(branch);
            ui.addPara("%s    %s/%s",
                    6f,
                    sub,
                    accent,
                    branch.getDisplayName(),
                    String.valueOf(mastered),
                    String.valueOf(total));
            addSpecStateBar(ui, PCSpecializationConstants.getSpecsForBranch(branch), getCappedBarWidth(width, 78f, 900f), dim, green, complete, 2f);
        }

        addBoxHeading(ui, "当前加成摘要", title, cardBg, Math.min(280f, width - 140f), 18f);
        boolean hasAnyEffect = false;
        for (PCSpecializationSpec spec : PCSpecializationConstants.getAllSpecs()) {
            int level = data.getLevel(spec.getId());
            if (level <= 0) {
                continue;
            }
            hasAnyEffect = true;
            ui.addPara("%s Lv%s：%s",
                    5f,
                    sub,
                    green,
                    spec.getName(),
                    String.valueOf(level),
                    spec.getEffectDescription(level));
        }

        if (!hasAnyEffect) {
            ui.addPara("尚未掌握任何专长。选择左侧分支进入技能页，开始第一项研修。", 5f, sub);
        }

        addBoxHeading(ui, "操作提示", title, cardBg, Math.min(240f, width - 140f), 18f);
        ui.addPara("使用左侧导航选择总览或任一分支，查看可研修专长。开始研修后，项目会进入底部队列；可在队列中暂停、继续或取消并返还部分资源。", 5f, sub);
        ui.addPara("按 Esc 或点击右上角红色 × 可退出研修面板。", 3f, sub);
    }

    protected void buildQueueBar(TooltipMakerAPI ui, float width, float height) {
        PCSpecializationData data = PCSpecializationData.get();

        Color title = new Color(235, 244, 255);
        Color panel = new Color(18, 28, 44);
        Color sub = new Color(148, 160, 178);
        Color accent = getBranchAccent(255);
        Color dim = new Color(72, 88, 110);

        ui.setBgAlpha(0.82f);
        ui.setParaInsigniaLarge();
        float queueBarWidth = getCappedBarWidth(width, 560f, 340f);

        addBoxHeading(ui, "研修队列", title, panel, Math.min(240f, width - 140f), 8f);
        ui.addPara("槽位占用：%s/%s", 5f, sub, accent,
                String.valueOf(data.getQueueSize()),
                String.valueOf(PCSpecializationConstants.QUEUE_SIZE));
        addSlotOccupancyBar(ui, data.getQueueSize(), PCSpecializationConstants.QUEUE_SIZE, queueBarWidth, accent, dim, 2f);

        if (data.getQueue().isEmpty()) {
            ui.addPara("槽位状态：%s", 7f, sub, "全部空闲，等待新的专长研修指令。");
            ui.setParaFontDefault();
            return;
        }

        List<QueueItem> queue = data.getQueue();
        for (int i = 0; i < queue.size(); i++) {
            QueueItem item = queue.get(i);
            PCSpecializationSpec spec = PCSpecializationConstants.getSpec(item.getSpecId());
            if (spec == null) continue;

            String state = item.isPaused() ? "已暂停" : "进行中";
            String stateDetail = item.isPaused() ? "不会推进" : "剩余 " + String.format("%.1f", item.getRemainingDays()) + " 天";
            float progressValue = item.getProgressFraction();
            String progress = String.valueOf(Math.round(progressValue * 100f)) + "%";

            ui.addPara("槽位 %s    %s Lv%s    %s：%s    进度 %s",
                    5f,
                    sub,
                    accent,
                    String.valueOf(i + 1),
                    spec.getName(),
                    String.valueOf(item.getTargetLevel()),
                    state,
                    stateDetail,
                    progress);
            addTextProgressBar(ui, progressValue, queueBarWidth, item.isPaused() ? dim : accent, dim, 1f);

            ui.setButtonFontDefault();
            ButtonAPI pause = ui.addButton(
                    item.isPaused() ? "继续" : "暂停",
                    BUTTON_PREFIX_PAUSE + i,
                    accent,
                    getActionButtonBg(),
                    Alignment.MID,
                    CutStyle.C2_MENU,
                    102f,
                    32f,
                    1f
            );
            ButtonAPI cancel = ui.addButton(
                    getCancelRefundLabel(item),
                    BUTTON_PREFIX_CANCEL + i,
                    new Color(248, 120, 88),
                    new Color(36, 20, 18),
                    Alignment.MID,
                    CutStyle.C2_MENU,
                    310f,
                    32f,
                    1f
            );
            ui.setButtonFontDefault();
            pause.setEnabled(true);
            cancel.setEnabled(true);
            trackButton(pause, BUTTON_PREFIX_PAUSE + i);
            trackButton(cancel, BUTTON_PREFIX_CANCEL + i);
        }
        ui.setParaFontDefault();
    }

    /**
     * 绘制“非交互按钮式”短框标题。
     *
     * 说明：
     * 1. 不再使用 addSectionHeading()，因为它会生成带分割线语义的整行标题条；
     * 2. 使用普通按钮外观模拟小标题框，宽度固定且不会自动铺满滚动面板；
     * 3. 按钮不登记到 trackedButtons，因此点击不会触发实际逻辑，只作为视觉标签。
     */
    protected void addUpgradeCostBlock(TooltipMakerAPI ui, PCSpecializationSpec spec, int targetLevel, Color text, Color highlight, float pad) {
        /*
         * 之前直接把资源 id 当成 spriteName 传给 beginImageWithText()，
         * API 实际需要的是完整图片路径，因此会留下一大块空白，并把原始字符串裁到边缘。
         *
         * 这里统一先从 commodity spec 取真实 iconName：
         * 1. credits -> 原版星币图标；
         * 2. supplies -> 原版补给图标；
         * 3. 若某个图标取不到，则自动回退成纯文字，避免再出现空白占位。
         */
        addResourceCostRow(ui,
                "credits",
                "星币",
                Misc.getWithDGS(spec.getCreditCostForTargetLevel(targetLevel)),
                text,
                highlight,
                pad);

        int supplyCost = spec.getSupplyCostForTargetLevel(targetLevel);
        if (supplyCost > 0) {
            addResourceCostRow(ui,
                    "supplies",
                    "补给",
                    Misc.getWithDGS(supplyCost),
                    text,
                    highlight,
                    1f);
        }

        for (Map.Entry<String, Integer> entry : spec.getCommodityCostsForTargetLevel(targetLevel).entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().intValue() <= 0) {
                continue;
            }

            addResourceCostRow(ui,
                    entry.getKey(),
                    getCommodityDisplayName(entry.getKey()),
                    Misc.getWithDGS(entry.getValue().intValue()),
                    text,
                    highlight,
                    1f);
        }

        int interceptorLpcCost = spec.getInterceptorLpcCostForTargetLevel(targetLevel);
        if (interceptorLpcCost > 0) {
            ui.addPara("截击机类型舰载机 LPC：%s", 1f, text, highlight, Misc.getWithDGS(interceptorLpcCost));
        }

        ui.addPara("研修周期：%s 天", 1f, text, highlight, String.format("%.1f", spec.getDaysPerLevel()));
    }

    protected void addResourceCostRow(TooltipMakerAPI ui, String resourceId, String label, String value, Color text, Color highlight, float pad) {
        String spriteName = getCommodityIcon(resourceId);
        if (spriteName == null || spriteName.length() <= 0) {
            ui.addPara("%s：%s", pad, text, highlight, label, value);
            return;
        }

        TooltipMakerAPI row = ui.beginImageWithText(spriteName, 30f);
        row.addPara("%s：%s", 0f, text, highlight, label, value);
        ui.addImageWithText(pad);
    }

    protected String getCommodityIcon(String commodityId) {
        if (Global.getSettings() == null || commodityId == null) {
            return null;
        }
        if (Global.getSettings().getCommoditySpec(commodityId) == null) {
            return null;
        }
        return Global.getSettings().getCommoditySpec(commodityId).getIconName();
    }

    protected String getCommodityDisplayName(String commodityId) {
        if (Global.getSettings() == null || commodityId == null) {
            return commodityId == null ? "未知物资" : commodityId;
        }
        if (Global.getSettings().getCommoditySpec(commodityId) == null) {
            return commodityId;
        }
        return Global.getSettings().getCommoditySpec(commodityId).getName();
    }

    protected String getCancelRefundLabel(QueueItem item) {
        if (item == null) {
            return "取消";
        }

        int refundCredits = Math.round(item.getCreditCost() * 0.5f);
        int refundSupplies = Math.round(item.getSupplyCost() * 0.5f);
        String extra = item.hasAdditionalCosts() ? " / 其他物资" : "";
        if (refundSupplies > 0) {
            return "取消：返还 " + Misc.getWithDGS(refundCredits) + " / " + Misc.getWithDGS(refundSupplies) + "补给" + extra;
        }
        return "取消：返还 " + Misc.getWithDGS(refundCredits) + extra;
    }

    protected void addBoxHeading(TooltipMakerAPI ui, String text, Color textColor, Color bgColor, float width, float pad) {
        /*
         * 装饰标题必须和实际按钮区分：
         * 1. 装饰标题改为深橙底 + 白字，解决原版禁用按钮会把颜色压灰的问题；
         * 2. 标题按钮保持 enabled，只是不登记到 trackedButtons，避免 Starsector 把禁用按钮统一渲染成灰色；
         * 3. 即使被点击也只会传入 heading 前缀，buttonPressed() 不处理该协议，因此不会触发实际逻辑；
         * 4. 实际可交互按钮仍使用青蓝底色，标题、背景、操作按钮三者层级更清楚。
         */
        ui.setButtonFontDefault();
        ButtonAPI heading = ui.addButton(
                text,
                "heading:" + text,
                getDecorativeHeadingText(),
                getDecorativeHeadingBg(),
                Alignment.MID,
                CutStyle.C2_MENU,
                Math.max(140f, width),
                34f,
                pad
        );
        heading.setEnabled(true);
        ui.setButtonFontDefault();
        ui.addSpacer(8f);
    }

    /**
     * 装饰标题文字色。
     *
     * 使用接近纯白的标题文字，保证在深橙底板上有足够对比度。
     */
    protected Color getDecorativeHeadingText() {
        return new Color(245, 246, 240);
    }

    /**
     * 装饰标题底色。
     *
     * 深橙色与青蓝操作按钮形成明确区分，同时亮度压低，避免破坏整体暗色 UI。
     */
    protected Color getDecorativeHeadingBg() {
        return new Color(46, 56, 70);
    }

    /**
     * 常规可点击按钮底色。
     *
     * 比装饰标题更亮、更蓝，玩家能直观看出这是可交互控件；
     * 但仍保持深色 UI 风格，不会从整体面板里突兀跳出。
     */
    protected Color getActionButtonBg() {
        return new Color(20, 47, 66);
    }

    /**
     * 当前选中/激活按钮底色。
     */
    protected Color getActiveButtonBg() {
        return new Color(33, 72, 96);
    }

    /**
     * 不可用按钮底色。
     *
     * 不再使用纯灰，改为低饱和深蓝灰，避免锁定按钮和背景糊成一片。
     */
    protected Color getDisabledButtonBg() {
        return new Color(20, 28, 38);
    }

    /**
     * 根据当前分支取得主强调色。
     *
     * 说明：
     * 1. 后勤优化偏向冷青色，强调“系统、供应、稳定”；
     * 2. 航空打击群偏向紫蓝色，强调“舰载机、战术链路、空优”；
     * 3. alpha 参数统一从调用处传入，便于自绘底板和文字高亮共用。
     */
    protected Color getBranchAccent(int alpha) {
        int safeAlpha = Math.max(0, Math.min(255, alpha));
        return new Color(75, 205, 225, safeAlpha);
    }

    /**
     * 分支短代号。
     *
     * 当前主界面已尽量减少英文短码，该方法保留给后续搜索、日志或调试入口使用。
     */
    protected String getBranchCode(Branch branch) {
        if (branch == Branch.CARRIER_GROUP) {
            return "CV-AIR";
        }
        if (branch == Branch.ARMED_CONVOY) {
            return "AUX-CVN";
        }
        if (branch == Branch.COLONY_MANAGEMENT) {
            return "COL-ADM";
        }
        return "LOGI-CORE";
    }

    /**
     * 分支描述文案，用较短文本放在顶部状态栏，避免内容区拥挤。
     */
    protected String getBranchFlavor(Branch branch) {
        if (branch == Branch.CARRIER_GROUP) {
            return "舰载机联队、甲板调度与制空火力的强化研究。";
        }
        if (branch == Branch.ARMED_CONVOY) {
            return "民船武装化、护航队形与商队战术协同的改造方案。";
        }
        if (branch == Branch.COLONY_MANAGEMENT) {
            return "殖民地政策、产业配给与社会秩序的长期改革方案。";
        }
        return "补给链路、维护协议与远航保障系统的综合优化。";
    }

    /**
     * 统计指定分支当前已经学到的等级总和。
     */
    protected int getLearnedLevelsForBranch(Branch branch) {
        PCSpecializationData data = PCSpecializationData.get();
        int result = 0;
        List<PCSpecializationSpec> specs = PCSpecializationConstants.getSpecsForBranch(branch);
        for (PCSpecializationSpec spec : specs) {
            result += data.getLevel(spec.getId());
        }
        return result;
    }

    /**
     * 统计所有分支的专长数量。
     */
    protected int getTotalSpecsForAllBranches() {
        int result = 0;
        for (Branch branch : Branch.values()) {
            result += getTotalSpecsForBranch(branch);
        }
        return result;
    }

    /**
     * 统计指定分支的专长数量，不再把多级专长拆成多个进度单位。
     */
    protected int getTotalSpecsForBranch(Branch branch) {
        return PCSpecializationConstants.getSpecsForBranch(branch).size();
    }

    /**
     * 统计所有分支已经完全掌握的专长数量。
     */
    protected int getMasteredSpecsForAllBranches() {
        int result = 0;
        for (Branch branch : Branch.values()) {
            result += getMasteredSpecsForBranch(branch);
        }
        return result;
    }

    /**
     * 统计指定分支已经完全掌握的专长数量。
     */
    protected int getMasteredSpecsForBranch(Branch branch) {
        PCSpecializationData data = PCSpecializationData.get();
        int result = 0;
        List<PCSpecializationSpec> specs = PCSpecializationConstants.getSpecsForBranch(branch);
        for (PCSpecializationSpec spec : specs) {
            if (data.getLevel(spec.getId()) >= spec.getMaxLevel()) {
                result++;
            }
        }
        return result;
    }

    /**
     * 统计所有分支的最大等级总和。
     */
    protected int getMaxLevelsForAllBranches() {
        int result = 0;
        for (Branch branch : Branch.values()) {
            result += getMaxLevelsForBranch(branch);
        }
        return result;
    }

    /**
     * 统计指定分支所有节点的最大等级总和。
     */
    protected int getMaxLevelsForBranch(Branch branch) {
        int result = 0;
        List<PCSpecializationSpec> specs = PCSpecializationConstants.getSpecsForBranch(branch);
        for (PCSpecializationSpec spec : specs) {
            result += spec.getMaxLevel();
        }
        return result;
    }

    /**
     * 生成节点标题。
     *
     * 避免继续使用过多英文短码，降低阅读负担。
     */
    protected String formatNodeTitle(int index, PCSpecializationSpec spec, int level) {
        String node = index < 9 ? "0" + String.valueOf(index + 1) : String.valueOf(index + 1);
        return "专长 " + node + "    " + spec.getName() + "    Lv " + level + "/" + spec.getMaxLevel();
    }

    /**
     * 将前置节点 id 转成可读名称。
     */
    protected String getPrerequisiteText(PCSpecializationSpec spec) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < spec.getPrerequisites().size(); i++) {
            PCSpecializationSpec pre = PCSpecializationConstants.getSpec(spec.getPrerequisites().get(i));
            if (pre == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(pre.getName());
        }
        if (builder.length() <= 0) {
            return "无";
        }
        return builder.toString();
    }

   /**
    * 获取节点状态短码，用于把纯文字列表转成类似系统状态面板的表达。
    */
   protected String getNodeStateText(boolean canLearn, boolean maxed) {
       if (maxed) {
           return "已掌握";
       }
       if (canLearn) {
           return "可研修";
       }
       return "未解锁";
   }

   /**
    * 用文本块绘制队列槽位占用条。
    */
   protected void addSlotOccupancyBar(TooltipMakerAPI ui, int used, int max, float width, Color fill, Color empty, float pad) {
       int safeMax = Math.max(1, max);
       int safeUsed = Math.max(0, Math.min(safeMax, used));
       addSegmentGraphicBar(ui, safeUsed, safeMax, width, fill, empty, pad);
   }

   /**
    * 绘制专长状态条。
    *
    * 每个格子对应一个专长，而不是一个升级等级：
    * 1. 灰色：尚未投入等级；
    * 2. 绿色：已经投入等级，但尚未完全掌握；
    * 3. 橙色：达到该专长等级上限。
    */
   protected void addSpecStateBar(TooltipMakerAPI ui,
                                  List<PCSpecializationSpec> specs,
                                  float width,
                                  Color untouched,
                                  Color partial,
                                  Color complete,
                                  float pad) {
       if (rootPanel == null || specs == null || specs.isEmpty()) {
           return;
       }

       Color[] colors = new Color[specs.size()];
       PCSpecializationData data = PCSpecializationData.get();
       for (int i = 0; i < specs.size(); i++) {
           PCSpecializationSpec spec = specs.get(i);
           int level = data.getLevel(spec.getId());
           if (level <= 0) {
               colors[i] = untouched;
           } else if (level >= spec.getMaxLevel()) {
               colors[i] = complete;
           } else {
               colors[i] = partial;
           }
       }

       float maxWidth = Math.max(20f, width);
       float gap = PROGRESS_SEGMENT_GAP;
       float segmentWidth = PROGRESS_SEGMENT_WIDTH;
       float visualWidth = segmentWidth * colors.length + gap * (colors.length - 1);

       if (visualWidth > maxWidth) {
           segmentWidth = Math.max(8f, (maxWidth - gap * (colors.length - 1)) / colors.length);
           visualWidth = segmentWidth * colors.length + gap * (colors.length - 1);
       }

       visualWidth = Math.max(20f, visualWidth);
       CustomPanelAPI bar = rootPanel.createCustomPanel(visualWidth, PROGRESS_SEGMENT_HEIGHT,
               new SpecStateBarPlugin(colors, segmentWidth, gap));
       ui.addCustom(bar, pad);
   }

   /**
    * 绘制分段等级条。
    *
    * 每个等级槽使用固定宽度排列，避免低等级上限的专长把 3~5 个槽拉伸成整行。
    * 后续如果加入图标或节点贴图，可以把这里替换为真正的 CustomPanel 子组件。
    */
    protected void addSegmentBar(TooltipMakerAPI ui, int level, int maxLevel, float width, Color fill, Color empty, float pad) {
        int safeMax = Math.max(1, maxLevel);
        int safeLevel = Math.max(0, Math.min(safeMax, level));
        addSegmentGraphicBar(ui, safeLevel, safeMax, width, fill, empty, pad);
    }

    /**
     * 绘制非等级型进度条。
     *
     * 等级进度必须直接使用 maxLevel 作为格数；这个方法只给队列时间进度等
     * 没有明确等级上限的百分比条使用，避免影响专长等级条的实际格数。
     */
    protected void addTextProgressBar(TooltipMakerAPI ui, float progress, float width, Color fill, Color empty, float pad) {
        float safeProgress = Math.max(0f, Math.min(1f, progress));
        int segments = PERCENT_PROGRESS_SEGMENTS;
        int filled = Math.round(segments * safeProgress);
        addSegmentGraphicBar(ui, filled, segments, width, fill, empty, pad);
    }

    /**
     * 计算安全的条形宽度。
     *
     * 说明：
     * 1. Tooltip 内部实际可用宽度通常小于创建时传入的 nominal width；
     * 2. 之前多个超框问题，本质上就是“按理论宽度画条”，但真实右侧还有 padding / scroller / 边框；
     * 3. 因此统一在这里做一次保守裁剪，避免各处分别硬编码又漏改。
     */
    protected float getSafeBarWidth(float sourceWidth, float reservedWidth) {
        return Math.max(36f, sourceWidth - reservedWidth);
    }

    /**
     * 计算有最大上限的条形宽度。
     *
     * Starsector 的 Tooltip 布局会把自定义条形组件宽度计入排版；
     * 如果直接使用内容区宽度，条形虽然看似没有越界，但会把布局撑得过宽。
     * 因此所有图形条都必须在调用层明确封顶，而不是依赖绘制层猜测。
     */
    protected float getCappedBarWidth(float sourceWidth, float reservedWidth, float maxWidth) {
        return Math.max(36f, Math.min(maxWidth, getSafeBarWidth(sourceWidth, reservedWidth)));
    }

    /**
     * 使用真实自绘矩形绘制固定槽位条，避免字体不支持图形字符时显示问号。
     *
     * 传入的 width 现在作为最大宽度上限使用；默认每格保持固定宽度，只在槽位数过多
     * 且超过上限时才保守压缩单格宽度。
     */
    protected void addSegmentGraphicBar(TooltipMakerAPI ui, int filled, int segments, float width, Color fill, Color empty, float pad) {
        if (rootPanel == null) {
            return;
        }
        int safeSegments = Math.max(1, segments);
        int safeFilled = Math.max(0, Math.min(safeSegments, filled));

        float maxWidth = Math.max(20f, width);
        float gap = PROGRESS_SEGMENT_GAP;
        float segmentWidth = PROGRESS_SEGMENT_WIDTH;
        float visualWidth = segmentWidth * safeSegments + gap * (safeSegments - 1);

        if (visualWidth > maxWidth) {
            segmentWidth = Math.max(8f, (maxWidth - gap * (safeSegments - 1)) / safeSegments);
            visualWidth = segmentWidth * safeSegments + gap * (safeSegments - 1);
        }

        visualWidth = Math.max(20f, visualWidth);
        CustomPanelAPI bar = rootPanel.createCustomPanel(visualWidth, PROGRESS_SEGMENT_HEIGHT,
                new SegmentBarPlugin(safeFilled, safeSegments, fill, empty, segmentWidth, gap));
        ui.addCustom(bar, pad);
    }

    /**
     * 自绘分段条插件。
     *
     * 只负责绘制一排小矩形槽位，不处理输入，因此不会影响按钮响应。
     */
    protected static class SegmentBarPlugin extends BaseCustomUIPanelPlugin {
        protected final int filled;
        protected final int segments;
        protected final Color fill;
        protected final Color empty;
        protected final float segmentWidth;
        protected final float gap;
        protected PositionAPI position;

        public SegmentBarPlugin(int filled, int segments, Color fill, Color empty, float segmentWidth, float gap) {
            this.filled = filled;
            this.segments = Math.max(1, segments);
            this.fill = fill;
            this.empty = empty;
            this.segmentWidth = Math.max(1f, segmentWidth);
            this.gap = Math.max(0f, gap);
        }

        @Override
        public void positionChanged(PositionAPI position) {
            this.position = position;
        }

        @Override
        public void renderBelow(float alphaMult) {
            if (position == null) {
                return;
            }

            float x = position.getX();
            float y = position.getY();
            float width = position.getWidth();
            float height = position.getHeight();
            float gap = this.gap;
            float segmentWidth = Math.min(this.segmentWidth, Math.max(1f, (width - gap * (segments - 1)) / segments));

            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            for (int i = 0; i < segments; i++) {
                Color color = i < filled ? fill : empty;
                Misc.renderQuad(x + i * (segmentWidth + gap), y, segmentWidth, height, color, alphaMult);
            }

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    /**
     * 专长状态条插件。
     *
     * 与普通分段条不同，这里每一格都可以使用独立颜色，用于总览页表达
     * “未研修 / 已开始 / 已完全掌握”三态。
     */
    protected static class SpecStateBarPlugin extends BaseCustomUIPanelPlugin {
        protected final Color[] colors;
        protected final float segmentWidth;
        protected final float gap;
        protected PositionAPI position;

        public SpecStateBarPlugin(Color[] colors, float segmentWidth, float gap) {
            this.colors = colors == null ? new Color[0] : colors;
            this.segmentWidth = Math.max(1f, segmentWidth);
            this.gap = Math.max(0f, gap);
        }

        @Override
        public void positionChanged(PositionAPI position) {
            this.position = position;
        }

        @Override
        public void renderBelow(float alphaMult) {
            if (position == null || colors.length <= 0) {
                return;
            }

            float x = position.getX();
            float y = position.getY();
            float width = position.getWidth();
            float height = position.getHeight();
            int segments = colors.length;
            float gap = this.gap;
            float segmentWidth = Math.min(this.segmentWidth, Math.max(1f, (width - gap * (segments - 1)) / segments));

            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            for (int i = 0; i < segments; i++) {
                Color color = colors[i] == null ? new Color(72, 88, 110) : colors[i];
                Misc.renderQuad(x + i * (segmentWidth + gap), y, segmentWidth, height, color, alphaMult);
            }

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    protected void closeUi() {
        if (closeRequested) {
            return;
        }
        closeRequested = true;

        if (visualCallbacks != null) {
            visualCallbacks.dismissDialog();
        } else if (callback != null) {
            callback.dismissCustomDialog(0);
        } else if (Global.getSector() != null
                && Global.getSector().getCampaignUI() != null
                && Global.getSector().getCampaignUI().getCurrentInteractionDialog() != null) {
            Global.getSector().getCampaignUI().getCurrentInteractionDialog().dismiss();
        }
    }

    /**
     * 关闭当前准全屏 UI，并在下一帧重新打开一个新的专长界面实例。
     *
     * 这样做是为了彻底丢弃旧的 tooltip/scroller/internal state，
     * 避免“文字叠加、滚动错乱、旧界面没有真正消失”的问题。
     *
     * 同时保留当前分支，避免点击“航空打击群”后因为重新构造界面又回到默认的“后勤优化”。
     */
    protected void requestReopenUi() {
        if (visualCallbacks != null) {
            visualCallbacks.dismissDialog();
        } else if (callback != null) {
            callback.dismissCustomDialog(0);
        }

        if (Global.getSector() == null) {
            return;
        }

        Global.getSector().addTransientScript(new DelayedActionScript(0f) {
            @Override
            public void doAction() {
                if (Global.getSector() == null || Global.getSector().getCampaignUI() == null) {
                    return;
                }
                Global.getSector().getCampaignUI().showInteractionDialog(new PCSpecializationDialogPlugin(intel, currentBranch, overviewPage, branchListExpanded), null);
            }
        });
    }

    @Override
    public void buttonPressed(Object buttonId) {
        if (!(buttonId instanceof String)) {
            return;
        }

        String id = (String) buttonId;
        PCSpecializationData data = PCSpecializationData.get();

        if (BUTTON_OVERVIEW.equals(id)) {
            /*
             * 总览页不属于任何单一分支。
             * 明确回到总览状态并把分支选择复位，避免重开后顶部继续显示上一个分支的标题/说明。
             */
            overviewPage = true;
            branchListExpanded = false;
            currentBranch = Branch.LOGISTICS;
            requestReopenUi();
            return;
        }

        if (BUTTON_CLOSE.equals(id)) {
            closeUi();
            return;
        }

        if (id.startsWith(BUTTON_PREFIX_BRANCH)) {
            try {
                currentBranch = Branch.valueOf(id.substring(BUTTON_PREFIX_BRANCH.length()));
                overviewPage = false;
                requestReopenUi();
            } catch (IllegalArgumentException ex) {
                // 非法分支 id 直接忽略，避免 UI 因异常中断。
            }
            return;
        }

        if (id.startsWith(BUTTON_PREFIX_LEARN)) {
            String specId = id.substring(BUTTON_PREFIX_LEARN.length());
            selectedSpecId = specId;
            if (data.enqueue(specId)) {
                PCSpecializationIntel.notifyUpdated();
                requestReopenUi();
            }
            return;
        }

        if (id.startsWith(BUTTON_PREFIX_PAUSE)) {
            try {
                int index = Integer.parseInt(id.substring(BUTTON_PREFIX_PAUSE.length()));
                if (index >= 0 && index < data.getQueue().size()) {
                    QueueItem item = data.getQueue().get(index);
                    item.setPaused(!item.isPaused());
                    PCSpecializationIntel.notifyUpdated();
                    requestReopenUi();
                }
            } catch (NumberFormatException ex) {
                // 非法按钮参数直接忽略。
            }
            return;
        }

        if (id.startsWith(BUTTON_PREFIX_CANCEL)) {
            try {
                int index = Integer.parseInt(id.substring(BUTTON_PREFIX_CANCEL.length()));
                QueueItem item = data.cancelQueueItem(index, 0.5f);
                if (item != null) {
                    PCSpecializationIntel.notifyUpdated();
                    requestReopenUi();
                }
            } catch (NumberFormatException ex) {
                // 非法按钮参数直接忽略。
            }
        }
    }

}
