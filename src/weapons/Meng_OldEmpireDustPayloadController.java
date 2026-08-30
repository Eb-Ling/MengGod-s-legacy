package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponGroupType;
import data.methods.Meng_OldEmpireMath;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

/**
 * Deterministic replacement for the vanilla DEM state machine used by the
 * split and heavy spirit-dust projectiles.
 */
public final class Meng_OldEmpireDustPayloadController {
    private static final float ACQUISITION_RANGE = 1900f;
    private static final float SPLIT_TRIGGER_RANGE = 825f;
    private static final float HEAVY_TRIGGER_RANGE = 750f;
    private static final float SPLIT_FIRE_TIME = 2.625f;
    private static final float HEAVY_FIRE_TIME = 1.05f;
    private static final float SPLIT_MAX_ORBIT_TIME = 0.8f;

    private Meng_OldEmpireDustPayloadController() {
    }

    public static void start(MissileAPI missile, WeaponAPI parentWeapon,
                             CombatEngineAPI engine, boolean heavy) {
        if (missile == null || parentWeapon == null || engine == null) return;
        ShipAPI source = parentWeapon.getShip();
        if (source == null) return;

        missile.setSpriteAlphaOverride(1f);
        missile.setMaxFlightTime(25f);
        missile.setForceAlwaysArmed(true);

        Vector2f relativeVelocity = Vector2f.sub(
                missile.getVelocity(), source.getVelocity(), null);
        float minimumSpeed = heavy ? 448f : 150f;
        float cruiseSpeed = Math.max(minimumSpeed, relativeVelocity.length());
        engine.addPlugin(new Controller(
                missile, source, engine, heavy, cruiseSpeed,
                Math.max(1f, missile.getDamageAmount())));
    }

    private enum State {
        SEEK,
        ORBIT,
        FIRE,
        DONE
    }

    private static final class Controller extends BaseEveryFrameCombatPlugin {
        private final MissileAPI missile;
        private final ShipAPI source;
        private final CombatEngineAPI engine;
        private final boolean heavy;
        private final float cruiseSpeed;
        private final float payloadDamage;
        private final float orbitDirection;
        private final float orbitDuration;

        private State state = State.SEEK;
        private ShipAPI target;
        private ShipAPI payloadTarget;
        private ShipAPI payloadDrone;
        private WeaponAPI payloadWeapon;
        private final Vector2f anchor = new Vector2f();
        private final Vector2f lastTargetPoint = new Vector2f();
        private float elapsed;
        private float stateElapsed;
        private float retargetTimer;
        private float orbitStartedAt = -1f;

        private Controller(MissileAPI missile, ShipAPI source,
                           CombatEngineAPI engine, boolean heavy,
                           float cruiseSpeed, float payloadDamage) {
            this.missile = missile;
            this.source = source;
            this.engine = engine;
            this.heavy = heavy;
            this.cruiseSpeed = cruiseSpeed;
            this.payloadDamage = payloadDamage;
            this.orbitDirection = Math.random() < 0.5d ? -1f : 1f;
            this.orbitDuration = Meng_OldEmpireMath.random(0.70f, 0.95f);
            this.target = acquireTarget();
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (state == State.DONE) return;
            if (engine.isPaused()) return;

            elapsed += amount;
            stateElapsed += amount;

            if (state == State.FIRE) {
                advancePayload(amount);
                return;
            }

            if (!isMissileActive()) {
                finish(false);
                return;
            }

            // Keep the projectile alive while it is searching or maneuvering.
            if (missile.getFlightTime() > 23f) missile.setFlightTime(23f);

            retargetTimer -= amount;
            if (!isRetainedTargetValid(target) || retargetTimer <= 0f) {
                retargetTimer = 0.12f;
                if (!isRetainedTargetValid(target)) target = acquireTarget();
            }

            if (!heavy && orbitStartedAt >= 0f
                    && elapsed - orbitStartedAt >= SPLIT_MAX_ORBIT_TIME) {
                beginPayload();
                return;
            }

            if (target == null) {
                if (!heavy && orbitStartedAt >= 0f) {
                    beginPayload();
                    return;
                }
                flyStraight(amount);
                if (elapsed >= 24.5f) finish(true);
                return;
            }

            lastTargetPoint.set(target.getLocation());
            if (heavy) {
                guideToward(target.getLocation(), amount, cruiseSpeed, 170f);
                float trigger = Math.max(HEAVY_TRIGGER_RANGE,
                        target.getCollisionRadius() + 170f);
                if (elapsed >= 1f && distance(missile.getLocation(), target.getLocation()) <= trigger) {
                    beginPayload();
                }
                return;
            }

            if (state == State.SEEK) {
                guideToward(leadPoint(target, cruiseSpeed), amount, cruiseSpeed, 220f);
                if (distance(missile.getLocation(), target.getLocation()) <= SPLIT_TRIGGER_RANGE) {
                    state = State.ORBIT;
                    stateElapsed = 0f;
                    orbitStartedAt = elapsed;
                }
            } else {
                orbitTarget(amount);
                if (stateElapsed >= orbitDuration) beginPayload();
            }
        }

