package data.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class Meng_Dragonfire_blueonhit implements OnHitEffectPlugin {
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof ShipAPI) {
            engine.spawnEmpArcPierceShields(projectile.getSource(), point, target, target, DamageType.HIGH_EXPLOSIVE, 100f, 100f, 100000f, null, 15f, new Color(85, 224, 255, 255), new Color(255, 255, 255, 255));
            engine.spawnEmpArcPierceShields(projectile.getSource(), point, target, target, DamageType.HIGH_EXPLOSIVE, 100f, 100f, 100000f, null, 15f, new Color(85, 224, 255, 255), new Color(255, 255, 255, 255));
            engine.spawnEmpArcPierceShields(projectile.getSource(), point, target, target, DamageType.HIGH_EXPLOSIVE, 100f, 100f, 100000f, null, 15f, new Color(85, 224, 255, 255), new Color(255, 255, 255, 255));
            engine.spawnEmpArcPierceShields(projectile.getSource(), point, target, target, DamageType.HIGH_EXPLOSIVE, 100f, 100f, 100000f, null, 15f, new Color(85, 224, 255, 255), new Color(255, 255, 255, 255));
            engine.spawnEmpArcPierceShields(projectile.getSource(), point, target, target, DamageType.HIGH_EXPLOSIVE, 100f, 100f, 100000f, null, 15f, new Color(85, 224, 255, 255), new Color(255, 255, 255, 255));
        }
    }

}
