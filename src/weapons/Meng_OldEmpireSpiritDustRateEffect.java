package data.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.methods.Meng_OldEmpireIds;

public class Meng_OldEmpireSpiritDustRateEffect implements EveryFrameWeaponEffectPlugin {
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon == null) return;
        ShipAPI ship = weapon.getShip();
        weapon.setRefireDelay(Meng_OldEmpireIds.has(ship, Meng_OldEmpireIds.B_RHYTHM) ? 1.5f : 3f);
    }
}
