package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.plugins.MagicRenderPlugin;

public class Meng_jianxingsprite extends BaseHullMod {
    public static final String KEY = "Mengjianxinglistener";
    private final IntervalUtil Interval = new IntervalUtil(0.25f, 0.25f);

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.getCustomData().containsKey(KEY)) {
            ship.setCustomData(KEY, new Meng_jianxingsprite.DataContainer());
        }
        Meng_jianxingsprite.DataContainer data = (Meng_jianxingsprite.DataContainer) ship.getCustomData().get(KEY);
        if (data.x == 7) {
            data.x = 1;
        }
        data.timer += amount;
        SpriteAPI sprite = Global.getSettings().getSprite("Meng", "Meng_jianxing" + data.x);
        MagicRenderPlugin.addSingleframe(sprite, ship.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
        sprite.setAngle(ship.getFacing() - 90f);
        Interval.advance(amount);
        if (Interval.intervalElapsed()) {
            data.x++;
        }
    }

    private static class DataContainer {

        float flux;
        float timer = 0f;
        int x = 1;
    }
}
