package data.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;

public class Meng_Gravityweapon implements EveryFrameWeaponEffectPlugin {
    private final boolean init = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!init) {
            weapon.getDamage().getModifier().modifyMult("Meng_Gravitys", 0.05f);
        }
        if (weapon.getChargeLevel() == 1f) {

            for (int f = 0; f < 19; f++) {

                float offset = ((float) Math.random() * 20) - (20 * 0.5f);
                float angle = weapon.getCurrAngle() + offset;
                Vector2f Location = findlocation(weapon);
                DamagingProjectileAPI proj = (DamagingProjectileAPI) engine.spawnProjectile(weapon.getShip(), weapon, "Meng_Gravity", Location, angle, weapon.getShip().getVelocity());
                float randomSpeed = (float) Math.random() * 0.2f + 0.9f;
                proj.getVelocity().scale(randomSpeed);
            }
        }
    }

    private Vector2f findlocation(WeaponAPI weapon) {
        float size = 0f;
        if (weapon.getSlot().isHardpoint()) {
            size = weapon.getSpec().getHardpointFireOffsets().get(0).getX();
        } else {
            size = weapon.getSpec().getTurretFireOffsets().get(0).getX();
        }
        double angle = Math.toRadians(weapon.getCurrAngle());
        Vector2f dir = new Vector2f((float) Math.cos(angle), (float) Math.sin(angle));
        if (dir.lengthSquared() > 0f) dir.normalise();
        dir.scale(size);
        Vector2f loc = new Vector2f(weapon.getLocation());
        return Vector2f.add(loc, dir, new Vector2f());

    }
}
