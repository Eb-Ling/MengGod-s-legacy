package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;

/**
 * 超限界域船插效果。
 *
 * 继承 Starsector 的 BaseHullMod，由 hull_mods.csv 的 script 字段挂载。
 * 该船插配合卡巴拉船插使用：当舰船处于零辐能状态时，每帧刷新舰船战术系统
 * 与 phasecloak 右键系统的冷却、装填进度和弹药。
 *
 * 兼容性限制：不允许与原版安全协议超驰同时存在；如果变体已有安全协议超驰，
 * 在舰船创建后自动移除并播放警告音，保持项目内其它同类船插的处理方式。
 */
public class Meng_OverLimitDomain extends BaseHullMod {
    /** 原版安全协议超驰 hullmod id。 */
    private static final String SAFETY_OVERRIDES = "safetyoverrides";
    /** 玩家舰船状态栏 key，避免与其它状态提示冲突。 */
    private static final String STATUS_KEY = "Meng_OverLimitDomain_status";
    /** 状态栏使用的项目内图标路径。 */
    private static final String STATUS_ICON = "graphics/fx/Meng_logo1.png";

    /**
     * 每帧战斗推进逻辑，由战斗引擎调用。
     *
     * 执行路径：先检查舰船和辐能状态，只有当前辐能为 0 时才继续；随后分别读取
     * ship.getSystem() 战术系统与 ship.getPhaseCloak() 右键系统，对存在的系统做刷新。
     * 这里不主动激活或关闭系统，只清除冷却并补足弹药/装填，避免打断系统脚本自身状态机。
     *
     * @param ship 安装该船插的舰船实例
     * @param amount 本帧经过时间，当前效果不需要直接使用
     */
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) return;

        FluxTrackerAPI tracker = ship.getFluxTracker();
        if (tracker == null || tracker.getCurrFlux() > 0f) return;

        refreshTacticalSystem(ship);
        refreshPhaseCloak(ship);

        if (ship == Global.getCombatEngine().getPlayerShip()) {
            Global.getCombatEngine().maintainStatusForPlayerShip(
                    STATUS_KEY,
                    STATUS_ICON,
                    "超限界域",
                    "系统刷新完成",
                    false
            );
        }
    }

    /**
     * 刷新舰船战术系统。
     *
     * 项目内战术系统 AI 通常通过 ship.getSystem().isCoolingDown() 判断冷却状态；
     * 这里保持同一对象来源，先检测是否存在，再刷新其冷却与弹药。
     *
     * @param ship 安装该船插的舰船实例
     */
    private void refreshTacticalSystem(ShipAPI ship) {
        refreshSystem(ship.getSystem());
    }

    /**
     * 刷新 phasecloak 右键系统。
     *
     * 项目内右键系统 AI 使用 ship.getPhaseCloak() 判断和触发，例如通过
     * ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK 启用 phasecloak。这里明确读取同一个对象，
     * 避免把右键系统误当成普通战术系统。
     *
     * @param ship 安装该船插的舰船实例
     */
    private void refreshPhaseCloak(ShipAPI ship) {
        refreshSystem(ship.getPhaseCloak());
    }

    /**
     * 刷新单个 ShipSystemAPI 实例。
     *
     * 会先检测 null，再将剩余冷却清零、装填进度置满，并在系统有弹药上限时补满弹药。
     *
     * @param system 待刷新的舰船系统，可能为 null
     */
    private void refreshSystem(ShipSystemAPI system) {
        if (system == null) return;
        if (system.isActive()) return;
        if (system.getAmmo() == system.getMaxAmmo() && system.getCooldownRemaining() == 0f) return;
        system.setCooldownRemaining(0f);
        if (system.getMaxAmmo() > 0 && system.getAmmo() < system.getMaxAmmo()) {
            system.setAmmo(system.getMaxAmmo());
        }
    }

    /**
     * 舰船创建完成后的兼容性清理。
     *
     * 如果变体同时存在安全协议超驰，则移除安全协议超驰，保持“超限界域优先生效”的结果。
     * 这种写法沿用项目中 Meng_timecontrol 与 Mengtime_S 的处理方式。
     *
     * @param ship 安装船插的舰船实例
     * @param id 当前船插的运行时修改 id
     */
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null || ship.getVariant() == null) return;
        if (ship.getVariant().getHullMods().contains(SAFETY_OVERRIDES)) {
            ship.getVariant().removeMod(SAFETY_OVERRIDES);
            Global.getSoundPlayer().playUISound("cr_allied_warning", 1.0F, 1.0F);
        }
    }

    /**
     * 装配界面安装条件检测。
     *
     * 只禁止与安全协议超驰同装，不限制船体大小；卡巴拉配合关系由玩家/船体配置决定。
     *
     * @param ship 装配界面中被检测的舰船
     * @return true 表示可安装
     */
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship != null && ship.getVariant() != null && !ship.getVariant().hasHullMod(SAFETY_OVERRIDES);
    }

    /**
     * 返回不可安装原因文本。
     *
     * @param ship 装配界面中被检测的舰船
     * @return 安全协议超驰冲突提示；没有冲突时返回 null
     */
    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship != null && ship.getVariant() != null && ship.getVariant().hasHullMod(SAFETY_OVERRIDES)) {
            return "不兼容于 安全协议超驰";
        }
        return null;
    }

    /**
     * 船插 tooltip 扩展说明。
     *
     * 由游戏在船插信息面板中调用，用于显示数据分析、设定描述和不兼容提示。
     * 当前文本先按卡巴拉/虚想树设定写一版，后续可直接替换文案而不影响逻辑。
     *
     * @param tooltip tooltip 构建器
     * @param hullSize 当前预览船体大小
     * @param ship 当前舰船，可能为 null
     * @param width tooltip 宽度
     * @param isForModSpec 是否为船插规格预览
     */
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10f;
        Color highlight = new Color(88, 213, 168, 218);
        Color negative = Misc.getNegativeHighlightColor();

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);
        LabelAPI label = tooltip.addPara(
                "-当舰船辐能为 0 时，每帧刷新 战术系统 与 右键系统 。\n-不兼容于 安全协议超驰 。",
                opad,
                highlight
        );
        label.setHighlight("0", "战术系统", "右键系统", "安全协议超驰");
        label.setHighlightColors(highlight, highlight, highlight, negative);

        tooltip.addSectionHeading("圣殿史录", Alignment.MID, opad);
        tooltip.addPara(
                "超越，超越，超越空间，超越现实...超越时间。",
                opad,
                highlight
        );
    }
}
