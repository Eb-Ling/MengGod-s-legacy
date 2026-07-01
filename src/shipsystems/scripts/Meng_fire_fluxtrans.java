package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.methods.Meng_hullsizeint;

public class Meng_fire_fluxtrans extends BaseShipSystemScript {
    boolean init = false;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        FluxTrackerAPI tracker = ship.getFluxTracker();
        if (!init) {
            init = true;
            float hardflux = tracker.getHardFlux();
            tracker.setHardFlux(tracker.getCurrFlux() - hardflux);
        }
        ShipAPI target = ship.getShipTarget();
        if (target != null && Meng_hullsizeint.Getsize(target) <= Meng_hullsizeint.Getsize(ship)) {
            if (ship.getAI() != null) {
                if (tracker.getCurrFlux() <= ship.getMaxFlux() * 0.6f && target.getVelocity().length() <= ship.getMaxSpeed()) {
                    if (target.getFluxTracker().getCurrFlux() >= target.getMaxFlux() * 0.5f) {
                        ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.PURSUING);
                    } else {
                        ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF);
                    }
                }
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        init = false;
        if (ship.getAI() != null) {
            ship.getAIFlags().removeFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF);
            ship.getAIFlags().removeFlag(ShipwideAIFlags.AIFlags.PURSUING);
        }
        FluxTrackerAPI tracker = ship.getFluxTracker();
        float hardflux = tracker.getHardFlux();
        tracker.setHardFlux(tracker.getCurrFlux() - hardflux);
    }
}
