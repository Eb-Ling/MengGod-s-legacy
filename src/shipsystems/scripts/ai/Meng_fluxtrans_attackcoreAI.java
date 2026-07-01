package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class Meng_fluxtrans_attackcoreAI implements ShipSystemAIScript {
    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipwideAIFlags flags;
    private float cooldown = 0f;
    private static final float CHECK_INTERVAL = 0.15f;

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
        this.flags = flags;
    }

    private void use(ShipAPI ship) {
        if (ship.getPhaseCloak() != null && ship.getPhaseCloak().getId().contentEquals("Meng_fire_fluxtrans_attackcore")) {
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

        FluxTrackerAPI tracker = this.ship.getFluxTracker();
        float fluxRatio = tracker.getHardFlux() / tracker.getMaxFlux();
        
        boolean isAttacking = flags.hasFlag(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) 
                || flags.hasFlag(ShipwideAIFlags.AIFlags.IN_ATTACK_RUN) 
                || flags.hasFlag(ShipwideAIFlags.AIFlags.BIGGEST_THREAT);

        cooldown += amount;
        if (cooldown < CHECK_INTERVAL) {
            return;
        }
        cooldown = 0f;

        if (isAttacking && fluxRatio >= 0.4f) {
            float useChance = 0.3f + 0.4f * (fluxRatio * fluxRatio);
            if (Math.random() <= useChance) {
                use(this.ship);
            }
        }
    }
}
