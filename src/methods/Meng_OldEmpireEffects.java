package data.methods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;

public final class Meng_OldEmpireEffects {
    private Meng_OldEmpireEffects() {}

    private static final Color ORANGE = new Color(255, 105, 35, 220);
    private static final Color ORANGE_CORE = new Color(255, 235, 170, 255);
    private static final Color ORANGE_DEEP = new Color(118, 38, 10, 210);
    private static final Color CYAN = new Color(65, 210, 255, 235);
    private static final Color CYAN_CORE = new Color(225, 255, 255, 255);
    private static final Color CRIMSON = new Color(155, 0, 28, 235);
    private static final Color CRIMSON_CORE = new Color(255, 45, 70, 255);
    private static final Color DEEP_RED = new Color(82, 3, 15);
    private static final Color HOT_RED = new Color(255, 92, 104);
    private static final Color DUST_BLUE = new Color(74, 220, 255);
    private static final Color DUST_SILVER = new Color(218, 247, 255);
    private static final Color DUST_DEEP = new Color(38, 92, 154);
    private static final String VOLATILE_TRIGGER_HISTORY = "Meng_OldEmpire_volatile_trigger_history";
    private static final String CHARGED_TRIGGER_HISTORY = "Meng_OldEmpire_charged_trigger_history";
    private static final String CRIMSON_TRIGGER_HISTORY = "Meng_OldEmpire_crimson_trigger_history";

