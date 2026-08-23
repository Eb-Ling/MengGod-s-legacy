package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import data.methods.Meng_BlackHolePlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

/**
 * Meng_Dragonfire_blueonhit - 蓝龙火命中特效。
 *
 * <h3>功能概述</h3>
 * 命中舰船时产生电弧 + 在命中点召唤一个2秒的黑洞效果。
 *
 * <h3>调用方式</h3>
 * 由 weapon_data.csv 中的 onHit 字段引用，引擎在弹丸命中时自动调用。
 */
public class Meng_Dragonfire_blueonhit implements OnHitEffectPlugin {

    /** 黑洞持续时间（秒） */
    private static final float BLACKHOLE_DURATION = 10.0f;
    /** 黑洞渲染半径（世界坐标单位） */
    private static final float BLACKHOLE_RADIUS = 500f;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit,
                      ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof ShipAPI) {
            // 电弧效果
            for (int i = 0; i < 5; i++) {
                engine.spawnEmpArcPierceShields(
                        projectile.getSource(), point, target, target,
                        DamageType.HIGH_EXPLOSIVE,
                        100f, 100f, 100000f, null, 15f,
                        new Color(85, 224, 255, 255),
                        new Color(255, 255, 255, 255));
            }

            // 在命中点召唤黑洞（光线步进 + 引力透镜 + 背景扭曲）
            Meng_BlackHolePlugin blackHole = new Meng_BlackHolePlugin(
                    new Vector2f(point.x, point.y), BLACKHOLE_RADIUS);
            blackHole.setLifetime(BLACKHOLE_DURATION);
            blackHole.setPosition(Global.getCombatEngine().getPlayerShip().getMouseTarget());
            engine.addLayeredRenderingPlugin(blackHole);
        }
    }
}
