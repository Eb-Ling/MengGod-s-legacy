package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class Meng_fire_fluxtrans_endlessAI implements ShipSystemAIScript {
    private CombatEngineAPI engine;
    private ShipAPI ship;
    private float cooldown = 0f;
    private static final float CHECK_INTERVAL = 0.25f;

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
    }

    private void use(ShipAPI ship) {
        if (ship.getPhaseCloak() != null && ship.getPhaseCloak().getId().contentEquals("Meng_fire_fluxtrans_endless")) {
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
        
        boolean needsMobility = this.ship.getVelocity().length() < this.ship.getMaxSpeed() * 0.3f;
        
        boolean inCombat = target != null && target.isAlive() 
                && MathUtils.getDistance(this.ship.getLocation(), target.getLocation()) < 1000f;

        if (hardFluxRatio >= 0.5f || (needsMobility && inCombat)) {
            use(this.ship);
        }
    }
}
