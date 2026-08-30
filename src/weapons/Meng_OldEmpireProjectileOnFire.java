package data.weapons;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.methods.Meng_OldEmpireEffects;
import data.methods.Meng_OldEmpireIds;
import data.methods.Meng_OldEmpireMath;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class Meng_OldEmpireProjectileOnFire implements OnFireEffectPlugin {
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (projectile == null || weapon == null || engine == null) return;
        ShipAPI source = weapon.getShip();
        if (source == null) return;

        Vector2f origin = new Vector2f(projectile.getLocation());
        float facing = projectile.getFacing();
        engine.removeEntity(projectile);

        String id = weapon.getId();
        if (Meng_OldEmpireIds.CHAIN.equals(id)) {
            float cooldown = chainCooldown(source);
            weapon.setRemainingCooldownTo(cooldown);
            engine.addPlugin(new DeferredCooldown(engine, weapon, cooldown));
        } else if (Meng_OldEmpireIds.DUST.equals(id)) {
            float cooldown = Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.B_RHYTHM) ? 1.5f : 3f;
            weapon.setRemainingCooldownTo(cooldown);
            engine.addPlugin(new DeferredCooldown(engine, weapon, cooldown));
        } else if (Meng_OldEmpireIds.TRACER.equals(id)) {
            float cooldown = tracerCooldown(source);
            weapon.setRemainingCooldownTo(cooldown);
            engine.addPlugin(new DeferredCooldown(engine, weapon, cooldown));
        }

        boolean burst = Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.B_BURST);
        float burstAngle = 15f;
        if (Meng_OldEmpireIds.DUST.equals(id)) {
            burstAngle = 25f;
        } else if (Meng_OldEmpireIds.CHAIN.equals(id) && Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.A_HEAVY)) {
            burstAngle = 8f;
        }
        float[] directions = burst
                ? new float[]{-burstAngle, 0f, burstAngle} : new float[]{0f};
        float directionDamage = burst ? 0.55f : 1f;

        for (float direction : directions) {
            if (Meng_OldEmpireIds.CHAIN.equals(id)) {
                fireChain(engine, weapon, source, origin, facing + direction, directionDamage);
            } else if (Meng_OldEmpireIds.DUST.equals(id)) {
                fireDust(engine, weapon, source, origin, facing + direction, directionDamage);
            } else if (Meng_OldEmpireIds.TRACER.equals(id)) {
                fireTracer(engine, weapon, source, origin, facing + direction, directionDamage);
            }
        }
    }

    private void fireChain(CombatEngineAPI engine, WeaponAPI weapon, ShipAPI source,
                           Vector2f origin, float angle, float directionDamage) {
        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.A_SPLIT)) {
            boolean precision = Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.B_PRECISION);
            float cone = precision ? 10f : 26f;
            float step = cone / 5f;
            for (int i = 0; i < 6; i++) {
                float pelletOffset = -cone * 0.5f + i * step
                        + Meng_OldEmpireMath.random(precision ? -0.3f : -1.2f, precision ? 0.3f : 1.2f);
                spawn(engine, weapon, source, "Meng_OldEmpire_spawn_chain_pellet", origin,
                        angle + pelletOffset,
                        directionDamage * commonDamageMult(source) * 0.25f,
                        chainVelocityMult(source), false);
            }
            return;
        }

        String spec = Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.A_HEAVY)
                ? "Meng_OldEmpire_spawn_chain_heavy" : "Meng_OldEmpire_spawn_chain";
        float alphaDamage = Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.A_HEAVY) ? 1.60f : 1f;
        alphaDamage *= commonDamageMult(source);
        float spread = Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.B_PRECISION) ? 0.5f : 5f;
        float shotAngle = angle + Meng_OldEmpireMath.random(-spread, spread);

        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.A_MULTI)) {
            spawn(engine, weapon, source, spec, Meng_OldEmpireMath.offset(origin, angle + 90f, -5f),
                    shotAngle, directionDamage * alphaDamage * 0.65f, chainVelocityMult(source), false);
            spawn(engine, weapon, source, spec, Meng_OldEmpireMath.offset(origin, angle + 90f, 5f),
                    shotAngle, directionDamage * alphaDamage * 0.65f, chainVelocityMult(source), false);
        } else {
            spawn(engine, weapon, source, spec, origin, shotAngle,
                    directionDamage * alphaDamage, chainVelocityMult(source), false);
        }
    }

    private void fireDust(CombatEngineAPI engine, WeaponAPI weapon, ShipAPI source,
                          Vector2f origin, float angle, float directionDamage) {
        String spec = "Meng_OldEmpire_spawn_dust";
        float alphaDamage = 1f;
        boolean cyber = false;
        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.A_HEAVY)) {
            spec = "Meng_OldEmpire_spawn_dust_heavy";
            alphaDamage *= 1.60f;
        } else if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.A_SPLIT)) {
            spec = "Meng_OldEmpire_spawn_dust_cyber";
            alphaDamage *= 0.70f;
            cyber = true;
        }
        alphaDamage *= commonDamageMult(source);
        float finalDamage = directionDamage * alphaDamage;
        float shotAngle = angle + Meng_OldEmpireMath.random(-dustSpread(source), dustSpread(source));

        if (cyber) {
            for (int i = -1; i <= 1; i++) {
                spawn(engine, weapon, source, spec,
                        Meng_OldEmpireMath.offset(origin, angle + 90f, i * 7f),
                        angle + i * 5f + Meng_OldEmpireMath.random(-0.7f, 0.7f),
                        finalDamage * 0.45f, dustVelocityMult(source), false);
            }
            return;
        }

        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.A_MULTI)) {
            spawn(engine, weapon, source, spec, Meng_OldEmpireMath.offset(origin, angle + 90f, -6f),
                    shotAngle, finalDamage * 0.65f, dustVelocityMult(source), cyber);
            spawn(engine, weapon, source, spec, Meng_OldEmpireMath.offset(origin, angle + 90f, 6f),
                    shotAngle, finalDamage * 0.65f, dustVelocityMult(source), cyber);
        } else {
            spawn(engine, weapon, source, spec, origin, shotAngle,
                    finalDamage, dustVelocityMult(source), cyber);
        }
    }

    private void fireTracer(CombatEngineAPI engine, WeaponAPI weapon, ShipAPI source,
                            Vector2f origin, float angle, float directionDamage) {
        String spec = Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.A_HEAVY)
                ? "Meng_OldEmpire_spawn_tracer_heavy" : "Meng_OldEmpire_spawn_tracer";
        float alphaDamage = Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.A_HEAVY) ? 1.60f : 1f;
        alphaDamage *= commonDamageMult(source);
        float spread = Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.B_PRECISION) ? 0.15f : 0.8f;

        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.A_SPLIT)) {
            for (int i = -1; i <= 1; i++) {
                spawn(engine, weapon, source, "Meng_OldEmpire_spawn_tracer_small",
                        Meng_OldEmpireMath.offset(origin, angle + 90f, i * 5f),
                        angle + i * 4.5f + Meng_OldEmpireMath.random(-spread, spread),
                        directionDamage * commonDamageMult(source) * 0.50f,
                        tracerVelocityMult(source), false);
            }
        } else if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.A_MULTI)) {
            float shotAngle = angle + Meng_OldEmpireMath.random(-spread, spread);
            spawn(engine, weapon, source, spec, Meng_OldEmpireMath.offset(origin, angle + 90f, -5f),
                    shotAngle, directionDamage * alphaDamage * 0.65f,
                    tracerVelocityMult(source), false);
            spawn(engine, weapon, source, spec, Meng_OldEmpireMath.offset(origin, angle + 90f, 5f),
                    shotAngle, directionDamage * alphaDamage * 0.65f,
                    tracerVelocityMult(source), false);
        } else {
            spawn(engine, weapon, source, spec, origin,
                    angle + Meng_OldEmpireMath.random(-spread, spread), directionDamage * alphaDamage,
                    tracerVelocityMult(source), false);
        }
    }

    private DamagingProjectileAPI spawn(CombatEngineAPI engine, WeaponAPI weapon, ShipAPI source,
                                        String weaponId, Vector2f point, float angle, float damageMult,
                                        float velocityMult, boolean cyber) {
        DamagingProjectileAPI spawned = (DamagingProjectileAPI) engine.spawnProjectile(
                source, weapon, weaponId, point, angle, new Vector2f(source.getVelocity()));
        if (spawned == null) return null;
        spawned.setDamageAmount(spawned.getDamageAmount() * damageMult);
        Meng_OldEmpireEffects.decorateSpawn(spawned, source, engine, weaponId, angle);

        Vector2f velocity = spawned.getVelocity();
        Vector2f shipVelocity = source.getVelocity();
        velocity.x = shipVelocity.x + (velocity.x - shipVelocity.x) * velocityMult;
        velocity.y = shipVelocity.y + (velocity.y - shipVelocity.y) * velocityMult;

        if (spawned instanceof com.fs.starfarer.api.combat.MissileAPI) {
            boolean heavyDust = "Meng_OldEmpire_spawn_dust_heavy".equals(weaponId);
            boolean splitDust = "Meng_OldEmpire_spawn_dust_cyber".equals(weaponId);
            if (heavyDust || splitDust) {
                Meng_OldEmpireDustPayloadController.start(
                        (com.fs.starfarer.api.combat.MissileAPI) spawned,
                        weapon, engine, heavyDust);
            }
        }

        return spawned;
    }

    private float chainVelocityMult(ShipAPI source) {
        float result = 1f;
        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.B_PRECISION)) result *= 1.80f;
        return result;
    }

    private float tracerVelocityMult(ShipAPI source) {
        float result = 1f;
        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.B_RHYTHM)) result *= 1.30f;
        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.B_PRECISION)) result *= 1.80f;
        return result;
    }

    private float commonDamageMult(ShipAPI source) {
        return Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.B_PRECISION) ? 1.45f : 1f;
    }

    private float dustSpread(ShipAPI source) {
        return Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.B_PRECISION) ? 0.2f : 1.4f;
    }

    private float dustVelocityMult(ShipAPI source) {
        float result = 1f;
        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.B_RHYTHM)) result *= 1.28f;
        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.B_PRECISION)) result *= 1.80f;
        return result;
    }

    private float chainCooldown(ShipAPI source) {
        float cooldown;
        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.A_SPLIT)) {
            cooldown = 1f;
        } else if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.A_HEAVY)) {
            cooldown = 0.7f;
        } else {
            cooldown = 0.25f;
        }
        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.B_RHYTHM)) cooldown /= 1.5f;
        return cooldown;
    }

    static float tracerCooldown(ShipAPI source) {
        float cooldown = Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.A_HEAVY) ? 4f : 1.05f;
        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.B_RHYTHM)) cooldown *= 0.65f;
        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.B_PRECISION)) cooldown *= 1.45f;
        return cooldown;
    }

    private static final class DeferredCooldown extends BaseEveryFrameCombatPlugin {
        private final CombatEngineAPI engine;
        private final WeaponAPI weapon;
        private final float cooldown;

        private DeferredCooldown(CombatEngineAPI engine, WeaponAPI weapon, float cooldown) {
            this.engine = engine;
            this.weapon = weapon;
            this.cooldown = cooldown;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (weapon != null) {
                weapon.setRemainingCooldownTo(cooldown);
            }
            engine.removePlugin(this);
        }
    }
}
