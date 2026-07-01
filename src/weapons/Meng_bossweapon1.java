package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.plugins.MagicRenderPlugin;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

public class Meng_bossweapon1 implements EveryFrameWeaponEffectPlugin {
    public static final String KEY = "Mengbossweapon1_";
    private final IntervalUtil Interval = new IntervalUtil(0.17f, 0.23f);
    private float lastchargelevel = 0f;
    private boolean init = false;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        ShipAPI ship = weapon.getShip();
        float chargeLevel = weapon.getChargeLevel();
        SpriteAPI sprite0 = Global.getSettings().getSprite("Meng", "Meng_Zero_1");
        SpriteAPI sprite1 = Global.getSettings().getSprite("Meng", "Meng_Zero_2");
        SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_Zero_3");

        RippleDistortion ripple = new RippleDistortion(weapon.getLocation(), new Vector2f());
        ripple.setSize(chargeLevel * 500.0F * 2.0F);
        ripple.setIntensity(chargeLevel * 500.0F * 0.2F);
        ripple.fadeInSize(12f);
        ripple.fadeInIntensity(5f);
        ripple.setFrameRate(60f);

        ShipAPI source = weapon.getShip();


        if (chargeLevel < 1f && chargeLevel > this.lastchargelevel) {
            Interval.advance(amount);
            if (Interval.intervalElapsed()) {
                DistortionShader.addDistortion(ripple);
            }
            if (!init) {
                init = true;

                if (source.getHitpoints() >= source.getMaxHitpoints() * 0.4f) {
                    Global.getSoundPlayer().playSound("Meng_bossweapon", 0.9f, 1.0f, weapon.getShip().getLocation(), new Vector2f());
                    return;
                } else
                    Global.getSoundPlayer().playSound("Meng_bossweapon", 1.0f, 0.8f, weapon.getShip().getLocation(), new Vector2f());


            }
            MagicRenderPlugin.addSingleframe(sprite0, weapon.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
            MagicRenderPlugin.addSingleframe(sprite1, weapon.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
            MagicRenderPlugin.addSingleframe(sprite2, weapon.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
            sprite0.setSize(Math.max(200 - chargeLevel * 220, 0), Math.max(200 - chargeLevel * 220, 0));
            sprite0.setAngle(360 * chargeLevel * 3);
            sprite0.setAlphaMult(1.0f);
            sprite2.setSize(Math.max(250 - chargeLevel * 350, 0), Math.max(250 - chargeLevel * 350, 0));
            sprite2.setAngle(360 * chargeLevel * 3);
            sprite2.setAlphaMult(1.0f);
            sprite1.setSize(Math.max(200 - chargeLevel * 400, 0), Math.max(200 - chargeLevel * 400, 0));
            sprite1.setAngle(360 * chargeLevel * 3);
            sprite0.setAlphaMult(1.0f);
        } else {
            init = false;
        }

        if (weapon.isFiring()) {

            this.lastchargelevel = chargeLevel;

        }

    }
}



