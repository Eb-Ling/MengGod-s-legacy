package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;

public class Meng_MBosssystem extends BaseShipSystemScript {
    private final float fluxspeed = 700f;
    float lastflux = 0f;
    private boolean init1 = false;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        stats.getMaxSpeed().modifyMult(id, 0f);
        for (ShipAPI target : Global.getCombatEngine().getShips()) {
            if (!target.isDrone() && target.isAlive() && target.getOwner() != ship.getOwner()) {
                float dis = MathUtils.getDistance(ship.getLocation(), target.getLocation());
                if (dis <= 1600f) {
                    if (target.getSystem() != null) {
                        target.getSystem().deactivate();
                    }
                    if (target.getShield() != null && target.getShield().getType() == ShieldAPI.ShieldType.PHASE && target.getPhaseCloak() != null) {
                        target.getPhaseCloak().deactivate();
                    }
                }
            }
        }
        if (ship.getFluxTracker().getCurrFlux() < lastflux) {
            ship.getFluxTracker().setCurrFlux(lastflux);
        }
        ship.getFluxTracker().setCurrFlux(Math.min(ship.getFluxTracker().getMaxFlux(), ship.getCurrFlux() + fluxspeed * amount));
        ship.getFluxTracker().setHardFlux(Math.min(ship.getFluxTracker().getMaxFlux(), ship.getFluxTracker().getHardFlux() + fluxspeed * amount));
        if (lastflux >= ship.getFluxTracker().getMaxFlux() * 0.8f) {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getSpec().getWeaponId().equals("Meng_MBossweapon")) {
                    if (!init1) {
                        init1 = true;
                        weapon.setAmmo(1);
                    }
                }
            }
        }

        if (init1) {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getSpec().getWeaponId().equals("Meng_MBossweapon")) {
                    if (init1 && weapon.getAmmo() == 0) {
                        ship.getSystem().deactivate();
                        init1 = false;
                    }
                }
            }
        }
        lastflux = ship.getCurrFlux();
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        init1 = false;
        lastflux = 0f;
        stats.getMaxSpeed().unmodifyMult(id);
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSpec().getWeaponId().equals("Meng_MBossweapon")) {
                weapon.setAmmo(0);
            }
        }
    }
}
