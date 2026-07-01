package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class Meng_timecontrol extends BaseHullMod {
    public float timer = 0f;

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10.0F;
        Color highlight = new Color(255, 0, 0, 255);
        Color highlight1 = new Color(255, 232, 87, 255);
        LabelAPI label = tooltip.addPara(
                "利用时间锚点的技术对时流系统进行了修改，彻底改变幅能系统的同时实现了与幅能水平挂钩的被动时流，并部分占据护盾效率。",
                opad, highlight);

        label.setHighlightColors(highlight1, highlight1, highlight1, highlight1, highlight1);

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        LabelAPI label1 = tooltip.addPara(
                "#舰船所有实弹与能量武器射程降低 %s%% 。\n#满幅能情况下时间流速最多从 %s%% 增幅至 %s%% 。\n#舰船每参加战斗超过峰值时间的 %s%% ，将损失 %s%% 全部防御性能。",
                opad, highlight1,
                "40",
                "120",
                "170",
                "10",
                "2.5");
        label1.setHighlight(
                "40%",
                "120%",
                "170%",
                "10%",
                "2.5%");
        label1.setHighlightColors(highlight1, highlight1, highlight1, highlight1, highlight1);

        tooltip.addSectionHeading("圣殿史录", Alignment.MID, opad);

        label = tooltip.addPara(
                "当你超越了能量的界限，也就触摸到了时间的轴线。\n\n不兼容于 安全协议超驰 。",
                opad, highlight1);

        label.setHighlight("安全协议超驰");
        highlight = Misc.getNegativeHighlightColor();
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight);


    }


    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getBallisticWeaponRangeBonus().modifyPercent(id, -40f);
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, -40f);
    }


    public void advanceInCombat(ShipAPI ship, float amount) {

        FluxTrackerAPI tracker = ship.getFluxTracker();
        float flux = tracker.getCurrFlux() / tracker.getMaxFlux();
        MutableShipStatsAPI stats = ship.getMutableStats();
        String id = "Meng_timecontrols";
        if (flux > 0f) {
            ship.setJitterUnder(this, new Color(142 - Math.round(flux * 50), 255, 214, 218), 1.0F, 25, 0.0F, 0f + Math.round(flux * 20f));
        }
        timer += amount;

        float basebonus = ship.getHullSpec().getNoCRLossSeconds();
        float bonus = basebonus;
        if (ship.getMutableStats().getPeakCRDuration().getFlatBonus() != 0f || ship.getMutableStats().getPeakCRDuration().getPercentMod() != 0f || ship.getMutableStats().getPeakCRDuration().getMult() != 0f) {
            if (ship.getMutableStats().getPeakCRDuration().getFlatBonus() != 0f) {
                bonus += ship.getMutableStats().getPeakCRDuration().getFlatBonus();
            }
            if (ship.getMutableStats().getPeakCRDuration().getPercentMod() != 0f) {
                bonus += basebonus * ship.getMutableStats().getPeakCRDuration().getPercentMod();
            }
            if (ship.getMutableStats().getPeakCRDuration().getMult() != 0f) {
                bonus *= ship.getMutableStats().getPeakCRDuration().getMult();
            }
        } else {
            bonus = basebonus;
        }

        stats.getTimeMult().modifyMult(id, 1.2f + flux * 0.5f);
        float mult = Math.round(Math.floor(timer / bonus * 10));
        stats.getShieldDamageTakenMult().modifyMult(id, 1f + mult * 0.025f);
        stats.getArmorDamageTakenMult().modifyMult(id, 1f + mult * 0.025f);
        stats.getHullDamageTakenMult().modifyMult(id, 1f + mult * 0.025f);
        if (ship == Global.getCombatEngine().getPlayerShip()) {
            if (ship.isAlive())
                Global.getCombatEngine().maintainStatusForPlayerShip("Meng_timecontrol1", "graphics/fx/Meng_logo1.png", "线性时流倍率", Math.round((1.2f + flux * 0.5f) * 100f) + "%", false);
            Global.getCombatEngine().maintainStatusForPlayerShip("Meng_timecontrol2", "graphics/fx/Meng_logo1.png", "护盾承受伤害比率", Math.round((1f + mult * 0.025f) * 100f) + "%", true);
            Global.getCombatEngine().maintainStatusForPlayerShip("Meng_timecontrol4", "graphics/fx/Meng_logo1.png", "船体受损增加", Math.round((1f + mult * 0.025f) * 100f) + "%", true);
        }
    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().getHullMods().contains("safetyoverrides")) {
            ship.getVariant().removeMod("safetyoverrides");
            Global.getSoundPlayer().playUISound("cr_allied_warning", 1.0F, 1.0F);
        }
    }


}