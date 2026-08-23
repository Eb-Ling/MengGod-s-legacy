package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.methods.MengProjectileReplacementUtil;

import java.awt.Color;

/**
 * 火羽弹体导引船体插件（data.hullmods.Meng_fire_ProjectileGuidance）。
 * <p>
 * 继承 {@link BaseHullMod}，通过 {@link #advanceInCombat(ShipAPI, float)}
 * 在舰船战术系统启动期间每帧调用 {@link MengProjectileReplacementUtil}，
 * 将本舰发射出的普通弹体替换为预留 projectileSpecId 的追踪导弹弹体。
 * <p>
 * 本类只承担船插入口与说明文本职责，实际筛选、替换与伤害复制逻辑位于
 * {@code data.methods.MengProjectileReplacementUtil}。
 */
public class Meng_fire_ProjectileGuidance extends BaseHullMod {

    /**
     * 替换导弹弹体 ID。
     * <p>
     * 这是预留 projectileSpecId 位置。后续只需要在武器/弹体配置中创建同名可追踪导弹弹体，
     * 本船插就会把舰船发射出的符合条件弹体替换为该弹体。
     */
    public static final String REPLACEMENT_PROJECTILE_SPEC_ID = "Meng_fire_li_targeting_missile_proj";

    /**
     * 船插说明高亮颜色。
     * <p>
     * 与火羽系列船插保持一致，用于 tooltip 的重点数据展示。
     */
    private static final Color HIGHLIGHT_COLOR = new Color(255, 55, 40, 255);

    /**
     * 添加船插扩展说明。
     * <p>
     * 由游戏在装配界面生成 tooltip 时调用，用于说明弹体替换规则和预留弹体 ID。
     *
     * @param tooltip      当前 tooltip 构建器
     * @param hullSize     当前舰船大小
     * @param ship         当前舰船
     * @param width        tooltip 宽度
     * @param isForModSpec 是否为船插规格说明
     */
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship,
                                          float width, boolean isForModSpec) {
        float opad = 12.0F;

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);
        LabelAPI label = tooltip.addPara(
                "- 舰船战术系统启动期间 ，本舰发射出的有效实弹与能量弹体会被替换为 流光导弹 。\n" +
                        "-流光导弹的 伤害数值 与 伤害类型 继承自原始弹体。\n"+
                        "-流光导弹拥有一定的追踪能力，不具备原始武器特效，几乎不可被拦截。",
                opad, HIGHLIGHT_COLOR, REPLACEMENT_PROJECTILE_SPEC_ID);
        label.setHighlight("舰船战术系统启动期间", "流光导弹", "伤害数值", "伤害类型");
        label.setHighlightColors(HIGHLIGHT_COLOR, HIGHLIGHT_COLOR, HIGHLIGHT_COLOR, HIGHLIGHT_COLOR);
    }

    /**
     * 每帧执行船插战斗逻辑。
     * <p>
     * 由 Starsector 战斗系统每帧调用。方法本身不直接操作弹体，
     * 仅在舰船战术系统处于启动状态时，把舰船、时间步长与预留 projectileSpecId 传递给方法层工具类。
     *
     * @param ship   装备该船插的舰船
     * @param amount 本帧经过时间
     */
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        if (!isShipSystemActive(ship)) {
            return;
        }
        MengProjectileReplacementUtil.advanceReplacement(ship, amount, REPLACEMENT_PROJECTILE_SPEC_ID);
    }

    /**
     * 判断舰船战术系统是否处于启动期间。
     * <p>
     * 该方法由 {@link #advanceInCombat(ShipAPI, float)} 调用。
     * 当前使用 {@link ShipSystemAPI#isActive()} 作为触发条件，
     * 覆盖系统启动后真正生效的时间段，系统未启动或冷却时不会替换弹体。
     *
     * @param ship 装备该船插的舰船
     * @return true 表示战术系统正在生效，允许执行弹体替换
     */
    private boolean isShipSystemActive(ShipAPI ship) {
        if (ship == null || ship.getSystem() == null) {
            return false;
        }
        return ship.getSystem().isActive();
    }

    /**
     * 判断船插是否可安装。
     * <p>
     * 当前沿用火羽系列船插限制：只能安装在拥有虚子炉核心或虚子炉-璃的舰船上。
     *
     * @param ship 待检查舰船
     * @return true 表示可安装
     */
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship.getVariant().getHullMods().contains("Meng_fire_core") ||
                ship.getVariant().getHullMods().contains("Meng_fire_core_li");
    }

    /**
     * 获取不可安装原因。
     * <p>
     * 由装配界面在 {@link #isApplicableToShip(ShipAPI)} 返回 false 时调用。
     *
     * @param ship 待检查舰船
     * @return 不可安装原因文本
     */
    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (!ship.getVariant().getHullMods().contains("Meng_fire_core") &&
                !ship.getVariant().getHullMods().contains("Meng_fire_core_li")) {
            return "只能用于火羽计划舰船";
        }
        return null;
    }
}
