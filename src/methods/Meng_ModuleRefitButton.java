package data.methods;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.methods.Meng_ModuleSelectorScript;
import lunalib.backend.util.ReflectionUtils;
import lunalib.backend.ui.refit.RefitButtonAdder;
import lunalib.lunaRefit.BaseRefitButton;

/**
 * OldEmpire浮动模块改装按钮。
 * 继承LunaLib的BaseRefitButton，在改装界面中为每种舰船提供切换浮动模块模式的按钮。
 * 支持A/B两组独立切换，每组4种模式（轻型4选1 + 重型4选1）。
 */
public final class Meng_ModuleRefitButton extends BaseRefitButton {

    /** 用于防止并发刷新冲突的代际计数器 */
    private static int refreshGeneration;

    /** 是否为轻型舰（true=轻型，false=重型） */
    private final boolean light;
    /** 模块模式索引（0~3，对应4种模块模式） */
    private final int mode;
    /** 槽位组索引（0=A组，1=B组） */
    private final int group;

    /**
     * 构造函数（默认A组）。
     * @param light  是否为轻型舰
     * @param mode   模块模式索引（0~3）
     */
    public Meng_ModuleRefitButton(boolean light, int mode) {
        this(light, mode, 0);
    }

    /**
     * 构造函数（指定槽位组）。
     * @param light  是否为轻型舰
     * @param mode   模块模式索引（0~3）
     * @param group  槽位组索引（0=A组，1=B组）
     */
    public Meng_ModuleRefitButton(boolean light, int mode, int group) {
        this.light = light;
        this.mode = mode;
        this.group = group;
    }

    /**
     * 获取按钮显示名称。
     * 根据当前是否为活动模式，显示"当前：xxx"或"切换：xxx"。
     * 若舰船拥有双组（A/B），则在名称前附加组标识。
     */
    @Override
    public String getButtonName(FleetMemberAPI member, ShipVariantAPI variant) {
        boolean current = Meng_ModuleSelectorScript.getMode(variant, group) == mode;
        String groupName = Meng_ModuleSelectorScript.hasSecondaryGroup(variant)
                ? (group == 0 ? "A组 " : "B组 ")
                : "";
        return groupName + (current ? "当前：" : "切换：")
                + Meng_ModuleSelectorScript.getModuleName(light, mode);
    }

    /**
     * 获取按钮图标路径。
     * 轻型舰使用弹药供弹器图标，重型舰使用高能聚焦图标。
     */
    @Override
    public String getIconName(FleetMemberAPI member, ShipVariantAPI variant) {
        return light ? "graphics/icons/hullsys/ammo_feeder.png"
                : "graphics/icons/hullsys/high_energy_focus.png";
    }

    /**
     * 获取按钮排序权重。
     * 轻型起始30，重型起始40，B组偏移+10，mode值直接叠加保证排序唯一。
     */
    @Override
    public int getOrder(FleetMemberAPI member, ShipVariantAPI variant) {
        return (light ? 30 : 40) + group * 10 + mode;
    }

    /**
     * 判断按钮是否应当显示。
     * 条件：必须是OldEmpire父级舰、B组需要双组支持、舰船尺寸必须匹配轻/重型。
     */
    @Override
    public boolean shouldShow(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        // 非OldEmpire主舰不显示
        if (!Meng_ModuleSelectorScript.isOldEmpireParent(variant)) {
            return false;
        }
        // B组需要舰船支持双组浮动模块
        if (group > 0 && !Meng_ModuleSelectorScript.hasSecondaryGroup(variant)) {
            return false;
        }
        // 根据舰船尺寸决定显示轻型还是重型按钮
        ShipAPI.HullSize size = variant.getHullSpec().getHullSize();
        return Meng_ModuleSelectorScript.isModeAvailable(light, mode) && (light
                ? Meng_ModuleSelectorScript.isLightHull(size)
                : Meng_ModuleSelectorScript.isHeavyHull(size));
    }

    /**
     * 判断按钮是否可点击（当前已激活的模式不可重复点击）。
     */
    @Override
    public boolean isClickable(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return Meng_ModuleSelectorScript.isModeAvailable(light, mode)
                && Meng_ModuleSelectorScript.getMode(variant, group) != mode;
    }

    /**
     * 点击事件处理：切换浮动模块模式并刷新改装面板。
     */
    @Override
    public void onClick(FleetMemberAPI member, ShipVariantAPI variant, InputEventAPI event, MarketAPI market) {
        if (variant == null || !isClickable(member, variant, market)) {
            return;
        }

        Global.getLogger(Meng_ModuleRefitButton.class)
                .info("OldEmpire floating module group " + group + " direct select mode " + mode
                        + " for " + variant.getHullVariantId());
        // 获取当前舰船显示面板引用（用于后续刷新）
        Object shipDisplay = findCurrentShipDisplay();
        // 执行模块模式切换
        Meng_ModuleSelectorScript.selectMode(variant, light, mode, group);
        // 刷新舰船variant使改装面板更新
        refreshVariant();
        // 关闭Luna按钮下拉列表
        closeLunaButtonList();
        // 调度一次延迟刷新，确保面板正确更新
        scheduleModuleRefresh(shipDisplay, member);
    }

    /**
     * 始终显示tooltip。
     */
    @Override
    public boolean hasTooltip(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return true;
    }

