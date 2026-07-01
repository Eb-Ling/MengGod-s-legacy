package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;
import java.util.EnumSet;

public class Meng_fire_fluxtrans_wingsacc extends BaseShipSystemScript {
    boolean init = false;
    float speedmult = 2f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        if (!init) {
            init = true;
            FluxTrackerAPI tracker = ship.getFluxTracker();
            float hardflux = tracker.getHardFlux();
            tracker.setHardFlux(tracker.getCurrFlux() - hardflux);
        } else {
            for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                if (bay.getWing() != null && bay.getWing().getLeader() != null && bay.getWing().getLeader().getWing() != null && bay.getWing().getLeader().getWing().getWingMembers() != null) {
                    for (ShipAPI target : bay.getWing().getLeader().getWing().getWingMembers()) {
                        target.getMutableStats().getMaxSpeed().modifyPercent(id, 100 * (speedmult - 1f));
                        target.getMutableStats().getAcceleration().modifyPercent(id, 100 * (speedmult - 1f));
                        target.getMutableStats().getTurnAcceleration().modifyPercent(id, 100 * (speedmult - 1f));
                        target.getMutableStats().getMaxTurnRate().modifyPercent(id, 100 * (speedmult - 1f));
                        target.getMutableStats().getAcceleration().modifyPercent(id, 100 * (speedmult - 1f));
                        target.getMutableStats().getDeceleration().modifyPercent(id, 100 * (speedmult - 1f));
                        target.getMutableStats().getMissileMaxSpeedBonus().modifyPercent(id, 70 * (speedmult - 1f));
                        target.getMutableStats().getMissileMaxTurnRateBonus().modifyPercent(id, 70 * (speedmult - 1f));
                        target.getMutableStats().getMissileAccelerationBonus().modifyPercent(id, 70 * (speedmult - 1f));
                        target.getMutableStats().getMissileGuidance().modifyPercent(id, 70 * (speedmult - 1f));
                        target.getMutableStats().getProjectileSpeedMult().modifyPercent(id, 70 * (speedmult - 1f));
                        target.setWeaponGlow(effectLevel, new Color(253, 14, 14, 255), EnumSet.allOf(WeaponAPI.WeaponType.class));
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
        for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
            if (bay.getWing() != null && bay.getWing().getLeader() != null && bay.getWing().getLeader().getWing() != null && bay.getWing().getLeader().getWing().getWingMembers() != null) {
                for (ShipAPI target : bay.getWing().getLeader().getWing().getWingMembers()) {
                    target.getMutableStats().getMaxSpeed().unmodifyPercent(id);
                    target.getMutableStats().getAcceleration().unmodifyPercent(id);
                    target.getMutableStats().getTurnAcceleration().unmodifyPercent(id);
                    target.getMutableStats().getMaxTurnRate().unmodifyPercent(id);
                    target.getMutableStats().getAcceleration().unmodifyPercent(id);
                    target.getMutableStats().getDeceleration().unmodifyPercent(id);
                    target.getMutableStats().getMissileMaxSpeedBonus().unmodifyPercent(id);
                    target.getMutableStats().getMissileMaxTurnRateBonus().unmodifyPercent(id);
                    target.getMutableStats().getMissileAccelerationBonus().unmodifyPercent(id);
                    target.getMutableStats().getMissileGuidance().unmodifyPercent(id);
                    target.getMutableStats().getProjectileSpeedMult().unmodifyPercent(id);
                }
            }
        }
    }
}
