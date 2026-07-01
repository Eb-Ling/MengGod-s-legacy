package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class Meng_fluxtrans_wingsaccAI implements ShipSystemAIScript {
    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipwideAIFlags flags;
    private float cooldown = 0f;
    private static final float CHECK_INTERVAL = 0.3f;

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
        this.flags = flags;
    }

    private void use(ShipAPI ship) {
        if (ship.getPhaseCloak() != null && ship.getPhaseCloak().getId().contentEquals("Meng_fire_fluxtrans_wingsacc")) {
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        } else {
            ship.useSystem();
        }
    }

    private float calculateFighterHealthRatio() {
        List<FighterLaunchBayAPI> bays = this.ship.getLaunchBaysCopy();
        if (bays == null || bays.isEmpty()) {
            return 0f;
        }

        int activeBays = 0;
        float totalHealthRatio = 0f;

        for (FighterLaunchBayAPI bay : bays) {
            if (bay.getWing() != null && bay.getWing().getLeader() != null && bay.getWing().getLeader().getWing() != null) {
                activeBays++;
                
                int maxFighters = bay.getWing().getSpec().getNumFighters();
                if (maxFighters <= 0) {
                    continue;
                }
                
                int aliveFighters = 0;
                for (ShipAPI fighter : bay.getWing().getLeader().getWing().getWingMembers()) {
                    if (fighter != null && fighter.isAlive()) {
                        aliveFighters++;
                    }
                }
                
                totalHealthRatio += (float) aliveFighters / maxFighters;
            }
        }

        if (activeBays == 0) {
            return 0f;
        }

        return totalHealthRatio / activeBays;
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
        float fluxRatio = tracker.getHardFlux() / tracker.getMaxFlux();
        float fluxWeight = fluxRatio * fluxRatio;

        if (fluxWeight >= 0.5f && Math.random() <= amount * fluxWeight * 2f) {
            use(this.ship);
            return;
        }

        float fighterHealthRatio = calculateFighterHealthRatio();
        
        if (fighterHealthRatio > 0.7f && !this.ship.isPullBackFighters()) {
            boolean fightersEngaged = false;
            for (FighterLaunchBayAPI bay : this.ship.getLaunchBaysCopy()) {
                if (bay.getWing() != null && bay.getWing().getLeader() != null) {
                    fightersEngaged = true;
                    break;
                }
            }
            
            if (fightersEngaged && Math.random() <= fighterHealthRatio + 0.2f) {
                use(this.ship);
            }
        }
    }
}
