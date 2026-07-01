package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.*;

public class Mengprotect extends BaseHullMod {
    public static final String KEY = "Mengprotectlistener";

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10.0F;
        Color highlight = new Color(118, 255, 108, 218);
        Color highlight1 = new Color(255, 0, 0, 255);
        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "在系统过载后，神的庇佑将会苏醒并加注于舰船。\n瞬间 结束过载 ，舰船结构值增加当前结构量的 20%% ，并允许超出量成为 最大结构上限 ，且舰船幅能降低 50%% 。\n\n该效果战斗中只能触发两次。",
                opad, highlight);
        label.setHighlight(
                "结束过载",
                "20%",
                "最大结构上限",
                "50%");
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight1);

        tooltip.addSectionHeading("圣殿史录", Alignment.MID, opad);

        label = tooltip.addPara(
                "神的目光注视着子民，神的威严便显现于寰宇。",
                opad, highlight);

    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        MutableShipStatsAPI stats = ship.getMutableStats();
        if (ship.isAlive()) {
            FluxTrackerAPI tracker = ship.getFluxTracker();
            float maxFlux = tracker.getMaxFlux();
            float nowFlux = ship.getCurrFlux();
            float nowHull = ship.getHitpoints();
            float MaxHull = ship.getMaxHitpoints();
            if (!ship.getCustomData().containsKey(KEY)) {
                Mengprotect.DataContainer data = new Mengprotect.DataContainer();
                ship.setCustomData(KEY, data);
            }
            Mengprotect.DataContainer data = (Mengprotect.DataContainer) ship.getCustomData().get(KEY);
            int i = data.timer;
            if (i <= 1) {
                if (nowFlux >= maxFlux) {
                    float hardflux = tracker.getHardFlux();
                    float currflux = tracker.getCurrFlux();
                    tracker.stopOverload();
                    ship.setHitpoints(ship.getHitpoints() + nowHull * 0.2f);

                    tracker.setCurrFlux(currflux * 0.5f);
                    tracker.setHardFlux(hardflux * 0.5f);


                    if (MaxHull <= ship.getHitpoints()) {
                        float Hull = ship.getHitpoints();
                        ship.setMaxHitpoints(Hull);
                        data.timer = i + 1;
                    }

                }
            }
        }
    }

    public static class DataContainer {

        int timer = 0;

    }


}