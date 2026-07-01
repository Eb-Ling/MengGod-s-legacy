package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class Meng_fire_fluxtrans_phaseAI implements ShipSystemAIScript {
    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipwideAIFlags flags;
    private float cooldown = 0f;
    
    private static final float CHECK_INTERVAL = 0.3f;
    private static final float MIN_FLUX_FOR_RECOVERY = 0.4f;
    private static final float HULL_DAMAGE_THRESHOLD = 0.05f;
    private static final float SAFE_HULL_RATIO = 0.9f;

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
        this.flags = flags;
    }

    private void use(ShipAPI ship) {
        if (ship.getPhaseCloak() != null && ship.getPhaseCloak().getId().contentEquals("Meng_fire_fluxtrans_phase")) {
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        } else {
            ship.useSystem();
        }
    }

    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (this.engine.isPaused() || !this.ship.isAlive()) {
            return;
        }

        if (this.ship.getSystem() == null || !this.ship.getSystem().getId().contentEquals("Meng_fire_fluxtrans_phase")) {
            return;
        }

        if (this.ship.getPhaseCloak() != null && 
            this.ship.getPhaseCloak().getState() == ShipSystemAPI.SystemState.ACTIVE) {
            return;
        }

        if (this.ship.getSystem().isCoolingDown()) {
            return;
        }

        cooldown += amount;
        if (cooldown < CHECK_INTERVAL) {
            return;
        }
        cooldown = 0f;

        FluxTrackerAPI tracker = this.ship.getFluxTracker();
        float hardFlux = tracker.getHardFlux();
        float currFlux = tracker.getCurrFlux();
        float maxFlux = tracker.getMaxFlux();
        
        float fluxDifference = Math.abs(currFlux - 2f * hardFlux);
        float fluxDiffRatio = fluxDifference / maxFlux;
        
        if (fluxDiffRatio < MIN_FLUX_FOR_RECOVERY) {
            return;
        }

        float hullRatio = this.ship.getHitpoints() / this.ship.getMaxHitpoints();
        float hullDamage = 1f - hullRatio;

        boolean hasSignificantSoftFlux = fluxDiffRatio > 0.15f;
        
        float recoveryPriority = 0f;
        
        if (hasSignificantSoftFlux) {
            recoveryPriority += 0.4f;
        }
        
        recoveryPriority += hullDamage * 0.4f;
        
        recoveryPriority += (fluxDiffRatio - MIN_FLUX_FOR_RECOVERY) * 0.2f;

        recoveryPriority = Math.max(0f, Math.min(1f, recoveryPriority));

        float useChance = recoveryPriority * 0.7f;

        if (Math.random() <= useChance) {
            use(this.ship);
        }
    }
}