        private void flyStraight(float amount) {
            Vector2f wanted = Meng_OldEmpireMath.fromAngle(missile.getFacing(), cruiseSpeed);
            blendVelocity(wanted, amount, 4.5f);
        }

        private void orbitTarget(float amount) {
            if (!isRetainedTargetValid(target)) {
                beginPayload();
                return;
            }

            float radius = clamp(target.getCollisionRadius() + 190f, 280f, 560f);
            float currentAngle = angle(target.getLocation(), missile.getLocation());
            float lookAhead = orbitDirection * 24f;
            Vector2f orbitPoint = Meng_OldEmpireMath.offset(target.getLocation(),
                    currentAngle + lookAhead, radius);
            guideToward(orbitPoint, amount, cruiseSpeed, 260f);
        }

        private void guideToward(Vector2f point, float amount, float speed, float turnRate) {
            float desired = angle(missile.getLocation(), point);
            float turn = clamp(shortestRotation(missile.getFacing(), desired),
                    -turnRate * amount, turnRate * amount);
            missile.setFacing(missile.getFacing() + turn);

            Vector2f wanted = Meng_OldEmpireMath.fromAngle(missile.getFacing(), speed);
            blendVelocity(wanted, amount, heavy ? 8f : 6f);
        }

        private void blendVelocity(Vector2f wanted, float amount, float response) {
            Vector2f velocity = missile.getVelocity();
            float blend = clamp(amount * response, 0f, 1f);
            velocity.x += (wanted.x - velocity.x) * blend;
            velocity.y += (wanted.y - velocity.y) * blend;
        }

        private void beginPayload() {
            if (state == State.FIRE || state == State.DONE) return;

            state = State.FIRE;
            stateElapsed = 0f;
            payloadTarget = target;
            anchor.set(missile.getLocation());
            if (isRetainedTargetValid(payloadTarget)) {
                lastTargetPoint.set(payloadTarget.getLocation());
            } else {
                lastTargetPoint.set(Meng_OldEmpireMath.offset(anchor, missile.getFacing(), 1000f));
            }

            missile.getVelocity().set(0f, 0f);
            missile.setCollisionClass(CollisionClass.NONE);
            missile.setFacing(angle(anchor, lastTargetPoint));

            if (heavy) {
                spawnShapedExplosion(anchor, missile.getFacing());
                if (engine.isEntityInPlay(missile)) {
                    missile.explode();
                }
            }

            createPayloadDrone();
            updatePayloadAim();
            forcePayloadFire();
        }

