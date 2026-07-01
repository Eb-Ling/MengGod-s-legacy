package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class MengMEC extends BaseHullMod {
    public static final String KEY = "MengMEClistener";

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10.0F;
        Color highlight = new Color(118, 255, 108, 218);

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "舰船产生的可计量幅能都将被部分收集并转化为质量，所有幅能增长量的 %s%% 将会转化为舰船结构值， %s%% 将会转化为舰船装甲值进行修复。",
                opad, highlight, "2", "0.1");
        label.setHighlight("2%", "0.1%");
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("圣殿史录", Alignment.MID, opad);

        label = tooltip.addPara(
                "神从不在乎物质的流失，因为一切失去的都终将报偿。",
                opad, highlight);

        tooltip.addSectionHeading("研究日志", Alignment.MID, opad);

        label = tooltip.addPara(
                "我们习得了神的手段，也就成为了他的代行者。\n\n不兼容于 延展时空 。",
                opad, highlight);

        label.setHighlight("延展时空");
        highlight = Misc.getNegativeHighlightColor();
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight);


    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        FluxTrackerAPI tracker = ship.getFluxTracker();
        if (!ship.getCustomData().containsKey(KEY)) {
            DataContainer data = new DataContainer();

            data.lastflux = tracker.getCurrFlux();
            ship.setCustomData(KEY, data);


        }
        DataContainer data = (DataContainer) ship.getCustomData().get(KEY);

        float lastflux = data.lastflux;
        if (ship.isAlive()) {

            float Armor = ship.getArmorGrid().getMaxArmorInCell();
            float maxHull = ship.getMaxHitpoints();
            float flux = tracker.getCurrFlux();
            float hullbonus = (flux - lastflux) * 0.02f;
            ship.setHitpoints(Math.min(Math.max((ship.getHitpoints() + hullbonus), ship.getHitpoints()), maxHull));
            float[][] grid1 = ship.getArmorGrid().getGrid();
            for (int x = 0; x < grid1.length; x++)
                for (int y = 0; y < grid1[0].length; y++)
                    ship.getArmorGrid().setArmorValue(x, y, Math.min(grid1[x][y] + hullbonus * 0.05f, Armor));
            data.lastflux = tracker.getCurrFlux();
            if (hullbonus > 0f && ship.getHitpoints() < ship.getMaxHitpoints()) {
                Global.getCombatEngine().addFloatingDamageText(new Vector2f(ship.getLocation().getX() - 200f, ship.getLocation().getY() + 200f), hullbonus, new Color(0, 255, 157, 255), ship, ship);
            }

        }
    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().getHullMods().contains("Meng_God_Spec")) {
            ship.getVariant().removeMod("Meng_God_Spec");
            Global.getSoundPlayer().playUISound("cr_allied_warning", 1.0F, 1.0F);
        }
    }

    public static class DataContainer {

        float lastflux;

    }
}