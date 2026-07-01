package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.plugins.MagicRenderPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class Meng_protectshipweaponeffect implements EveryFrameWeaponEffectPlugin {
    private final CombatEngineLayers layer = CombatEngineLayers.ABOVE_SHIPS_LAYER;
    private float lastlevel = 0f;
    private float timer = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        float level = weapon.getChargeLevel();
        ShipAPI ship = weapon.getShip();
        weapon.setCurrHealth(weapon.getMaxHealth());
        if (level == 0f) {
            weapon.getSprite().setColor(new Color(255, 255, 255, 255));
            timer = 0f;
        } else {
            if (timer <= 1f) {
                weapon.getSprite().setColor(new Color(0, 0, 0, 0));
            } else {
                weapon.getSprite().setColor(new Color(255, 255, 255, 255));
            }
        }
        float length = 13f;
        if (weapon.getChargeLevel() >= lastlevel) {
            if (weapon.isFiring()) {
                SpriteAPI sprite1 = Global.getSettings().getSprite("Meng", "Meng_protectweaponleft");
                SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_protectweaponright");
                MagicRenderPlugin.addSingleframe(sprite1, new Vector2f(weapon.getLocation().getX() - length * level * (float) Math.cos(Math.toRadians(90f - ship.getFacing())), weapon.getLocation().getY() + length * level * (float) Math.sin(Math.toRadians(90f - ship.getFacing()))), layer);
                MagicRenderPlugin.addSingleframe(sprite2, new Vector2f(weapon.getLocation().getX() + length * level * (float) Math.cos(Math.toRadians(90f - ship.getFacing())), weapon.getLocation().getY() - length * level * (float) Math.sin(Math.toRadians(90f - ship.getFacing()))), layer);
                sprite1.setAngle(ship.getFacing() - 90f);
                sprite2.setAngle(ship.getFacing() - 90f);
                sprite1.setAlphaMult(ship.getAlphaMult());
                sprite2.setAlphaMult(ship.getAlphaMult());

            }
        } else if (weapon.getChargeLevel() < lastlevel) {
            timer += amount;
            if (timer <= 1f) {
                SpriteAPI sprite1 = Global.getSettings().getSprite("Meng", "Meng_protectweaponleft");
                SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_protectweaponright");

                MagicRenderPlugin.addSingleframe(sprite1, new Vector2f(weapon.getLocation().getX() - length * (1 - timer) * (float) Math.cos(Math.toRadians(90f - ship.getFacing())), weapon.getLocation().getY() + length * (1 - timer) * (float) Math.sin(Math.toRadians(90f - ship.getFacing()))), layer);
                MagicRenderPlugin.addSingleframe(sprite2, new Vector2f(weapon.getLocation().getX() + length * (1 - timer) * (float) Math.cos(Math.toRadians(90f - ship.getFacing())), weapon.getLocation().getY() - length * (1 - timer) * (float) Math.sin(Math.toRadians(90f - ship.getFacing()))), layer);
                sprite1.setAlphaMult(ship.getAlphaMult());
                sprite2.setAlphaMult(ship.getAlphaMult());
                sprite1.setAngle(ship.getFacing() - 90f);
                sprite2.setAngle(ship.getFacing() - 90f);
            }
        }
        lastlevel = weapon.getChargeLevel();
    }
}
