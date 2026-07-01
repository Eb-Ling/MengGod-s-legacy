package data.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class Meng_Sunder_onhit implements OnHitEffectPlugin {
    public final float size = 200f;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        float damage = projectile.getDamageAmount();
        Color color1 = new Color(250, 40, 40, 255);
        Color color2 = new Color(255, 200, 200, 200);
        DamagingExplosionSpec spc = new DamagingExplosionSpec(
                size * 0.005f,
                size,
                size * 0.75f,
                damage,
                damage * 0.5f,
                CollisionClass.PROJECTILE_FF,
                CollisionClass.PROJECTILE_FIGHTER, // irrelevant - no explosion for fighters
                size * 0.06f,
                size * 0.08f,
                size * 0.005f,
                Math.round(size * 0.15f),
                color1, color2);
        spc.setDamageType(DamageType.HIGH_EXPLOSIVE);
        spc.setShowGraphic(true);
        engine.spawnDamagingExplosion(spc, projectile.getSource(), point);
        RippleDistortion ripple = new RippleDistortion(point, new Vector2f());
        ripple.setSize(size);
        ripple.setIntensity(size * 0.25f);
        ripple.fadeOutSize(4f);
        ripple.fadeOutIntensity(2f);
        ripple.setFrameRate(90f);
        DistortionShader.addDistortion(ripple);
    }
}
