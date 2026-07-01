package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.hullmods.Meng_fire_core;
import data.methods.Meng_hullsizeint;
import org.magiclib.util.MagicSettings;


public class Meng_fire_fluxtrans_attackcore extends BaseShipSystemScript {
    public static final String KEY = "Meng_fire_coreeffect";
    public final float fluxmult = MagicSettings.getFloat("Meng_fireset", "Meng_fire_fluxtrans_attackcore");
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        if (!ship.getCustomData().containsKey(KEY)) {
            Meng_fire_core.DataContainer data = new Meng_fire_core.DataContainer();
            ship.setCustomData(KEY, data);
        }
        Meng_fire_core.DataContainer data = (Meng_fire_core.DataContainer) ship.getCustomData().get(KEY);
        FluxTrackerAPI tracker = ship.getFluxTracker();
        ShipAPI target = ship.getShipTarget();
        if (ship.getAI() != null) {
            for(WeaponAPI w:ship.getAllWeapons()){
                if(ship.getFluxTracker().getFluxLevel()<=0.7f&&w.getFluxCostToFire()>10f) w.setForceFireOneFrame(true);
            }
            ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF,8f);
        }
        float hardflux = tracker.getHardFlux();
        if (hardflux >= 1f) {
            tracker.setHardFlux(hardflux - data.weaponflux * fluxmult);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }

    }
}
