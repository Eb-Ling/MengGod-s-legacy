package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class Meng_MBoss_target extends BaseHullMod {
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getBeamWeaponRangeBonus().modifyPercent(id, -80f);
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, 120f);
        stats.getBallisticWeaponRangeBonus().modifyPercent(id, 120f);
    }
}
