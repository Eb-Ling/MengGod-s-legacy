package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class Meng_fluxtransAI implements ShipSystemAIScript {
    private CombatEngineAPI engine;
    private ShipAPI ship;
    private float cooldown = 0f;
    private static final float CHECK_INTERVAL = 0.3f;

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
    }

    private void use(ShipAPI ship) {
        if (ship.getPhaseCloak() != null && ship.getPhaseCloak().getId().contentEquals("Meng_fire_fluxtrans")) {
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        } else {
            ship.useSystem();
        }
    }

    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (this.engine.isPaused() || !this.ship.isAlive()) {
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
        float hardFluxRatio = tracker.getHardFlux() / tracker.getMaxFlux();
        
        boolean hasSignificantSoftFlux = (tracker.getCurrFlux() - tracker.getHardFlux()) > tracker.getMaxFlux() * 0.3f;
        
        boolean chasingLowMobilityTarget = false;
        if (target != null && target.isAlive()) {
            float targetSpeed = target.getVelocity().length();
            float myMaxSpeed = this.ship.getMaxSpeed();
            if (targetSpeed <= myMaxSpeed && tracker.getCurrFlux() <= tracker.getMaxFlux() * 0.6f) {
                chasingLowMobilityTarget = true;
            }
        }

        if (hardFluxRatio >= 0.5f || (hasSignificantSoftFlux && chasingLowMobilityTarget)) {
            use(this.ship);
        }
    }
}
