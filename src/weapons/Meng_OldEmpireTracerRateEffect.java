package data.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class Meng_OldEmpireTracerRateEffect implements EveryFrameWeaponEffectPlugin {
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon == null) return;
        ShipAPI ship = weapon.getShip();
        if (ship == null) return;
        weapon.setRefireDelay(Meng_OldEmpireProjectileOnFire.tracerCooldown(ship));
    }
}