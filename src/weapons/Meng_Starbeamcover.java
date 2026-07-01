package data.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.awt.*;

public class Meng_Starbeamcover implements EveryFrameWeaponEffectPlugin {
    private float lastlight = 0f;
    private float timer = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        float light = weapon.getChargeLevel();
        if (light > lastlight) {
            weapon.getSprite().setColor(new Color(255, 255, 255, Math.round(Math.max(0, 255 - light * 255 * 2))));
        } else if (light < lastlight) {
            timer += amount;
            weapon.getSprite().setColor(new Color(255, 255, 255, Math.round(Math.min(255, timer / 2f * 255))));
        } else {
            timer = 0f;
        }
        lastlight = weapon.getChargeLevel();
    }
}
