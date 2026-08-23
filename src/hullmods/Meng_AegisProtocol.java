package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

/**
 * 适应性圣盾协议 - 仅限护卫舰和驱逐舰的防御型船插。
 *
 * 继承 BaseHullMod，实现以下效果：
 * - 基础效果（始终生效）：结构值 -15%，峰值时间 -30%，外观永不破损，免疫 EMP 伤害
 * - 自适应免疫（条件生效）：
 *   - 受到弹体武器伤害后进入状态 A，免疫弹体伤害
 *   - 受到光束武器伤害后进入状态 B，免疫光束伤害
 *   - 每次状态转换提高 250 辐能，恢复 5% 最大结构值，恢复 1% CR
 * - 失效条件：CR < 30%、过载、耗散时自适应免疫失效
 *
 * 调用方式：由 hull_mods.csv 中 "Meng_AegisProtocol" 条目通过 script 字段挂载。
 */
public class Meng_AegisProtocol extends BaseHullMod {

    /** 自定义数据 key，用于 ship.getCustomData() 存储 DataContainer 实例 */
    public static final String KEY = "Meng_AegisProtocol_data";

    /** 结构值乘数，0.85 即降低 15% */
    private static final float HULL_MULT = 0.85f;

    /** 峰值时间乘数，0.7 即降低 30% */
    private static final float PEAK_MULT = 0.7f;

    /** CR 失效阈值，低于此值时自适应免疫失效 */
    private static final float CR_THRESHOLD = 0.3f;

    /** 每次转换增加的赋能上限 */
    private static final float FLUX_BONUS_PER_CONVERSION = 250f;

    /** 每次转换恢复的最大结构值比例 */
    private static final float HULL_HEAL_PERCENT = 0.05f;

    /** 每次转换恢复的 CR 比例 */
    private static final float CR_GAIN_PER_CONVERSION = 0.01f;

    /** 空白破损覆盖贴图路径，用于替换舰船破损外观 */
    private static final String COVER_SPRITE = "graphics/Meng/hullmods/meng_cover.png";

    /**
     * 自适应免疫状态枚举。
     * NONE：初始状态，无免疫
     * PROJECTILE_IMMUNE：状态 A，免疫弹体伤害
     * BEAM_IMMUNE：状态 B，免疫光束伤害
     */
    private enum AegisState {
        NONE,
        PROJECTILE_IMMUNE,
        BEAM_IMMUNE
    }

    /**
     * 舰船专属数据容器，存储自适应免疫的运行状态。
     * 通过 ship.setCustomData(KEY, data) 绑定到舰船，防止多舰船数据冲突。
     */
    private static class DataContainer {
        /** 当前免疫状态 */
        AegisState state = AegisState.NONE;
        /** 当前是否处于失效状态 */
        boolean disabled = false;
        /** 当前挂载的伤害修改监听器引用，用于失效时移除 */
        AegisDamageModifier listener = null;
    }

