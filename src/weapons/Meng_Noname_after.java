package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.methods.Meng_Noname_afterplugin;

import java.util.Objects;

public class Meng_Noname_after implements EveryFrameWeaponEffectPlugin {
    boolean isincombat = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!isincombat) {
            isincombat = true;
            if (Objects.equals(weapon.getSpec().getWeaponId(), "Meng_Noname_after_left")) {
                Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_Noname_afterplugin(weapon, weapon.getShip(), true));
            } else {
                Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_Noname_afterplugin(weapon, weapon.getShip(), false));
            }
        }
    }
}
