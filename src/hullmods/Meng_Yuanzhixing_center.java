package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.scripts.util.MagicUI;

import java.awt.*;

public class Meng_Yuanzhixing_center extends BaseHullMod {
    public static final String KEY = "Meng_Yuanzhixing";
    public static final String id = "Meng_Yuanzhixing_id";

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 11.0F;
        Color highlight = new Color(118, 255, 108, 218);
        Color highlight1 = new Color(192, 255, 233, 218);
        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "当任意舰船武器开火时，每秒将会为舰船提供 1 点源能，当源能达到 10 点时，舰船将会获得短暂的高倍时流。",
                opad, highlight, "1",
                "10");
        label.setHighlight(
                "1",
                "10");
        label.setHighlightColors(highlight, highlight, highlight1, highlight, highlight1, highlight, highlight);

        tooltip.addSectionHeading("圣殿史录", Alignment.MID, opad);

        LabelAPI label1 = tooltip.addPara(
                "这是主的一瞥，是他一点伟大的神性之光。",
                opad, highlight1);

        label1.italicize();
        label1.setHighlight(0, 100);

    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.getCustomData().containsKey(KEY)) {
            DataContainer data = new DataContainer();
            data.energy = 0f;
            data.timer = 0f;
            ship.setCustomData(KEY, data);
        }
        DataContainer data = (DataContainer) ship.getCustomData().get(KEY);
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getChargeLevel() > 0.4f && !ship.getFluxTracker().isVenting() && !ship.getFluxTracker().isOverloaded()) {
                data.energy += amount;
            }
        }
        MagicUI.drawHUDStatusBar(ship, data.energy / 10f, new Color(34, 248, 241, 255), new Color(0, 165, 255, 255), 0f, "源能", "            " + Math.round(data.energy) + "点", false);
        if (data.energy >= 10f) {
            data.timer += amount;
            if (data.timer <= 4f) {
                data.energy = 10f;
                ship.getMutableStats().getTimeMult().modifyMult(id, 4f);
                if (ship == Global.getCombatEngine().getPlayerShip()) {
                    Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / 4f);
                }
                ship.addAfterimage(new Color(155, 218, 255, 50), 0, 0, -ship.getVelocity().getX(), -ship.getVelocity().getY(),
                        0, 0, 0.05f, 0.25f, true, true, true);
            } else {
                ship.getMutableStats().getTimeMult().unmodifyMult(id);
                if (ship == Global.getCombatEngine().getPlayerShip()) {
                    Global.getCombatEngine().getTimeMult().unmodifyMult(id);
                }
                data.energy = 0f;
                data.timer = 0f;
            }
        }
    }

    public static class DataContainer {
        float energy;
        float timer;
    }
}