    /**
     * 在舰船创建前应用基础属性修改。
     * 由引擎在战斗开始时自动调用，修改舰船的 MutableShipStatsAPI。
     *
     * 修改内容：
     * - 结构值降低 15%（modifyMult 0.85）
     * - 峰值时间降低 30%（modifyMult 0.7）
     * - EMP 伤害免疫（modifyMult 0）
     *
     * @param hullSize 舰船尺寸类型
     * @param stats    舰船可变属性接口
     * @param id       船插唯一标识符
     */
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getHullBonus().modifyMult(id, HULL_MULT);
        stats.getPeakCRDuration().modifyMult(id, PEAK_MULT);
        stats.getEmpDamageTakenMult().modifyMult(id, 0f);
    }

    /**
     * 每帧战斗推进逻辑。
     * 由引擎在每帧调用，负责：
     * 1. 替换破损贴图为空白贴图，清除破损贴花（外观永不破损）
     * 2. 检测失效条件（CR < 30%、过载、耗散）
     * 3. 管理伤害监听器的挂载与移除
     * 4. 失效时清除赋能上限增益并重置免疫状态
     * 5. 为玩家舰船显示状态栏信息
     *
     * @param ship   当前舰船实例
     * @param amount 本帧时间增量（秒）
     */
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()) return;

        ship.setDHullOverlay(COVER_SPRITE);
        ship.clearDamageDecals();

        if (!ship.getCustomData().containsKey(KEY)) {
            ship.setCustomData(KEY, new DataContainer());
        }
        DataContainer data = (DataContainer) ship.getCustomData().get(KEY);

        boolean shouldDisable = ship.getCurrentCR() < CR_THRESHOLD
                || ship.getFluxTracker().isOverloaded()
                || ship.getFluxTracker().isVenting();

        if (shouldDisable) {
            if (data.listener != null) {
                ship.removeListener(data.listener);
                data.listener = null;
                data.state = AegisState.NONE;
                data.disabled = true;
            }
        } else {
            if (data.listener == null) {
                data.listener = new AegisDamageModifier(ship, data);
                ship.addListener(data.listener);
                data.disabled = false;
            }
        }

        if (ship == Global.getCombatEngine().getPlayerShip()) {
            String statusKey = "Meng_AegisProtocol_status";
            if (data.disabled) {
                Global.getCombatEngine().maintainStatusForPlayerShip(statusKey,
                        "graphics/fx/Meng_logo1.png", "适应性圣盾", "已失效", true);
            } else {
                String stateText;
                switch (data.state) {
                    case PROJECTILE_IMMUNE:
                        stateText = "状态A - 弹体免疫";
                        break;
                    case BEAM_IMMUNE:
                        stateText = "状态B - 光束免疫";
                        break;
                    default:
                        stateText = "待激活";
                        break;
                }
                Global.getCombatEngine().maintainStatusForPlayerShip(statusKey,
                        "graphics/fx/Meng_logo1.png", "适应性圣盾", stateText, false);
            }
        }
    }

    /**
     * 在 Tooltip 中显示船插的详细数据分析。
     * 由引擎在玩家查看船插信息时调用。
     *
     * @param tooltip        Tooltip 构建接口
     * @param hullSize       舰船尺寸类型
     * @param ship           舰船实例（可能为 null）
     * @param width          Tooltip 宽度
     * @param isForModSpec   是否为 ModSpec 预览
     */
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10f;
        Color highlight = new Color(100, 200, 255, 255);
        Color penalty = new Color(255, 80, 60, 255);

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);

        LabelAPI label = tooltip.addPara(
                "#舰船结构值降低 %s%% ，峰值时间降低 %s%% 。\n#外观永不破损，免疫 EMP 伤害。\n#受到弹体武器伤害后进入状态A，免疫弹体伤害。\n#受到光束武器伤害后进入状态B，免疫光束伤害。\n#每次状态转换提高 %s 辐能，恢复 %s%% 最大结构值和 %s%% CR。\n#当 CR 低于 %s%% 、过载或耗散时，自适应免疫失效。",
                opad, highlight,
                "15", "30",
                String.valueOf(Math.round(FLUX_BONUS_PER_CONVERSION)),
                String.valueOf(Math.round(HULL_HEAL_PERCENT * 100f)),
                String.valueOf(Math.round(CR_GAIN_PER_CONVERSION * 100f)),
                String.valueOf(Math.round(CR_THRESHOLD * 100f))
        );
        label.setHighlight("15", "30",
                String.valueOf(Math.round(FLUX_BONUS_PER_CONVERSION)),
                String.valueOf(Math.round(HULL_HEAL_PERCENT * 100f)),
                String.valueOf(Math.round(CR_GAIN_PER_CONVERSION * 100f)),
                String.valueOf(Math.round(CR_THRESHOLD * 100f))
        );
        label.setHighlightColors(penalty, penalty, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("圣殿史录", Alignment.MID, opad);
        tooltip.addPara("圣盾并非坚不可摧，而是在破碎中学会适应，在适应中获得永生。", opad, highlight);
    }

    /**
     * 判断船插是否可安装到指定舰船。
     * 仅限护卫舰（FRIGATE）和驱逐舰（DESTROYER）。
     *
     * @param ship 目标舰船
     * @return true 表示可安装
     */
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship.getHullSize() == ShipAPI.HullSize.FRIGATE
                || ship.getHullSize() == ShipAPI.HullSize.DESTROYER;
    }

    /**
     * 返回船插不可安装时的提示文本。
     *
     * @param ship 目标舰船
     * @return 不可安装的原因文本，可安装时返回 null
     */
    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship.getHullSize() != ShipAPI.HullSize.FRIGATE && ship.getHullSize() != ShipAPI.HullSize.DESTROYER) {
            return "只能用于护卫舰和驱逐舰";
        }
        return null;
    }

    /**
     * 自适应伤害修改监听器。
     *
     * 实现 DamageTakenModifier 接口，通过 ship.addListener() 挂载到舰船。
     * 在每次受到伤害时被引擎回调，根据伤害类型和当前状态决定是否免疫并触发转换增益。
     *
     * 调用路径：
     * - 引擎每帧检测伤害事件 → 回调 modifyDamageTaken → 修改 DamageAPI 实现免疫
     * - 状态转换时直接操作 ship 的 stats 和属性实现增益
     */
    private static class AegisDamageModifier implements DamageTakenModifier {

        /** 所绑定的舰船实例 */
        private final ShipAPI ship;

        /** 所绑定的数据容器引用 */
        private final DataContainer data;

        /**
         * 构造伤害修改监听器。
         *
         * @param ship 绑定的舰船
         * @param data 绑定的数据容器
         */
        AegisDamageModifier(ShipAPI ship, DataContainer data) {
            this.ship = ship;
            this.data = data;
        }

        /**
         * 伤害修改回调，由引擎在伤害应用前调用。
         *
         * 处理逻辑：
         * 1. 判断伤害来源类型（弹体 DamagingProjectileAPI / 光束 BeamAPI）
         * 2. 根据当前状态决定是否免疫（setDamage(0)）
         * 3. 状态不同时触发转换：切换状态、增加辐能、恢复结构值和 CR
         *
         * @param param     伤害来源对象（DamagingProjectileAPI / BeamAPI / 其他）
         * @param target    受击实体（舰船或模块）
         * @param damage    伤害数据接口，可直接修改伤害值
         * @param point     命中点世界坐标
         * @param shieldHit 是否命中护盾
         * @return 修改标识符，始终返回 null
         */
        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage,
                                        Vector2f point, boolean shieldHit) {
            if (!ship.isAlive()) return null;
            if (data.disabled) return null;

            boolean isProjectile = param instanceof DamagingProjectileAPI;
            boolean isBeam = param instanceof BeamAPI;

            if (!isProjectile && !isBeam) return null;

            if (isProjectile) {
                if (data.state != AegisState.PROJECTILE_IMMUNE) {
                    triggerConversion();
                    data.state = AegisState.PROJECTILE_IMMUNE;
                }
                damage.setDamage(0f);
            } else {
                if (data.state != AegisState.BEAM_IMMUNE) {
                    triggerConversion();
                    data.state = AegisState.BEAM_IMMUNE;
                }
                damage.setDamage(0f);
            }

            return null;
        }

        /**
         * 触发状态转换增益。
         * 增加 250 辐能、恢复 5% 最大结构值、恢复 1% CR。
         * 辐能通过 FluxTrackerAPI.increaseFlux() 直接增加，
         * 该增益为一次性效果，失效时不回退已获得的辐能。
         */
        private void triggerConversion() {
            FluxTrackerAPI tracker = ship.getFluxTracker();
            tracker.increaseFlux(FLUX_BONUS_PER_CONVERSION, true);

            float healAmount = ship.getMaxHitpoints() * HULL_HEAL_PERCENT;
            ship.setHitpoints(Math.min(ship.getMaxHitpoints(), ship.getHitpoints() + healAmount));

            ship.setCurrentCR(Math.min(1f, ship.getCurrentCR() + CR_GAIN_PER_CONVERSION));
        }
    }
}
