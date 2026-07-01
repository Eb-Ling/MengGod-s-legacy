package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.plugins.MagicRenderPlugin;

public class Meng_DragonRbiucover implements EveryFrameWeaponEffectPlugin {
    private float lastlight = 0f;
    private float timer = 0f;
    private boolean fire = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        float light = weapon.getChargeLevel();
        if (light > lastlight) {
            int step = Math.round(light / 0.1f);
            fire = true;
            if (step >= 8) step = 8;
            SpriteAPI sprite = Global.getSettings().getSprite("Meng", "Meng_DragonRcover0" + step);
            MagicRenderPlugin.addSingleframe(sprite, weapon.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
            sprite.setAngle(weapon.getShip().getFacing() - 90f);
        } else if (light < lastlight) {
            timer += amount;
            fire = false;
            SpriteAPI sprite = Global.getSettings().getSprite("Meng", "Meng_DragonRcover08");
            MagicRenderPlugin.addSingleframe(sprite, weapon.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
            sprite.setAlphaMult(Math.max(0f, 1f - timer));
            sprite.setAngle(weapon.getShip().getFacing() - 90f);
        } else {
            if (fire) {
                SpriteAPI sprite = Global.getSettings().getSprite("Meng", "Meng_DragonRcover08");
                MagicRenderPlugin.addSingleframe(sprite, weapon.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                sprite.setAngle(weapon.getShip().getFacing() - 90f);
            }
            timer = 0f;
        }
        lastlight = weapon.getChargeLevel();
    }
}