    public static void applyGamma(ShipAPI source, CombatEntityAPI target, Vector2f point,
                                  CombatEngineAPI engine, float proportionalDamage,
                                  float triggerWeight) {
        if (source == null || target == null || point == null || engine == null) return;
        proportionalDamage = Math.max(0f, proportionalDamage);
        triggerWeight = Math.max(0f, triggerWeight);

        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.G_VOLATILE)
                && Math.random() < weightedChance(0.50f, triggerWeight)
                && allowTrigger(source, engine, VOLATILE_TRIGGER_HISTORY, 6)) {
            spawnVolatileBurst(engine, source, point, proportionalDamage);
        }

        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.G_CHARGED)
                && Math.random() < weightedChance(0.25f, triggerWeight)
                && allowTrigger(source, engine, CHARGED_TRIGGER_HISTORY, 3)) {
            spawnChargedArc(engine, source, target, point, proportionalDamage);
        }

        if (Meng_OldEmpireIds.has(source, Meng_OldEmpireIds.G_CRIMSON)
                && Math.random() < weightedChance(0.08f, triggerWeight)
                && allowTrigger(source, engine, CRIMSON_TRIGGER_HISTORY, 1)) {
            float fixedMax = 650f / 3f;
            float fixedMin = 100f;
            spawnExplosion(engine, source, point, 210f, 100f,
                    fixedMax + proportionalDamage,
                    fixedMin + proportionalDamage * (fixedMin / fixedMax),
                    DamageType.ENERGY, CRIMSON, CRIMSON_CORE);
            for (int i = 0; i < 8; i++) {
                Vector2f p = Meng_OldEmpireMath.offset(point, i * 45f + Meng_OldEmpireMath.random(-10f, 10f),
                        Meng_OldEmpireMath.random(45f, 135f));
                engine.spawnEmpArcVisual(point, target, p, null, 10f, CRIMSON, CRIMSON_CORE);
            }
        }
    }

    public static float projectileGammaDamage(DamagingProjectileAPI projectile) {
        if (projectile == null) return 0f;
        String id = projectile.getProjectileSpecId();
        if (id == null) return 0f;

        float ratio;
        if (id.startsWith("Meng_OldEmpire_dust")) {
            ratio = 1f;
        } else if ("Meng_OldEmpire_chain_pellet_shot".equals(id)) {
            ratio = 0.10f;
        } else if (id.startsWith("Meng_OldEmpire_chain")) {
            ratio = 0.23f;
        } else if (id.startsWith("Meng_OldEmpire_tracer")) {
            ratio = 0.60f;
        } else {
            ratio = 0f;
        }
        return Math.max(0f, projectile.getDamageAmount() * ratio);
    }

    public static float projectileGammaTriggerWeight(DamagingProjectileAPI projectile) {
        if (projectile == null) return 0f;
        String id = projectile.getProjectileSpecId();
        if (id == null) return 0f;

        if (id.startsWith("Meng_OldEmpire_dust")) return 2f;
        if ("Meng_OldEmpire_chain_pellet_shot".equals(id)) return 0.50f;
        if (id.startsWith("Meng_OldEmpire_chain")) return 0.80f;
        if (id.startsWith("Meng_OldEmpire_tracer")) return 1.20f;
        return 0f;
    }

    private static float weightedChance(float baseChance, float triggerWeight) {
        return Math.min(1f, Math.max(0f, baseChance * triggerWeight));
    }

    @SuppressWarnings("unchecked")
    private static boolean allowTrigger(ShipAPI source, CombatEngineAPI engine,
                                        String key, int maxPerSecond) {
        Object stored = source.getCustomData().get(key);
        Deque<Float> history;
        if (stored instanceof Deque<?>) {
            history = (Deque<Float>) stored;
        } else {
            history = new ArrayDeque<Float>();
            source.setCustomData(key, history);
        }

        float now = engine.getTotalElapsedTime(false);
        while (!history.isEmpty() && now - history.peekFirst() >= 1f) {
            history.removeFirst();
        }
        if (history.size() >= maxPerSecond) return false;
        history.addLast(now);
        return true;
    }

    private static void spawnVolatileBurst(CombatEngineAPI engine, ShipAPI source,
                                           Vector2f point, float proportionalDamage) {
        spawnExplosion(engine, source, point, 62f, 26f,
                45f + proportionalDamage,
                16f + proportionalDamage * (16f / 45f),
                DamageType.HIGH_EXPLOSIVE,
                new Color(190, 70, 18, 112),
                new Color(255, 160, 70, 155));
        engine.spawnExplosion(point, new Vector2f(), new Color(255, 118, 42, 105),
                48f, 0.16f);
        for (int i = 0; i < 9; i++) {
            float angle = i * 40f + Meng_OldEmpireMath.random(-13f, 13f);
            Vector2f velocity = Meng_OldEmpireMath.fromAngle(angle, Meng_OldEmpireMath.random(28f, 88f));
            engine.addHitParticle(point, velocity, Meng_OldEmpireMath.random(3f, 7f),
                    0.42f, 0.02f, 0.14f + Meng_OldEmpireMath.random(0f, 0.08f),
                    new Color(255, 120, 38, 132));
        }
    }

    private static void spawnChargedArc(CombatEngineAPI engine, ShipAPI source,
                                        CombatEntityAPI target, Vector2f point,
                                        float proportionalDamage) {
        Vector2f end = randomPointNearTarget(target);
        engine.spawnEmpArcVisual(point, target, end, target, Meng_OldEmpireMath.random(10f, 17f),
                CYAN, CYAN_CORE);
        engine.applyDamage(target, end, 115f / 3f + proportionalDamage,
                DamageType.ENERGY, 245f / 3f,
                true, false, source, false);

        for (int i = 0; i < 2; i++) {
            Vector2f spark = randomPointNearTarget(target);
            engine.spawnEmpArcVisual(end, target, spark, target, Meng_OldEmpireMath.random(5f, 8f),
                    new Color(55, 205, 255, 160),
                    new Color(220, 255, 255, 215));
        }
    }

    private static Vector2f randomPointNearTarget(CombatEntityAPI target) {
        Vector2f center = target.getLocation();
        float radius = Math.max(16f, target.getCollisionRadius() * Meng_OldEmpireMath.random(0.20f, 0.86f));
        return Meng_OldEmpireMath.offset(center, Meng_OldEmpireMath.random(0f, 360f), radius);
    }

    public static void decorateSpawn(DamagingProjectileAPI projectile, ShipAPI source,
                                     CombatEngineAPI engine, String weaponId, float angle) {
        if (projectile == null || engine == null) return;
        Vector2f point = new Vector2f(projectile.getLocation());
        if (weaponId == null) weaponId = "";

        if (weaponId.startsWith("Meng_OldEmpire_spawn_chain")) {
            int heavy = weaponId.contains("heavy") ? 1 : weaponId.contains("pellet") ? -1 : 0;
            chainMuzzle(engine, source, point, angle, heavy);
        } else if (weaponId.startsWith("Meng_OldEmpire_spawn_dust")) {
            int style = weaponId.contains("heavy") ? 2 : weaponId.contains("cyber") ? 1 : 0;
            dustMuzzle(engine, source, point, angle, style);
            if (projectile instanceof MissileAPI) {
                MissileAPI missile = (MissileAPI) projectile;
                if (style == 0) {
                    missile.setMissileAI(new DustTrailAI(missile, engine, source, style));
                    engine.addLayeredRenderingPlugin(new DustFlightVisual(missile, engine, style));
                } else {
                    // Keep the native body visible even if the optional overlay is viewport-culled.
                    missile.setSpriteAlphaOverride(1f);
                    engine.addLayeredRenderingPlugin(new DustSpinVisual(missile, engine, style));
                }
            }
        } else if (weaponId.startsWith("Meng_OldEmpire_spawn_tracer")) {
            int style = weaponId.contains("heavy") ? 4 : weaponId.contains("small") ? 3 : 2;
            tracerMuzzle(engine, source, point, angle, style);
            if (projectile instanceof MissileAPI) {
                MissileAPI missile = (MissileAPI) projectile;
                missile.setMissileAI(new TracerTrailAI(missile, engine, source, style));
                engine.addLayeredRenderingPlugin(new TracerFlightVisual(missile, engine, style));
            }
        }
    }

    public static void prepareDemApproach(MissileAPI missile, CombatEngineAPI engine,
                                          ShipAPI source, boolean heavy) {
        if (missile == null || engine == null) return;
        missile.setMissileAI(new DustDemApproachAI(missile, engine, source, heavy));
    }

    public static void decorateBeamFrame(CombatEngineAPI engine, ShipAPI source,
                                         Vector2f from, Vector2f to, float brightness,
                                         float phase, int index) {
        if (engine == null || from == null || to == null || brightness <= 0.05f) return;
        if (((int) (phase * 36f + index)) % 5 != 0) return;
        engine.addLayeredRenderingPlugin(new BeamRhythmVisual(
                new Vector2f(from), new Vector2f(to), brightness, phase + index * 0.37f));
    }

    public static void decorateHeavyBeamLightning(CombatEngineAPI engine, ShipAPI source,
                                                  Vector2f from, Vector2f to, float brightness,
                                                  float phase, int index) {
        if (engine == null || from == null || to == null || brightness <= 0.05f) return;
        float facing = Meng_OldEmpireMath.angle(from, to);
        float length = Math.max(1f, Vector2f.sub(to, from, null).length());
        float center = Meng_OldEmpireMath.random(length * 0.18f, length * 0.92f);
        float span = Meng_OldEmpireMath.random(42f, 86f);
        float sideA = Meng_OldEmpireMath.random(-9f, 9f);
        float sideB = Meng_OldEmpireMath.random(-14f, 14f);
        Vector2f start = Meng_OldEmpireMath.offset(Meng_OldEmpireMath.offset(from, facing, center - span * 0.5f),
                facing + 90f, sideA);
        Vector2f end = Meng_OldEmpireMath.offset(Meng_OldEmpireMath.offset(from, facing, center + span * 0.5f),
                facing + 90f, sideB);
        engine.spawnEmpArcVisual(start, null, end, null,
                7f + brightness * 8f,
                new Color(118, 0, 22, 210),
                new Color(255, 38, 60, 245));
    }

    public static void decorateBeamHit(CombatEngineAPI engine, Vector2f point,
                                       float angle, float brightness) {
        if (engine == null || point == null || brightness <= 0.05f) return;
        engine.addLayeredRenderingPlugin(new HitRingVisual(new Vector2f(point), angle,
                0.32f + brightness * 0.36f, CRIMSON, HOT_RED));
        ripple(point, 62f + brightness * 54f, 1.1f + brightness, 0.28f);
    }

    public static void decorateSweepingMembrane(CombatEngineAPI engine, BeamAPI center,
                                                BeamAPI left, BeamAPI right, float brightness) {
        if (engine == null || center == null || left == null || right == null || brightness <= 0.05f) return;
        engine.addLayeredRenderingPlugin(new BeamMembraneVisual(
                new Vector2f(center.getFrom()), new Vector2f(center.getTo()),
                new Vector2f(left.getTo()), new Vector2f(right.getTo()), brightness));
    }

    public static void decorateProjectileImpact(DamagingProjectileAPI projectile,
                                                Vector2f point, CombatEngineAPI engine) {
        if (projectile == null || point == null || engine == null) return;
        String id = projectile.getProjectileSpecId();
        if (id == null) id = "";

        if (id.startsWith("Meng_OldEmpire_dust")) {
            engine.addLayeredRenderingPlugin(new DustImpactVisual(new Vector2f(point),
                    projectile.getFacing(), id.contains("heavy")));
            MagicLensFlare.createSmoothFlare(engine, projectile.getSource(), point, 1.35f, 28f,
                    projectile.getFacing(), id.contains("heavy") ? CRIMSON_CORE : DUST_SILVER,
                    id.contains("heavy") ? DEEP_RED : DUST_DEEP);
        } else if (id.startsWith("Meng_OldEmpire_tracer")) {
            engine.addLayeredRenderingPlugin(new TracerImpactVisual(new Vector2f(point),
                    projectile.getFacing()));
            MagicLensFlare.createSharpFlare(engine, projectile.getSource(), point, 1.8f, 36f,
                    projectile.getFacing(), CRIMSON_CORE, DEEP_RED);
        } else if (id.startsWith("Meng_OldEmpire_chain")) {
            engine.addHitParticle(point, new Vector2f(), 7f, 0.42f, 0.03f,
                    new Color(208, 70, 28, 105));
        }
    }

    public static void dragonDustStrike(CombatEngineAPI engine, ShipAPI source,
                                        Vector2f from, Vector2f to) {
        if (engine == null || from == null || to == null) return;
        engine.addLayeredRenderingPlugin(new DragonDustStrikeVisual(
                new Vector2f(from), new Vector2f(to)));
        MagicLensFlare.createSmoothFlare(engine, source, from, 1.25f, 24f,
                Meng_OldEmpireMath.angle(from, to), HOT_RED, DEEP_RED);
    }

    public static void cyberDustBeam(CombatEngineAPI engine, ShipAPI source,
                                     Vector2f from, Vector2f to, float strength) {
        if (engine == null || from == null || to == null) return;
        float clamped = clamp(strength, 0.08f, 1f);
        engine.addLayeredRenderingPlugin(new CyberDustBeamVisual(
                new Vector2f(from), new Vector2f(to), clamped));
        engine.addHitParticle(to, new Vector2f(), 11f, 0.48f * clamped,
                0.05f, new Color(255, 45, 70, 185));
    }

    public static void heavyTracerImpact(DamagingProjectileAPI projectile, CombatEntityAPI target,
                                         Vector2f point, CombatEngineAPI engine) {
        if (projectile == null || point == null || engine == null) return;
        ShipAPI source = projectile.getSource();
        float facing = projectile.getFacing();
        Vector2f start = Meng_OldEmpireMath.offset(point, facing + 180f, 65f);
        Vector2f end = Meng_OldEmpireMath.offset(point, facing, 650f);
        engine.spawnEmpArcVisual(start, null, end, null, 22f,
                new Color(255, 55, 70, 235), new Color(255, 235, 235, 255));

        Vector2f perpendicular = Meng_OldEmpireMath.fromAngle(facing + 90f, 1f);
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        for (int i = 0; i < 8; i++) {
            float t = (i + 0.5f) / 8f;
            float lateral = ((i & 1) == 0 ? -1f : 1f) * Meng_OldEmpireMath.random(8f, 22f);
            Vector2f p = new Vector2f(start.x + dx * t + perpendicular.x * lateral,
                    start.y + dy * t + perpendicular.y * lateral);
            spawnExplosion(engine, source, p, 72f, 38f, 47.5f, 22.5f,
                    DamageType.ENERGY,
                    new Color(0, 0, 0, 0), new Color(0, 0, 0, 0));
            spawnScaledRiftMine(engine, source, p, 0.25f);
        }
    }

    private static void spawnScaledRiftMine(CombatEngineAPI engine, ShipAPI source,
                                            Vector2f point, float damageMult) {
        CombatEntityAPI entity = engine.spawnProjectile(source, null,
                "rift_lightning_minelayer", point, Meng_OldEmpireMath.random(0f, 360f), null);
        if (!(entity instanceof MissileAPI)) return;
        MissileAPI mine = (MissileAPI) entity;
        if (source != null) {
            engine.applyDamageModifiersToSpawnedProjectileWithNullWeapon(
                    source, WeaponAPI.WeaponType.ENERGY, false, mine.getDamage());
            mine.addDamagedAlready(source);
        }
        mine.getVelocity().scale(0f);
        mine.fadeOutThenIn(0.05f);
        mine.setNoMineFFConcerns(true);
        mine.setDamageAmount(mine.getDamageAmount() * damageMult);
        mine.explode();
    }

    public static void spawnExplosion(CombatEngineAPI engine, ShipAPI source, Vector2f point,
                                      float radius, float coreRadius, float maxDamage, float minDamage,
                                      DamageType type, Color fringe, Color core) {
        DamagingExplosionSpec spec = new DamagingExplosionSpec(
                0.45f, radius, coreRadius, maxDamage, minDamage,
                CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FIGHTER,
                2f, 5f, 0.55f, 30,
                fringe, core
        );
        spec.setDamageType(type);
        spec.setUseDetailedExplosion(false);
        engine.spawnDamagingExplosion(spec, source, point);
    }

    private static void chainMuzzle(CombatEngineAPI engine, ShipAPI source, Vector2f point,
                                    float facing, int heavy) {
        float scale = heavy > 0 ? 1.45f : heavy < 0 ? 0.72f : 1f;
        for (int i = 0; i < 2 + (heavy > 0 ? 2 : 0); i++) {
            float a = facing + 180f + Meng_OldEmpireMath.random(-12f, 12f);
            Vector2f v = Meng_OldEmpireMath.fromAngle(a, Meng_OldEmpireMath.random(14f, 42f) * scale);
            engine.addHitParticle(point, v, Meng_OldEmpireMath.random(2.5f, 5.5f) * scale,
                    0.38f, 0.02f, 0.11f + Meng_OldEmpireMath.random(0f, 0.04f),
                    new Color(190, 46, 16, 92));
        }
    }

    private static void dustMuzzle(CombatEngineAPI engine, ShipAPI source, Vector2f point,
                                   float facing, int style) {
        float scale = style == 2 ? 1.4f : style == 1 ? 1.15f : 1f;
        for (int i = 0; i < 5 + style; i++) {
            float a = facing + 180f + Meng_OldEmpireMath.random(-18f, 18f);
            Vector2f v = Meng_OldEmpireMath.fromAngle(a, Meng_OldEmpireMath.random(12f, 42f) * scale);
            Color c = i == 0 ? DUST_SILVER : DUST_BLUE;
            engine.addSmoothParticle(point, v, Meng_OldEmpireMath.random(4f, 9f) * scale,
                    0.34f, 0.03f, 0.16f + Meng_OldEmpireMath.random(0f, 0.05f),
                    new Color(c.getRed(), c.getGreen(), c.getBlue(), 112));
        }
        MagicLensFlare.createSmoothFlare(engine, source, point, 1.7f * scale,
                34f * scale, facing, DUST_SILVER, DUST_DEEP);
    }

    private static void tracerMuzzle(CombatEngineAPI engine, ShipAPI source, Vector2f point,
                                     float facing, int style) {
        float scale = style == 4 ? 1.45f : style == 3 ? 0.72f : 1f;
        MagicLensFlare.createSharpFlare(engine, source, point, 2.25f * scale,
                44f * scale, facing, CRIMSON_CORE, DEEP_RED);
        for (int i = 0; i < 3; i++) {
            float a = facing + 180f + Meng_OldEmpireMath.random(-10f, 10f);
            Vector2f v = Meng_OldEmpireMath.fromAngle(a, Meng_OldEmpireMath.random(14f, 42f) * scale);
            engine.addSmoothParticle(point, v, Meng_OldEmpireMath.random(3f, 7f) * scale,
                    0.45f, 0.02f, 0.12f, new Color(240, 72, 92, 92));
        }
    }

    private static void ripple(Vector2f point, float size, float intensity, float fade) {
        try {
            RippleDistortion ripple = new RippleDistortion(new Vector2f(point), new Vector2f());
            ripple.setSize(size);
            ripple.setIntensity(intensity);
            ripple.setFrameRate(90f);
            ripple.fadeInSize(0.04f);
            ripple.fadeOutIntensity(fade);
            DistortionShader.addDistortion(ripple);
        } catch (Throwable ignored) {
        }
    }

    private static void steerMissile(MissileAPI missile, CombatEngineAPI engine,
                                     ShipAPI source, ShipAPI target, float amount,
                                     float freeTime, float elapsed, float speed,
                                     float turnRate, float wiggle) {
        if (missile == null || missile.isExpired() || missile.didDamage()) return;
        float desired = missile.getFacing();
        if (target != null && elapsed > freeTime) {
            desired = Meng_OldEmpireMath.angle(missile.getLocation(), target.getLocation());
        } else {
            desired += (float) Math.sin(elapsed * 5.6f + missile.hashCode() * 0.01f) * wiggle;
        }
        float diff = shortest(missile.getFacing(), desired);
        float turn = clamp(diff, -turnRate * amount, turnRate * amount);
        missile.setFacing(missile.getFacing() + turn);
        Vector2f wanted = Meng_OldEmpireMath.fromAngle(missile.getFacing(), speed);
        Vector2f velocity = missile.getVelocity();
        float blend = clamp(amount * 5.5f, 0f, 1f);
        velocity.x += (wanted.x - velocity.x) * blend;
        velocity.y += (wanted.y - velocity.y) * blend;
    }

    private static ShipAPI nearestEnemy(CombatEngineAPI engine, MissileAPI missile,
                                        ShipAPI source, float range) {
        if (engine == null || missile == null) return null;
        ShipAPI result = null;
        float best = range * range;
        for (ShipAPI ship : engine.getShips()) {
            if (ship == null || !ship.isAlive() || ship.isHulk() || ship.isPhased()
                    || ship.isFighter() || ship.isDrone()) continue;
            if (source != null && ship.getOwner() == source.getOwner()) continue;
            float dist = Meng_OldEmpireMath.distanceSquared(missile.getLocation(), ship.getLocation());
            if (dist < best) {
                best = dist;
                result = ship;
            }
        }
        return result;
    }

    private static float shortest(float from, float to) {
        float diff = ((to - from + 540f) % 360f) - 180f;
        return diff;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void color(Color color, float alpha) {
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f, clamp(alpha, 0f, 1f));
    }

    private static void vertex(Vector2f point) {
        GL11.glVertex2f(point.x, point.y);
    }

    private static Vector2f ellipsePoint(Vector2f center, float facing,
                                         float forward, float side) {
        Vector2f result = Meng_OldEmpireMath.fromAngle(facing, forward);
        Vector2f lateral = Meng_OldEmpireMath.fromAngle(facing + 90f, side);
        result.x += lateral.x + center.x;
        result.y += lateral.y + center.y;
        return result;
    }

    private static void drawEllipseRing(Vector2f center, float facing,
                                         float forwardRadius, float sideRadius,
                                         float width, Color color, float alpha,
                                         int segments) {
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= segments; i++) {
            float radians = (float) (Math.PI * 2d * i / segments);
            float cosine = (float) Math.cos(radians);
            float sine = (float) Math.sin(radians);
            color(color, 0f);
            vertex(ellipsePoint(center, facing, cosine * forwardRadius * (1f + width),
                    sine * sideRadius * (1f + width)));
            color(color, alpha);
            vertex(ellipsePoint(center, facing, cosine * forwardRadius,
                    sine * sideRadius));
            color(color, 0f);
            vertex(ellipsePoint(center, facing, cosine * forwardRadius * (1f - width),
                    sine * sideRadius * (1f - width)));
        }
        GL11.glEnd();
    }

    private static void drawTrailLine(List<TrailSample> samples, float now, float duration,
                                      Color color, float alpha, float width) {
        if (samples.isEmpty()) return;
        GL11.glLineWidth(width);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = samples.size() - 1; i >= 0; i--) {
            TrailSample sample = samples.get(i);
            float age = clamp((now - sample.time) / duration, 0f, 1f);
            float fade = 1f - age;
            color(color, alpha * fade * fade);
            vertex(sample.point);
        }
        GL11.glEnd();
    }

    private static final class DustDemApproachAI implements MissileAIPlugin, GuidedMissileAI {
        private final MissileAPI missile;
        private final CombatEngineAPI engine;
        private final ShipAPI source;
        private final boolean heavy;
        private ShipAPI target;
        private float elapsed;
        private float retarget;

        private DustDemApproachAI(MissileAPI missile, CombatEngineAPI engine,
                                  ShipAPI source, boolean heavy) {
            this.missile = missile;
            this.engine = engine;
            this.source = source;
            this.heavy = heavy;
            this.target = chooseTarget();
        }

        public void advance(float amount) {
            if (missile == null || missile.isExpired() || missile.didDamage()) return;
            elapsed += amount;
            retarget -= amount;
            if (retarget <= 0f || !isValidTarget(target)) {
                retarget = 0.12f;
                target = chooseTarget();
            }
            steerMissile(missile, engine, source, target, amount, 0f, elapsed,
                    heavy ? 448f : 150f, heavy ? 170f : 200f, 0f);
        }

        private ShipAPI chooseTarget() {
            ShipAPI selected = source == null ? null : source.getShipTarget();
            if (isValidTarget(selected)) return selected;
            return nearestEnemy(engine, missile, source, 1900f);
        }

        private boolean isValidTarget(ShipAPI candidate) {
            if (candidate == null || !candidate.isAlive() || candidate.isHulk()
                    || candidate.isPhased() || candidate.isFighter() || candidate.isDrone()) {
                return false;
            }
            if (source != null && candidate.getOwner() == source.getOwner()) return false;
            return Meng_OldEmpireMath.distanceSquared(missile.getLocation(), candidate.getLocation())
                    <= 1900f * 1900f;
        }

        public CombatEntityAPI getTarget() {
            return target;
        }

        public void setTarget(CombatEntityAPI target) {
            this.target = target instanceof ShipAPI ? (ShipAPI) target : null;
        }
    }

    private static final class DustTrailAI implements MissileAIPlugin, GuidedMissileAI {
        private final MissileAPI missile;
        private final CombatEngineAPI engine;
        private final ShipAPI source;
        private final int style;
        private ShipAPI target;
        private float elapsed;
        private float retarget;

        private DustTrailAI(MissileAPI missile, CombatEngineAPI engine, ShipAPI source, int style) {
            this.missile = missile;
            this.engine = engine;
            this.source = source;
            this.style = style;
        }

        public void advance(float amount) {
            elapsed += amount;
            retarget -= amount;
            if (retarget <= 0f || target == null || !target.isAlive()) {
                retarget = 0.18f;
                target = nearestEnemy(engine, missile, source,
                        style == 0 ? 1900f : style == 2 ? 980f : 820f);
            }
            steerMissile(missile, engine, source, target, amount,
                    style == 2 ? 0.18f : 0.10f, elapsed,
                    style == 2 ? 448f : style == 1 ? 150f : 150f,
                    style == 2 ? 170f : 230f, style == 2 ? 7f : 11f);
        }

        public CombatEntityAPI getTarget() {
            return target;
        }

        public void setTarget(CombatEntityAPI target) {
            this.target = target instanceof ShipAPI ? (ShipAPI) target : null;
        }
    }

    private static final class TracerTrailAI implements MissileAIPlugin, GuidedMissileAI {
        private final MissileAPI missile;
        private final CombatEngineAPI engine;
        private final ShipAPI source;
        private final int style;
        private ShipAPI target;
        private float elapsed;
        private float retarget;

        private TracerTrailAI(MissileAPI missile, CombatEngineAPI engine, ShipAPI source, int style) {
            this.missile = missile;
            this.engine = engine;
            this.source = source;
            this.style = style;
        }

        public void advance(float amount) {
            elapsed += amount;
            retarget -= amount;
            if (retarget <= 0f || target == null || !target.isAlive()) {
                retarget = 0.12f;
                target = nearestEnemy(engine, missile, source, 1200f);
            }
            steerMissile(missile, engine, source, target, amount, 0.06f, elapsed,
                    style == 4 ? 920f : style == 3 ? 880f : 860f,
                    style == 4 ? 235f : 310f, style == 3 ? 4f : 6f);
        }

        public CombatEntityAPI getTarget() {
            return target;
        }

        public void setTarget(CombatEntityAPI target) {
            this.target = target instanceof ShipAPI ? (ShipAPI) target : null;
        }
    }

    private abstract static class GuidedFlightVisual extends BaseCombatLayeredRenderingPlugin {
        protected final MissileAPI missile;
        protected final CombatEngineAPI engine;
        protected final List<TrailSample> samples = new ArrayList<TrailSample>();
        protected float elapsed;
        private float sampleTimer;
        private boolean retired;

        private GuidedFlightVisual(MissileAPI missile, CombatEngineAPI engine) {
            super(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            this.missile = missile;
            this.engine = engine;
        }

        protected abstract float sampleInterval();

        protected abstract float trailDuration();

        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        public float getRenderRadius() {
            return 1500f;
        }

        public boolean isExpired() {
            return retired && samples.isEmpty();
        }

        public void advance(float amount) {
            elapsed += amount;
            boolean active = missile != null && engine != null && engine.isEntityInPlay(missile)
                    && !missile.isExpired() && !missile.didDamage();
            if (active) {
                sampleTimer -= amount;
                while (sampleTimer <= 0f) {
                    samples.add(0, new TrailSample(samplePoint(), missile.getFacing(), elapsed));
                    sampleTimer += sampleInterval();
                }
            } else {
                retired = true;
            }
            for (int i = samples.size() - 1; i >= 0; i--) {
                if (elapsed - samples.get(i).time > trailDuration()) samples.remove(i);
            }
        }

        protected float opacity(TrailSample sample) {
            float age = clamp((elapsed - sample.time) / trailDuration(), 0f, 1f);
            return 1f - age;
        }

        protected Vector2f samplePoint() {
            return new Vector2f(missile.getLocation());
        }
    }

    private static final class TrailSample {
        private final Vector2f point;
        private final float facing;
        private final float time;

        private TrailSample(Vector2f point, float facing, float time) {
            this.point = point;
            this.facing = facing;
            this.time = time;
        }
    }

    private static final class DustFlightVisual extends GuidedFlightVisual {
        private final int style;
        private final float phase;

        private DustFlightVisual(MissileAPI missile, CombatEngineAPI engine, int style) {
            super(missile, engine);
            this.style = style;
            this.phase = (missile.hashCode() & 1023) * 0.013f;
        }

        protected float sampleInterval() {
            return 0.042f;
        }

        protected float trailDuration() {
            return style == 2 ? 0.48f : 0.40f;
        }

        public void render(CombatEngineLayers layer, com.fs.starfarer.api.combat.ViewportAPI viewport) {
            if (samples.isEmpty()) return;
            boolean heavy = style == 2;
            Color fringe = heavy ? DEEP_RED : DUST_DEEP;
            Color core = heavy ? HOT_RED : DUST_SILVER;

            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_POINT_SMOOTH);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            GL11.glPointSize(heavy ? 10f : 8f);
            GL11.glBegin(GL11.GL_POINTS);
            for (int i = samples.size() - 1; i >= 0; i--) {
                TrailSample sample = samples.get(i);
                float alpha = opacity(sample);
                float side = (float) Math.sin((sample.time + phase) * 18f + i * 1.7f)
                        * (heavy ? 10f : 7f) * (0.22f + alpha * 0.78f);
                color(fringe, 0.15f * alpha * alpha);
                vertex(Meng_OldEmpireMath.offset(sample.point, sample.facing + 90f, side));
            }
            GL11.glEnd();

            GL11.glPointSize(heavy ? 3.7f : 3.2f);
            GL11.glBegin(GL11.GL_POINTS);
            for (int i = samples.size() - 1; i >= 0; i--) {
                TrailSample sample = samples.get(i);
                float alpha = opacity(sample);
                float side = (float) Math.sin((sample.time + phase) * 18f + i * 1.7f)
                        * (heavy ? 10f : 7f) * (0.22f + alpha * 0.78f);
                color(core, 0.52f * alpha * alpha);
                vertex(Meng_OldEmpireMath.offset(sample.point, sample.facing + 90f, side));
            }
            GL11.glEnd();
            GL11.glPopAttrib();
        }
    }

    private static final class DustSpinVisual extends BaseCombatLayeredRenderingPlugin {
        private final MissileAPI missile;
        private final CombatEngineAPI engine;
        private final boolean heavy;
        private final SpriteAPI sprite;
        private final List<DustAfterimage> afterimages = new ArrayList<DustAfterimage>();
        private final float spinRate;
        private float elapsed;
        private float spin;
        private float sampleTimer;
        private boolean retired;

        private DustSpinVisual(MissileAPI missile, CombatEngineAPI engine, int style) {
            super(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            this.missile = missile;
            this.engine = engine;
            this.heavy = style == 2;
            this.spinRate = heavy ? 170f : 310f;
            this.spin = Meng_OldEmpireMath.random(0f, 360f);
            SpriteAPI loaded = null;
            try {
                loaded = Global.getSettings().getSprite(heavy
                        ? "graphics/weapons/oldempire/krg_dust_heavy.png"
                        : "graphics/weapons/oldempire/krg_dust_cyber.png");
            } catch (Throwable ignored) {
            }
            this.sprite = loaded;
        }

        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        public float getRenderRadius() {
            return heavy ? 180f : 80f;
        }

        public CombatEntityAPI getEntity() {
            return missile;
        }

        public boolean isExpired() {
            return retired && afterimages.isEmpty();
        }

        public void advance(float amount) {
            elapsed += amount;
            spin += spinRate * amount;
            boolean active = missile != null && engine != null && engine.isEntityInPlay(missile)
                    && !missile.isExpired() && !missile.didDamage();
            if (!active) {
                retired = true;
            } else {
                missile.getSpriteAPI().setAngle(spin);
                if (heavy) {
                    sampleTimer -= amount;
                    while (sampleTimer <= 0f) {
                        afterimages.add(new DustAfterimage(new Vector2f(missile.getLocation()),
                                missile.getFacing() + spin,
                                Meng_OldEmpireMath.random(-3.6f, 3.6f), Meng_OldEmpireMath.random(-3.6f, 3.6f), elapsed));
                        sampleTimer += 0.035f;
                    }
                }
            }
            for (int i = afterimages.size() - 1; i >= 0; i--) {
                if (elapsed - afterimages.get(i).time > 0.20f) afterimages.remove(i);
            }
        }

        public void render(CombatEngineLayers layer, com.fs.starfarer.api.combat.ViewportAPI viewport) {
            if (sprite == null) return;
            if (heavy) {
                for (int i = 0; i < afterimages.size(); i++) {
                    DustAfterimage image = afterimages.get(i);
                    float age = clamp((elapsed - image.time) / 0.20f, 0f, 1f);
                    draw(image.point.x + image.jitterX, image.point.y + image.jitterY,
                            image.angle, (1f - age) * (1f - age) * 0.34f,
                            new Color(210, 20, 42));
                }
            }
            if (missile != null && engine != null && engine.isEntityInPlay(missile)
                    && !missile.isExpired() && !missile.didDamage()) {
                draw(missile.getLocation().x, missile.getLocation().y,
                        missile.getFacing() + spin, heavy ? 0.28f : 0.18f, Color.WHITE);
            }
        }

        private void draw(float x, float y, float angle, float alpha, Color tint) {
            sprite.setNormalBlend();
            sprite.setSize(heavy ? 56f : 18f, heavy ? 56f : 18f);
            sprite.setCenter(heavy ? 28f : 9f, heavy ? 28f : 9f);
            sprite.setAngle(angle);
            sprite.setColor(tint);
            sprite.setAlphaMult(clamp(alpha, 0f, 1f));
            sprite.renderAtCenter(x, y);
        }
    }

    private static final class DustAfterimage {
        private final Vector2f point;
        private final float angle;
        private final float jitterX;
        private final float jitterY;
        private final float time;

        private DustAfterimage(Vector2f point, float angle, float jitterX,
                               float jitterY, float time) {
            this.point = point;
            this.angle = angle;
            this.jitterX = jitterX;
            this.jitterY = jitterY;
            this.time = time;
        }
    }

    private static final class TracerFlightVisual extends GuidedFlightVisual {
        private final float scale;
        private final float trailWidthScale;
        private final float tailOffset;
        private final Color trailFringe;
        private final Color trailBody;
        private final Color trailCore;

        private TracerFlightVisual(MissileAPI missile, CombatEngineAPI engine, int style) {
            super(missile, engine);
            this.scale = style == 4 ? 1.55f : style == 3 ? 0.72f : 1f;
            this.trailWidthScale = style == 4 ? 2.20f : this.scale;
            this.tailOffset = style == 4 ? 28f : style == 3 ? 10f : 18f;
            this.trailFringe = style == 3 ? ORANGE_DEEP : DEEP_RED;
            this.trailBody = style == 3 ? ORANGE : CRIMSON;
            this.trailCore = style == 3 ? ORANGE_CORE : CRIMSON_CORE;
        }

        protected float sampleInterval() {
            return 0.016f;
        }

        protected float trailDuration() {
            return 0.30f * scale;
        }

        protected Vector2f samplePoint() {
            return Meng_OldEmpireMath.offset(missile.getLocation(), missile.getFacing() + 180f, tailOffset);
        }

        public void render(CombatEngineLayers layer, com.fs.starfarer.api.combat.ViewportAPI viewport) {
            if (samples.isEmpty()) return;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            drawTrailLine(samples, elapsed, trailDuration(), trailFringe, 0.18f,
                    5.5f * trailWidthScale);
            drawTrailLine(samples, elapsed, trailDuration(), trailBody, 0.36f,
                    2.3f * trailWidthScale);
            drawTrailLine(samples, elapsed, trailDuration(), trailCore, 0.62f,
                    0.85f * trailWidthScale);
            GL11.glPopAttrib();
        }
    }

    private static final class DustImpactVisual extends BaseCombatLayeredRenderingPlugin {
        private final Vector2f point;
        private final float facing;
        private final boolean crimson;
        private float elapsed;

        private DustImpactVisual(Vector2f point, float facing, boolean crimson) {
            super(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            this.point = point;
            this.facing = facing;
            this.crimson = crimson;
        }

        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        public float getRenderRadius() {
            return 180f;
        }

        public boolean isExpired() {
            return elapsed >= 0.24f;
        }

        public void advance(float amount) {
            elapsed += amount;
        }

        public void render(CombatEngineLayers layer, com.fs.starfarer.api.combat.ViewportAPI viewport) {
            float progress = clamp(elapsed / 0.24f, 0f, 1f);
            float alpha = (1f - progress) * (1f - progress);
            Color core = crimson ? HOT_RED : DUST_SILVER;
            Color fringe = crimson ? DEEP_RED : DUST_DEEP;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glLineWidth(1.8f);
            GL11.glBegin(GL11.GL_LINES);
            for (int i = 0; i < 9; i++) {
                float angle = facing + i * 40f + (float) Math.sin(i * 2.1f) * 11f;
                color(core, 0.50f * alpha);
                vertex(Meng_OldEmpireMath.offset(point, angle, 4f + progress * 14f));
                color(fringe, 0f);
                vertex(Meng_OldEmpireMath.offset(point, angle, 28f + progress * 62f));
            }
            GL11.glEnd();
            GL11.glPopAttrib();
        }
    }

    private static final class TracerImpactVisual extends BaseCombatLayeredRenderingPlugin {
        private final Vector2f point;
        private final float facing;
        private float elapsed;

        private TracerImpactVisual(Vector2f point, float facing) {
            super(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            this.point = point;
            this.facing = facing;
        }

        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        public float getRenderRadius() {
            return 140f;
        }

        public boolean isExpired() {
            return elapsed >= 0.16f;
        }

        public void advance(float amount) {
            elapsed += amount;
        }

        public void render(CombatEngineLayers layer, com.fs.starfarer.api.combat.ViewportAPI viewport) {
            float progress = clamp(elapsed / 0.16f, 0f, 1f);
            float alpha = 1f - progress;
            float reach = 12f + progress * 58f;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glLineWidth(2.2f);
            GL11.glBegin(GL11.GL_LINES);
            for (int i = 0; i < 4; i++) {
                float angle = facing + 45f + i * 90f;
                color(CRIMSON_CORE, 0.60f * alpha);
                vertex(Meng_OldEmpireMath.offset(point, angle, 3f));
                color(DEEP_RED, 0f);
                vertex(Meng_OldEmpireMath.offset(point, angle, reach));
            }
            GL11.glEnd();
            GL11.glPopAttrib();
        }
    }

    private static final class DragonDustStrikeVisual extends BaseCombatLayeredRenderingPlugin {
        private final Vector2f from;
        private final Vector2f to;
        private final float facing;
        private float elapsed;

        private DragonDustStrikeVisual(Vector2f from, Vector2f to) {
            super(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            this.from = from;
            this.to = to;
            this.facing = Meng_OldEmpireMath.angle(from, to);
        }

        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        public float getRenderRadius() {
            return 700f;
        }

        public boolean isExpired() {
            return elapsed >= 0.38f;
        }

        public void advance(float amount) {
            elapsed += amount;
        }

        public void render(CombatEngineLayers layer, com.fs.starfarer.api.combat.ViewportAPI viewport) {
            float progress = clamp(elapsed / 0.38f, 0f, 1f);
            float head = clamp(progress * 1.65f, 0f, 1f);
            float tail = Math.max(0f, head - 0.48f);
            float alpha = (1f - progress) * 0.72f;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            for (int strand = 0; strand < 5; strand++) {
                GL11.glLineWidth(strand == 0 ? 5f : strand < 3 ? 2.4f : 1.2f);
                GL11.glBegin(GL11.GL_LINE_STRIP);
                for (int i = 0; i <= 12; i++) {
                    float p = tail + (head - tail) * i / 12f;
                    float curve = (float) Math.sin(p * 18f - progress * 16f + strand * 1.9f)
                            * (5f + strand * 2.5f) * (float) Math.sin(Math.PI * i / 12f);
                    Vector2f point = new Vector2f(from.x + (to.x - from.x) * p,
                            from.y + (to.y - from.y) * p);
                    Vector2f side = Meng_OldEmpireMath.fromAngle(facing + 90f, curve);
                    point.x += side.x;
                    point.y += side.y;
                    color(strand == 0 ? DEEP_RED : strand < 3 ? CRIMSON : HOT_RED,
                            alpha * (0.35f + 0.65f * i / 12f));
                    vertex(point);
                }
                GL11.glEnd();
            }
            GL11.glPopAttrib();
        }
    }

    private static final class CyberDustBeamVisual extends BaseCombatLayeredRenderingPlugin {
        private final Vector2f from;
        private final Vector2f to;
        private final float facing;
        private final float strength;
        private float elapsed;

        private CyberDustBeamVisual(Vector2f from, Vector2f to, float strength) {
            super(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            this.from = from;
            this.to = to;
            this.facing = Meng_OldEmpireMath.angle(from, to);
            this.strength = strength;
        }

        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        public float getRenderRadius() {
            return 1600f;
        }

        public boolean isExpired() {
            return elapsed >= 0.085f;
        }

        public void advance(float amount) {
            elapsed += amount;
        }

        public void render(CombatEngineLayers layer, com.fs.starfarer.api.combat.ViewportAPI viewport) {
            float alpha = (1f - clamp(elapsed / 0.085f, 0f, 1f)) * strength;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            float length = (float) Math.sqrt(Meng_OldEmpireMath.distanceSquared(from, to));
            for (int i = 0; i < 3; i++) {
                float side = (i - 1) * 2.6f;
                Vector2f a = Meng_OldEmpireMath.offset(from, facing + 90f, side);
                Vector2f b = Meng_OldEmpireMath.offset(to, facing + 90f, side * 0.45f);
                GL11.glLineWidth(i == 1 ? 7.5f : 3.5f);
                GL11.glBegin(GL11.GL_LINES);
                color(i == 1 ? CRIMSON : DEEP_RED, (i == 1 ? 0.38f : 0.18f) * alpha);
                vertex(a);
                color(CRIMSON_CORE, (i == 1 ? 0.78f : 0.36f) * alpha);
                vertex(b);
                GL11.glEnd();
            }

            GL11.glLineWidth(1.5f);
            GL11.glBegin(GL11.GL_LINES);
            color(new Color(255, 220, 224), 0.88f * alpha);
            vertex(from);
            color(new Color(255, 96, 112), 0.92f * alpha);
            vertex(to);
            GL11.glEnd();

            for (int i = 1; i <= 3; i++) {
                float p = i / 4f;
                Vector2f point = new Vector2f(from.x + (to.x - from.x) * p,
                        from.y + (to.y - from.y) * p);
                drawEllipseRing(point, facing, 3.5f + length * 0.006f,
                        10f + i * 3f, 0.12f, CRIMSON_CORE, 0.20f * alpha, 24);
            }
            GL11.glPopAttrib();
        }
    }

    private static final class BeamMembraneVisual extends BaseCombatLayeredRenderingPlugin {
        private final Vector2f from;
        private final Vector2f center;
        private final Vector2f left;
        private final Vector2f right;
        private final float brightness;
        private float elapsed;

        private BeamMembraneVisual(Vector2f from, Vector2f center, Vector2f left,
                                   Vector2f right, float brightness) {
            super(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            this.from = from;
            this.center = center;
            this.left = left;
            this.right = right;
            this.brightness = brightness;
        }

        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        public float getRenderRadius() {
            return 1500f;
        }

        public boolean isExpired() {
            return elapsed >= 0.09f;
        }

        public void advance(float amount) {
            elapsed += amount;
        }

        public void render(CombatEngineLayers layer, com.fs.starfarer.api.combat.ViewportAPI viewport) {
            float alpha = (1f - clamp(elapsed / 0.09f, 0f, 1f)) * brightness;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            drawMembrane(from, center, left, alpha);
            drawMembrane(from, right, center, alpha);
            GL11.glPopAttrib();
        }

        private void drawMembrane(Vector2f origin, Vector2f inner, Vector2f outer, float alpha) {
            GL11.glBegin(GL11.GL_TRIANGLES);
            color(DEEP_RED, 0f);
            vertex(origin);
            color(CRIMSON, 0.14f * alpha);
            vertex(inner);
            color(HOT_RED, 0.035f * alpha);
            vertex(outer);
            GL11.glEnd();
        }
    }

    private static final class BeamRhythmVisual extends BaseCombatLayeredRenderingPlugin {
        private final Vector2f from;
        private final Vector2f to;
        private final float brightness;
        private final float phase;
        private float elapsed;

        private BeamRhythmVisual(Vector2f from, Vector2f to, float brightness, float phase) {
            super(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            this.from = from;
            this.to = to;
            this.brightness = brightness;
            this.phase = phase;
        }

        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        public float getRenderRadius() {
            return 1200f;
        }

        public boolean isExpired() {
            return elapsed >= 0.22f;
        }

        public void advance(float amount) {
            elapsed += amount;
        }

        public void render(CombatEngineLayers layer, com.fs.starfarer.api.combat.ViewportAPI viewport) {
            float alpha = (1f - clamp(elapsed / 0.22f, 0f, 1f)) * brightness;
            float facing = Meng_OldEmpireMath.angle(from, to);
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            for (int i = 0; i < 4; i++) {
                float p = (phase * 0.8f + i * 0.23f) % 1f;
                Vector2f center = new Vector2f(from.x + (to.x - from.x) * p,
                        from.y + (to.y - from.y) * p);
                float scale = 26f + i * 9f + elapsed * 76f;
                drawEllipseRing(center, facing, scale * 0.18f, scale * 0.72f,
                        0.16f, i % 2 == 0 ? CRIMSON : HOT_RED,
                        0.26f * alpha, 28);
            }
            GL11.glPopAttrib();
        }
    }

    private static final class HitRingVisual extends BaseCombatLayeredRenderingPlugin {
        private final Vector2f point;
        private final float angle;
        private final float strength;
        private final Color first;
        private final Color second;
        private float elapsed;

        private HitRingVisual(Vector2f point, float angle, float strength, Color first, Color second) {
            super(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            this.point = point;
            this.angle = angle;
            this.strength = strength;
            this.first = first;
            this.second = second;
        }

        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        public float getRenderRadius() {
            return 260f;
        }

        public boolean isExpired() {
            return elapsed >= 0.34f;
        }

        public void advance(float amount) {
            elapsed += amount;
        }

        public void render(CombatEngineLayers layer, com.fs.starfarer.api.combat.ViewportAPI viewport) {
            float alpha = 1f - clamp(elapsed / 0.34f, 0f, 1f);
            float radius = 24f + elapsed * 185f;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            drawEllipseRing(point, angle, radius * 0.22f, radius,
                    0.13f, first, 0.56f * alpha * strength, 34);
            drawEllipseRing(point, angle + 90f, radius * 0.12f, radius * 0.58f,
                    0.18f, second, 0.36f * alpha * strength, 26);
            GL11.glPopAttrib();
        }
    }
}
