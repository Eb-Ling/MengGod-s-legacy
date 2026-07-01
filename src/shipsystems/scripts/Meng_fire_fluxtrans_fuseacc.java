package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.methods.Meng_hullsizeint;
import org.magiclib.util.MagicSettings;

public class Meng_fire_fluxtrans_fuseacc extends BaseShipSystemScript {
    public final float fuseacc = MagicSettings.getFloat("Meng_fireset", "Meng_fire_fluxtrans_fuse_Accelerate");
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
        ship.getMutableStats().getMaxTurnRate().modifyPercent(id, fuseacc * 100f);
        ship.getMutableStats().getTurnAcceleration().modifyPercent(id, fuseacc * 100f);
        ship.getMutableStats().getAcceleration().modifyPercent(id, fuseacc * 100f);
        ship.getMutableStats().getDeceleration().modifyPercent(id, fuseacc * 100f);
        ship.getMutableStats().getMaxSpeed().modifyPercent(id, fuseacc * 50f);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        init = false;
        FluxTrackerAPI tracker = ship.getFluxTracker();
        float hardflux = tracker.getHardFlux();
        tracker.setHardFlux(tracker.getCurrFlux() - hardflux);
        ship.getMutableStats().getMaxTurnRate().unmodifyPercent(id);
        ship.getMutableStats().getTurnAcceleration().unmodifyPercent(id);
        ship.getMutableStats().getAcceleration().unmodifyPercent(id);
        ship.getMutableStats().getDeceleration().unmodifyPercent(id);
        ship.getMutableStats().getMaxSpeed().unmodifyPercent(id);
    }
}
