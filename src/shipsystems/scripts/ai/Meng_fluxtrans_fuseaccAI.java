package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class Meng_fluxtrans_fuseaccAI implements ShipSystemAIScript {
    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipwideAIFlags flags;
    private float cooldown = 0f;
    private static final float CHECK_INTERVAL = 0.25f;

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
        this.flags = flags;
    }

    private void use(ShipAPI ship) {
        if (ship.getPhaseCloak() != null && ship.getPhaseCloak().getId().contentEquals("Meng_fire_fluxtrans_fuseacc")) {
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
        
        boolean highHardFlux = hardFluxRatio >= 0.5f;
        
        boolean needsEvasion = flags.hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE)
                || flags.hasFlag(ShipwideAIFlags.AIFlags.IN_CRITICAL_DPS_DANGER);
        
        boolean needsMobility = !flags.hasFlag(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) 
                && (flags.hasFlag(ShipwideAIFlags.AIFlags.RUN_QUICKLY) 
                || flags.hasFlag(ShipwideAIFlags.AIFlags.TURN_QUICKLY));
        
        boolean lowSpeed = this.ship.getVelocity().length() < this.ship.getMaxSpeed() * 0.4f;

        if (highHardFlux || needsEvasion || needsMobility || lowSpeed) {
            use(this.ship);
        }
    }
}
