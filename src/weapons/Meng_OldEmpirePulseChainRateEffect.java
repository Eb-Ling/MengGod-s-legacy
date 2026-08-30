package data.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.methods.Meng_OldEmpireIds;

public class Meng_OldEmpirePulseChainRateEffect implements EveryFrameWeaponEffectPlugin {
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon == null) return;
        ShipAPI ship = weapon.getShip();
        if (ship == null) return;

        weapon.setRefireDelay(cooldown(ship));
    }

    private float cooldown(ShipAPI ship) {
        float cooldown;
        if (Meng_OldEmpireIds.has(ship, Meng_OldEmpireIds.A_SPLIT)) {
            cooldown = 1f;
        } else if (Meng_OldEmpireIds.has(ship, Meng_OldEmpireIds.A_HEAVY)) {
            cooldown = 0.7f;
        } else {
            cooldown = 0.25f;
        }
        if (Meng_OldEmpireIds.has(ship, Meng_OldEmpireIds.B_RHYTHM)) cooldown /= 1.5f;
        return cooldown;
    }
}
