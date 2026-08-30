package data.weapons;

import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import data.methods.Meng_OldEmpireEffects;
import org.lwjgl.util.vector.Vector2f;

public class Meng_OldEmpireOnHitEffect implements OnHitEffectPlugin {
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit,
                      ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (projectile == null || target == null || point == null || engine == null) return;
        if ("Meng_OldEmpire_tracer_heavy_shot".equals(projectile.getProjectileSpecId())) {
            Meng_OldEmpireEffects.heavyTracerImpact(projectile, target, point, engine);
        }
        Meng_OldEmpireEffects.decorateProjectileImpact(projectile, point, engine);
        Meng_OldEmpireEffects.applyGamma(projectile.getSource(), target, point, engine,
                Meng_OldEmpireEffects.projectileGammaDamage(projectile),
                Meng_OldEmpireEffects.projectileGammaTriggerWeight(projectile));
    }
}