    /**
     * 按钮提示信息：说明A/B两组独立切换的行为。
     */
    @Override
    public void addTooltip(TooltipMakerAPI tooltip, FleetMemberAPI member,
                           ShipVariantAPI variant, MarketAPI market) {
        tooltip.addPara("A、B 两组节点独立切换，并分别保留武器、武器组和船插配置。"
                + "切换后仅刷新当前舰船一次。", 0f);
    }

    /**
     * 关闭Luna按钮列表面板。
     * 通过查找RefitButtonAdder脚本并移除背景面板实现。
     */
    private static void closeLunaButtonList() {
        if (Global.getSector() == null) {
            return;
        }
        for (EveryFrameScript script : Global.getSector().getTransientScripts()) {
            if (!(script instanceof RefitButtonAdder)) {
                continue;
            }
            RefitButtonAdder adder = (RefitButtonAdder) script;
            CustomPanelAPI mainPanel = adder.getMainPanel();
            CustomPanelAPI backgroundPanel = adder.getBackgroundPanel();
            if (mainPanel != null && backgroundPanel != null) {
                // 从主面板中移除背景面板，关闭按钮列表
                mainPanel.removeComponent(backgroundPanel);
                adder.setBackgroundPanel(null);
            }
            return;
        }
    }

    /**
     * 通过反射查找当前舰船显示面板。
     * 用于后续刷新舰船改装信息。
     * @return 舰船显示面板对象，查找失败返回null
     */
    private static Object findCurrentShipDisplay() {
        RefitButtonAdder adder = findRefitButtonAdder();
        if (adder == null || adder.getMainPanel() == null) {
            return null;
        }
        try {
            // 反射路径: mainPanel → getParent() → getShipDisplay()
            Object refitPanel = invokeNoArg(adder.getMainPanel(), "getParent");
            return invokeNoArg(refitPanel, "getShipDisplay");
        } catch (Throwable ex) {
            Global.getLogger(Meng_ModuleRefitButton.class)
                    .warn("Unable to locate the refit ship display", ex);
            return null;
        }
    }

    /**
     * 调度一次延迟刷新，等待variant更新完成后刷新舰船显示。
     * 使用代际计数器确保多次快速切换时仅最后一次刷新生效。
     */
    private static void scheduleModuleRefresh(final Object shipDisplay,
                                              final FleetMemberAPI selectedMember) {
        if (Global.getSector() == null || shipDisplay == null) {
            return;
        }
        // 递增代际计数器，使旧的刷新调度失效
        final int generation = ++refreshGeneration;
        Global.getSector().addTransientScript(new EveryFrameScript() {
            private boolean done;
            private int frames;

            @Override
            public boolean isDone() {
                return done;
            }

            @Override
            public boolean runWhilePaused() {
                return true;
            }

            @Override
            public void advance(float amount) {
                RefitButtonAdder adder = findRefitButtonAdder();
                // 代际不匹配说明已被新操作取代，或添加器/成员已变化，终止
                if (generation != refreshGeneration
                        || adder == null
                        || adder.getMember() != selectedMember) {
                    done = true;
                    return;
                }

                frames++;
                // 等待variant标记更新完成（至少2帧且无待处理更新）
                if (RefitButtonAdder.getRequiresVariantUpdate() || frames < 2) {
                    return;
                }

                try {
                    // 通过反射重新加载当前舰船成员，刷新改装面板显示
                    reloadCurrentMember(shipDisplay, selectedMember);
                    Global.getLogger(Meng_ModuleRefitButton.class)
                            .info("Refreshed floating module display once");
                } catch (Throwable ex) {
                    Global.getLogger(Meng_ModuleRefitButton.class)
                            .warn("Unable to refresh floating module display", ex);
                } finally {
                    done = true;
                }
            }
        });
    }

    /**
     * 从transient脚本列表中查找LunaRefitButtonAdder实例。
     */
    private static RefitButtonAdder findRefitButtonAdder() {
        if (Global.getSector() == null) {
            return null;
        }
        for (EveryFrameScript script : Global.getSector().getTransientScripts()) {
            if (script instanceof RefitButtonAdder) {
                return (RefitButtonAdder) script;
            }
        }
        return null;
    }

    /**
     * 反射调用无参方法。
     * @param target     目标对象
     * @param methodName 方法名
     * @return 方法返回值，失败返回null
     */
    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        return ReflectionUtils.invoke(methodName, target, new Object[0], null, 0);
    }

    /**
     * 通过反射重新加载当前舰船成员信息。
     * 先设置强制刷新标记，再调用setFleetMember方法，最后恢复标记。
     */
    private static void reloadCurrentMember(Object shipDisplay, FleetMemberAPI member) {
        Object refitPanel = invokeNoArg(shipDisplay, "getParent");
        Object memberIndex = invokeNoArg(refitPanel, "getFleetMemberIndex");
        Object facing = invokeNoArg(shipDisplay, "getShipFacing");
        // 设置强制刷新标记，确保setFleetMember完全重新加载
        invokeArgs(refitPanel, "setForceSetMemberNextCall", Boolean.TRUE);
        try {
            invokeArgs(refitPanel, "setFleetMember", member, memberIndex, facing);
        } finally {
            invokeArgs(refitPanel, "setForceSetMemberNextCall", Boolean.FALSE);
        }
    }

    /**
     * 反射调用带参方法。
     * @param target     目标对象
     * @param methodName 方法名
     * @param arguments  参数列表
     * @return 方法返回值，失败返回null
     */
    private static Object invokeArgs(Object target, String methodName, Object... arguments) {
        if (target == null) {
            return null;
        }
        return ReflectionUtils.invoke(methodName, target, arguments, null, arguments.length);
    }
}
