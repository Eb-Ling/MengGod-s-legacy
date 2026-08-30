package data.weapons;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.input.InputEventAPI;
import data.hullmods.Meng_OldEmpireBlackHoleCoreHullmod;
import java.awt.Color;
import java.util.List;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicFakeBeam;
import org.magiclib.util.MagicLensFlare;

/** Instant rift-lightning strike for Crimson Spike. */
public final class Meng_OldEmpireAxialWeaponEffect implements OnFireEffectPlugin {
    private static final float EMPOWERED_DAMAGE_MULT = 1.4f;

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon,
            CombatEngineAPI engine) {
        if (projectile == null || weapon == null || engine == null
                || !Meng_OldEmpireAxialIds.CRIMSON_SPIKE.equals(weapon.getId())) {
            return;
        }

        ShipAPI source = weapon.getShip();
        boolean empowered = Meng_OldEmpireBlackHoleCoreHullmod
                .tryConsumeEmpoweredShot(source);
        Vector2f start = weapon.getFirePoint(0);
        if (start == null) {
            start = projectile.getLocation();
        }
        if (start == null) {
            engine.removeEntity(projectile);
            return;
        }

        start = new Vector2f(start);
        float facing = weapon.getCurrAngle();
        StrikeHit hit = findFirstShipHit(
                engine, source, start, facing, weapon.getRange());
        float totalDamage = projectile.getDamageAmount()
                * (empowered ? EMPOWERED_DAMAGE_MULT : 1f);
        float totalEmp = projectile.getEmpAmount();

        engine.removeEntity(projectile);
        engine.addPlugin(new CrimsonSpikeStrike(
                engine, source, start, hit.point, hit.target, facing,
                empowered, totalDamage, totalEmp));
        spawnMuzzleBurst(engine, source, start, facing, empowered);
    }

    private static StrikeHit findFirstShipHit(CombatEngineAPI engine,
            ShipAPI source, Vector2f start, float facing, float range) {
        Vector2f end = Meng_OldEmpireAxialFx.point(start, facing, range);
        Vector2f best = new Vector2f(end);
        float bestDistance = range;
        ShipAPI bestTarget = null;
        List ships = engine.getShips();
        for (int i = 0; i < ships.size(); i++) {
            Object entry = ships.get(i);
            if (!(entry instanceof ShipAPI)) {
                continue;
            }
            ShipAPI candidate = (ShipAPI) entry;
            if (!isValidEnemy(candidate, source)) {
                continue;
            }
            Vector2f hit = MagicFakeBeam.getShipCollisionPoint(
                    start, end, candidate, facing);
            if (hit == null) {
                continue;
            }
            float distance = Meng_OldEmpireAxialFx.distance(start, hit);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = new Vector2f(hit);
                bestTarget = candidate;
            }
        }
        return new StrikeHit(best, bestTarget);
    }

    private static boolean isValidEnemy(ShipAPI candidate, ShipAPI source) {
        return candidate != null && candidate.isAlive()
                && !candidate.isHulk() && !candidate.isPhased()
                && candidate != source
                && (source == null
                        || candidate.getOwner() != source.getOwner());
    }

    private static void spawnMuzzleBurst(CombatEngineAPI engine,
            ShipAPI source, Vector2f location, float facing,
            boolean empowered) {
        for (int i = 0; i < 26; i++) {
            float angle = facing + 180f
                    + (float) Math.random() * 76f - 38f;
            Vector2f velocity = Meng_OldEmpireAxialFx.polar(angle,
                    38f + (float) Math.random() * 155f);
            Color color = i % 3 == 0 ? Meng_OldEmpireAxialFx.hot(empowered)
                    : i % 2 == 0 ? Meng_OldEmpireAxialFx.accent(empowered)
                            : Meng_OldEmpireAxialFx.deep(empowered);
            engine.addSmoothParticle(location, velocity,
                    5f + (float) Math.random() * 12f,
                    0.72f, 0.08f,
                    0.22f + (float) Math.random() * 0.16f,
                    Meng_OldEmpireAxialFx.withAlpha(color, 145));
        }
        MagicLensFlare.createSharpFlare(engine, source, location,
                5.5f, 98f, facing,
                Meng_OldEmpireAxialFx.hot(empowered),
                Meng_OldEmpireAxialFx.deep(empowered));
    }

    private static EmpArcEntityAPI.EmpArcParams travellingArcParams(
            float distance, float speed) {
        EmpArcEntityAPI.EmpArcParams params =
                new EmpArcEntityAPI.EmpArcParams();
        params.segmentLengthMult = 7f;
        params.zigZagReductionFactor = 0.17f;
        params.fadeOutDist = 28f;
        params.minFadeOutMult = 8f;
        params.flickerRateMult = 0.28f;
        params.glowSizeMult = 0.92f;
        params.brightSpotFullFraction = 0.11f;
        params.brightSpotFadeFraction = 0.17f;
        params.movementDurOverride = Math.max(0.08f, distance / speed);
        return params;
    }

    private static void spawnRiftVisual(CombatEngineAPI engine,
            ShipAPI source, Vector2f location, boolean empowered,
            float size, float fadeOut) {
        CombatEntityAPI entity = engine.spawnProjectile(
                source, null, Meng_OldEmpireAxialIds.CRIMSON_SPIKE_BURST,
                location, (float) Math.random() * 360f, new Vector2f());
        if (!(entity instanceof DamagingProjectileAPI)) {
            if (entity != null) {
                engine.removeEntity(entity);
            }
            return;
        }

        DamagingProjectileAPI visual = (DamagingProjectileAPI) entity;
        NegativeExplosionVisual.NEParams params =
                RiftCascadeMineExplosion.createStandardRiftParams(
                        Meng_OldEmpireAxialFx.withAlpha(
                                Meng_OldEmpireAxialFx.hot(empowered), 255),
                        size);
        params.fadeOut = fadeOut;
        params.hitGlowSizeMult = 0.50f;
        params.thickness = 32f + size;
        RiftCascadeMineExplosion.spawnStandardRift(visual, params);
        engine.removeEntity(visual);
    }

    private static void spawnRipple(Vector2f location, float size,
            float intensity, float fadeOut) {
        RippleDistortion ripple =
                new RippleDistortion(new Vector2f(location), new Vector2f());
        ripple.setSize(size);
        ripple.setIntensity(intensity);
        ripple.setFrameRate(96f);
        ripple.fadeInSize(0.04f);
        ripple.fadeOutIntensity(fadeOut);
        DistortionShader.addDistortion(ripple);
    }

    private static final class StrikeHit {
        private final Vector2f point;
        private final ShipAPI target;

        private StrikeHit(Vector2f point, ShipAPI target) {
            this.point = new Vector2f(point);
            this.target = target;
        }
    }

    private static final class CrimsonSpikeStrike
            extends BaseEveryFrameCombatPlugin {
        private static final int ARC_COUNT = 6;
        private static final int HOP_COUNT = 3;

        private final CombatEngineAPI engine;
        private final ShipAPI source;
        private final Vector2f start;
        private final Vector2f impact;
        private final ShipAPI target;
        private final float facing;
        private final boolean empowered;
        private final float damagePerHop;
        private final float empPerHop;
        private float elapsed;
        private int arcsSpawned;
        private int hopsSpawned;

        private CrimsonSpikeStrike(CombatEngineAPI engine, ShipAPI source,
                Vector2f start, Vector2f impact, ShipAPI target,
                float facing, boolean empowered,
                float totalDamage, float totalEmp) {
            this.engine = engine;
            this.source = source;
            this.start = new Vector2f(start);
            this.impact = new Vector2f(impact);
            this.target = target;
            this.facing = facing;
            this.empowered = empowered;
            this.damagePerHop = totalDamage / HOP_COUNT;
            this.empPerHop = totalEmp / HOP_COUNT;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (engine == null || engine.isPaused()) {
                return;
            }
            elapsed += amount;
            while (arcsSpawned < ARC_COUNT
                    && elapsed >= arcsSpawned * 0.030f) {
                spawnInitialArc(arcsSpawned);
                arcsSpawned++;
            }
            while (arcsSpawned >= ARC_COUNT && hopsSpawned < HOP_COUNT
                    && elapsed >= 0.18f + hopsSpawned * 0.13f) {
                spawnDamageHop(hopsSpawned);
                hopsSpawned++;
            }
            if (hopsSpawned >= HOP_COUNT && elapsed >= 0.50f) {
                engine.removePlugin(this);
            }
        }

        private void spawnInitialArc(int index) {
            Vector2f arcStart;
            if (index == 0) {
                arcStart = new Vector2f(start);
            } else {
                Vector2f backward = Meng_OldEmpireAxialFx.point(
                        start, facing + 180f,
                        6f + (float) Math.random() * 24f);
                arcStart = Meng_OldEmpireAxialFx.point(
                        backward, facing + 90f,
                        (float) Math.random() * 32f - 16f);
            }
            Vector2f arcEnd = Meng_OldEmpireAxialFx.point(
                    impact, (float) Math.random() * 360f,
                    (float) Math.random() * 3f);

            EmpArcEntityAPI.EmpArcParams params =
                    new EmpArcEntityAPI.EmpArcParams();
            params.segmentLengthMult = 7f;
            params.zigZagReductionFactor = 0.18f;
            params.fadeOutDist = 42f;
            params.minFadeOutMult = 8f;
            params.flickerRateMult = 0.34f;
            params.glowSizeMult = 1.15f;
            params.movementDurOverride = Math.max(
                    0.05f,
                    Meng_OldEmpireAxialFx.distance(arcStart, arcEnd) / 10000f);

            EmpArcEntityAPI arc = engine.spawnEmpArcVisual(
                    arcStart, null, arcEnd, null,
                    20f + (float) Math.random() * 7f,
                    Meng_OldEmpireAxialFx.withAlpha(
                            Meng_OldEmpireAxialFx.accent(empowered), 235),
                    Meng_OldEmpireAxialFx.pale(empowered), params);
            arc.setCoreWidthOverride(8f + (float) Math.random() * 4f);
            arc.setRenderGlowAtStart(false);
            arc.setFadedOutAtStart(true);
            arc.setSingleFlickerMode(true);
        }

        private void spawnDamageHop(int index) {
            Vector2f previous = new Vector2f(impact);
            Vector2f point = index == 0
                    ? new Vector2f(impact)
                    : pickNextSurfacePoint(previous, index);
            if (index > 0) {
                EmpArcEntityAPI.EmpArcParams params =
                        travellingArcParams(
                                Meng_OldEmpireAxialFx.distance(previous, point),
                                720f);
                EmpArcEntityAPI arc = engine.spawnEmpArcVisual(
                        previous, target, point, target,
                        13f + (float) Math.random() * 5f,
                        Meng_OldEmpireAxialFx.withAlpha(
                                Meng_OldEmpireAxialFx.accent(empowered), 230),
                        Meng_OldEmpireAxialFx.pale(empowered), params);
                arc.setCoreWidthOverride(
                        5f + (float) Math.random() * 2f);
                arc.setRenderGlowAtStart(false);
                arc.setFadedOutAtStart(true);
                arc.setSingleFlickerMode(true);
            }

            impact.set(point);
            spawnRiftVisual(engine, source, point, empowered,
                    19f + index * 2.5f, 0.82f);
            spawnRipple(point, 92f + index * 13f,
                    1.25f, 0.30f);
            if (target != null && isValidEnemy(target, source)
                    && engine.isEntityInPlay(target)) {
                engine.applyDamage(target, point,
                        damagePerHop, DamageType.ENERGY, empPerHop,
                        false, false, source);
            }
        }

        private Vector2f pickNextSurfacePoint(
                Vector2f previous, int index) {
            if (target == null || !target.isAlive()
                    || !engine.isEntityInPlay(target)) {
                return Meng_OldEmpireAxialFx.point(
                        previous, (float) Math.random() * 360f,
                        28f + (float) Math.random() * 36f);
            }

            float radius = target.getCollisionRadius()
                    * (0.72f + (float) Math.random() * 0.22f);
            if (target.getShield() != null
                    && target.getShield().isOn()) {
                radius = target.getShield().getRadius()
                        * (0.88f + (float) Math.random() * 0.08f);
            }
            float base = Meng_OldEmpireAxialFx.angle(
                    target.getLocation(), previous);
            float direction = index % 2 == 0 ? 1f : -1f;
            float angle = base + direction
                    * (68f + (float) Math.random() * 76f);
            return Meng_OldEmpireAxialFx.point(
                    target.getLocation(), angle, radius);
        }
    }
}
