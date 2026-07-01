package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.boxutil.units.standard.entity.FlareEntity;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;
import org.magiclib.util.MagicUI;

import java.awt.*;

public class Mengtime_S extends BaseHullMod {
    public static final Object Mengtime_S = new Object();
    public static final String KEY = "Mengtimelistener";

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10.0F;
        Color highlight = new Color(118, 255, 108, 218);
        Color highlight1 = new Color(255, 0, 0, 255);
        Color highlight3 = Misc.getHighlightColor();
        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "在进入战斗时，每隔 15 秒，舰船的时间将会 逆转，\n消除负面状态，且结构将回复与 %s 秒前差值的 %s%% 。\n装甲将回复与 %s 秒前差值的 %s%% 。\n幅能水平将掉落与 %s 秒前差值的 %s%% 。\n\n不稳定的时间锚点也影响到了舰船的系统，舰船结构值、装甲值、幅能容量以及峰值时间减少 %S%% 。",
                opad, highlight, "15",
                "40",
                "15",
                "30",
                "15",
                "30",
                "10");
        label.setHighlight("15",
                "逆转",
                "15",
                "40%",
                "15",
                "30%",
                "15",
                "30%",
                "10%");
        label.setHighlightColors(highlight3, highlight, highlight3, highlight, highlight3, highlight, highlight3, highlight, highlight1);

        tooltip.addSectionHeading("圣殿史录", Alignment.MID, opad);

        label = tooltip.addPara(
                "神也不能赐予你笃定的未来，但他能为你定格过去。",
                opad, highlight);

        tooltip.addSectionHeading("研究日志", Alignment.MID, opad);

        label = tooltip.addPara(
                "我们打开了通向更高维度的大门。\n\n不兼容于 安全协议超驰 。",
                opad, highlight);
        label.setHighlight("安全协议超驰");
        highlight = Misc.getNegativeHighlightColor();
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight);


    }

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getFluxCapacity().modifyMult(id, 0.9f);
        stats.getHullBonus().modifyMult(id, 0.9f);
        stats.getArmorBonus().modifyMult(id, 0.9f);
        stats.getPeakCRDuration().modifyMult(id, 0.9f);
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        FluxTrackerAPI tracker = ship.getFluxTracker();
        CombatEngineAPI engine = Global.getCombatEngine();
        ArmorGridAPI armor = ship.getArmorGrid();
        MutableShipStatsAPI stats = ship.getMutableStats();

        if (!ship.getCustomData().containsKey(KEY)) {
            DataContainer data = new DataContainer();

            data.cooldown = false;
            data.timer = 900f;
            data.nowHull = ship.getMaxHitpoints();
            data.grid = armor.getGrid();
            data.Flux = tracker.getCurrFlux();
            data.HardFlux = tracker.getHardFlux();
            ship.setCustomData(KEY, data);


        }
        DataContainer data = (DataContainer) ship.getCustomData().get(KEY);
        boolean cooldown = data.cooldown;
        float timer = data.timer;
        float[][] grid = data.grid;
        float nowHull = data.nowHull;
        float Flux = data.Flux;
        float HardFlux = data.HardFlux;
        if (ship.isAlive()) {
            if (timer >= 1.0f) {
                data.timer -= Global.getCombatEngine().getTimeMult().getMult();
                data.cooldown = false;
            } else {
                data.cooldown = true;
            }


            float Armor = armor.getMaxArmorInCell();
            MagicUI.drawHUDStatusBar(ship, data.timer / 900f, new Color(34, 248, 241, 255), new Color(0, 165, 255, 255), 0f, "时间逆转倒计时", "            " + Math.round(data.timer / 60f) + "秒", false);
            if (timer <= 90f && timer >= 86f) {
                Global.getSoundPlayer().playSound("Meng_time_change", 1.0F, 1.0F, ship.getLocation(), new Vector2f());
            }
            if (timer <= 90f) {
                ship.setJitterUnder(this, new Color(255, 155, 255, 75), 1.0F, 25, 0.0F, 15.0F);

            }
            if (timer <= 90f) {
                String id = "Meng_times";
                stats.getCombatEngineRepairTimeMult().modifyMult(id, 0.01f);
                stats.getCombatWeaponRepairTimeMult().modifyMult(id, 0.01f);
            } else {
                String id = "Meng_times";
                stats.getCombatEngineRepairTimeMult().unmodify(id);
                stats.getCombatWeaponRepairTimeMult().unmodify(id);
            }
            if (cooldown) {
                tracker.stopOverload();
                float softflux = tracker.getCurrFlux() - tracker.getHardFlux();
                float hardflux = tracker.getHardFlux();
                float[][] grid1 = armor.getGrid();
                float Hull1 = ship.getHitpoints();

                for (int x = 0; x < grid1.length; x++)
                    for (int y = 0; y < grid1[0].length; y++) {
                        armor.setArmorValue(x, y, Math.min(grid1[x][y] + (grid[x][y] - grid1[x][y]) * 0.3f, Armor));
                        data.grid[x][y] = armor.getGrid()[x][y];
                    }
                if (Flux - HardFlux < softflux) {
                    tracker.increaseFlux(-(softflux - Flux + HardFlux) * 0.3f, false);
                }
                if (HardFlux < hardflux) {
                    tracker.increaseFlux(-(hardflux - HardFlux) * 0.3f, true);
                }
                data.Flux = tracker.getCurrFlux();
                data.HardFlux = tracker.getHardFlux();

                ship.setHitpoints(Math.max(Hull1 + (nowHull - Hull1) * 0.4f, Hull1));
                data.nowHull = ship.getHitpoints();
                data.timer = 900f;
            }
            if (ship == Global.getCombatEngine().getPlayerShip()) {
                if (timer != 0.0F)
                    Global.getCombatEngine().maintainStatusForPlayerShip(Mengtime_S, "graphics/fx/Meng_logo1.png", "重置时间锚定倒计时", Math.round(timer / 60.0F) + "秒", false);

                else {
                    Global.getCombatEngine().maintainStatusForPlayerShip(Mengtime_S, "graphics/fx/Meng_logo1.png", "重置时间锚定倒计时", "完成", true);
                }
            }
        }


    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().getHullMods().contains("safetyoverrides")) {
            ship.getVariant().removeMod("safetyoverrides");
            Global.getSoundPlayer().playUISound("cr_allied_warning", 1.0F, 1.0F);
        }
    }

    public static class DataContainer {

        float timer;
        boolean cooldown;
        float nowHull;
        float[][] grid;
        float Flux;
        float HardFlux;
    }


}	