        private void createPayloadDrone() {
            String payloadId = heavy
                    ? "Meng_OldEmpire_dust_heavy_payload" : "Meng_OldEmpire_dust_cyber_payload";
            ShipHullSpecAPI spec = Global.getSettings().getHullSpec("dem_drone");
            if (spec == null) return;

            ShipVariantAPI variant = Global.getSettings().createEmptyVariant(
                    "Meng_OldEmpire_dust_payload_fx", spec);
            variant.addWeapon("WS 001", payloadId);
            WeaponGroupSpec group = new WeaponGroupSpec(WeaponGroupType.LINKED);
            group.addSlot("WS 001");
            group.setAutofireOnByDefault(false);
            variant.addWeaponGroup(group);

            payloadDrone = engine.createFXDrone(variant);
            if (payloadDrone == null) return;
            payloadDrone.setLayer(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            payloadDrone.setOwner(source.getOwner());
            payloadDrone.setOriginalOwner(source.getOriginalOwner());
            payloadDrone.setAlly(source.isAlly());
            payloadDrone.setDrone(true);
            payloadDrone.setCollisionClass(CollisionClass.NONE);
            payloadDrone.setForceHideFFOverlay(true);
            payloadDrone.setShipAI(null);
            payloadDrone.getMutableStats().getHullDamageTakenMult()
                    .modifyMult("Meng_OldEmpire_dust_payload", 0f);
            float baseDamage = heavy ? 510f : 100f;
            payloadDrone.getMutableStats().getEnergyWeaponDamageMult()
                    .modifyMult("Meng_OldEmpire_dust_payload", payloadDamage / baseDamage);
            engine.addEntity(payloadDrone);

            for (WeaponAPI candidate : payloadDrone.getAllWeapons()) {
                if (payloadId.equals(candidate.getId())) {
                    payloadWeapon = candidate;
                    break;
                }
            }
            if (payloadWeapon != null) {
                payloadWeapon.setSuspendAutomaticTurning(true);
                payloadWeapon.setKeepBeamTargetWhileChargingDown(true);
                payloadWeapon.setScaleBeamGlowBasedOnDamageEffectiveness(false);
            }
            payloadDrone.giveCommand(ShipCommand.SELECT_GROUP, null, 0);
        }

        private void advancePayload(float amount) {
            if (isRetainedTargetValid(payloadTarget)) {
                lastTargetPoint.set(payloadTarget.getLocation());
            }
            updatePayloadAim();
            forcePayloadFire();

            float duration = heavy ? HEAVY_FIRE_TIME : SPLIT_FIRE_TIME;
            if (stateElapsed >= duration) finish(!heavy);
        }

        private void updatePayloadAim() {
            float facing = angle(anchor, lastTargetPoint);

            if (!heavy && engine.isEntityInPlay(missile)) {
                missile.getLocation().set(anchor);
                missile.getVelocity().set(0f, 0f);
                missile.setFacing(facing);
                missile.setSpriteAlphaOverride(1f);
            }

            if (payloadDrone == null || !engine.isEntityInPlay(payloadDrone)) return;
            payloadDrone.getLocation().set(anchor);
            payloadDrone.getVelocity().set(0f, 0f);
            payloadDrone.setFacing(facing);
            payloadDrone.setShipTarget(isRetainedTargetValid(payloadTarget)
                    ? payloadTarget : null);
            payloadDrone.getMouseTarget().set(lastTargetPoint);

            if (payloadWeapon != null) {
                payloadWeapon.setCurrAngle(facing);
                payloadWeapon.setFacing(facing);
                payloadWeapon.updateBeamFromPoints();
            }
        }

        private void forcePayloadFire() {
            if (payloadDrone == null || !engine.isEntityInPlay(payloadDrone)) return;
            payloadDrone.giveCommand(ShipCommand.FIRE, lastTargetPoint, 0);
            if (payloadWeapon != null) payloadWeapon.setForceFireOneFrame(true);
        }

        private void spawnShapedExplosion(Vector2f point, float facing) {
            Vector2f zero = new Vector2f();
            engine.spawnExplosion(point, zero, new Color(255, 70, 70, 235),
                    150f, 0.30f);
            engine.addSmoothParticle(point, zero, 350f, 1f, 0.18f,
                    new Color(255, 70, 95, 220));

            for (int i = 0; i < 50; i++) {
                float particleAngle = facing + Meng_OldEmpireMath.random(-22.5f, 22.5f);
                Vector2f spawn = Meng_OldEmpireMath.offset(point,
                        facing + Meng_OldEmpireMath.random(-90f, 90f), Meng_OldEmpireMath.random(0f, 100f));
                Vector2f velocity = Meng_OldEmpireMath.fromAngle(
                        particleAngle, Meng_OldEmpireMath.random(50f, 250f));
                engine.addNebulaParticle(spawn, velocity,
                        Meng_OldEmpireMath.random(50f, 70f),
                        Meng_OldEmpireMath.random(1f, 2f),
                        0.05f, 0.18f,
                        Meng_OldEmpireMath.random(0.7f, 1.1f),
                        new Color(255, 40, 40, 155));
            }
        }

        private ShipAPI acquireTarget() {
            ShipAPI selected = source.getShipTarget();
            if (isAcquirable(selected)) return selected;

            ShipAPI nearest = null;
            float best = ACQUISITION_RANGE * ACQUISITION_RANGE;
            for (ShipAPI candidate : engine.getShips()) {
                if (!isBasicTargetValid(candidate)) continue;
                float distanceSquared = distanceSquared(
                        missile.getLocation(), candidate.getLocation());
                if (distanceSquared < best) {
                    best = distanceSquared;
                    nearest = candidate;
                }
            }
            return nearest;
        }

        private boolean isAcquirable(ShipAPI candidate) {
            return isBasicTargetValid(candidate)
                    && distanceSquared(missile.getLocation(), candidate.getLocation())
                    <= ACQUISITION_RANGE * ACQUISITION_RANGE;
        }

        private boolean isRetainedTargetValid(ShipAPI candidate) {
            return isBasicTargetValid(candidate);
        }

        private boolean isBasicTargetValid(ShipAPI candidate) {
            if (candidate == null || !candidate.isAlive() || candidate.isHulk()
                    || candidate.isPhased() || candidate.isFighter()
                    || candidate.isDrone()) {
                return false;
            }
            return candidate.getOwner() != source.getOwner()
                    && engine.isEntityInPlay(candidate);
        }

        private Vector2f leadPoint(ShipAPI ship, float speed) {
            float travelTime = Math.min(0.45f,
                    distance(missile.getLocation(), ship.getLocation())
                            / Math.max(1f, speed));
            return new Vector2f(
                    ship.getLocation().x + ship.getVelocity().x * travelTime,
                    ship.getLocation().y + ship.getVelocity().y * travelTime);
        }

        private boolean isMissileActive() {
            return missile != null && engine.isEntityInPlay(missile)
                    && !missile.isExpired() && !missile.didDamage();
        }

        private void finish(boolean explodeMissile) {
            if (state == State.DONE) return;
            state = State.DONE;

            if (payloadDrone != null && engine.isEntityInPlay(payloadDrone)) {
                engine.removeEntity(payloadDrone);
            }
            if (missile != null && engine.isEntityInPlay(missile)) {
                if (explodeMissile) {
                    Vector2f point = new Vector2f(missile.getLocation());
                    engine.spawnExplosion(point, new Vector2f(),
                            new Color(205, 35, 105, 190), 42f, 0.18f);
                    engine.addSmoothParticle(point, new Vector2f(),
                            74f, 0.75f, 0.16f,
                            new Color(255, 120, 210, 190));
                }
                // explode() does not reliably retire this collision-disabled custom missile.
                engine.removeEntity(missile);
            }
            engine.removePlugin(this);
        }
    }

    private static float angle(Vector2f from, Vector2f to) {
        return (float) Math.toDegrees(Math.atan2(to.y - from.y, to.x - from.x));
    }

    private static float shortestRotation(float from, float to) {
        float result = (to - from) % 360f;
        if (result > 180f) result -= 360f;
        if (result < -180f) result += 360f;
        return result;
    }

    private static float distance(Vector2f a, Vector2f b) {
        return (float) Math.sqrt(distanceSquared(a, b));
    }

    private static float distanceSquared(Vector2f a, Vector2f b) {
        float x = a.x - b.x;
        float y = a.y - b.y;
        return x * x + y * y;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
