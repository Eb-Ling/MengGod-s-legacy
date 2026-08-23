package data.methods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 舰船弹体替换工具类（data.methods.MengProjectileReplacementUtil）。
 * <p>
 * 该工具类不实现任何制导算法，只负责在战斗中扫描指定舰船发射的弹体，
 * 将符合条件的原始弹体替换为预留 projectileSpecId 的追踪导弹弹体，
 * 并把替换弹体的伤害数值与原始弹体保持一致。
 * <p>
 * 调用路径：船体插件的 {@code advanceInCombat()} 每帧调用
 * {@link #advanceReplacement(ShipAPI, float, String)}。
 */
public class MengProjectileReplacementUtil {

    /**
     * 替换弹体标记键。
     * <p>
     * 通过 projectile customData 标记由本工具生成的替换弹，
     * 避免替换弹在下一帧被再次替换导致循环生成。
     */
    public static final String REPLACED_PROJECTILE_KEY = "Meng_replacement_guided_projectile";

    /**
     * 舰船替换状态键。
     * <p>
     * 通过 ship customData 保存每艘舰船已经处理过的原始弹体，
     * 避免同一颗原始弹体在同一帧或后续帧被重复处理。
     */
    public static final String SHIP_DATA_KEY = "Meng_projectile_replacement_data";

    /**
     * 日志输出器。
     * <p>
     * 遵循 ModRule 日志规范，所有异常状态通过 Starsector Logger 输出。
     */
    private static final org.apache.log4j.Logger LOG = Global.getLogger(MengProjectileReplacementUtil.class);

    /**
     * 是否已经记录过替换弹体生成失败。
     * <p>
     * 替换 projectileSpecId 尚未配置时，战斗中可能每帧都会失败；
     * 此开关用于避免刷屏日志。
     */
    private static boolean warnedSpawnFailure = false;

    /**
     * 私有构造方法。
     * <p>
     * 本类只提供静态工具方法，不需要实例化。
     */
    private MengProjectileReplacementUtil() {
    }

    /**
     * 每帧执行指定舰船的弹体替换。
     * <p>
     * 该方法由船体插件调用。方法会扫描战场上的所有弹体，
     * 只处理 {@code projectile.getSource() == ship} 的弹体，
     * 并使用原武器 ID 加指定 replacementProjectileSpecId 生成替换弹。
     *
     * @param ship                        需要执行弹体替换的舰船
     * @param amount                      本帧经过时间，由船体插件传入；当前逻辑仅用于接口一致性
     * @param replacementProjectileSpecId 预留的追踪导弹 projectileSpecId
     */
    public static void advanceReplacement(ShipAPI ship, float amount, String replacementProjectileSpecId) {
        if (ship == null || replacementProjectileSpecId == null || replacementProjectileSpecId.length() <= 0) {
            return;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || engine.isCombatOver() || !ship.isAlive()) {
            return;
        }

        ReplacementData data = getOrCreateData(ship);
        List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
        for (DamagingProjectileAPI projectile : projectiles) {
            if (!shouldReplaceProjectile(projectile, ship, engine, data, replacementProjectileSpecId)) {
                continue;
            }
            replaceProjectile(projectile, ship, engine, data, replacementProjectileSpecId);
        }
    }

    /**
     * 获取或创建舰船弹体替换状态。
     * <p>
     * 状态挂在舰船 customData 上，避免 BaseHullMod 实例被复用时造成不同舰船之间的数据串扰。
     *
     * @param ship 当前执行替换逻辑的舰船
     * @return 当前舰船的替换状态容器
     */
    private static ReplacementData getOrCreateData(ShipAPI ship) {
        Object stored = ship.getCustomData().get(SHIP_DATA_KEY);
        if (stored instanceof ReplacementData) {
            return (ReplacementData) stored;
        }

        ReplacementData data = new ReplacementData();
        ship.setCustomData(SHIP_DATA_KEY, data);
        return data;
    }

    /**
     * 判断弹体是否需要替换。
     * <p>
     * 边界处理包括：跳过非本舰弹体、已处理弹体、已经是替换弹体的弹体、
     * 已命中/过期/淡出/离场弹体、无武器来源弹体以及无碰撞装饰弹体。
     *
     * @param projectile                  待判断弹体
     * @param ship                        当前船体插件所属舰船
     * @param engine                      当前战斗引擎
     * @param data                        当前舰船替换状态
     * @param replacementProjectileSpecId 预留的追踪导弹 projectileSpecId
     * @return true 表示应当替换
     */
    private static boolean shouldReplaceProjectile(DamagingProjectileAPI projectile, ShipAPI ship, CombatEngineAPI engine,
                                                   ReplacementData data, String replacementProjectileSpecId) {
        if (projectile == null) {
            return false;
        }
        if (data.handledProjectiles.containsKey(projectile)) {
            return false;
        }
        if (projectile.getCustomData().containsKey(REPLACED_PROJECTILE_KEY)) {
            return false;
        }
        if (replacementProjectileSpecId.equals(projectile.getProjectileSpecId())) {
            return false;
        }
        if (projectile.getSource() != ship) {
            return false;
        }
        if (projectile.getWeapon() == null) {
            return false;
        }
        if (projectile.didDamage() || projectile.isExpired() || projectile.isFading()) {
            return false;
        }
        if (!engine.isEntityInPlay(projectile) || projectile.wasRemoved()) {
            return false;
        }
        return projectile.getCollisionClass() != CollisionClass.NONE;
    }

    /**
     * 执行单颗弹体替换。
     * <p>
     * 先尝试生成替换弹并复制伤害；只有生成成功后才移除原始弹体。
     * 这样 replacementProjectileSpecId 尚未配置或生成失败时，不会吞掉原弹体。
     *
     * @param original                    原始弹体
     * @param ship                        原始弹体来源舰船
     * @param engine                      当前战斗引擎
     * @param data                        当前舰船替换状态
     * @param replacementProjectileSpecId 预留的追踪导弹 projectileSpecId
     */
    private static void replaceProjectile(DamagingProjectileAPI original, ShipAPI ship, CombatEngineAPI engine,
                                          ReplacementData data, String replacementProjectileSpecId) {
        data.handledProjectiles.put(original, Boolean.TRUE);

        WeaponAPI weapon = original.getWeapon();
        Vector2f spawnLocation = new Vector2f(original.getLocation());
        Vector2f originalVelocity = new Vector2f(original.getVelocity());
        float originalFacing = original.getFacing();

        CombatEntityAPI spawned = engine.spawnProjectile(ship, weapon, weapon.getId(), replacementProjectileSpecId,
                spawnLocation, originalFacing, new Vector2f());
        if (!(spawned instanceof DamagingProjectileAPI)) {
            warnSpawnFailure(replacementProjectileSpecId);
            return;
        }

        DamagingProjectileAPI replacement = (DamagingProjectileAPI) spawned;
        copyProjectileState(original, replacement, originalVelocity, originalFacing);
        engine.removeEntity(original);
    }

    /**
     * 将原始弹体的关键状态复制到替换弹体。
     * <p>
     * 当前只复制伤害、伤害类型、来源、朝向、角速度与速度。
     * 追踪能力由替换 projectileSpecId 对应的导弹规格或导弹 AI 自身提供。
     *
     * @param original         原始弹体
     * @param replacement      替换生成的追踪导弹弹体
     * @param originalVelocity 原始弹体速度快照
     * @param originalFacing   原始弹体朝向快照
     */
    private static void copyProjectileState(DamagingProjectileAPI original, DamagingProjectileAPI replacement,
                                            Vector2f originalVelocity, float originalFacing) {
        float damageAmount = original.getBaseDamageAmount();
        replacement.setDamageAmount(damageAmount);
        replacement.getDamage().setDamage(damageAmount);
        replacement.getDamage().setType(original.getDamageType());
        replacement.setSource(original.getSource());
        replacement.setOwner(original.getOwner());
        replacement.setFacing(originalFacing);
        replacement.setAngularVelocity(original.getAngularVelocity());
        replacement.getVelocity().set(new Vector2f(originalVelocity.x*0.2f,originalVelocity.y*0.2f));
        replacement.setCustomData(REPLACED_PROJECTILE_KEY, Boolean.TRUE);
    }

    /**
     * 记录替换弹体生成失败。
     * <p>
     * 该日志通常意味着预留 projectileSpecId 尚未在武器/弹体配置中完成。
     *
     * @param replacementProjectileSpecId 生成失败的替换 projectileSpecId
     */
    private static void warnSpawnFailure(String replacementProjectileSpecId) {
        if (warnedSpawnFailure) {
            return;
        }
        warnedSpawnFailure = true;
        LOG.warn("Failed to spawn replacement guided projectile: " + replacementProjectileSpecId);
    }

    /**
     * 单舰弹体替换状态容器。
     * <p>
     * 由 {@link #getOrCreateData(ShipAPI)} 创建并保存在舰船 customData 中，
     * 用于记录已经尝试处理过的原始弹体。
     */
    public static class ReplacementData {

        /**
         * 已处理原始弹体集合。
         * <p>
         * 使用 IdentityHashMap 按对象身份记录弹体实例，
         * 避免不同弹体因 equals 实现变化而互相影响。
         */
        public final Map<DamagingProjectileAPI, Boolean> handledProjectiles =
                new IdentityHashMap<DamagingProjectileAPI, Boolean>();
    }
}
