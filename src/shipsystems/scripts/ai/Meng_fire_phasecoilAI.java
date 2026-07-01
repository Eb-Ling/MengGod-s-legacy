package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class Meng_fire_phasecoilAI implements ShipSystemAIScript {
    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipwideAIFlags flags;
    private float cooldown = 0f;
    private float exitCheckCooldown = 0f;
    
    private static final float MIN_FLUX_RATIO_TO_USE = 0.2f;
    private static final float MAX_FLUX_RATIO_TO_USE = 0.75f;
    private static final float OPTIMAL_FLUX_RATIO = 0.55f;
    private static final float HULL_DANGER_THRESHOLD = 0.3f;
    private static final float CHECK_INTERVAL = 0.2f;
    private static final float EXIT_CHECK_INTERVAL = 0.2f;
    private static final float SAFE_DISTANCE_FROM_ENEMIES = 2000f;
    private static final float VERY_SAFE_DISTANCE = 1800f;
    private static final float FLUX_SYSTEM_COOLDOWN_BUFFER = 1.5f;
    private static final float HIGH_FLUX_URGENCY_THRESHOLD = 0.6f;

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
        this.flags = flags;
    }

    private void use(ShipAPI ship) {
        if (ship.getPhaseCloak() != null && ship.getPhaseCloak().getId().contentEquals("Meng_fire_phasecoil")) {
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        } else {
            ship.useSystem();
        }
    }

    private void deactivatePhase(ShipAPI ship) {
        if (ship.getPhaseCloak() != null && ship.getPhaseCloak().isOn()) {
            ship.getPhaseCloak().forceState(ShipSystemAPI.SystemState.OUT, 0f); 
        }
    }

    private boolean isSafeToExitPhase() {
        List<ShipAPI> allShips = Global.getCombatEngine().getShips();
        
        if (allShips == null || allShips.isEmpty()) {
            return true;
        }

        float totalThreat = 0f;

        for (ShipAPI otherShip : allShips) {
            if (otherShip == null || !otherShip.isAlive()) {
                continue;
            }
            
            if (otherShip == ship) {
                continue;
            }
            
            if (otherShip.getOwner() == ship.getOwner()) {
                continue;
            }

            float distance = MathUtils.getDistance(ship.getLocation(), otherShip.getLocation());
            
            if (distance >= SAFE_DISTANCE_FROM_ENEMIES) {
                continue;
            }

            float enemyFluxRatio = otherShip.getFluxTracker().getCurrFlux() / otherShip.getMaxFlux();
            
            float threatWeight = 0f;
            
            if (enemyFluxRatio >= 0.95f) {
                threatWeight = 0.1f;
            } else if (enemyFluxRatio >= 0.85f) {
                threatWeight = 0.3f;
            } else if (enemyFluxRatio >= 0.7f) {
                threatWeight = 0.6f;
            } else {
                threatWeight = 1.0f;
            }

            if (distance < VERY_SAFE_DISTANCE) {
                threatWeight *= 1.5f;
                
                if (otherShip.getVelocity().length() > otherShip.getMaxSpeed() * 0.5f) {
                    Vector2f toEnemy = new Vector2f(otherShip.getLocation());
                    Vector2f.sub(toEnemy, ship.getLocation(), toEnemy);
                    float dotProduct = Vector2f.dot(toEnemy, otherShip.getVelocity());
                    if (dotProduct > 0) {
                        threatWeight *= 1.3f;
                    }
                }
            } else {
                float distanceFactor = 1.0f - (distance - VERY_SAFE_DISTANCE) / (SAFE_DISTANCE_FROM_ENEMIES - VERY_SAFE_DISTANCE);
                threatWeight *= Math.max(0.3f, distanceFactor);
            }

            totalThreat += threatWeight;
        }

        return totalThreat < 0.8f;
    }

    private boolean shouldUseFluxSystem() {
        if (ship.getSystem() == null || ship.getSystem().isCoolingDown()) {
            return false;
        }
        
        if (!ship.getSystem().getId().contentEquals("Meng_fire_fluxtrans_phase")) {
            return false;
        }

        FluxTrackerAPI tracker = ship.getFluxTracker();
        float hardFlux = tracker.getHardFlux();
        float currFlux = tracker.getCurrFlux();
        float maxFlux = tracker.getMaxFlux();
        
        float fluxDifference = Math.abs(currFlux - 2f * hardFlux);
        float fluxDiffRatio = fluxDifference / maxFlux;
        
        if (fluxDiffRatio < 0.2f) {
            return false;
        }

        return true;
    }

    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (this.engine.isPaused() || !this.ship.isAlive()) {
            return;
        }

        if (this.ship.getPhaseCloak() == null || !this.ship.getPhaseCloak().getId().contentEquals("Meng_fire_phasecoil")) {
            return;
        }

        ShipSystemAPI.SystemState phaseState = this.ship.getPhaseCloak().getState();

        if (phaseState == ShipSystemAPI.SystemState.ACTIVE) {
            exitCheckCooldown += amount;
            if (exitCheckCooldown >= EXIT_CHECK_INTERVAL) {
                exitCheckCooldown = 0f;

                float hullRatio = this.ship.getHitpoints() / this.ship.getMaxHitpoints();
                if (hullRatio < 0.12f) {
                    deactivatePhase(this.ship);
                    return;
                }

                FluxTrackerAPI tracker = this.ship.getFluxTracker();
                float fluxRatio = tracker.getCurrFlux() / tracker.getMaxFlux();
                
                if (fluxRatio < 0.2f && isSafeToExitPhase()) {
                    deactivatePhase(this.ship);
                    return;
                }
                
                if (fluxRatio > 0.45f && shouldUseFluxSystem() && isSafeToExitPhase()) {
                    deactivatePhase(this.ship);
                    return;
                }
            }
            return;
        }

        if (phaseState == ShipSystemAPI.SystemState.IN) {
            return;
        }

        boolean highFluxUrgency = false;
        FluxTrackerAPI tracker = this.ship.getFluxTracker();
        float fluxRatio = tracker.getCurrFlux() / tracker.getMaxFlux();
        if (fluxRatio >= HIGH_FLUX_URGENCY_THRESHOLD) {
            highFluxUrgency = true;
        }

        if (!highFluxUrgency) {
            if (this.ship.getSystem().isCoolingDown() && 
                this.ship.getSystem().getCooldownRemaining() < FLUX_SYSTEM_COOLDOWN_BUFFER) {
                return;
            }

            if (shouldUseFluxSystem()) {
                return;
            }
        }

        cooldown += amount;
        if (cooldown < CHECK_INTERVAL) {
            return;
        }
        cooldown = 0f;

        float hullRatio = this.ship.getHitpoints() / this.ship.getMaxHitpoints();
        
        if (hullRatio < HULL_DANGER_THRESHOLD) {
            return;
        }

        boolean isAttacking = flags.hasFlag(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) 
                || flags.hasFlag(ShipwideAIFlags.AIFlags.IN_ATTACK_RUN) 
                || flags.hasFlag(ShipwideAIFlags.AIFlags.BIGGEST_THREAT);
        
        boolean inDanger = flags.hasFlag(ShipwideAIFlags.AIFlags.IN_CRITICAL_DPS_DANGER);

        boolean fluxTooHigh = fluxRatio > MAX_FLUX_RATIO_TO_USE;
        
        if (fluxTooHigh && !highFluxUrgency) {
            return;
        }

        if (fluxRatio < MIN_FLUX_RATIO_TO_USE && !isAttacking && !highFluxUrgency) {
            return;
        }

        float fluxWeight = 0f;
        if (fluxTooHigh) {
            fluxWeight = 1f;
        } else if (fluxRatio <= OPTIMAL_FLUX_RATIO) {
            fluxWeight = (fluxRatio - MIN_FLUX_RATIO_TO_USE) / (OPTIMAL_FLUX_RATIO - MIN_FLUX_RATIO_TO_USE);
        } else {
            fluxWeight = 1f - (fluxRatio - OPTIMAL_FLUX_RATIO) / (MAX_FLUX_RATIO_TO_USE - OPTIMAL_FLUX_RATIO);
        }
        fluxWeight = Math.max(0f, Math.min(1f, fluxWeight));

        float useChance = 0f;
        
        if (fluxTooHigh) {
            useChance = 0.85f + 0.15f * fluxWeight;
        } else if (highFluxUrgency) {
            useChance = 0.75f + 0.25f * fluxWeight;
        } else if (isAttacking && !inDanger) {
            useChance = 0.6f + 0.35f * fluxWeight;
        } else if (isAttacking && inDanger) {
            useChance = 0.4f + 0.3f * fluxWeight;
        } else if (inDanger) {
            useChance = 0.25f * fluxWeight;
        } else {
            useChance = 0.2f + 0.25f * fluxWeight;
        }

        if (target != null && target.isAlive()) {
            float distanceToTarget = MathUtils.getDistance(ship.getLocation(), target.getLocation());
            if (distanceToTarget < 600f && isAttacking) {
                useChance *= 1.15f;
            }
        }

        if (highFluxUrgency && fluxRatio > 0.65f) {
            useChance *= 1.3f;
        }
        
        if (fluxTooHigh) {
            useChance *= 1.4f;
        }

        useChance = Math.min(1f, useChance);

        if (Math.random() <= useChance) {
            use(this.ship);
        }
    }
}
