package data.weapons;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.methods.Meng_OldEmpireEffects;
import data.methods.Meng_OldEmpireIds;
import org.lwjgl.util.vector.Vector2f;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class Meng_OldEmpireContinuousBeamEffect implements EveryFrameWeaponEffectPlugin {
    private boolean initialized;
    private float time;
    private float gammaTimer;
    private float membraneTimer;
    private final float[] lightningTimers = new float[9];
    private final Map<CombatEntityAPI, BeamGammaAccumulator> gammaHits =
            new IdentityHashMap<CombatEntityAPI, BeamGammaAccumulator>();

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || weapon == null || engine.isPaused()) return;
        ShipAPI ship = weapon.getShip();
        if (ship == null) return;

        if (!initialized) {
            weapon.ensureClonedSpec();
            initialized = true;
        }

        time += amount;
        gammaTimer += amount;
        for (int i = 0; i < lightningTimers.length; i++) {
            lightningTimers[i] -= amount;
        }
        List<Float> angles = getAngles(weapon);
        List<BeamAPI> beams = weapon.getBeams();
        if (angles == null || beams == null || angles.size() < 9 || beams.size() < 9) return;

        boolean burst = Meng_OldEmpireIds.has(ship, Meng_OldEmpireIds.B_BURST);
        boolean multi = Meng_OldEmpireIds.has(ship, Meng_OldEmpireIds.A_MULTI);
        boolean split = Meng_OldEmpireIds.has(ship, Meng_OldEmpireIds.A_SPLIT);
        boolean heavy = Meng_OldEmpireIds.has(ship, Meng_OldEmpireIds.A_HEAVY);

        float common = 1f;
        if (heavy) common *= 1.60f;
        if (Meng_OldEmpireIds.has(ship, Meng_OldEmpireIds.B_PRECISION)) common *= 1.45f;
        if (Meng_OldEmpireIds.has(ship, Meng_OldEmpireIds.B_RHYTHM)) common *= 1.30f;
        float directionMult = burst ? 0.55f : 1f;
        float open = 10f * Math.abs((float) Math.cos(Math.PI * time / 2f));
        boolean[] activeBeams = new boolean[9];
        for (int group = 0; group < 3; group++) {
            boolean groupActive = burst || group == 1;
            float burstAngle = heavy ? 6f : 15f;
            float groupDirection = burst ? (group - 1) * burstAngle : 0f;
            for (int sub = 0; sub < 3; sub++) {
                int index = group * 3 + sub;
                boolean active = false;
                float angle = groupDirection;
                float damage = 0f;

                if (groupActive) {
                    if (split) {
                        if (sub == 2) {
                            active = true;
                            damage = common * directionMult;
                        } else if (sub == 0) {
                            active = true;
                            angle -= open;
                            damage = common * directionMult * 0.50f;
                        } else if (sub == 1) {
                            active = true;
                            angle += open;
                            damage = common * directionMult * 0.50f;
                        }
                    } else if (multi) {
                        if (sub == 0 || sub == 1) {
                            active = true;
                            damage = common * directionMult * 0.65f;
                        }
                    } else if (sub == 2) {
                        active = true;
                        damage = common * directionMult;
                    }
                }

                activeBeams[index] = active;
                angles.set(index, angle);
                BeamAPI beam = beams.get(index);
                beam.getDamage().getModifier().modifyMult("Meng_OldEmpire_form_" + index, active ? damage : 0f);
                beam.setWidth(active ? (heavy ? 30f : 12f) : 0.1f);
                if (heavy && active && lightningTimers[index] <= 0f
                        && beam.getBrightness() > 0.22f) {
                    Meng_OldEmpireEffects.decorateHeavyBeamLightning(engine, ship, beam.getFrom(),
                            beam.getTo(), beam.getBrightness(), time, index);
                    lightningTimers[index] = 0.055f;
                }
            }
        }

        membraneTimer -= amount;
        if (split && !burst && activeBeams[3] && activeBeams[4] && activeBeams[5]
                && membraneTimer <= 0f) {
            membraneTimer = 0.045f;
            Meng_OldEmpireEffects.decorateSweepingMembrane(engine, beams.get(5), beams.get(3), beams.get(4),
                    Math.min(beams.get(5).getBrightness(),
                            Math.min(beams.get(3).getBrightness(), beams.get(4).getBrightness())));
        }

        if (Meng_OldEmpireIds.has(ship, Meng_OldEmpireIds.B_PRECISION)) {
            weapon.setRefireDelay(0.45f);
        } else if (Meng_OldEmpireIds.has(ship, Meng_OldEmpireIds.B_RHYTHM)) {
            weapon.setRefireDelay(0.15f);
        } else {
            weapon.setRefireDelay(0.30f);
        }

        for (int i = 0; i < beams.size() && i < activeBeams.length; i++) {
            if (!activeBeams[i]) continue;
            BeamAPI beam = beams.get(i);
            CombatEntityAPI target = beam.getDamageTarget();
            if (target == null) continue;

            BeamGammaAccumulator accumulator = gammaHits.get(target);
            if (accumulator == null) {
                accumulator = new BeamGammaAccumulator();
                gammaHits.put(target, accumulator);
            }
            float brightness = Math.max(0f, Math.min(1f, beam.getBrightness()));
            accumulator.damage += Math.max(0f, beam.getDamage().getDamage())
                    * amount * brightness;
            accumulator.point.set(beam.getTo());
        }

        if (gammaTimer >= 0.5f) {
            gammaTimer -= 0.5f;
            for (Map.Entry<CombatEntityAPI, BeamGammaAccumulator> entry : gammaHits.entrySet()) {
                CombatEntityAPI target = entry.getKey();
                BeamGammaAccumulator accumulator = entry.getValue();
                if (target == null || accumulator.damage <= 0f) continue;
                Meng_OldEmpireEffects.applyGamma(ship, target, accumulator.point, engine,
                        accumulator.damage * 0.03f, 0.90f);
                Meng_OldEmpireEffects.decorateBeamHit(engine, accumulator.point, weapon.getCurrAngle(), 1f);
            }
            gammaHits.clear();
        }
    }

    private List<Float> getAngles(WeaponAPI weapon) {
        if (weapon.getSlot().isTurret()) return weapon.getSpec().getTurretAngleOffsets();
        if (weapon.getSlot().isHardpoint()) return weapon.getSpec().getHardpointAngleOffsets();
        return weapon.getSpec().getHiddenAngleOffsets();
    }

    private static final class BeamGammaAccumulator {
        private float damage;
        private final Vector2f point = new Vector2f();
    }
}
