package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.plugins.MagicRenderPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class Meng_river extends BaseHullMod {
    public static final String KEY = "Mengtimeriverlistener_S";

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10.0F;
        Color highlight = new Color(118, 255, 108, 218);
        Color highlight1 = new Color(255, 0, 0, 255);
        Color highlight2 = Misc.getHighlightColor();
        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "形成难以逾越的时间屏障，前方400~1200su范围内与锁定的目标距离越接近 800 ，\n目标的最大航速与机动性越慢，最多降低至最高航速的 %s%% 、最大机动性的 %s%% 。\n当与目标距离小于400或大于1200时，目标移速不受影响。",
                opad, highlight, "40",
                "60");
        label.setHighlight("800", "40%", "60%");
        label.setHighlightColors(highlight2, highlight, highlight, highlight1, highlight1, highlight1, highlight1);
        tooltip.addSectionHeading("圣殿史录", Alignment.MID, opad);

        label = tooltip.addPara(
                "对于你们的维度而言，时间是最无法跨越的屏障。",
                opad, highlight);


    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        ShipAPI target = ship.getShipTarget();
        if (!ship.getCustomData().containsKey(KEY)) {
            ship.setCustomData(KEY, new Meng_river.DataContainer());
        }
        Meng_river.DataContainer data = (Meng_river.DataContainer) ship.getCustomData().get(KEY);
        if (target != null && target.getOwner() != ship.getOwner()) {
            if (!data.init) {
                data.init = true;
                data.timer1 = 0f;
            }
            data.timer1 += amount;
            SpriteAPI sprite = Global.getSettings().getSprite("Meng", "Meng_timeriver1");
            MagicRenderPlugin.addSingleframe(sprite, ship.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
            sprite.setAlphaMult(Math.min(0.2f * data.timer1, 0.2f));
            sprite.setAngle(data.timer1 * 30f);
            sprite.setSize(4300f, 4300f);

            Vector2f sl = ship.getLocation();
            Vector2f tl = target.getLocation();
            float d = Vector2f.sub(tl, sl, new Vector2f()).length();

            float speed1 = (d - 1000f) / 500f;
            float speed2 = (1000f - d) / 500f;
            if (d > 1000f && d <= 1500) {
                String id = "MengRiver";
                MutableShipStatsAPI stats = target.getMutableStats();
                stats.getMaxSpeed().modifyMult(id, 0.4f + 0.6f * speed1);
                stats.getMaxTurnRate().modifyMult(id, 0.6f + 0.4f * speed1);
                Global.getCombatEngine().maintainStatusForPlayerShip("Meng_river1", "graphics/fx/Meng_logo1.png", "时流之河：目标移速水平", Math.round((0.4f + 0.6f * speed1) * 100f) + "%", true);
            }
            if (d >= 500 && d <= 1000f) {
                String id = "MengRiver";
                MutableShipStatsAPI stats = target.getMutableStats();
                stats.getMaxSpeed().modifyMult(id, 0.4f + 0.6f * speed2);
                stats.getMaxTurnRate().modifyMult(id, 0.6f + 0.4f * speed2);
                Global.getCombatEngine().maintainStatusForPlayerShip("Meng_river2", "graphics/fx/Meng_logo1.png", "时流之河：目标移速水平", Math.round((0.4f + 0.6f * speed2) * 100f) + "%", true);
            }
            if (d > 1000f && d <= 1500f) {
                target.setJitterUnder(this, new Color(Math.round((d - 1000f) / 2f), 180 + Math.round((d - 1000f) / 10f), 255, 252), 1.0F, 25, 0.0F, Math.round((1500f - d) / 10f));
            }

            if (d >= 500f && d <= 1000f) {
                target.setJitterUnder(this, new Color(Math.round((1000f - d) / 2f), 180 + Math.round((1000f - d) / 10f), 255, 252), 1.0F, 25, 0.0F, Math.round((d - 500f) / 10f));
            }

        } else {
            data.init = false;
        }
    }

    public static class DataContainer {

        float timer1;
        boolean init = false;

    }

}

